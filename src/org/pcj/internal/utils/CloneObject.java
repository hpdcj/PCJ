/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;

/**
 * Class for cloning object using serialization.
 *
 * It uses proper ClassLoader for object, so it is possible to
 * move objects between different ClassLoaders space.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class CloneObject {

    static public Object clone(Object object, ClassLoader loader) throws ClassNotFoundException, IOException {
        FastOutputStream fos = serialize0(object);
        return deserialize(fos.getFastInputStream(), loader);
    }

    static private FastOutputStream serialize0(Object object) {
        try (FastOutputStream fos = new FastOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(object);
            return fos;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    static public byte[] serialize(Object object) {
        return serialize0(object).toByteArray();
    }

    static private Object deserialize(InputStream fis, ClassLoader loader) throws ClassNotFoundException, IOException {
        try (ObjectClassLoaderInputStream mis = new ObjectClassLoaderInputStream(fis, loader)) {
            return mis.readObject();
        }
    }

    static public Object deserialize(byte[] bytes, ClassLoader loader) throws ClassNotFoundException, IOException {
        return deserialize(new FastInputStream(bytes, bytes.length), loader);
    }

    private static final class ObjectClassLoaderInputStream extends java.io.ObjectInputStream {

        final private ClassLoader loader;

        public ObjectClassLoaderInputStream(InputStream in, ClassLoader loader) throws IOException {
            super(in);
            this.loader = loader;
        }

        @Override
        final protected Class<?> resolveClass(ObjectStreamClass classDesc) throws IOException, ClassNotFoundException {
            if (loader != null) {
                Class<?> clazz = Class.forName(classDesc.getName(), false, loader);

                if (clazz != null) {
                    return clazz;
                }
            }
            return super.resolveClass(classDesc);
        }
    }
}
