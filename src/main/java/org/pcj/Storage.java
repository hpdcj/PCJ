/*
 * Copyright (c) 2011-2022, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Storage annotation is used for selecting class that can be Storage class.
 * <p>
 * This annotation has to annotate enum class, which constants will be names of shareable variables.
 * All enum constants has to have field equivalent in class provided as annotation value.
 * <p>
 * If the storage class is not provided, the immediately enclosing class of the enum will be taken as the storage class.
 * <p>
 * Not all fields in the class has to be shareable and exists in enum.
 * <p>
 * Shareable variables can use any access modifier.
 * <p>
 * Example of usage:
 * <pre>
 * {@code
 * public class StorageClass {
 *     \@Storage(StorageClass.class)
 *     enum ShareableEnum {
 *         array, avg,
 *     }
 *
 *     private int[] array;
 *     public double avg;
 * }
 * }
 * </pre>
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Storage {

    Class<?> value() default EnclosingClass.class;

    /**
     * Internal class for default value that will search for Storage in enclosing class
     */
    final class EnclosingClass {}
}
