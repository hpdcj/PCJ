/*
 * Copyright (c) 2011-2019, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj;

import java.io.Serializable;
import java.util.function.BinaryOperator;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
@FunctionalInterface
public interface ReduceOperation<T> extends Serializable, BinaryOperator<T> {
}