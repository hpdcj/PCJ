package org.pcj.test;

/*
 * @author Piotr
 */
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.Locale;
import org.pcj.PCJ;
import org.pcj.Shared;
import org.pcj.StartPoint;
import org.pcj.Storage;

public class PcjMicroBenchmarkBroadcast extends Storage implements StartPoint {

    @Shared
    double[] a;

    @Override
    public void main() {

        int[] transmit = {1, 10, 100, 1024, 2048, 4096, 8192};
//            ,16348, 32768, 65536, 131072, 262144, 524288,
//            1048576};
        //    int[] transmit = { 131072 }; 

        for (int j = 0; j < transmit.length; j++) {
            int n = transmit[j];
            PCJ.barrier();

            double[] b = new double[n];
            for (int i = 0; i < n; i++) {
                b[i] = (double) i + 1;
            }
            PCJ.monitor("a");

            PCJ.barrier();

            int ntimes = 100;

            double time = System.nanoTime();

            for (int i = 0; i < ntimes; i++) {
                if (PCJ.myId() == 0) {
                    PCJ.broadcast("a", b);
                }
                PCJ.waitFor("a");
                PCJ.barrier();
            }

            time = System.nanoTime() - time;
            time = (time / (double) ntimes) * 1e-9;
            PCJ.barrier();

            PCJ.log(PCJ.threadCount() + " " + time + " " + a[n - 1]);
            if (PCJ.myId() == 0) {
                System.out.format(Locale.FRANCE, "%5d size %10f time %f7 %n",
                        PCJ.threadCount(), (double) n / 128, time);
            }
        }
    }

    public static void main(String[] args) {
        int[] threads = {1, 4, 16};

        String[] nodesUniq = new String[1024];
        Scanner nf = null;
        try {
            nf = new Scanner(new File("nodes.uniq"));
        } catch (FileNotFoundException ex) {
        }

        int n_nodes = 0;
        if (nf != null) {
            while (nf.hasNextLine()) {
                nodesUniq[n_nodes] = nf.nextLine() + ":9100";
                System.out.println(nodesUniq[n_nodes]);
                n_nodes++;
            }
        } else {
            for (int i = 0; i < 5; ++i) {
                nodesUniq[n_nodes] = "localhost:910" + i;
                n_nodes++;
            }
        }

        int nn;
        nn = n_nodes;
        int nt;

        for (int m = n_nodes; m > 0; m = m / 2) {
            nn = m;

            for (int n = 0; n < threads.length; n++) {
                nt = threads[n];
                String[] nodes = new String[nt * nn];
                System.out.println(" Start deploy nn=" + nn + " nt=" + nt);
                int ii = 0;
                for (int i = 0; i < nn; i++) {
                    for (int j = 0; j < nt; j++) {
                        nodes[ii] = nodesUniq[i];
                        //       System.out.println(ii + " " + nodes[ii]);
                        ii++;

                    }
                }
                //System.out.println(" deploy ");
                PCJ.deploy(PcjMicroBenchmarkBroadcast.class, PcjMicroBenchmarkBroadcast.class, nodes);
            }
        }
    }
}
