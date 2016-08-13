/*
 * Copyright (c) 2011-2016, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Storage annotation is used for selecting class that can be Storage class.
 *
 * It requires providing enum class which constants will be names of shared variables.
 * All enum constants has to have field equivalent in annotated class.
 * Not all fields in class has to be shared and exists in enum.
 *
 * Example of usage:
 * <pre>
 * {@code
 *
 * \@Storage(Shared.class)
 * public class StorageClass {
 *     enum Shared {
 *         t, avg
 *     }
 *
 *     int[] t;
 *     double avg;
 * }
 * }
 * </pre>
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
@Documented
@Inherited
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Storage {

    Class<? extends Enum<?>> value();
}
