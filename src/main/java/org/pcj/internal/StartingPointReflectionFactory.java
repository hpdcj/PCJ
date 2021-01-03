package org.pcj.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import org.pcj.StartPoint;
import org.pcj.StartPointFactory;

public class StartingPointReflectionFactory<StartingPointT extends StartPoint> implements StartPointFactory<StartingPointT> {

  private final Class<StartingPointT> startPointClass;

  public StartingPointReflectionFactory(Class<StartingPointT> startPointClass) {
    if (isZeroArgumentConstructorMissing(startPointClass)) {
      throw new IllegalArgumentException("Provided starting point " + startPointClass + " has no zero-argument constructor");
    }
    this.startPointClass = startPointClass;
  }

  @Override
  public StartingPointT create()
      throws NoSuchMethodException, InstantiationException, InvocationTargetException, IllegalAccessException, SecurityException, IllegalArgumentException {
    Constructor<StartingPointT> startPointClassConstructor = startPointClass.getConstructor();
    startPointClassConstructor.setAccessible(true);
    return startPointClassConstructor.newInstance();
  }

  private boolean isZeroArgumentConstructorMissing(Class<StartingPointT> startPointClass) {
    return Arrays.stream(startPointClass.getConstructors()).noneMatch(constructor -> constructor.getParameterCount() == 0);
  }

  @Override
  public String toString() {
    return "StartingPointReflectionFactory{" +
        "startPointClass=" + startPointClass +
        '}';
  }
}
