/*
 * This file is the part of the PCJ Library
 */
package org.pcj;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for classes with native methods. These classes will be shared
 * within JVM.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ContainsNative {
}
