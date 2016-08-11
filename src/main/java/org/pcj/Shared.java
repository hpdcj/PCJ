/* 
 * Copyright (c) 2011-2016, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj;

import java.io.Serializable;

/**
 * Class for naming shared variables. Contains information about variable name, its type and storage
 * associated with the variable.
 *
 * The easiest way of using the shared variable is to modyfy Enum as described in the sample code
 * below.
 *
 * <pre>
 * {@code
 * public enum SharedEnum implements Shared {
 *     variableName(double[].class);
 *
 *     private final Class<?> type;
 *
 *     private SharedEnum(Class<?> type) {
 *         this.type = type;
 *     }
 *
 *     \@Override
 *     public Class<?> type() {
 *         return type;
 *     }
 * }
 * }
 * </pre>
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public interface Shared {

    /**
     * Name of shared variable.
     *
     * The name have to be not null.
     *
     * @return string name that represents the name
     */
    String name();

    /**
     * Type of shared variable.
     *
     * The type have to be not null.
     *
     * @return associated class type
     */
    default Class<?> type() {
        return Serializable.class;
    }

    /**
     * Storage associated with shared variable.
     *
     * The storage name have to be not null.
     *
     * Contains default implementation that returns class name.
     *
     * @return string name that represents storage
     */
    @SuppressWarnings("unchecked")
    default String parent() {
        if (this instanceof Enum) {
            return ((Enum<? extends Shared>) this).getDeclaringClass().getName();
        } else {
            return this.getClass().getName();
        }
    }
}
