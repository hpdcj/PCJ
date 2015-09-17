package org.pcj.test;

/*
 * @author Piotr
 */
import java.io.File;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.util.Scanner;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.pcj.PCJ;
import org.pcj.Shared;
import org.pcj.StartPoint;
import org.pcj.Storage;

public class PcjMicroBenchmarkPingPongMemBench extends Storage implements StartPoint {

    static public class MemUsage implements Serializable {

        private long minMem = Long.MAX_VALUE;
        private long maxMem = 0;
        private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        public void start() {

            Runnable r = new Runnable(){@Override
            public void run(){
                long mem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
                if (mem < minMem) {
                    minMem = mem;
                }
                if (mem > maxMem) {
                    maxMem = mem;
                }
                System.gc();
            }};
            scheduler.scheduleAtFixedRate(r, 0, 1, TimeUnit.SECONDS);
        }

        public long[] get() {
            long[] arr = new long[2];
            arr[0] = minMem;
            arr[1] = maxMem;
            return arr;
        }

        public long[] stop() {
            scheduler.shutdown();
            return get();
        }
    }

    @Shared
    double[] a;

    @Override
    public void main() {
        MemUsage memUsage = new MemUsage();
        memUsage.start();

        int[] transmit = {//1, 10, 100, 1024, 2048, 4096, 8192, 16384,
//            32768, 65536, 131072, 262144, 524288, 1048576, 2097152,
            4194304, //            8388608, 16777216,
    };

        System.out.println("Maximum Heap Size: " + Runtime.getRuntime().maxMemory() + " B");

        final int ntimes = 100;
        final int number_of_tests = 5;
        double[] b;

        for (int j = 0; j < transmit.length; j++) {
            int n = transmit[j];

            PCJ.barrier();

            a = new double[n];
            b = new double[n];

            PCJ.barrier();

            for (int i = 0; i < n; i++) {
                a[i] = (double) i + 1;
            }
            PCJ.barrier();

            // put  
            PCJ.monitor("a"); // dodane
            PCJ.barrier();
            for (int i = 0; i < n; i++) {
                a[i] = 0.0d;
                b[i] = (double) i + 1;
            }

            double tmin_put = Double.MAX_VALUE;
            for (int k = 0; k < number_of_tests; k++) {
                long time = System.nanoTime();
                for (int i = 0; i < ntimes; i++) {
                    if (PCJ.myId() == 0) {
                        PCJ.put(1, "a", b);
                    } else {
                        PCJ.waitFor("a");
                    }
                }

                time = System.nanoTime() - time;
                double dtime = (time / (double) ntimes) * 1e-9;
                
//                PCJ.log(PCJ.threadCount() + " put " + dtime + " " + b[n - 1]);

                PCJ.barrier();
                if (tmin_put > dtime) {
                    tmin_put = dtime;
                }
            }

            if (PCJ.myId() == 0) {
                System.out.format(Locale.FRANCE, "%5d size %10f \t t_put %7f %n",
                        PCJ.threadCount(), (double) n / 128, tmin_put);
            }
            long[] arr = memUsage.get();
            PCJ.log(String.format(n + " minMem: %.2f MB\tmaxMem: %.2f MB\n", arr[0] / 1024. / 1024., arr[1] / 1024. / 1024.));

        }

        long[] arr = memUsage.stop();
        PCJ.log(String.format("minMem: %.2f MB\tmaxMem: %.2f MB\n", arr[0] / 1024. / 1024., arr[1] / 1024. / 1024.));
    }

    public static void main(String[] args) {
        String[] nodesTxt = new String[1024];
        Scanner nf = null;
        try {
            nf = new Scanner(new File(args.length > 0 ? args[0] : "nodes.txt"));
        } catch (FileNotFoundException ex) {
            System.err.println("File not found!");
        }
        System.out.println("Maximum Heap Size: " + Runtime.getRuntime().maxMemory() + " B");

        int n_nodes = 0;
        if (nf != null) {
            while (nf.hasNextLine()) {
                nodesTxt[n_nodes] = nf.nextLine();
                n_nodes++;
            }
        } else {
            for (int i = 0; i < 2; ++i) {
//                nodesTxt[n_nodes] = "localhost:" + (8091 + i);
                nodesTxt[n_nodes] = "localhost:" + (8097);
                n_nodes++;
            }
        }

        String[] nodes = new String[2];
        nodes[0] = nodesTxt[0];
        nodes[1] = nodesTxt[1];
        PCJ.deploy(PcjMicroBenchmarkPingPongMemBench.class, PcjMicroBenchmarkPingPongMemBench.class, nodes);
    }
}
