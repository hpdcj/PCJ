/* 
 * Copyright (c) 2016, HPDCJ
 * All rights reserved.
 * 
 * Licensed under New BSD License (3-clause license).
 * 
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.test;

/*
 * @author Piotr
 */
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.pcj.NodesDescription;
import org.pcj.PCJ;
import org.pcj.RegisterStorage;
import org.pcj.StartPoint;
import org.pcj.Storage;
import org.pcj.test.PcjMicroBenchmarkBroadcast.SharedEnum;

@RegisterStorage(SharedEnum.class)
public class PcjMicroBenchmarkBroadcast implements StartPoint {

    @Storage(PcjMicroBenchmarkBroadcast.class)
    enum SharedEnum {
        a
    }
    double[] a;

    @Override
    public void main() {

        int[] transmit = {
            1, 10, 100, 1024, 2048, 4096, 8192, 16348,
            32768, 65536, 131072, 262144, 524288,
            1048576,
            2097152,
            4194304,
        };

        for (int n : transmit) {
            if (PCJ.myId() == 0) {
                System.out.println("n=" + n);
            }
            PCJ.barrier();

            double[] b = new double[n];
            for (int i = 0; i < n; i++) {
                b[i] = (double) i + 1;
            }
            PCJ.monitor(SharedEnum.a);

            PCJ.barrier();

            int ntimes = 100;

            long time = System.nanoTime();

            for (int i = 0; i < ntimes; i++) {
//                if (PCJ.myId() == i % PCJ.threadCount()) {
                if (PCJ.myId() == 0) {
                    PCJ.broadcast(b, SharedEnum.a);
//                    PCJ.asyncBroadcast(b, SharedEnum.a);
                }
//                PCJ.waitFor(SharedEnum.a);
//                PCJ.barrier();
            }

            time = System.nanoTime() - time;
            double dtime = (time / (double) ntimes) * 1e-9;
            PCJ.barrier();

//            System.out.println(PCJ.threadCount() + " " + time + " " + a[n - 1]);
            if (PCJ.myId() == 0) {
                System.out.format(Locale.FRANCE, "%5d size %.10f time %.7f %n",
                        PCJ.threadCount(), (double) n / 128, dtime);
            }
        }
    }

    public static void main(String[] args) {
//        Level level = Level.CONFIG;
//        Level level = Level.FINEST;
//        Logger logger = Logger.getLogger("");
//        Arrays.stream(logger.getHandlers()).forEach(handler -> handler.setLevel(level));
//        logger.setLevel(level);

        String nodesDescription = "nodes.uniq";
        if (args.length > 0) {
            nodesDescription = args[0];
        }
        Scanner nf = null;
        try {
            nf = new Scanner(new File(nodesDescription));
        } catch (FileNotFoundException ex) {
            System.err.println("File not found: " + nodesDescription);
        }

        //        int[] threads = {1, 4, 16};
        int maxNodes = 2;
        int[] threads = {4};
        // run.jvmargs=-Xmx64m

        String[] nodesUniq = new String[maxNodes * Arrays.stream(threads).max().orElse(1)];

        int n_nodes = 0;
        if (nf != null) {
            while (nf.hasNextLine()) {
                nodesUniq[n_nodes] = nf.nextLine() + ":8091";
                System.out.println(nodesUniq[n_nodes]);
                n_nodes++;
            }
        } else {
            for (int i = 0; i < maxNodes; ++i) {
                nodesUniq[n_nodes] = "localhost:" + (8091 + i);
                n_nodes++;
            }
        }

//        for (int m = n_nodes; m > 0; m = m / 2) 
        int m = n_nodes;
        {
            int nn = m;

            for (int nt : threads) {
                String[] nodes = new String[nt * nn];
                System.out.println(" Start deploy nn=" + nn + " nt=" + nt);
                int ii = 0;
                for (int i = 0; i < nn; i++) {
                    for (int j = 0; j < nt; j++) {
                        nodes[ii] = nodesUniq[i];
                        ii++;

                    }
                }

                PCJ.deploy(PcjMicroBenchmarkBroadcast.class,
                        new NodesDescription(nodes));
            }
        }
    }
}
