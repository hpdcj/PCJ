/*
 * This file is the part of the PCJ Library
 */
package org.pcj;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for shared variables.
 * 
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Shared {
}
