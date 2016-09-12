/*
 * Copyright (c) 2011-2016, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.pcj.RegisterStorage;

/**
 * Container for {@link org.pcj.RegisterStorage @RegisterStorage} annotation using
 * {@link java.lang.annotation.Repeatable @Repeatable}.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RegisterStorageRepeatableContainer {

    RegisterStorage[] value();
}
