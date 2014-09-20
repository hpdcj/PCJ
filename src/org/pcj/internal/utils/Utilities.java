/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.utils;

import java.util.List;

/**
 * This class contains some useful static methods like
 * converting List of Integer into array of int.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class Utilities {

    public static int[] listAsArray(List<Integer> list) {
        int[] array = new int[list.size()];
        int i = 0;
        for (int e : list) {
            array[i++] = e;
        }
        return array;
    }

    /**
     * Creates new instance of class
     *
     * @param className name of class
     * @param classType type of class
     * @return new instance of class
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    public static <T> T newInstanceFromClassName(String className, Class<T> classType)
            throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        @SuppressWarnings("unchecked")
        final Class<T> clazz = (Class<T>) Class.forName(className).asSubclass(classType);
        return clazz.newInstance();
    }

    /**
     * Pauses current thread for specified amount of time
     * without throwing an exception.
     *
     * @param millis the length of time to sleep in
     * milliseconds nanos
     * @see java.lang.Thread#sleep(long)
     */
    public static void sleep(long millis) {
        long stop = System.currentTimeMillis() + millis;
        do {
            try {
                Thread.sleep(stop - System.currentTimeMillis());
            } catch (InterruptedException ex) {
                ex.printStackTrace(System.err);
            }
        } while (System.currentTimeMillis() < stop);
    }
}
