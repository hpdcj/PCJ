package org.pcj.test;

import org.pcj.PCJ;
import org.pcj.StartPoint;

public class StartExecutionBuilderWithSupplier implements StartPoint {

    private final String text;
    
    public StartExecutionBuilderWithSupplier(String text){
        this.text = text;
    }

    @Override
    public void main() {
        System.out.println("text = " + text);
    }

    public static void main(String[] args) {
        PCJ.executionBuilder(StartExecutionBuilderWithSupplier.class, () -> new StartExecutionBuilderWithSupplier("Hello from supplied starting point!"))
                .addNode("localhost")
                .addNode("localhost")
                .start();
    }
}
