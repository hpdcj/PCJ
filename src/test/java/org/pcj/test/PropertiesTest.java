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
import org.pcj.ExecutionBuilder;
import org.pcj.PCJ;
import org.pcj.StartPoint;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class PropertiesTest implements StartPoint {

    public static void main(String[] args) throws IOException {
        ExecutionBuilder builder = PCJ.executionBuilder(PropertiesTest.class);

        if (args.length > 0) {
            builder.addNodes(new File(args[0]));
        } else {
            String[] nodes = {
                    "localhost",
//                    "localhost",
//                    "localhost:8002",
//                    "localhost:8003",
//                    "localhost:8004",
//                    "localhost:8003",
//                    "localhost:8005",
//                    "localhost:8006",
//                    "localhost:8007",
//                    "localhost:8008",
//                    "localhost:8008",
//                    "localhost:8008",
//                    "localhost:8008",
//                    "localhost:8009", // run.jvmargs=-Xmx64m
                    //                    "localhost:8010", "localhost:8011", "localhost:8012", "localhost:8013", "localhost:8014",
                    //                    "localhost:8015", "localhost:8016", "localhost:8017", "localhost:8018", "localhost:8019",
                    //                    "localhost:8020", "localhost:8021", "localhost:8022", "localhost:8023", "localhost:8024",
                    //                    "localhost:8025", "localhost:8026", "localhost:8027", "localhost:8028", "localhost:8029",
                    //                    "localhost:8030", "localhost:8031", "localhost:8032", "localhost:8033", "localhost:8034",
                    //                    "localhost:8035", "localhost:8036", "localhost:8037", "localhost:8038", "localhost:8039",
                    //                    "localhost:8040", "localhost:8041", "localhost:8042", "localhost:8043", "localhost:8044",
                    //                    "localhost:8045", "localhost:8046", "localhost:8047", "localhost:8048", "localhost:8049",
            };
            builder.addNodes(nodes);
        }
        builder
                .addProperty("klucz", "Wartość\n#wielol ini\njkowa?")
                .addProperty("#drugi", "trzeci")
                .addProperty("No dobra", "teraz test kilku Ṫ spacji a n\uAB4Dwet \t tabulacji po spacji\n nowej linii")
                .addProperty("klucz\nz kilkoma znakami\nnowej linii bez wartości", "")
                .addProperty("\u0040\uFFFF", "\uEFFE\uC1C2");
        builder.deploy();

        ExecutionBuilder clone = builder.clone();
        clone.addProperty("#drugi", "zmieniony");
        clone.deploy();

        builder.deploy();
    }

    @Override
    public void main() throws Throwable {
        for (int i = 0; i < PCJ.threadCount(); ++i) {
            if (PCJ.myId() == i) {
                Thread.sleep(1000);
                System.out.printf("--- %d ---%n", i);
                PCJ.getProperties().forEach((k, v) -> System.out.printf("'%s'->'%s'%n", k, v));
            }
            PCJ.barrier();
        }
    }
}