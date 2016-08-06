/*
 * This file is the part of the PCJ Library
 */
package org.pcj.internal;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.pcj.PcjRuntimeException;
import org.pcj.Shared;

/**
 * External class with methods do handle shared variables.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class InternalStorage {

    private class StorageField {

        private final Class<?> type;
        private final AtomicInteger modificationCount;
        private Object value;

        public StorageField(Class<?> type) {
            this.type = type;
            this.modificationCount = new AtomicInteger(0);

            if (type.isPrimitive()) {
                this.value = PrimitiveTypes.defaultValue(type);
            } else {
                this.value = null;
            }
        }

        public Class<?> getType() {
            return type;
        }

        public AtomicInteger getModificationCount() {
            return modificationCount;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }

    }

    private final transient ConcurrentMap<String, ConcurrentMap<String, StorageField>> storageMap;

    public InternalStorage() {
        storageMap = new ConcurrentHashMap<>();
    }

    public void registerShared(Shared variable)
            throws NullPointerException, IllegalArgumentException, IllegalStateException {
        if (variable instanceof Shared == false) {
            throw new IllegalArgumentException("Not shared type: " + variable);
        }
        createShared0(((Shared) variable).parent(), ((Shared) variable).name(), ((Shared) variable).type());
    }

    private void createShared0(String storageName, String name, Class<?> type)
            throws NullPointerException, IllegalArgumentException, IllegalStateException {
        if (type == null) {
            throw new NullPointerException("Variable type cannot be null");
        }

        if (type.isPrimitive() == false && Serializable.class.isAssignableFrom(type) == false) {
            throw new IllegalArgumentException("Variable type is not serializable");
        }

        ConcurrentMap<String, StorageField> storage = getStorage(storageName);
        StorageField field = new StorageField(type);

        if (storage.putIfAbsent(name, field) != null) {
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
     * @param name    name of Shared variable
     * @param indices (optional) indices into the array
     *
     * @return value of variable[indices] or variable if indices omitted
     *
     * @throws ClassCastException             there is more indices than variable dimension
     * @throws ArrayIndexOutOfBoundsException one of indices is out of bound
     */
    final public <T> T get(Shared variable, int... indices) throws ArrayIndexOutOfBoundsException, ClassCastException {
        if (variable == null) {
            throw new NullPointerException("Variable name cannot be null");
        }

        return get0(variable.parent(), variable.name());
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
     * @param name     name of Shared variable
     * @param newValue new value of variable
     * @param indices  (optional) indices into the array
     *
     * @throws ClassCastException             there is more indices than variable dimension
     *                                        or value cannot be assigned to the variable
     * @throws ArrayIndexOutOfBoundsException one of indices is out of bound
     */
    final public <T> void put(Shared variable, T value, int... indices) throws ArrayIndexOutOfBoundsException, ClassCastException, NullPointerException {
        if (variable == null) {
            throw new NullPointerException("Variable name cannot be null");
        }

        put0(variable.parent(), variable.name(), value, indices);
    }

    final public <T> void put0(String storageName, String name, T value, int... indices) throws ArrayIndexOutOfBoundsException, ClassCastException, NullPointerException {
        ConcurrentMap<String, StorageField> storage = getStorage(storageName);

        StorageField field = storage.get(name);
        if (field == null) {
            throw new IllegalArgumentException("Variable not found: " + name);
        }

        Class<?> variableClass = field.getType();
        Class<?> targetClass = getTargetClass(variableClass, indices);
        Class<?> fromClass = getValueClass(value);

        if (isAssignableFrom(targetClass, fromClass) == false) {
            throw new ClassCastException("Cannot cast " + fromClass.getName()
                    + " to the type of variable '" + name + "'"
                    + (indices.length == 0 ? "" : " with indices: " + Arrays.toString(indices))
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
     * @param variable name of Shared variable
     */
    final public int monitor(Shared variable) {
        if (variable == null) {
            throw new NullPointerException("Variable name cannot be null");
        }

        return monitor0(variable.parent(), variable.name());
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
     * @param variable name of Shared variable
     * @param count    number of modifications. If 0 - the method exits immediately.
     *
     *
     */
    final public int waitFor(Shared variable, int count) {
        if (variable == null) {
            throw new NullPointerException("Variable name cannot be null");
        }

        return waitFor0(variable.parent(), variable.name(), count);
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
     * @param variable name of Shared variable
     * @param count    number of modifications. If 0 - the method exits immediately.
     */
    final public int waitFor(Shared variable, int count, long timeout, TimeUnit unit) throws TimeoutException {
        if (variable == null) {
            throw new NullPointerException("Variable name cannot be null");
        }

        return waitFor0(variable.parent(), variable.name(), count, timeout, unit);
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
