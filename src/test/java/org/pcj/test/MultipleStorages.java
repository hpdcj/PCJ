package org.pcj.test;

import org.pcj.NodesDescription;
import org.pcj.PCJ;
import org.pcj.RegisterStorage;
import org.pcj.StartPoint;
import org.pcj.Storage;

@RegisterStorage({MultipleStorages.Vars.class, MultipleStorages.Vars.class})
@RegisterStorage(MultipleStorages.Vars.class)
public class MultipleStorages implements StartPoint {

    @Storage(MultipleStorages.class)
    enum Vars {v}

    private static int V = 0;
    private int v = ++V;
    
    private final String test;
    
    public MultipleStorages(String test){
        this.test = test;
    }

    @Override
    public void main() {
        System.out.println("v = " + v + ", test = " + test);
        PCJ.registerStorage(MultipleStorages.Vars.class, this);
        System.out.println("v = " + v + ", test = " + test);
        PCJ.registerStorage(MultipleStorages.Vars.class, this);
        System.out.println("v = " + v + ", test = " + test);
    }

    public static void main(String[] args) {
        PCJ.executionBuilder(MultipleStorages.class, () -> new MultipleStorages("Some text"))
                .addNode("localhost")
                .addNode("localhost")
                .start();
    }
}
