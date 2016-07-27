/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pcj;

/**
 * Sample code for Shared enums.
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

    String name();

    Class<?> type();

    @SuppressWarnings("unchecked")
    default String parent() {
        if (this instanceof Enum) {
            return ((Enum<? extends Shared>) this).getDeclaringClass().getName();
        } else {
            return this.getClass().getName();
        }
    }
}
