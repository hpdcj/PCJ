package org.pcj.test;

import org.pcj.PCJ;
import org.pcj.StartPoint;
import org.pcj.StartPointFactory;

public class DeployUsingFactory implements StartPoint {

    private final String text;
    private final int myId = PCJ.myId();
    private final int nodeId = PCJ.getNodeId();
    private final int nodeCount = PCJ.getNodeCount();

    public DeployUsingFactory(String text){
        this.text = text;
    }

    @Override
    public void main() {
        System.out.println("myId = " + myId + ", nodeId = " + nodeId + ", node count = " + nodeCount + ", text = " + text);
    }

    public static void main(String[] args) {
        PCJ.executionBuilder(new DeployExecutionBuilderWithSupplierFactory("Hello from supplied starting point!"))
                .addNode("localhost")
                .addNode("localhost")
                .addNode("localhost")
                .deploy();
    }

    public static class DeployExecutionBuilderWithSupplierFactory implements StartPointFactory {

        private final String someText;

        public DeployExecutionBuilderWithSupplierFactory(String someText) {
            this.someText = someText;
        }

        @Override
        public DeployUsingFactory create() {
            return new DeployUsingFactory(someText);
        }
    }
}
