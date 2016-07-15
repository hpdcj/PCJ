/*
 * This file is the part of the PCJ Library
 */
package org.pcj;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.pcj.internal.PrimitiveTypes;

/**
 * External class with methods do handle shared variables.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class Storage {

    private static final Set<String> RESERVED_WORDS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    // https://docs.oracle.com/javase/specs/jls/se8/html/jls-3.html#jls-3.8
                    // keywords:
                    "abstract", "continue", "for", "new", "switch",
                    "assert", "default", "if", "package", "synchronized",
                    "boolean", "do", "goto", "private", "this",
                    "break", "double", "implements", "protected", "throw",
                    "byte", "else", "import", "public", "throws",
                    "case", "enum", "instanceof", "return", "transient",
                    "catch", "extends", "int", "short", "try",
                    "char", "final", "interface", "static", "void",
                    "class", "finally", "long", "strictfp", "volatile",
                    "const", "float", "native", "super", "while",
                    // boolean literals:
                    "true", "false",
                    // null literal:
                    "null")));

    private final transient ConcurrentMap<String, Class<?>> typesMap;
    private final transient ConcurrentMap<String, Object> valueMap;
    private final transient ConcurrentMap<String, AtomicInteger> modificationCountMap;

    public Storage() {
        typesMap = new ConcurrentHashMap<>();
        valueMap = new ConcurrentHashMap<>();
        modificationCountMap = new ConcurrentHashMap<>();
    }

    public void createShared(String name, Class<?> type)
            throws NullPointerException, IllegalArgumentException, IllegalStateException {
        if (type == null) {
            throw new NullPointerException("Variable type cannot be null");
        }

        if (type.isPrimitive() == false && Serializable.class.isAssignableFrom(type) == false) {
            throw new IllegalArgumentException("Variable type is not serializable");
        }

        identifierNameCheck(name);

        if (typesMap.containsKey(name)) {
            throw new IllegalStateException("Variable has already been created: " + name);
        }

        modificationCountMap.put(name, new AtomicInteger(0));
        if (type.isPrimitive()) {
            valueMap.put(name, PrimitiveTypes.defaultValue(type));
        }
        typesMap.put(name, type);
    }

    private void identifierNameCheck(String name)
            throws NullPointerException, IllegalArgumentException {
        if (name == null) {
            throw new NullPointerException("Variable name cannot be null");
        }

        if (name.isEmpty()) {
            throw new IllegalArgumentException("Variable name cannot be empty");
        }

        if (RESERVED_WORDS.contains(name)) {
            throw new IllegalArgumentException("Variable name is reserved word: " + name);
        }

        if (Character.isJavaIdentifierStart(name.charAt(0)) == false
                || name.codePoints().skip(1).allMatch(Character::isJavaIdentifierPart) == false) {
            throw new IllegalArgumentException("Variable name does not meet requirements for identifiers as stated in JSL: " + name);
        }
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
    @SuppressWarnings("unchecked")
    final public <T> T get(String name, int... indices) throws ArrayIndexOutOfBoundsException, ClassCastException {
        if (name == null) {
            throw new NullPointerException("Variable name cannot be null");
        }
        if (typesMap.containsKey(name) == false) {
            throw new IllegalArgumentException("Variable not found: " + name);
        }
        Object value = valueMap.get(name);

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
     * Puts new value of variable to Storage into the array, or as variable
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
    final public <T> void put(String name, T value, int... indices) throws ArrayIndexOutOfBoundsException, ClassCastException, NullPointerException {
        if (name == null) {
            throw new NullPointerException("Variable name cannot be null");
        }
        Class<?> variableClass = typesMap.get(name);
        if (variableClass == null) {
            throw new IllegalArgumentException("Variable not found: " + name);
        }
        Class<?> targetClass = getTargetClass(variableClass, indices);
        Class<?> fromClass = getValueClass(value);

        if (isAssignableFrom(targetClass, fromClass) == false) {
            throw new ClassCastException("Cannot cast " + fromClass.getCanonicalName()
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
            if (newValue != null) {
                valueMap.put(name, newValue);
            } else {
                valueMap.remove(name);
            }
        } else {
            Object array = getArrayElement(valueMap.get(name), indices, indices.length - 1);

            if (array == null) {
                throw new NullPointerException("Cannot put value to: " + name);
            } else if (array.getClass().isArray() == false) {
                throw new ClassCastException("Cannot put value to " + name + Arrays.toString(indices));
            } else if (Array.getLength(array) <= indices[indices.length - 1]) {
                throw new ArrayIndexOutOfBoundsException("Cannot put value to " + name + Arrays.toString(indices));
            }

            Array.set(array, indices[indices.length - 1], newValue);
        }

        AtomicInteger monitor = modificationCountMap.get(name);
        monitor.getAndIncrement();
        synchronized (monitor) {
            monitor.notifyAll();
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
     * Checks if value can be assigned to variable stored in Storage
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
    final public void monitor(String variable) {
        modificationCountMap.get(variable).set(0);
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
    final public int waitFor(String variable, int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Value count is less than zero:" + count);
        }

        AtomicInteger monitor = modificationCountMap.get(variable);
        if (count == 0) {
            return monitor.get();
        }
        int v;
        do {
            synchronized (monitor) {
                while ((v = monitor.get()) < count) {
                    try {
                        monitor.wait();
                    } catch (InterruptedException ex) {
                        throw new PcjRuntimeException(ex);
                    }
                }
            }
        } while (monitor.compareAndSet(v, v - count) == false);

        return monitor.get();
    }

    /**
     * Pauses current Thread and wait for <code>count</code> modifications of
     * variable. After modification decreases the variable modification counter by
     * <code>count</code>.
     *
     * @param variable name of Shared variable
     * @param count    number of modifications. If 0 - the method exits immediately.
     */
    final public int waitFor(String variable, int count, long timeout, TimeUnit unit) throws TimeoutException {
        if (count < 0) {
            throw new IllegalArgumentException("Value count is less than zero:" + count);
        }

        AtomicInteger monitor = modificationCountMap.get(variable);
        if (count == 0) {
            return monitor.get();
        }
        long nanosTimeout = unit.toNanos(timeout);
        final long deadline = System.nanoTime() + nanosTimeout;

        int v;
        do {

            synchronized (monitor) {
                while ((v = monitor.get()) < count) {
                    if (nanosTimeout <= 0L) {
                        throw new TimeoutException();
                    }
                    try {
                        monitor.wait(nanosTimeout / 1_000_000, (int) (nanosTimeout % 1_000_000));
                    } catch (InterruptedException ex) {
                        throw new PcjRuntimeException(ex);
                    }
                    nanosTimeout = deadline - System.nanoTime();
                }
            }
        } while (monitor.compareAndSet(v, v - count) == false);

        return monitor.get();
    }

}
