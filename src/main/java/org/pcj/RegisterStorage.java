/*
 * Copyright (c) 2011-2019, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.pcj.internal.RegisterStorageRepeatableContainer;

/**
 * Annotation for class implementing StartPoint interface.
 * <p>
 * It tells which storage should be automatically register on start up of class implementing
 * StartPoint interface.
 * <p>
 * When value is omitted, all enums annotated with {@literal @}Storage in StartPoint class will be registered.
 * <p>
 * Can be used multiple times per StartPoint class.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
@Repeatable(RegisterStorageRepeatableContainer.class)
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RegisterStorage {

    Class<? extends Enum<?>>[] value() default {};
}
