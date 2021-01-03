package org.pcj;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;

/**
 * Encapsulates StartPoint object creation when type has no zero-argument constructor.
 * Implementation of this interface should have serializable fields so system is able to share it with other nodes.
 */
@FunctionalInterface
public interface StartPointFactory<StartPointT extends StartPoint> extends Serializable {
    StartPointT create() throws NoSuchMethodException, InstantiationException, InvocationTargetException, IllegalAccessException, SecurityException, IllegalArgumentException;
}
