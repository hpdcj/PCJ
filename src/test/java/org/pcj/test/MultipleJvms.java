/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pcj.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author faramir
 */
public class MultipleJvms {

    public static void main(String[] args) throws IOException, InterruptedException {
        String classpath = System.getProperty("java.class.path");

        List<Process> processes = new ArrayList<>(args.length);
        for (int i = 0; i < args.length; ++i) {
            List<String> command = Arrays.asList(
                    "java",
                    "-cp",
                    classpath,
                    "-Dpcj.chunksize=8900",
                    "-Dpcj.port=" + args[i],
                    "org.pcj.test.EasyTest",
                    "nodes.txt"
            );
            System.err.println(command);
            Process process = new ProcessBuilder(command)
                    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .start();
            process.getOutputStream().close();
            processes.add(process);
        }
        for (Process process : processes) {
            process.waitFor();
        }
//        while (!processes.isEmpty()) {
//            Iterator<Process> it = processes.iterator();
//            while (it.hasNext()) {
//                Process process = it.next();
//                if (process.isAlive() == false) {
//                    int exitCode = process.waitFor();
//                    System.err.println(process + " is dead with EXIT_CODE: "+exitCode);
//                    it.remove();
//                }
//            }
////            java.util.concurrent.locks.LockSupport.parkNanos(2_000_000_000);
//            java.util.concurrent.TimeUnit.SECONDS.sleep(1);
//        }
    }
}
