/*
 * Test app
 */
package org.pcj.test;

import org.pcj.PCJ;

/**
 *
 * @author faramir
 */
public class EasyTest {

    public static void main(String[] args) {
        PCJ.deploy(EasyTestInner.class, EasyTestInner.class,
                new String[]{
                    "localhost:8091",
                    "localhost:8001",
                    "localhost:8002",
                    "localhost:8002",
                    "localhost:8002",
//                    "localhost:8003",
//                    "localhost:8003",
//                    "localhost:8003",
                });
//        System.setProperty("pcj.port", "8002");
//                PCJ.start(EasyTestInner.class, EasyTestInner.class,
//                new String[]{
//                    "localhost:8091",
////                    "localhost:8002",
////                    "localhost:8003",
//                    "localhost:8001",
////                    "localhost:8002",
////                    "localhost:8002",
//                    "localhost:8002",
////                    "localhost:8003",
////                    "localhost:8003",
////                    "localhost:8003",
//                });
    }
}