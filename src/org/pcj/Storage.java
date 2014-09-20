/*
 * This file is the part of the PCJ Library
 */
package org.pcj;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * External class with methods do handle shared variables.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public abstract class Storage implements org.pcj.internal.storage.InternalStorage {

    private transient final Map<String, Field> sharedFields = new HashMap<>();
    private transient final Map<String, Integer> monitorFields = new HashMap<>();

    protected Storage() {
        for (Field field : this.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Shared.class)) {
                String key = field.getAnnotation(Shared.class).value();
                if (key.isEmpty()) {
                    key = field.getName();
                }
                if (sharedFields.containsKey(key)) {
                    throw new ArrayStoreException("Duplicate key value (" + key + ")");
                }

                if (field.getType().isPrimitive() == false
                        && Serializable.class.isAssignableFrom(field.getType()) == false) {
                    throw new ClassCastException("Field (" + key + ") is not serializable");
                }

                field.setAccessible(true);
                sharedFields.put(key, field);
                monitorFields.put(key, 0);
            }
        }
    }

    /**
     * Gets names of all Shared variables of the Storage
     *
     * @return array with names of all Shared variables
     */
    @Override
    final public String[] getSharedFields() {
        return sharedFields.keySet().toArray(new String[0]);
    }

    private Field getField(String name) {
        final Field field = sharedFields.get(name);
        if (field == null) {
            throw new ArrayStoreException("Key not found (" + name + ")");
        }
        return field;
    }

    private Object getArrayElement(Object array, int[] indexes, int length) throws ArrayIndexOutOfBoundsException, IllegalArgumentException, ClassCastException {
        for (int index = 0; index < length; ++index) {
            if (array.getClass().isArray() == false) {
                throw new ClassCastException("Wrong dimension at point " + index + ".");
            } else if (Array.getLength(array) <= indexes[index]) {
                throw new ArrayIndexOutOfBoundsException("Wrong size at point " + index + ".");
            }
            array = Array.get(array, indexes[index]);
        }

        return array;
    }

    private boolean isAssignableFrom(String variable, Class<?> clazz, int... indexes) {
        Class<?> fieldClass = getField(variable).getType();

        for (int i = 0; i < indexes.length; ++i) {
            if (fieldClass.isArray() == false) {
                return false;
            }
            fieldClass = fieldClass.getComponentType();
        }

        if (fieldClass.isAssignableFrom(clazz)) {
            return true;
        }
        if (fieldClass.isPrimitive()) {
            return (fieldClass.equals(boolean.class) && clazz.equals(Boolean.class))
                    || (fieldClass.equals(byte.class) && clazz.equals(Byte.class))
                    || (fieldClass.equals(short.class) && clazz.equals(Short.class))
                    || (fieldClass.equals(int.class) && clazz.equals(Integer.class))
                    || (fieldClass.equals(long.class) && clazz.equals(Long.class))
                    || (fieldClass.equals(float.class) && clazz.equals(Float.class))
                    || (fieldClass.equals(double.class) && clazz.equals(Double.class))
                    || (fieldClass.equals(char.class) && clazz.equals(Character.class));
        }
        return false;
    }

    /**
     * Checks if value can be assigned to variable stored in
     * Storage
     *
     * @param variable name of variable stored in Storage
     * @param value to check
     * @return true if the value can be assigned to the
     * variable
     */
    @Override
    final public boolean isAssignable(String variable, Object value, int... indexes) {
        return isAssignableFrom(variable, value.getClass(), indexes);
    }

    /**
     * Returns variable from Storages
     *
     * @param variable name of Shared variable
     * @param indexes (optional) indexes into the array
     *
     * @return value of variable[indexes] or variable if
     * indexes omitted
     *
     * @throws ClassCastException there is more indexes than
     * variable dimension
     * @throws ArrayIndexOutOfBoundsException one of indexes
     * is out of bound
     */
    @Override
    @SuppressWarnings("unchecked")
    final public <T> T get(String variable, int... indexes) throws ArrayIndexOutOfBoundsException, ClassCastException {
        try {
            final Field field = getField(variable);

            if (indexes.length == 0) {
                return (T) field.get(this);
            } else {

                Object array = getArrayElement(field.get(this), indexes, indexes.length - 1);

                if (array.getClass().isArray() == false) {
                    throw new ClassCastException("Cannot put value to " + variable + Arrays.toString(indexes) + ".");
                } else if (Array.getLength(array) <= indexes[indexes.length - 1]) {
                    throw new ArrayIndexOutOfBoundsException("Cannot put value to " + variable + Arrays.toString(indexes) + ".");
                }

                return (T) Array.get(array, indexes[indexes.length - 1]);
            }
        } catch (IllegalAccessException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Puts new value of variable to Storage into the array,
     * or as variable value if indexes omitted
     *
     * @param variable name of Shared variable
     * @param newValue new value of variable
     * @param indexes (optional) indexes into the array
     *
     * @throws ClassCastException there is more indexes than
     * variable dimension or value cannot be assigned to the
     * variable
     * @throws ArrayIndexOutOfBoundsException one of indexes
     * is out of bound
     */
    @Override
    final public void put(String variable, Object newValue, int... indexes) throws ArrayIndexOutOfBoundsException, ClassCastException {
        if (isAssignable(variable, newValue, indexes) == false) {
            throw new ClassCastException("Cannot cast " + newValue.getClass().getCanonicalName()
                    + " to the type of variable '" + variable + "'");
        }
        try {
            final Field field = getField(variable);

            synchronized (field) {
                if (indexes.length == 0) {
                    field.set(this, newValue);
                } else {
                    Object array = getArrayElement(field.get(this), indexes, indexes.length - 1);

                    if (array == null) {
                        throw new ClassCastException("Cannot put value to " + variable + " - NullPointerException.");
                    } else if (array.getClass().isArray() == false) {
                        throw new ClassCastException("Cannot put value to " + variable + Arrays.toString(indexes) + ".");
                    } else if (Array.getLength(array) <= indexes[indexes.length - 1]) {
                        throw new ArrayIndexOutOfBoundsException("Cannot put value to " + variable + Arrays.toString(indexes) + ".");
                    }

                    Array.set(array, indexes[indexes.length - 1], newValue);
                }

                monitorFields.put(variable, monitorFields.get(variable) + 1);
                field.notifyAll();
            }
        } catch (IllegalAccessException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Tells to monitor variable. Set the field modification
     * counter to zero.
     *
     * @param variable name of Shared variable
     */
    @Override
    final public void monitor(String variable) {
        final Field field = getField(variable);
        synchronized (field) {
            monitorFields.put(variable, 0);
        }
    }

    /**
     * Pauses current Thread and wait for modification of
     * variable.
     * <p>
     * The same as calling waitFor method using
     * <code>waitFor(variable, 1)</code>. After modification
     * decreases the field modification counter by one.
     *
     * @param variable name of Shared variable
     */
    @Override
    final public void waitFor(String variable) {
        waitFor(variable, 1);
    }

    /**
     * Pauses current Thread and wait for <code>count</code>
     * modifications of variable. After modification decreases
     * the field modification counter by <code>count</code>.
     *
     * @param variable name of Shared variable
     */
    @Override
    final public void waitFor(String variable, int count) {
        final Field field = getField(variable);
        synchronized (field) {
            int v;
            while ((v = monitorFields.get(variable)) < count) {
                try {
                    field.wait();
                } catch (InterruptedException ex) {
                    ex.printStackTrace(System.err);
                }
            }
            monitorFields.put(variable, v - count);
        }
    }
}
