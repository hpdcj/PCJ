package org.pcj.test;

import java.io.IOException;
import java.util.Scanner;
import java.util.function.Function;
import org.pcj.PCJ;
import org.pcj.PcjFuture;
import org.pcj.RegisterStorage;
import org.pcj.StartPoint;
import org.pcj.Storage;

@RegisterStorage//(PcjExample.Shareable.class)
public class PcjExamplePiIntReduce implements StartPoint {

  private static final Function<Double, Double> f
                               = x -> 4.0 / (1 + x * x);
  @Storage
  enum Shareable {n, area}

  private int n;
  private double area;

  public static void main(String[] args) throws IOException {
    PCJ.executionBuilder(PcjExamplePiIntReduce.class)
            .addNode("localhost")
            .addNode("localhost")
            .addNode("localhost")
            .addNode("localhost")
//            .addNodes(new File("nodes.txt"))
            .deploy(); 
  }

  @Override
  public void main() throws Throwable {
    if (PCJ.myId() == 0) {
      int n = new Scanner("100000000").nextInt();
      PCJ.broadcast(n, Shareable.n); 
    }
    PCJ.waitFor(Shareable.n); 

    double width = 1.0 / n;
    for (int i = PCJ.myId(); i < n; i += PCJ.threadCount()) {
      area += f.apply((i + 0.5) * width);
    }
    area *= width;

    PcjFuture<Void> barrier = PCJ.asyncBarrier();
    if (PCJ.myId() == 0) {
      barrier.get();
      double s = PCJ.reduce(Double::sum, Shareable.area); 
      System.out.println(s);
    }
  }
}