/*
 * Copyright (c) 2011-2016, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.test;

import org.pcj.NodesDescription;
import org.pcj.PCJ;

/**
 *
 * @author faramir
 */
public class StorageExampleMain{

    public static void main(String[] args) {
        PCJ.deploy(StorageExample.class,
                new NodesDescription(new String[]{"localhost", "localhost", "localhost"})
        );
    }
}
