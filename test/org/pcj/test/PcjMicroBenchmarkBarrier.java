package org.pcj.test;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Scanner;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.pcj.PCJ;

public class PcjMicroBenchmarkBarrier
        extends org.pcj.Storage
        implements org.pcj.StartPoint {

    @Override
    public void main() {
        int number_of_tests = 5;
        int ntimes = 1000;

        PCJ.barrier();

        double tmin = Double.MAX_VALUE;
        for (int k = 0; k < number_of_tests; k++) {
            long rTime = System.nanoTime();

            for (int i = 0; i < ntimes; i++) {
                PCJ.barrier();
            }

            rTime = System.nanoTime() - rTime;
            double t = (rTime / (double) ntimes) * 1e-9;

            if (tmin > t) {
                tmin = t;
            }

            PCJ.log(PCJ.threadCount() + " " + t);
            PCJ.barrier();
        }

        if (PCJ.myId() == 0) {
            System.out.format(Locale.FRANCE,
                    "%5d \t time %7f%n",
                    PCJ.threadCount(), tmin);
        }
    }

    public static void main(String[] args) {
        int[] threads = {1, 2, 4, 8, 16, 32};

        Set<String> nodesSet = new LinkedHashSet<>();
        try (Scanner s = new Scanner(new File("nodes.txt"))) {
            while (s.hasNextLine()) {
                String node = s.nextLine();
                nodesSet.add(node);
            }
        } catch (IOException ex) {
            Logger.getLogger(
                    PcjMicroBenchmarkBarrier.class.getName())
                    .log(Level.SEVERE,
                            "Unable to load descriptor file",
                            ex);
            System.exit(1);
        }

        String[] nodesUniq = nodesSet.toArray(new String[0]);

        for (int nn = nodesUniq.length; nn > 0; nn = nn / 2) {
            for (int nt : threads) {
                String[] nodes = new String[nt * nn];
                System.out.printf(" Start deploy nn=%d nt=%d",
                        nn, nt);
                int ii = 0;
                for (int i = 0; i < nn; i++) {
                    for (int j = 0; j < nt; j++, ii++) {
                        nodes[ii] = nodesUniq[i];
                    }
                }
                PCJ.deploy(PcjMicroBenchmarkBarrier.class,
                        PcjMicroBenchmarkBarrier.class,
                        nodes);
            }
        }
    }
}
