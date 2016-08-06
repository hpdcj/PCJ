/* 
 * Copyright (c) 2011-2016, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class PrimitiveTypes {

    private static final Set<Class<?>> BOXED_PRIMITIVES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    Byte.class, Short.class, Character.class, Integer.class, Long.class,
                    Float.class, Double.class,
                    Boolean.class)));
    private static final Map<Class<?>, Function<Object, ?>> CONVERSION_MAP = new HashMap<>(8, 1);
    private static final Map<Class<?>, Object> DEFAULT_VALUE_MAP = new HashMap<>(8, 1);

    static {
        CONVERSION_MAP.put(double.class, PrimitiveTypes::convertToDouble);
        CONVERSION_MAP.put(float.class, PrimitiveTypes::convertToFloat);
        CONVERSION_MAP.put(long.class, PrimitiveTypes::convertToLong);
        CONVERSION_MAP.put(int.class, PrimitiveTypes::convertToInt);
        CONVERSION_MAP.put(short.class, PrimitiveTypes::convertToShort);
        CONVERSION_MAP.put(char.class, PrimitiveTypes::convertToChar);
        CONVERSION_MAP.put(byte.class, PrimitiveTypes::convertToByte);
        CONVERSION_MAP.put(boolean.class, PrimitiveTypes::convertToBoolean);

        CONVERSION_MAP.put(Double.class, PrimitiveTypes::convertToDouble);
        CONVERSION_MAP.put(Float.class, PrimitiveTypes::convertToFloat);
        CONVERSION_MAP.put(Long.class, PrimitiveTypes::convertToLong);
        CONVERSION_MAP.put(Integer.class, PrimitiveTypes::convertToInt);
        CONVERSION_MAP.put(Short.class, PrimitiveTypes::convertToShort);
        CONVERSION_MAP.put(Character.class, PrimitiveTypes::convertToChar);
        CONVERSION_MAP.put(Byte.class, PrimitiveTypes::convertToByte);
        CONVERSION_MAP.put(Boolean.class, PrimitiveTypes::convertToBoolean);

        DEFAULT_VALUE_MAP.put(double.class, 0.0);
        DEFAULT_VALUE_MAP.put(float.class, 0.0f);
        DEFAULT_VALUE_MAP.put(long.class, 0L);
        DEFAULT_VALUE_MAP.put(int.class, 0);
        DEFAULT_VALUE_MAP.put(short.class, 0);
        DEFAULT_VALUE_MAP.put(char.class, 0);
        DEFAULT_VALUE_MAP.put(byte.class, 0);
        DEFAULT_VALUE_MAP.put(boolean.class, false);

    }

    public static boolean isBoxedClass(Class<?> clazz) {
        return BOXED_PRIMITIVES.contains(clazz);
    }

    public static Object defaultValue(Class<?> clazz) {
        return CONVERSION_MAP.get(clazz);
    }

    public static <T> Object convert(Class<?> targetClass, T value) {
        Function<Object, ?> converter = CONVERSION_MAP.get(targetClass);
        if (converter == null) {
            return value;
        }
        return converter.apply(value);
    }

    public static <T> double convertToDouble(T value) {
        if (value instanceof Byte) {
            return ((Byte) value).doubleValue();
        } else if (value instanceof Short) {
            return ((Short) value).doubleValue();
        } else if (value instanceof Character) {
            return (double) ((Character) value);
        } else if (value instanceof Integer) {
            return ((Integer) value).doubleValue();
        } else if (value instanceof Long) {
            return ((Long) value).doubleValue();
        } else if (value instanceof Float) {
            return ((Float) value).doubleValue();
        } else if (value instanceof Double) {
            return ((Double) value);
        }
        throw new ClassCastException("Unable to cast: " + value.getClass().getName());
    }

    public static <T> float convertToFloat(T value) {
        if (value instanceof Byte) {
            return ((Byte) value).floatValue();
        } else if (value instanceof Short) {
            return ((Short) value).floatValue();
        } else if (value instanceof Character) {
            return (float) ((Character) value);
        } else if (value instanceof Integer) {
            return ((Integer) value).floatValue();
        } else if (value instanceof Long) {
            return ((Long) value).floatValue();
        } else if (value instanceof Float) {
            return ((Float) value);
        } else if (value instanceof Double) {
            return ((Double) value).floatValue();
        }
        throw new ClassCastException("Unable to cast: " + value.getClass().getName());
    }

    public static <T> long convertToLong(T value) {
        if (value instanceof Byte) {
            return ((Byte) value).longValue();
        } else if (value instanceof Short) {
            return ((Short) value).longValue();
        } else if (value instanceof Character) {
            return (long) ((Character) value);
        } else if (value instanceof Integer) {
            return ((Integer) value).longValue();
        } else if (value instanceof Long) {
            return ((Long) value);
        } else if (value instanceof Float) {
            return ((Float) value).longValue();
        } else if (value instanceof Double) {
            return ((Double) value).longValue();
        }
        throw new ClassCastException("Unable to cast: " + value.getClass().getName());
    }

    public static <T> int convertToInt(T value) {
        if (value instanceof Byte) {
            return ((Byte) value).intValue();
        } else if (value instanceof Short) {
            return ((Short) value).intValue();
        } else if (value instanceof Character) {
            return (int) ((Character) value);
        } else if (value instanceof Integer) {
            return ((Integer) value);
        } else if (value instanceof Long) {
            return ((Long) value).intValue();
        } else if (value instanceof Float) {
            return ((Float) value).intValue();
        } else if (value instanceof Double) {
            return ((Double) value).intValue();
        }
        throw new ClassCastException("Unable to cast: " + value.getClass().getName());
    }

    public static <T> char convertToChar(T value) {
        if (value instanceof Byte) {
            return (char) ((Byte) value).byteValue();
        } else if (value instanceof Short) {
            return (char) ((Short) value).shortValue();
        } else if (value instanceof Character) {
            return ((Character) value);
        } else if (value instanceof Integer) {
            return (char) ((Integer) value).intValue();
        } else if (value instanceof Long) {
            return (char) ((Long) value).longValue();
        } else if (value instanceof Float) {
            return (char) ((Float) value).floatValue();
        } else if (value instanceof Double) {
            return (char) ((Double) value).doubleValue();
        }
        throw new ClassCastException("Unable to cast: " + value.getClass().getName());
    }

    public static <T> short convertToShort(T value) {
        if (value instanceof Byte) {
            return ((Byte) value).shortValue();
        } else if (value instanceof Short) {
            return ((Short) value);
        } else if (value instanceof Character) {
            return (short) ((Character) value).charValue();
        } else if (value instanceof Integer) {
            return ((Integer) value).shortValue();
        } else if (value instanceof Long) {
            return ((Long) value).shortValue();
        } else if (value instanceof Float) {
            return ((Float) value).shortValue();
        } else if (value instanceof Double) {
            return ((Double) value).shortValue();
        }
        throw new ClassCastException("Unable to cast: " + value.getClass().getName());
    }

    public static <T> byte convertToByte(T value) {
        if (value instanceof Byte) {
            return ((Byte) value);
        } else if (value instanceof Short) {
            return ((Short) value).byteValue();
        } else if (value instanceof Character) {
            return (byte) ((Character) value).charValue();
        } else if (value instanceof Integer) {
            return ((Integer) value).byteValue();
        } else if (value instanceof Long) {
            return ((Long) value).byteValue();
        } else if (value instanceof Float) {
            return ((Float) value).byteValue();
        } else if (value instanceof Double) {
            return ((Double) value).byteValue();
        }
        throw new ClassCastException("Unable to cast: " + value.getClass().getName());
    }

    public static <T> boolean convertToBoolean(T value) {
        if (value instanceof Boolean) {
            return ((Boolean) value);
        }
        throw new ClassCastException("Unable to cast: " + value.getClass().getName());
    }
}
