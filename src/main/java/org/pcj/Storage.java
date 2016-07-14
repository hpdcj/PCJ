/*
 * This file is the part of the PCJ Library
 */
package org.pcj;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

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
    private static final Map<Class<?>, Set<Class<?>>> WIDENING_PRIMITIVES;

    static {
        Map<Class<?>, Set<Class<?>>> map = new HashMap<>(8, 1);
        // https://docs.oracle.com/javase/specs/jls/se8/html/jls-5.html#jls-5.1.2
        map.put(boolean.class, Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                Boolean.class
        ))));
        map.put(byte.class, Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                Byte.class
        ))));
        map.put(short.class, Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                Short.class, Byte.class
        ))));
        map.put(int.class, Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                Integer.class, Character.class, Short.class, Byte.class
        ))));
        map.put(long.class, Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                Long.class, Integer.class, Character.class, Short.class, Byte.class
        ))));
        map.put(float.class, Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                Float.class, Long.class, Integer.class, Character.class, Short.class, Byte.class
        ))));
        map.put(double.class, Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                Double.class, Float.class, Long.class, Integer.class, Character.class, Short.class, Byte.class
        ))));
        map.put(char.class, Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                Character.class
        ))));
        WIDENING_PRIMITIVES = Collections.unmodifiableMap(map);
    }

    private final transient ConcurrentMap<String, Class<?>> variablesTypesMap;
    private final transient ConcurrentMap<String, Object> variablesValueMap;
    private final transient ConcurrentMap<String, AtomicInteger> modificationCountMap;

    public Storage() {
        variablesTypesMap = new ConcurrentHashMap<>();
        variablesValueMap = new ConcurrentHashMap<>();
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

        if (variablesTypesMap.containsKey(name)) {
            throw new IllegalStateException("Variable has already been created: " + name);
        }

        modificationCountMap.put(name, new AtomicInteger(0));
        variablesTypesMap.put(name, type);
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

    private Class<?> variableType(String name) throws IllegalArgumentException {
        if (name == null) {
            throw new NullPointerException("Variable name cannot be null");
        }
        Class<?> clazz = variablesTypesMap.get(name);
        if (clazz == null) {
            throw new IllegalArgumentException("Variable not found: " + name);
        }
        return clazz;
    }

    /**
     * Returns variable from Storages
     *
     * @param name    name of Shared variable
     * @param indexes (optional) indexes into the array
     *
     * @return value of variable[indexes] or variable if indexes omitted
     *
     * @throws ClassCastException             there is more indexes than variable dimension
     * @throws ArrayIndexOutOfBoundsException one of indexes is out of bound
     */
    @SuppressWarnings("unchecked")
    final public <T> T get(String name, int... indexes) throws ArrayIndexOutOfBoundsException, ClassCastException {
        variableType(name);
        Object value = variablesValueMap.get(name);

        if (indexes.length == 0) {
            return (T) value;
        } else {
            Object array = getArrayElement(value, indexes, indexes.length - 1);
            if (array.getClass().isArray() == false) {
                throw new ClassCastException("Cannot put value to " + name + Arrays.toString(indexes) + ".");
            } else if (Array.getLength(array) <= indexes[indexes.length - 1]) {
                throw new ArrayIndexOutOfBoundsException("Cannot put value to " + name + Arrays.toString(indexes) + ".");
            }

            return (T) Array.get(array, indexes[indexes.length - 1]);
        }
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

    /**
     * Puts new value of variable to Storage into the array, or as variable
     * value if indexes omitted
     *
     * @param name     name of Shared variable
     * @param newValue new value of variable
     * @param indexes  (optional) indexes into the array
     *
     * @throws ClassCastException             there is more indexes than variable dimension
     *                                        or value cannot be assigned to the variable
     * @throws ArrayIndexOutOfBoundsException one of indexes is out of bound
     */
    final public <T> void put(String name, T newValue, int... indexes) throws ArrayIndexOutOfBoundsException, ClassCastException, NullPointerException {
        Class<?> variableClass = variableType(name);
        if (isAssignableFrom(variableClass, newValue.getClass(), indexes) == false) {
            throw new ClassCastException("Cannot cast " + newValue.getClass().getCanonicalName()
                    + " to the type of variable '" + name + "': " + variablesTypesMap.get(name));
        }

        if (indexes.length == 0) {
            variablesValueMap.put(name, newValue);
        } else {
            Object array = getArrayElement(variablesValueMap.get(name), indexes, indexes.length - 1);

            if (array == null) {
                throw new NullPointerException("Cannot put value to: " + name);
            } else if (array.getClass().isArray() == false) {
                throw new ClassCastException("Cannot put value to " + name + Arrays.toString(indexes));
            } else if (Array.getLength(array) <= indexes[indexes.length - 1]) {
                throw new ArrayIndexOutOfBoundsException("Cannot put value to " + name + Arrays.toString(indexes));
            }

            Array.set(array, indexes[indexes.length - 1], newValue);
        }

        AtomicInteger monitor = modificationCountMap.get(name);
        monitor.getAndIncrement();
        synchronized (monitor) {
            monitor.notifyAll();
        }
    }

    /**
     * Checks if value can be assigned to variable stored in Storage
     *
     * @return true if the value can be assigned to the variable
     */
    private boolean isAssignableFrom(Class<?> variableClass, Class<?> fromClass, int... indexes) {
        for (int i = 0; i < indexes.length; ++i) {
            if (variableClass.isArray() == false) {
                return false;
            }
            variableClass = variableClass.getComponentType();
        }

        if (fromClass == null) {
            return !variableClass.isPrimitive();
        }

        if (variableClass.isAssignableFrom(fromClass)) {
            return true;
        }

        if (variableClass.isPrimitive()) {
            return WIDENING_PRIMITIVES.get(variableClass).contains(fromClass);
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
                        ex.printStackTrace(System.err);
                    }
                }
            }
        } while (monitor.compareAndSet(v, v - count) == false);

        return monitor.get();
    }

}
