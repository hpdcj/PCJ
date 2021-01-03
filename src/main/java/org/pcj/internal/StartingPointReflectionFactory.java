package org.pcj.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import org.pcj.StartPoint;
import org.pcj.StartPointFactory;

public class StartingPointReflectionFactory implements StartPointFactory {

  private final Class<? extends StartPoint> startPointClass;

  public StartingPointReflectionFactory(Class<? extends StartPoint> startPointClass) {
    if (isZeroArgumentConstructorMissing(startPointClass)) {
      throw new IllegalArgumentException("Provided starting point " + startPointClass + " has no zero-argument constructor");
    }
    this.startPointClass = startPointClass;
  }

  private boolean isZeroArgumentConstructorMissing(Class<? extends StartPoint> startPointClass) {
    return Arrays.stream(startPointClass.getConstructors()).noneMatch(constructor -> constructor.getParameterCount() == 0);
  }
  
  @Override
  public StartPoint create()
      throws NoSuchMethodException, InstantiationException, InvocationTargetException, IllegalAccessException, SecurityException, IllegalArgumentException {
    Constructor<? extends StartPoint> startPointClassConstructor = startPointClass.getConstructor();
    startPointClassConstructor.setAccessible(true);
    return startPointClassConstructor.newInstance();
  }

  @Override
  public String toString() {
    return "StartingPointReflectionFactory{" +
        "startPointClass=" + startPointClass +
        '}';
  }
}
