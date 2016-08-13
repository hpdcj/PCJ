/* 
 * Copyright (c) 2011-2016, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.pcj.PcjRuntimeException;
import org.pcj.Storage;

/**
 * External class with methods do handle shared variables.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class InternalStorage {

    private class StorageField {

        private final Field field;
        private final AtomicInteger modificationCount;
        private final Object storageObject;

        public StorageField(Field field, Object storageObject) {
            this.field = field;
            this.modificationCount = new AtomicInteger(0);
            this.storageObject = storageObject;

            field.setAccessible(true);
        }

        public Class<?> getType() {
            return field.getType();
        }

        public AtomicInteger getModificationCount() {
            return modificationCount;
        }

        public Object getValue() {
            try {
                return field.get(storageObject);
            } catch (IllegalAccessException ex) {
                throw new RuntimeException("Cannot get value from storage", ex);
            }
        }

        public void setValue(Object value) {
            try {
                field.set(storageObject, value);
            } catch (IllegalAccessException ex) {
                throw new RuntimeException("Cannot set value to storage", ex);
            }
        }

    }

    private final transient ConcurrentMap<String, ConcurrentMap<String, StorageField>> storageMap;

    public InternalStorage() {
        storageMap = new ConcurrentHashMap<>();
    }

    public <T> T registerStorage(Class<? extends T> storageClass) throws InstantiationException {
        try {
            return registerStorage0(storageClass);
        } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException |
                IllegalArgumentException | InvocationTargetException ex) {
            throw new IllegalArgumentException("Provided class is not good Storage class.");
        }
    }

    private <T> T registerStorage0(Class<? extends T> storageClass) throws NoSuchFieldException, InstantiationException, IllegalAccessException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException {
        if (storageClass.isAnnotationPresent(Storage.class) == false) {
            throw new IllegalArgumentException("Class " + storageClass.getName() + " is not annotated by @Storage annotation.");
        }

        Storage annotation = storageClass.getAnnotation(Storage.class);
        Class<? extends Enum<?>> sharedEnum = annotation.value();
        if (sharedEnum.isEnum() == false) {
            throw new IllegalArgumentException("Annotation value is not Enum: " + sharedEnum.getTypeName());
        }

        Set<String> fieldNames = Arrays.stream(storageClass.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toCollection(HashSet::new));

        Optional<String> notFoundName = Arrays.stream(sharedEnum.getEnumConstants())
                .map(Enum::name)
                .filter(enumName -> fieldNames.contains(enumName) == false)
                .findFirst();
        if (notFoundName.isPresent()) {
            throw new NoSuchFieldException("Field '" + notFoundName.get() + "' not found in " + storageClass.getName());
        }

        Constructor<? extends T> storageConstructor = storageClass.getConstructor();
        storageConstructor.setAccessible(true);
        T storageObject = storageConstructor.newInstance();

        for (Enum<?> enumConstant : sharedEnum.getEnumConstants()) {
            String parent = enumConstant.getDeclaringClass().getName();
            String name = enumConstant.name();
            Field field = storageClass.getDeclaredField(name);

            createShared0(parent, name, field, storageObject);
        }

        return storageObject;
    }

    private void createShared0(String storageName, String name, Field field, Object storageObject)
            throws NullPointerException, IllegalArgumentException, IllegalStateException {
        Class<?> type = field.getType();

        if (type.isPrimitive() == false && Serializable.class.isAssignableFrom(type) == false) {
            throw new IllegalArgumentException("Variable type is not serializable");
        }

        ConcurrentMap<String, StorageField> storage = getStorage(storageName);
        StorageField storageField = new StorageField(field, storageObject);

        if (storage.putIfAbsent(name, storageField) != null) {
            throw new IllegalStateException("Variable has already been created: " + name);
        }
    }

    private ConcurrentMap<String, StorageField> getStorage(String storageName) {
        ConcurrentMap<String, StorageField> storage = storageMap.computeIfAbsent(storageName, key -> new ConcurrentHashMap<>());
        return storage;
    }

    /**
     * Returns variable from Storages
     *
     * @param name    name of shared variable
     * @param indices (optional) indices into the array
     *
     * @return value of variable[indices] or variable if indices omitted
     *
     * @throws ClassCastException             there is more indices than variable dimension
     * @throws ArrayIndexOutOfBoundsException one of indices is out of bound
     */
    final public <T> T get(Enum<?> variable, int... indices) throws ArrayIndexOutOfBoundsException, ClassCastException {
        if (variable == null) {
            throw new NullPointerException("Variable name cannot be null");
        }

        return get0(variable.getDeclaringClass().getName(), variable.name());
    }

    @SuppressWarnings("unchecked")
    final public <T> T get0(String storageName, String name, int... indices) throws ArrayIndexOutOfBoundsException, ClassCastException {
        ConcurrentMap<String, StorageField> storage = getStorage(storageName);

        StorageField field = storage.get(name);
        if (field == null) {
            throw new IllegalArgumentException("Variable not found: " + name);
        }
        Object value = field.getValue();

        if (indices.length == 0) {
            return (T) value;
        } else {
            Object array = getArrayElement(value, indices, indices.length - 1);
            if (array.getClass().isArray() == false) {
                throw new ClassCastException("Cannot put value to " + name + Arrays.toString(indices) + ".");
            } else if (Array.getLength(array) <= indices[indices.length - 1]) {
                throw new ArrayIndexOutOfBoundsException("Cannot put value to " + name + Arrays.toString(indices) + ".");
            }

            return (T) Array.get(array, indices[indices.length - 1]);
        }
    }

    private Object getArrayElement(Object array, int[] indices, int length) throws ArrayIndexOutOfBoundsException, IllegalArgumentException, ClassCastException {
        for (int index = 0; index < length; ++index) {
            if (array.getClass().isArray() == false) {
                throw new ClassCastException("Wrong dimension at point " + index + ".");
            } else if (Array.getLength(array) <= indices[index]) {
                throw new ArrayIndexOutOfBoundsException("Wrong size at point " + index + ".");
            }
            array = Array.get(array, indices[index]);
        }

        return array;
    }

    /**
     * Puts new value of variable to InternalStorage into the array, or as variable
     * value if indices omitted
     *
     * @param name     name of shared variable
     * @param newValue new value of variable
     * @param indices  (optional) indices into the array
     *
     * @throws ClassCastException             there is more indices than variable dimension
     *                                        or value cannot be assigned to the variable
     * @throws ArrayIndexOutOfBoundsException one of indices is out of bound
     */
    final public <T> void put(Enum<?> variable, T value, int... indices) throws ArrayIndexOutOfBoundsException, ClassCastException, NullPointerException {
        if (variable == null) {
            throw new NullPointerException("Variable name cannot be null");
        }

        put0(variable.getDeclaringClass().getName(), variable.name(), value, indices);
    }

    final public <T> void put0(String storageName, String name, T value, int... indices) throws ArrayIndexOutOfBoundsException, ClassCastException, NullPointerException {
        ConcurrentMap<String, StorageField> storage = getStorage(storageName);

        StorageField field = storage.get(name);
        if (field == null) {
            throw new IllegalArgumentException("Variable not found: " + name);
        }

        Class<?> variableClass = field.getType();
//        Class<?> targetClass = getTargetClass(variableClass, indices);
        Class<?> targetClass;
        if (field.getValue() == null) {
            targetClass = getTargetClass(variableClass, indices);
        } else {
            targetClass = getTargetClass(field.getValue().getClass(), indices);
        }
        Class<?> fromClass = getValueClass(value);

        if (isAssignableFrom(targetClass, fromClass) == false) {
            throw new ClassCastException("Cannot cast " + fromClass.getName()
                    + " to the type of variable '" + storageName + "." + name + ""
                    + (indices.length == 0 ? "" : Arrays.toString(indices))
                    + "': " + targetClass);
        }

        Object newValue = value;
        if (targetClass.isPrimitive()) {
            newValue = PrimitiveTypes.convert(targetClass, value);
        } else if (PrimitiveTypes.isBoxedClass(targetClass) && value != null) {
            newValue = PrimitiveTypes.convert(targetClass, value);
        }

        if (indices.length == 0) {
            field.setValue(newValue);
        } else {
            Object array = getArrayElement(field.getValue(), indices, indices.length - 1);

            if (array == null) {
                throw new NullPointerException("Cannot put value to: " + name);
            } else if (array.getClass().isArray() == false) {
                throw new ClassCastException("Cannot put value to " + name + Arrays.toString(indices));
            } else if (Array.getLength(array) <= indices[indices.length - 1]) {
                throw new ArrayIndexOutOfBoundsException("Cannot put value to " + name + Arrays.toString(indices));
            }

            Array.set(array, indices[indices.length - 1], newValue);
        }

        field.getModificationCount().getAndIncrement();
        synchronized (field) {
            field.notifyAll();
        }
    }

    private <T> Class<?> getValueClass(T value) {
        if (value == null) {
            return null;
        } else {
            return value.getClass();
        }
    }

    private Class<?> getTargetClass(Class<?> variableClass, int... indices) {
        for (int i = 0; i < indices.length; ++i) {
            if (variableClass.isArray() == false) {
                return null;
            }
            variableClass = variableClass.getComponentType();
        }
        return variableClass;
    }

    /**
     * Checks if value can be assigned to variable stored in InternalStorage
     *
     * @return true if the value can be assigned to the variable
     */
    private boolean isAssignableFrom(Class<?> targetClass, Class<?> fromClass) {
        if (targetClass == null) {
            return false;
        }

        if (fromClass == null) {
            return !targetClass.isPrimitive();
        }

        if (targetClass.isAssignableFrom(fromClass)) {
            return true;
        }

        if (targetClass.isPrimitive()) {
            /* cannot cast boolean to other primitive */
            if (targetClass.equals(boolean.class)) {
                return fromClass.equals(Boolean.class);
            } else {
                return fromClass.equals(Boolean.class) == false
                        && PrimitiveTypes.isBoxedClass(fromClass);
            }
        }

        if (PrimitiveTypes.isBoxedClass(targetClass) && PrimitiveTypes.isBoxedClass(fromClass)) {
            return targetClass.equals(Boolean.class) == false
                    && fromClass.equals(Boolean.class) == false;
        }

        return false;
    }

    /**
     * Tells to monitor variable. Set the variable modification counter to zero.
     *
     * @param variable name of shared variable
     */
    final public int monitor(Enum<?> variable) {
        if (variable == null) {
            throw new NullPointerException("Variable name cannot be null");
        }

        return monitor0(variable.getDeclaringClass().getName(), variable.name());
    }

    final public int monitor0(String storageName, String name) {
        ConcurrentMap<String, StorageField> storage = getStorage(storageName);

        StorageField field = storage.get(name);
        if (field == null) {
            throw new IllegalArgumentException("Variable not found: " + name);
        }

        return field.getModificationCount().getAndSet(0);
    }

    /**
     * Pauses current Thread and wait for <code>count</code> modifications of
     * variable. After modification decreases the variable modification counter by
     * <code>count</code>.
     *
     * @param variable name of shared variable
     * @param count    number of modifications. If 0 - the method exits immediately.
     *
     *
     */
    final public int waitFor(Enum<?> variable, int count) {
        if (variable == null) {
            throw new NullPointerException("Variable name cannot be null");
        }

        return waitFor0(variable.getDeclaringClass().getName(), variable.name(), count);
    }

    final public int waitFor0(String storageName, String name, int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Value count is less than zero:" + count);
        }
        ConcurrentMap<String, StorageField> storage = getStorage(storageName);

        StorageField field = storage.get(name);
        if (field == null) {
            throw new IllegalArgumentException("Variable not found: " + name);
        }

        AtomicInteger modificationCount = field.getModificationCount();
        if (count == 0) {
            return modificationCount.get();
        }

        int v;
        do {
            synchronized (field) {
                while ((v = modificationCount.get()) < count) {
                    try {
                        field.wait();
                    } catch (InterruptedException ex) {
                        throw new PcjRuntimeException(ex);
                    }
                }
            }
        } while (modificationCount.compareAndSet(v, v - count) == false);

        return modificationCount.get();
    }

    /**
     * Pauses current Thread and wait for <code>count</code> modifications of
     * variable. After modification decreases the variable modification counter by
     * <code>count</code>.
     *
     * @param variable name of shared variable
     * @param count    number of modifications. If 0 - the method exits immediately.
     */
    final public int waitFor(Enum<?> variable, int count, long timeout, TimeUnit unit) throws TimeoutException {
        if (variable == null) {
            throw new NullPointerException("Variable name cannot be null");
        }

        return waitFor0(variable.getDeclaringClass().getName(), variable.name(), count, timeout, unit);
    }

    final public int waitFor0(String storageName, String name, int count, long timeout, TimeUnit unit) throws TimeoutException {
        if (count < 0) {
            throw new IllegalArgumentException("Value count is less than zero:" + count);
        }

        ConcurrentMap<String, StorageField> storage = getStorage(storageName);

        StorageField field = storage.get(name);
        if (field == null) {
            throw new IllegalArgumentException("Variable not found: " + name);
        }

        AtomicInteger modificationCount = field.getModificationCount();
        if (count == 0) {
            return modificationCount.get();
        }
        long nanosTimeout = unit.toNanos(timeout);
        final long deadline = System.nanoTime() + nanosTimeout;

        int v;
        do {

            synchronized (field) {
                while ((v = modificationCount.get()) < count) {
                    if (nanosTimeout <= 0L) {
                        throw new TimeoutException();
                    }
                    try {
                        field.wait(nanosTimeout / 1_000_000, (int) (nanosTimeout % 1_000_000));
                    } catch (InterruptedException ex) {
                        throw new PcjRuntimeException(ex);
                    }
                    nanosTimeout = deadline - System.nanoTime();
                }
            }
        } while (modificationCount.compareAndSet(v, v - count) == false);

        return modificationCount.get();
    }
}
