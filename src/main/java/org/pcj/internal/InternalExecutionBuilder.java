package org.pcj.internal;

import java.util.Properties;
import org.pcj.StartPoint;

public abstract class InternalExecutionBuilder {

    protected InternalExecutionBuilder() {
    }

    protected void start(Class<? extends StartPoint> startPoint, String[] nodes, Properties props) {
        InternalPCJ.start(startPoint, new InternalNodesDescription(nodes), props);
    }

    protected void deploy(Class<? extends StartPoint> startPoint, String[] nodes, Properties props) {
        DeployPCJ.deploy(startPoint, new InternalNodesDescription(nodes), props);
    }
}