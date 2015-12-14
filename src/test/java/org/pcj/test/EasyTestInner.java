/*
 * This file is the test part of the PCJ Library
 */
package org.pcj.test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.pcj.Group;
import org.pcj.PCJ;
import org.pcj.Shared;
import org.pcj.StartPoint;
import org.pcj.Storage;

/**
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class EasyTestInner extends Storage implements StartPoint {

    private final static AtomicInteger atomic = new AtomicInteger(Integer.MAX_VALUE);
    /*
     * Shared fields
     */
    @Shared
    private int A;
    @Shared
    private int AB;
//    java.util.Random R = new java.util.Random(1010);

    public static class Btest implements Serializable {

        private int a;
        private int[] c = new int[12];
        private Object b = c;
        private List<Integer> d = new ArrayList<>();
    }

    @Shared
    private Btest B = new Btest();

    @Override
    public void main() {
        {
            int min;
            do {
                min = atomic.get();
                if (PCJ.myId() > min) {
                    break;
                }
            } while (atomic.compareAndSet(min, PCJ.myId()) == false);
        }

        PCJ.log("EasyTest.main()");
        PCJ.log("myNode:   " + PCJ.myId());
        PCJ.barrier();
        PCJ.log("numNodes:   " + PCJ.threadCount());

        PCJ.log("myNode: " + PCJ.myId() + ", numNodes: " + PCJ.threadCount());
        Group g = PCJ.join("group" + (PCJ.myId() % 2));
        PCJ.log(PCJ.myId() + ": I'm in group " + (PCJ.myId() % 2) + " as " + g.myId() + " (size: " + g.threadCount() + ")");
        PCJ.barrier();
        PCJ.log("In group '" + g.getGroupName() + "': " + g.threadCount() + " [groupNodeId:" + g.myId() + "/globalNodeId:" + PCJ.myId() + "]");
        if (PCJ.myId() == 0) {
            PCJ.log("broadcasting...");
            PCJ.broadcast("A", 0b010101);
            PCJ.log("syncWith(1)");
            PCJ.barrier(1);
            PCJ.log("synced(1)");
        }
        if (PCJ.myId() == 1) {
            PCJ.log("syncWith(0)");
            PCJ.barrier(0);
            PCJ.log("synced(0)");
        }
        for (int j = 0; j < 1_000; ++j) {
            //System.err.println(PCJ.myNode()+"> round "+j);
            if (PCJ.myId() == 0) {
                for (int i = 1; i < PCJ.threadCount(); ++i) {
                    //System.err.println("0: sync "+i);
                    PCJ.barrier(i);
                }
            } else {
                //System.err.println(PCJ.myNode()+": sync 0");
                PCJ.barrier(0);
            }
        }
        System.out.printf("[%d] sync ok\n", PCJ.myId());

        PCJ.barrier();
        if (PCJ.threadCount() >= 3 && PCJ.myId() <= 2) {
            for (int j = 0; j <= 3333; ++j) {
//                System.err.println(PCJ.myNode()+"> round "+j);
                if (PCJ.myId() == 0) {
//                    for (int i = 1; i < PCJ.numNodes(); ++i) {
//                    System.err.println("["+j+"]"+"0: sync "+(j%2+1));
                    PCJ.barrier(j % 2 + 1);
                    PCJ.waitFor("A");
//                    System.err.println("["+j+"]"+PCJ.myNode()+": waitFor(A)");

//                    System.err.println("["+j+"]"+"0: synced "+(j%2+1));
//                    }
                } else {
                    if (PCJ.myId() == j % 2 + 1) {
//                        System.err.println("["+j+"]"+PCJ.myNode()+": sync 0");
                        PCJ.barrier(0);
                        PCJ.put(0, "A", j);
//                        System.err.println("["+j+"]"+PCJ.myNode()+": putLocal(A)");

//                        System.err.println("["+j+"]"+PCJ.myNode()+": synced 0");
                    }
                }
            }
        }
        System.out.printf("[%d] wait ok\n", PCJ.myId());
        PCJ.barrier();

        System.out.printf("[%d] bef sync\n", PCJ.myId());
        PCJ.barrier();
        System.out.printf("[%d] A=%d\n", PCJ.myId(), A);

        if (PCJ.threadCount() >= 2 && PCJ.myId() == 0) {
            PCJ.log("Put 0x10 by 0 to node 1 to variable 'A'");
            PCJ.put(1, "A", 0x10);
        }

        PCJ.barrier();
        System.out.printf("[%d] A=%d\n", PCJ.myId(), A);

        if (PCJ.threadCount() >= 2 && PCJ.myId() == 0) {
            PCJ.log("Get 'A' by 0 from 1");
            A = PCJ.getFutureObject(1, "A").getObject();
        }

        PCJ.barrier();
        System.out.printf("[%d] A=%d\n", PCJ.myId(), A);
        if (PCJ.myId() == 0 && PCJ.threadCount() > 1) {
            Btest b = PCJ.getFutureObject(1, "B").getObject();
            //  PCJ.log("b.c = " + b.c.length);
            System.out.printf("[%d] b.a = %d\n", PCJ.myId(), b.a);
            //b.c = new int[1];
            b.a = 1;
            PCJ.put(1, "B", b);
        }
        PCJ.barrier();
        if (PCJ.myId() == 1) {
            Btest b = PCJ.getFutureObject(1, "B").getObject();
            System.out.printf("[%d] b.a = %d\n", PCJ.myId(), b.a);
        }

        PCJ.log("atomic: " + atomic.get());
    }
}
