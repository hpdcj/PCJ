/* 
 * Copyright (c) 2016, HPDCJ
 * All rights reserved.
 * 
 * Licensed under New BSD License (3-clause license).
 * 
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.test;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.IntStream;
import org.pcj.NodesDescription;
import org.pcj.PCJ;
import org.pcj.StartPoint;

public class PcjMicroBenchmarkBarrier implements StartPoint {
    
    @Override
    public void main() {
        int number_of_tests = 10;
        int ntimes = 1000;
        
        PCJ.barrier();
        
        double tmin = Double.MAX_VALUE;
        for (int k = 0; k < number_of_tests; k++) {
            long rTime = System.nanoTime();
            
            for (int i = 0; i < ntimes; i++) {
                PCJ.barrier();
            }
            
            rTime = System.nanoTime() - rTime;
            double dtime = (rTime / (double) ntimes) * 1e-9;
            
            if (tmin > dtime) {
                tmin = dtime;
            }

//            System.out.println(PCJ.threadCount() + " " + t);
            PCJ.barrier();
        }
        
        if (PCJ.myId() == 0) {
            System.out.format("Barrier\t%5d\ttime\t%12.7f\n",
                    PCJ.threadCount(), tmin);
        }
    }
    
    public static void main(String[] args) {
        int[] threads = {1, 2, 4, 8, 12, 24, 48};
        
        String nodesFile = "nodes.txt";
        if (args.length > 0) {
            nodesFile = args[0];
        }
        Set<String> nodesSet = new LinkedHashSet<>();
        try (Scanner s = new Scanner(new File(nodesFile))) {
            while (s.hasNextLine()) {
                String node = s.nextLine();
                nodesSet.add(node);
            }
        } catch (IOException ex) {
            System.err.println(nodesFile + ": file not found");
            IntStream.range(0, 30)
                    .mapToObj(i -> "localhost:" + (9000 + i))
                    .forEach(nodesSet::add);
        }
        
        String[] nodesUniq = nodesSet.toArray(new String[0]);
        
        int nn = nodesUniq.length;
//        for (int nn = nodesUniq.length; nn > 0; nn = nn / 2) {
        for (int nt : threads) {
            String[] nodes = new String[nt * nn];
            System.out.printf(" Start deploy nn=%d nt=%d\n", nn, nt);
            int ii = 0;
            for (int i = 0; i < nn; i++) {
                for (int j = 0; j < nt; j++, ii++) {
                    nodes[ii] = nodesUniq[i];
                }
            }
            
//            PCJ.start(PcjMicroBenchmarkBarrier.class, new NodesDescription(nodes));
                PCJ.deploy(PcjMicroBenchmarkBarrier.class, new NodesDescription(nodes));
        }
//        }
    }
}
