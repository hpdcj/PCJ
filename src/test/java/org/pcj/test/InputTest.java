/*
 * Copyright (c) 2016, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.test;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import org.pcj.ExecutionBuilder;
import org.pcj.PCJ;
import org.pcj.StartPoint;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class InputTest implements StartPoint {

    public static void main(String[] args) throws IOException {
        ExecutionBuilder builder = PCJ.executionBuilder(InputTest.class);

        if (args.length > 0) {
            builder.addNodes(new File(args[0]));
        } else {
            String[] nodes = {
                    "localhost",
                    "localhost:8002",
            };
            builder.addNodes(nodes);
        }

        builder.deploy();
    }

    @Override
    public void main() throws Throwable {
        if (PCJ.myId() == 0) {
            Scanner sc = new Scanner(System.in);
            while (sc.hasNext()) {
                System.out.println(PCJ.myId() + ": " + sc.next());
            }
            System.out.println(PCJ.myId() + ": Done.");
        }
    }
}