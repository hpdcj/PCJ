/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Class loader for different PCJ threads.
 *
 * Purpose: the <i>same</i> class - different static data in
 * the class.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class PcjClassLoader extends java.lang.ClassLoader {

    public PcjClassLoader() {
        super(PcjClassLoader.class.getClassLoader());
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (name == null) {
            return null;
        }

        if (name.startsWith("java.")
                || name.startsWith("javax.")
                || name.startsWith("sun.")
                || name.startsWith("org.pcj.internal.")) {
            return loadByParent(name);
        }

        Class<?> c = findLoadedClass(name);
        if (c != null) {
            return c;
        }

        try {
            byte[] classBytes = findClassBytecode(name);
            if (classBytes != null) {
                c = defineClass(name, classBytes, 0, classBytes.length);
                if (c != null) {
                    return c;
                }
            }
        } catch (ClassFormatError ex) {
            ex.printStackTrace(System.err);
        }

        c = loadByParent(name);
        if (c != null) {
            return c;
        }

        throw new ClassNotFoundException("Class not found: " + name);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve)
            throws ClassNotFoundException {
        if (name == null) {
            return null;
        }
        Class<?> c = findClass(name);
        if (resolve) {
            resolveClass(c);
        }

        return c;
    }

    /**
     * Tries to read content of class into byte array
     * (<tt>byte[]</tt>).
     *
     * @param name The binary name of class
     * @return The byte array (<tt>byte[]</tt>) of loaded file
     * or <tt>null</tt>
     * if class is not found.
     */
    private byte[] findClassBytecode(String name) {
        String path = name.replace('.', '/') + ".class";

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (InputStream is = PcjClassLoader.getSystemResourceAsStream(path);
                BufferedInputStream bis = new BufferedInputStream(is)) {
            byte[] buffer = new byte[4 * 1024];
            int len;
            while ((len = bis.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
        } catch (IOException ex) {
            return null;
        }

        return baos.toByteArray();
    }

    /**
     * Tries to load class using parent PcjClassLoader or if
     * it is
     * <tt>null</tt>, system PcjClassLoader.
     *
     * @param name The binary name of class
     * @return The resulting <tt>Class</tt> object
     * @throws ClassNotFoundException exception is thrown when
     * class is not found
     */
    private Class<?> loadByParent(String name) throws ClassNotFoundException {
        if (getParent() != null) {
            return getParent().loadClass(name);
        } else {
            return getSystemClassLoader().loadClass(name);
        }
    }
}
