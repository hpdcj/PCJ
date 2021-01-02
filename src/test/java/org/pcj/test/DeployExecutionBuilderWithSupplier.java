package org.pcj.test;

import org.pcj.PCJ;
import org.pcj.StartPoint;

public class DeployExecutionBuilderWithSupplier implements StartPoint {

    private final String text;
    
    public DeployExecutionBuilderWithSupplier(String text){
        this.text = text;
    }

    @Override
    public void main() {
        System.out.println("text = " + text);
    }

    public static void main(String[] args) {
        PCJ.executionBuilder(DeployExecutionBuilderWithSupplier.class, () -> new DeployExecutionBuilderWithSupplier("Hello from supplied starting point!"))
                .addNode("localhost")
                .addNode("localhost")
                .deploy();
    }
}
