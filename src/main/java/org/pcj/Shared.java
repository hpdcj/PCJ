/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pcj;

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
 *     private final Class&lt;?&gt; type;
 *
 *     private SharedEnum(Class&lt;?&gt; type) {
 *         this.type = type;
 *     }
 *
 *     @Override
 *     public Class<?> type() {
 *         return type;
 *     }
 * }
 * }
 * </pre>
 *
 * @author faramir
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
    Class<?> type();

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
