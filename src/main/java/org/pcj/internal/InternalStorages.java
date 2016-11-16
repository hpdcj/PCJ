/* 
 * Copyright (c) 2011-2016, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal;

import org.pcj.PcjRuntimeException;
import org.pcj.Storage;

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
import java.util.stream.Collectors;

/**
 * External class with methods do handle shared variables.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class InternalStorages {

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

    private final transient ConcurrentMap<String, String> enumToStorageMap;
    private final transient ConcurrentMap<String, Object> storageObjectsMap;
    private final transient ConcurrentMap<String, ConcurrentMap<String, StorageField>> sharedObjectsMap;

    public InternalStorages() {
        enumToStorageMap = new ConcurrentHashMap<>();
        storageObjectsMap = new ConcurrentHashMap<>();
        sharedObjectsMap = new ConcurrentHashMap<>();
    }

    public Object registerStorage(Class<? extends Enum<?>> storageClass) {
        try {
            return registerStorage0(storageClass);
        } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException |
                IllegalArgumentException | InvocationTargetException | InstantiationException ex) {
            throw new IllegalArgumentException("Provided class is not Storage class.", ex);
        }
    }

    private Object registerStorage0(Class<? extends Enum<?>> sharedEnumClass) throws NoSuchFieldException, InstantiationException, IllegalAccessException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException {
        if (sharedEnumClass.isEnum() == false) {
            throw new IllegalArgumentException("Class is not enum: " + sharedEnumClass.getName());
        }
        if (sharedEnumClass.isAnnotationPresent(Storage.class) == false) {
            throw new IllegalArgumentException("Enum is not annotated by @Storage annotation: " + sharedEnumClass.getName());
        }
        if (enumToStorageMap.containsKey(sharedEnumClass.getName())) {
            throw new IllegalArgumentException("Enum is already registered: " + sharedEnumClass.getName());
        }

        Storage annotation = sharedEnumClass.getAnnotation(Storage.class);
        Class<?> storageClass = annotation.value();

        Set<String> fieldNames = Arrays.stream(storageClass.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toCollection(HashSet::new));

        Optional<String> notFoundName = Arrays.stream(sharedEnumClass.getEnumConstants())
                .map(Enum::name)
                .filter(enumName -> fieldNames.contains(enumName) == false)
                .findFirst();
        if (notFoundName.isPresent()) {
            throw new NoSuchFieldException("Field not found in " + storageClass.getName() + ": " + notFoundName.get());
        }

        Object storageObject = storageObjectsMap.get(storageClass.getName());
        if (storageObject == null) {
            Constructor<?> storageConstructor = storageClass.getConstructor();
            storageConstructor.setAccessible(true);
            storageObject = storageConstructor.newInstance();
        }

        String parent = storageClass.getName();
        for (Enum<?> enumConstant : sharedEnumClass.getEnumConstants()) {
            String name = enumConstant.name();
            Field field = storageClass.getDeclaredField(name);

            createShared0(parent, name, field, storageObject);
        }

        storageObjectsMap.put(parent, storageObject);
        enumToStorageMap.put(sharedEnumClass.getName(), parent);

        return storageObject;
    }

    private void createShared0(String parent, String name, Field field, Object storageObject)
            throws NullPointerException, IllegalArgumentException, IllegalStateException {
        Class<?> type = field.getType();

        if (type.isPrimitive() == false && Serializable.class.isAssignableFrom(type) == false) {
            throw new IllegalArgumentException("Variable type is not serializable");
        }

        ConcurrentMap<String, StorageField> storage
                = sharedObjectsMap.computeIfAbsent(parent, key -> new ConcurrentHashMap<>());
        StorageField storageField = new StorageField(field, storageObject);

        storage.putIfAbsent(name, storageField);
    }

    public Object getStorage(Class<? extends Enum<?>> sharedEnumClass) {
        String sharedEnumClassName = sharedEnumClass.getName();
        if (enumToStorageMap.containsKey(sharedEnumClassName) == false) {
            throw new IllegalArgumentException("Enum is not registered: " + sharedEnumClassName);
        }
        String storageName = enumToStorageMap.get(sharedEnumClassName);
        return storageObjectsMap.get(storageName);
    }

    private String getParent(Enum<?> variable) throws NullPointerException, IllegalArgumentException {
        if (variable == null) {
            throw new NullPointerException("Variable name cannot be null");
        }
        return getParent(variable.getDeclaringClass().getName());
    }

    private String getParent(String sharedEnumClassName) throws NullPointerException, IllegalArgumentException {
        if (enumToStorageMap.containsKey(sharedEnumClassName) == false) {
            throw new IllegalArgumentException("Enum is not registered: " + sharedEnumClassName);
        }
        return enumToStorageMap.get(sharedEnumClassName);
    }

    /**
     * Returns variable from Storages
     *
     * @param variable name of shared variable
     * @param indices  (optional) indices into the array
     * @return value of variable[indices] or variable if indices omitted
     * @throws ClassCastException             there is more indices than variable dimension
     * @throws ArrayIndexOutOfBoundsException one of indices is out of bound
     */
    final public <T> T get(Enum<?> variable, int... indices) throws ArrayIndexOutOfBoundsException, ClassCastException {
        return get0(getParent(variable), variable.name(), indices);
    }

    final public <T> T get(String sharedEnumClassName, String name, int... indices) throws ArrayIndexOutOfBoundsException, ClassCastException {
        return get0(getParent(sharedEnumClassName), name, indices);
    }

    @SuppressWarnings("unchecked")
    private <T> T get0(String parent, String name, int... indices) throws ArrayIndexOutOfBoundsException, ClassCastException {
        ConcurrentMap<String, StorageField> storage = sharedObjectsMap.get(parent);

        StorageField storageField = storage.get(name);
        if (storageField == null) {
            throw new IllegalArgumentException("Variable not found: " + name);
        }
        Object value = storageField.getValue();

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
     * Puts new value of variable to InternalStorages into the array, or as variable
     * value if indices omitted
     *
     * @param variable name of shared variable
     * @param value    new value of variable
     * @param indices  (optional) indices into the array
     * @throws ClassCastException             there is more indices than variable dimension
     *                                        or value cannot be assigned to the variable
     * @throws ArrayIndexOutOfBoundsException one of indices is out of bound
     */
    final public <T> void put(Enum<?> variable, T value, int... indices) throws ArrayIndexOutOfBoundsException, ClassCastException, NullPointerException {
        put0(getParent(variable), variable.name(), value, indices);
    }

    final public <T> void put(String sharedEnumClassName, String name, T value, int... indices) throws ArrayIndexOutOfBoundsException, ClassCastException, NullPointerException {
        put0(getParent(sharedEnumClassName), name, value, indices);
    }

    private <T> void put0(String parent, String name, T value, int... indices) throws ArrayIndexOutOfBoundsException, ClassCastException, NullPointerException {
        ConcurrentMap<String, StorageField> storage = sharedObjectsMap.get(parent);

        StorageField field = storage.get(name);
        if (field == null) {
            throw new IllegalArgumentException("Variable not found: " + name);
        }

        Class<?> variableClass = field.getType();
        Class<?> targetClass;
        if (indices.length > 0) {
            if (field.getValue() == null) {
                targetClass = getTargetClass(variableClass, indices);
            } else {
                targetClass = getTargetClass(field.getValue().getClass(), indices);
            }
        } else {
            targetClass = variableClass;
        }

        Class<?> fromClass = getValueClass(value);

        if (isAssignableFrom(targetClass, fromClass) == false) {
            throw new ClassCastException("Cannot cast " + fromClass.getName()
                    + " to the type of variable '" + parent + "." + name + ""
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
            synchronized (field) {
                field.setValue(newValue);
                field.getModificationCount().incrementAndGet();
                field.notifyAll();
            }
        } else {
            Object array = getArrayElement(field.getValue(), indices, indices.length - 1);

            if (array == null) {
                throw new NullPointerException("Cannot put value to: " + name);
            } else if (array.getClass().isArray() == false) {
                throw new ClassCastException("Cannot put value to " + name + Arrays.toString(indices));
            } else if (Array.getLength(array) <= indices[indices.length - 1]) {
                throw new ArrayIndexOutOfBoundsException("Cannot put value to " + name + Arrays.toString(indices));
            }

            synchronized (field) {
                Array.set(array, indices[indices.length - 1], newValue);
                field.getModificationCount().incrementAndGet();
                field.notifyAll();
            }
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
     * Checks if value can be assigned to variable stored in InternalStorages
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
        return monitor0(getParent(variable), variable.name());
    }

    private int monitor0(String parent, String name) {
        ConcurrentMap<String, StorageField> storage = sharedObjectsMap.get(parent);

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
     */
    final public int waitFor(Enum<?> variable, int count) {
        return waitFor0(getParent(variable), variable.name(), count);
    }

    private int waitFor0(String parent, String name, int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Value count is less than zero:" + count);
        }
        ConcurrentMap<String, StorageField> storage = sharedObjectsMap.get(parent);

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
        return waitFor0(getParent(variable), variable.name(), count, timeout, unit);
    }

    private int waitFor0(String parent, String name, int count, long timeout, TimeUnit unit) throws TimeoutException {
        if (count < 0) {
            throw new IllegalArgumentException("Value count is less than zero:" + count);
        }

        ConcurrentMap<String, StorageField> storage = sharedObjectsMap.get(parent);

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
