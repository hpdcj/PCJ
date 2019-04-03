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
        MIN_LONG((ReduceOperation<Long>) Math::min),
        MIN_INT((ReduceOperation<Integer>) Math::min),
        MIN_DOUBLE((ReduceOperation<Double>) Math::min),
        MIN_FLOAT((ReduceOperation<Float>) Math::min),
        // Math.max(long|int|double|float, long|int|double|float) <==> {Long,Integer,Double,Float}.min(...)
        MAX_LONG((ReduceOperation<Long>) Math::max),
        MAX_INT((ReduceOperation<Integer>) Math::max),
        MAX_DOUBLE((ReduceOperation<Double>) Math::max),
        MAX_FLOAT((ReduceOperation<Float>) Math::max),
        // {Long,Integer,Double,Float}.sum(long|int|double|float, long|int|double|float)
        SUM_LONG((ReduceOperation<Long>) Long::sum),
        /**
         * {@link Integer#sum(int, int)}
         */
        SUM_INT((ReduceOperation<Integer>) Integer::sum),
        SUM_DOUBLE((ReduceOperation<Double>) Double::sum),
        SUM_FLOAT((ReduceOperation<Float>) Float::sum),
        // Math.addExact(long|int, long|int)
        ADD_LONG((ReduceOperation<Long>) Math::addExact),
        ADD_INT((ReduceOperation<Integer>) Math::addExact),
        // Math.multiplyExact(long|int, long|int)
        MUL_LONG((ReduceOperation<Long>) Math::multiplyExact),
        MUL_INT((ReduceOperation<Integer>) Math::multiplyExact),
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