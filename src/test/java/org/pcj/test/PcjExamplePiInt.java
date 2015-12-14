package org.pcj.test;

import org.pcj.FutureObject;
import org.pcj.PCJ;
import org.pcj.Shared;
import org.pcj.StartPoint;
import org.pcj.Storage;

/**
 *
 * @author faramir
 */
public class PcjExamplePiInt extends Storage implements StartPoint {

    @Shared
    private double sum;

    @SuppressWarnings("method")
    private double f(double x) {
        return (4.0 / (1.0 + x * x));
    }

    @SuppressWarnings("method")
    @Override
    public void main() throws Throwable {
        double pi = 0.0;
        long time = System.currentTimeMillis();
        for (int i = 1; i < 1000; ++i) {
            pi = calc(1000000);
        }
        time = System.currentTimeMillis() - time;
        if (PCJ.myId() == 0) {
            double err = pi - Math.PI;
            PCJ.log(String.format("time %d\tsum, err = %7.5f, %10e\n", time, pi, err));
        }
    }

    @SuppressWarnings("method")
    private double calc(int N) {
        double w;

        w = 1.0 / (double) N;
        sum = 0.0;
        for (int i = PCJ.myId() + 1; i <= N; i += PCJ.threadCount()) {
            sum = sum + f(((double) i - 0.5) * w);
        }
        sum = sum * w;

        PCJ.barrier();
        if (PCJ.myId() == 0) {
            FutureObject[] data = new FutureObject[PCJ.threadCount()];
            for (int i = 1; i < PCJ.threadCount(); ++i) {
                data[i] = PCJ.getFutureObject(i, "sum");
            }
            for (int i = 1; i < PCJ.threadCount(); ++i) {
                sum = sum + (double) data[i].getObject();
            }

            return sum;
        } else {
            return 0;
        }
    }

    @SuppressWarnings("method")
    public static void main(String[] args) {
        PCJ.deploy(PcjExamplePiInt.class, PcjExamplePiInt.class, new String[]{
            "localhost:8091",
            "localhost:8092",
            "localhost:8092",
            "localhost:8093",});
    }
}
