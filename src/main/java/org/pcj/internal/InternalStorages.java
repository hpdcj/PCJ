/*
 * Copyright (c) 2011-2019, PCJ Library, Marek Nowicki
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
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.pcj.PcjRuntimeException;
import org.pcj.ReduceOperation;
import org.pcj.Storage;

/**
 * External class with methods do handle shared variables.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class InternalStorages {

    private static class StorageField {

        private final Field field;
        private final Object storageObject;
        private final Semaphore modificationCounter;

        StorageField(Field field, Object storageObject) {
            this.field = field;
            this.storageObject = storageObject;

            field.setAccessible(true);

            this.modificationCounter = new Semaphore(0);
        }

        Class<?> getType() {
            return field.getType();
        }

        Object getValue() {
            try {
                return field.get(storageObject);
            } catch (IllegalAccessException ex) {
                throw new RuntimeException("Cannot get value from storage", ex);
            }
        }

        void setValue(Object value) {
            try {
                field.set(storageObject, value);
            } catch (IllegalAccessException ex) {
                throw new RuntimeException("Cannot set value to storage", ex);
            }
        }

        void incrementModificationCounter() {
            modificationCounter.release();
        }

        int resetModificationCounter() {
            return modificationCounter.drainPermits();
        }

        void decrementModificationCounter(int count) throws InterruptedException {
            modificationCounter.acquire(count);
        }

        void decrementModificationCounter(int count, long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
            if (!modificationCounter.tryAcquire(count, timeout, unit)) {
                throw new TimeoutException();
            }
        }

        int getModificationCounter() {
            return modificationCounter.availablePermits();
        }
    }

    private final transient ConcurrentMap<String, String> enumToStorageMap;
    private final transient ConcurrentMap<String, Object> storageObjectsMap;
    private final transient ConcurrentMap<String, ConcurrentMap<String, StorageField>> sharedObjectsMap;

    InternalStorages() {
        enumToStorageMap = new ConcurrentHashMap<>();
        storageObjectsMap = new ConcurrentHashMap<>();
        sharedObjectsMap = new ConcurrentHashMap<>();
    }

    public Object registerStorage(Class<? extends Enum<?>> storageClass) {
        return registerStorage(storageClass, null);
    }

    public Object registerStorage(Class<? extends Enum<?>> storageEnumClass, Object storageObject) {
        try {
            return registerStorage0(storageEnumClass, storageObject);
        } catch (NoSuchFieldException | IllegalArgumentException | ClassCastException ex) {
            throw new PcjRuntimeException("Exception while registering enum class: " + storageEnumClass, ex);
        }
    }

    private Object registerStorage0(Class<? extends Enum<?>> storageEnumClass, Object storageObject) throws NoSuchFieldException, IllegalArgumentException {
        if (!storageEnumClass.isEnum()) {
            throw new IllegalArgumentException("Class is not enum: " + storageEnumClass.getName());
        }
        if (!storageEnumClass.isAnnotationPresent(Storage.class)) {
            throw new IllegalArgumentException("Enum is not annotated by @Storage annotation: " + storageEnumClass.getName());
        }

        Storage annotation = storageEnumClass.getAnnotation(Storage.class);
        Class<?> storageClass = annotation.value();

        if (storageObject != null && !storageClass.isAssignableFrom(storageObject.getClass())) {
            throw new ClassCastException(storageObject.getClass() + " cannot be cast to " + storageClass);
        }

        String storageClassName = storageClass.getName();

        Set<String> fieldNames = Arrays.stream(storageClass.getDeclaredFields())
                                         .map(Field::getName)
                                         .collect(Collectors.toCollection(HashSet::new));

        Optional<String> notFoundName = Arrays.stream(storageEnumClass.getEnumConstants())
                                                .map(Enum::name)
                                                .filter(enumName -> !fieldNames.contains(enumName))
                                                .findFirst();
        if (notFoundName.isPresent()) {
            throw new NoSuchFieldException("Field not found in " + storageClassName + ": " + notFoundName.get());
        }

        Object storage = storageObjectsMap.computeIfAbsent(storageClassName, key -> {
            if (storageObject == null) {
                try {
                    Constructor<?> storageConstructor = storageClass.getConstructor();
                    storageConstructor.setAccessible(true);
                    return storageConstructor.newInstance();
                } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    throw new PcjRuntimeException(e);
                }
            } else {
                return storageObject;
            }
        });

        if (enumToStorageMap.putIfAbsent(storageEnumClass.getName(), storageClassName) != null) {
            return storage;
        }

        for (Enum<?> enumConstant : storageEnumClass.getEnumConstants()) {
            String name = enumConstant.name();
            Field field = storageClass.getDeclaredField(name);

            createShared0(storageClassName, name, field, storage);
        }

        return storage;
    }

    private void createShared0(String parent, String name, Field field, Object storageObject)
            throws NullPointerException, IllegalArgumentException, IllegalStateException {
        Class<?> type = field.getType();

        if (!type.isPrimitive() && !Serializable.class.isAssignableFrom(type) && Modifier.isFinal(type.getModifiers())) {
            throw new IllegalArgumentException("Type of '" + name + "' (" + type.getCanonicalName() + ") from class '" + parent + "' is not serializable but final");
        }

        ConcurrentMap<String, StorageField> storage
                = sharedObjectsMap.computeIfAbsent(parent, key -> new ConcurrentHashMap<>());
        StorageField storageField = new StorageField(field, storageObject);

        storage.putIfAbsent(name, storageField);
    }

    public Object getStorage(Class<? extends Enum<?>> sharedEnumClass) {
        String sharedEnumClassName = sharedEnumClass.getName();
        if (!enumToStorageMap.containsKey(sharedEnumClassName)) {
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
        if (!enumToStorageMap.containsKey(sharedEnumClassName)) {
            throw new IllegalArgumentException("Enum is not registered: " + sharedEnumClassName);
        }
        return enumToStorageMap.get(sharedEnumClassName);
    }

    public final Class<?> getClass(String sharedEnumClassName, String name, int depth) throws ArrayIndexOutOfBoundsException {
        return getClass0(getParent(sharedEnumClassName), name, depth);
    }

    private Class<?> getClass0(String parent, String name, int depth) {
        ConcurrentMap<String, StorageField> storage = sharedObjectsMap.get(parent);
        StorageField storageField = storage.get(name);
        if (storageField == null) {
            throw new IllegalArgumentException("Variable not found: " + parent + "." + name);
        }

        Class<?> clazz = getFieldClass(storageField, depth);
        if (clazz == null) {
            throw new ClassCastException("Wrong depth of variable " + parent + "." + name + ": " + depth);
        }
        return getFieldClass(storageField, depth);
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
    public final <T> T get(Enum<?> variable, int... indices) throws ArrayIndexOutOfBoundsException, ClassCastException {
        return get0(getParent(variable), variable.name(), indices);
    }

    public final <T> T get(String sharedEnumClassName, String name, int... indices) throws ArrayIndexOutOfBoundsException, ClassCastException {
        return get0(getParent(sharedEnumClassName), name, indices);
    }

    @SuppressWarnings("unchecked")
    private <T> T get0(String parent, String name, int... indices) throws ArrayIndexOutOfBoundsException, ClassCastException {
        ConcurrentMap<String, StorageField> storage = sharedObjectsMap.get(parent);

        StorageField field = storage.get(name);
        if (field == null) {
            throw new IllegalArgumentException("Variable not found: " + parent + "." + name);
        }

        if (indices.length == 0) {
            return (T) field.getValue();
        } else {
            Object array = getArrayElement(field.getValue(), indices, indices.length - 1);
            if (array == null) {
                throw new NullPointerException("Cannot get value from: " + parent + "." + name + Arrays.toString(indices));
            } else if (!array.getClass().isArray()) {
                throw new ClassCastException("Cannot get value from " + parent + "." + name + Arrays.toString(indices));
            } else if (Array.getLength(array) <= indices[indices.length - 1]) {
                throw new ArrayIndexOutOfBoundsException("Cannot get value from " + parent + "." + name + Arrays.toString(indices));
            }

            return (T) Array.get(array, indices[indices.length - 1]);
        }
    }

    /**
     * Accumulates new value of variable to InternalStorages into the array, or as
     * variable value if indices omitted
     *
     * @param function accumulate function
     * @param value    new value of variable
     * @param variable name of shared variable
     * @param indices  (optional) indices into the array
     * @throws ClassCastException             there is more indices than variable dimension
     *                                        or value cannot be assigned to the variable
     * @throws ArrayIndexOutOfBoundsException one of indices is out of bound
     */
    public final <T> void accumulate(ReduceOperation<T> function, T value, Enum<?> variable, int... indices) throws ArrayIndexOutOfBoundsException, ClassCastException, NullPointerException {
        accumulate0(function, value, getParent(variable), variable.name(), indices);
    }

    public final <T> void accumulate(ReduceOperation<T> function, T value, String sharedEnumClassName, String name, int... indices) throws ArrayIndexOutOfBoundsException, ClassCastException, NullPointerException {
        accumulate0(function, value, getParent(sharedEnumClassName), name, indices);
    }

    @SuppressWarnings("unchecked")
    private <T> void accumulate0(ReduceOperation<T> function, T value, String parent, String name, int... indices) throws ArrayIndexOutOfBoundsException, ClassCastException {
        ConcurrentMap<String, StorageField> storage = sharedObjectsMap.get(parent);

        StorageField field = storage.get(name);
        if (field == null) {
            throw new IllegalArgumentException("Variable not found: " + parent + "." + name);
        }

        Class<?> targetClass = getFieldClass(field, indices.length);

        Class<?> fromClass = getValueClass(value);

        if (!isAssignableFrom(targetClass, fromClass)) {
            throw new ClassCastException("Cannot cast " + fromClass.getName()
                                                 + " to the type of variable "
                                                 + "'" + parent + "." + name + (indices.length == 0 ? "" : Arrays.toString(indices)) + "'"
                                                 + ": " + targetClass);
        }

        Object updateValue = value;
        if (targetClass.isPrimitive()) {
            updateValue = PrimitiveTypes.convert(targetClass, value);
        } else if (PrimitiveTypes.isBoxedClass(targetClass) && value != null) {
            updateValue = PrimitiveTypes.convert(targetClass, value);
        }

        if (indices.length == 0) {
            synchronized (field) {
                field.setValue(function.apply((T) field.getValue(), (T) updateValue));
            }
        } else {
            Object array = getArrayElement(field.getValue(), indices, indices.length - 1);
            if (array == null) {
                throw new NullPointerException("Cannot get value from: " + parent + "." + name + Arrays.toString(indices));
            } else if (!array.getClass().isArray()) {
                throw new ClassCastException("Cannot get value from " + parent + "." + name + Arrays.toString(indices));
            } else if (Array.getLength(array) <= indices[indices.length - 1]) {
                throw new ArrayIndexOutOfBoundsException("Cannot get value from " + parent + "." + name + Arrays.toString(indices));
            }

            synchronized (field) {
                Array.set(array, indices[indices.length - 1],
                        function.apply((T) Array.get(array, indices[indices.length - 1]), (T) updateValue));
            }
        }
        field.incrementModificationCounter();
    }

    private Object getArrayElement(Object array, int[] indices, int length) throws ArrayIndexOutOfBoundsException, IllegalArgumentException, ClassCastException {
        for (int index = 0; index < length; ++index) {
            if (array == null) {
                throw new NullPointerException("Array is null at index " + index + " of " + Arrays.toString(indices));
            } else if (!array.getClass().isArray()) {
                throw new ClassCastException("Wrong dimension at point " + index + ".");
            } else if (Array.getLength(array) <= indices[index]) {
                throw new ArrayIndexOutOfBoundsException("Wrong size at point " + index + ".");
            }
            array = Array.get(array, indices[index]);
        }

        return array;
    }

    /**
     * Puts new value of variable to InternalStorages into the array, or as
     * variable value if indices omitted
     *
     * @param value    new value of variable
     * @param variable name of shared variable
     * @param indices  (optional) indices into the array
     * @throws ClassCastException             there is more indices than variable dimension
     *                                        or value cannot be assigned to the variable
     * @throws ArrayIndexOutOfBoundsException one of indices is out of bound
     */
    public final <T> void put(T value, Enum<?> variable, int... indices) throws ArrayIndexOutOfBoundsException, ClassCastException, NullPointerException {
        put0(value, getParent(variable), variable.name(), indices);
    }

    public final <T> void put(T value, String sharedEnumClassName, String name, int... indices) throws ArrayIndexOutOfBoundsException, ClassCastException, NullPointerException {
        put0(value, getParent(sharedEnumClassName), name, indices);
    }

    private <T> void put0(T value, String parent, String name, int... indices) throws ArrayIndexOutOfBoundsException, ClassCastException, NullPointerException {
        ConcurrentMap<String, StorageField> storage = sharedObjectsMap.get(parent);

        StorageField field = storage.get(name);
        if (field == null) {
            throw new IllegalArgumentException("Variable not found: " + parent + "." + name);
        }

        Class<?> targetClass = getFieldClass(field, indices.length);

        Class<?> fromClass = getValueClass(value);

        if (!isAssignableFrom(targetClass, fromClass)) {
            throw new ClassCastException("Cannot cast " + fromClass.getName()
                                                 + " to the type of variable "
                                                 + "'" + parent + "." + name + (indices.length == 0 ? "" : Arrays.toString(indices)) + "'"
                                                 + ": " + targetClass);
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
                throw new NullPointerException("Cannot put value to: " + parent + "." + name + Arrays.toString(indices));
            } else if (!array.getClass().isArray()) {
                throw new ClassCastException("Cannot put value to " + parent + "." + name + Arrays.toString(indices));
            } else if (Array.getLength(array) <= indices[indices.length - 1]) {
                throw new ArrayIndexOutOfBoundsException("Cannot put value to " + parent + "." + name + Arrays.toString(indices));
            }

            Array.set(array, indices[indices.length - 1], newValue);
        }
        field.incrementModificationCounter();
    }

    private Class<?> getFieldClass(StorageField field, int depth) {
        Class<?> variableClass = field.getType();
        Class<?> targetClass;
        if (depth > 0) {
            if (field.getValue() == null) {
                targetClass = getTargetClass(variableClass, depth);
            } else {
                targetClass = getTargetClass(field.getValue().getClass(), depth);
            }
        } else {
            targetClass = variableClass;
        }
        return targetClass;
    }

    private <T> Class<?> getValueClass(T value) {
        if (value == null) {
            return null;
        } else {
            return value.getClass();
        }
    }

    private Class<?> getTargetClass(Class<?> variableClass, int depth) {
        for (int index = 0; index < depth; ++index) {
            if (!variableClass.isArray()) {
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
                return !fromClass.equals(Boolean.class) && PrimitiveTypes.isBoxedClass(fromClass);
            }
        }

        if (PrimitiveTypes.isBoxedClass(targetClass) && PrimitiveTypes.isBoxedClass(fromClass)) {
            return !targetClass.equals(Boolean.class) && !fromClass.equals(Boolean.class);
        }

        return false;
    }

    /**
     * Tells to monitor variable. Set the variable modification counter to zero.
     *
     * @param variable name of shared variable
     */
    public final int monitor(Enum<?> variable) {
        return monitor0(getParent(variable), variable.name());
    }

    private int monitor0(String parent, String name) {
        ConcurrentMap<String, StorageField> storage = sharedObjectsMap.get(parent);

        StorageField field = storage.get(name);
        if (field == null) {
            throw new IllegalArgumentException("Variable not found: " + parent + "." + name);
        }
        return field.resetModificationCounter();
    }

    /**
     * Pauses current Thread and wait for {@code count} modifications of
     * variable. After modification decreases the variable modification counter
     * by {@code count}.
     *
     * @param variable name of shared variable
     * @param count    number of modifications. If 0 - the method exits
     *                 immediately.
     */
    public final int waitFor(Enum<?> variable, int count) {
        return waitFor0(getParent(variable), variable.name(), count);
    }

    private int waitFor0(String parent, String name, int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Value count is less than zero:" + count);
        }
        ConcurrentMap<String, StorageField> storage = sharedObjectsMap.get(parent);

        StorageField field = storage.get(name);
        if (field == null) {
            throw new IllegalArgumentException("Variable not found: " + parent + "." + name);
        }

        if (count > 0) {
            try {
                field.decrementModificationCounter(count);
            } catch (InterruptedException ex) {
                throw new PcjRuntimeException(ex);
            }
        }
        return field.getModificationCounter();
    }

    /**
     * Pauses current Thread and wait for {@code count} modifications of
     * variable. After modification decreases the variable modification counter
     * by {@code count}.
     *
     * @param variable name of shared variable
     * @param count    number of modifications. If 0 - the method exits
     *                 immediately.
     */
    public final int waitFor(Enum<?> variable, int count, long timeout, TimeUnit unit) throws TimeoutException {
        return waitFor0(getParent(variable), variable.name(), count, timeout, unit);
    }

    private int waitFor0(String parent, String name, int count, long timeout, TimeUnit unit) throws TimeoutException {
        if (count < 0) {
            throw new IllegalArgumentException("Value count is less than zero:" + count);
        }

        ConcurrentMap<String, StorageField> storage = sharedObjectsMap.get(parent);

        StorageField field = storage.get(name);
        if (field == null) {
            throw new IllegalArgumentException("Variable not found: " + parent + "." + name);
        }

        if (count > 0) {
            try {
                field.decrementModificationCounter(count, timeout, unit);
            } catch (InterruptedException ex) {
                throw new PcjRuntimeException(ex);
            }
        }
        return field.getModificationCounter();
    }
}