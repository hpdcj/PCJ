package org.pcj.test;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.pcj.PCJ;
import org.pcj.StartPoint;
import org.pcj.internal.network.MessageProc;


public class ReflectionStartingPointFactory {
    private static final Logger LOGGER = Logger.getLogger(MessageProc.class.getName());

    public static class NoZeroArgConstructor implements StartPoint {
    
        private final String text;
        private final int myId = PCJ.myId();
        private final int nodeId = PCJ.getNodeId();
        private final int nodeCount = PCJ.getNodeCount();
    
        public NoZeroArgConstructor(String text) {
            this.text = text;
        }
    
        @Override
        public void main() {
            System.out.println("No zero arg constructor: myId = " + myId + ", nodeId = " + nodeId + ", node count = " + nodeCount + ", text = " + text);
        }
    }
    
    public static class ZeroArgConstructor implements StartPoint {
    
        private final int myId = PCJ.myId();
        private final int nodeId = PCJ.getNodeId();
        private final int nodeCount = PCJ.getNodeCount();
    
        @Override
        public void main() {
            System.out.println("Zero arg constructor: myId = " + myId + ", nodeId = " + nodeId + ", node count = " + nodeCount);
        }
    }

    public static void main(String[] args) {
        LOGGER.log(Level.INFO, "Should throw illegal argument exception when provided with class not having zero-argument constructor");
        try {
            PCJ.executionBuilder(NoZeroArgConstructor.class)
                .addNode("localhost")
                .addNode("localhost")
                .addNode("localhost")
                .deploy();
            LOGGER.log(Level.SEVERE, "Expected IllegalArgumentException");
            assert(false);
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.INFO, "Caught expected exception");
        }

        LOGGER.log(Level.INFO, "Should not throw illegal argument exception when provided with class having zero-argument constructor");
        try {
            PCJ.executionBuilder(ZeroArgConstructor.class)
                .addNode("localhost")
                .addNode("localhost")
                .addNode("localhost")
                .deploy();
            LOGGER.log(Level.INFO, "Code did not throw any exception");
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.INFO, "Caught unexpected expected exception", e);
            assert(false);
        }
    }
}
