/*
 * Copyright (c) 2011-2019, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BinaryOperator;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
@FunctionalInterface
public interface ReduceOperation<T> extends Serializable, BinaryOperator<T> {

    enum Predefined implements PredefinedReduceOperation<Object> {
        // Math.min(long|int|double|float, long|int|double|float) <==> {Long,Integer,Double,Float}.min(...)
        /**
         * {@link Math#min(long, long)}
         */
        LONG_MIN((ReduceOperation<Long>) Math::min),
        INT_MIN((ReduceOperation<Integer>) Math::min),
        DOUBLE_MIN((ReduceOperation<Double>) Math::min),
        FLOAT_MIN((ReduceOperation<Float>) Math::min),
        // Math.max(long|int|double|float, long|int|double|float) <==> {Long,Integer,Double,Float}.min(...)
        LONG_MAX((ReduceOperation<Long>) Math::max),
        INT_MAX((ReduceOperation<Integer>) Math::max),
        DOUBLE_MAX((ReduceOperation<Double>) Math::max),
        FLOAT_MAX((ReduceOperation<Float>) Math::max),
        // {Long,Integer,Double,Float}.sum(long|int|double|float, long|int|double|float)
        LONG_SUM((ReduceOperation<Long>) Long::sum),
        /**
         * {@link Integer#sum(int, int)}
         */
        INT_SUM((ReduceOperation<Integer>) Integer::sum),
        DOUBLE_SUM((ReduceOperation<Double>) Double::sum),
        FLOAT_SUM((ReduceOperation<Float>) Float::sum),
        // Math.addExact(long|int, long|int)
        LONG_ADD((ReduceOperation<Long>) Math::addExact),
        INT_ADD((ReduceOperation<Integer>) Math::addExact),
        // Math.multiplyExact(long|int, long|int)
        LONG_MUL((ReduceOperation<Long>) Math::multiplyExact),
        INT_MUL((ReduceOperation<Integer>) Math::multiplyExact),
        ;
        /* **************************************************** */

        private static final Map<Byte, ReduceOperation> map;

        static {
            map = new HashMap<>(values().length, 1.0f);
            for (Predefined type : values()) {
                map.put((byte) type.ordinal(), type.function);
            }
            if (map.size() != values().length) {
                throw new InstantiationError("At least two PredefinedReduceOperation items have the same value");
            }
        }

        private final ReduceOperation function;

        Predefined(ReduceOperation<?> function) {
            this.function = function;
        }

        public static ReduceOperation getReduceOperation(byte id) {
            return map.get(id);
        }

        @Override
        public byte getId() {
            return (byte) ordinal();
        }

        @Override
        @SuppressWarnings("unchecked")
        public Object apply(Object o1, Object o2) {
            return function.apply(o1, o2);
        }

    }

    interface PredefinedReduceOperation<T> extends ReduceOperation<T> {
        byte getId();
    }
}