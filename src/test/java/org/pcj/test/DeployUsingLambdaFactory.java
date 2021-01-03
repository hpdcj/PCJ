package org.pcj.test;

import org.pcj.PCJ;
import org.pcj.StartPoint;
import org.pcj.StartPointFactory;

public class DeployUsingLambdaFactory implements StartPoint {

    private final String text;
    private final int myId = PCJ.myId();
    private final int nodeId = PCJ.getNodeId();
    private final int nodeCount = PCJ.getNodeCount();

    public DeployUsingLambdaFactory(String text){
        this.text = text;
    }

    @Override
    public void main() {
        System.out.println("myId = " + myId + ", nodeId = " + nodeId + ", node count = " + nodeCount + ", text = " + text);
    }

    public static void main(String[] args) {
        String text = "Hello from supplied starting point!";
        PCJ.executionBuilder(() -> new DeployUsingLambdaFactory(text))
                .addNode("localhost")
                .addNode("localhost")
                .addNode("localhost")
                .deploy();
    }
}
