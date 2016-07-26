/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pcj;

/**
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
