/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pcj.internal.network;

import org.pcj.internal.LargeByteArray;
import java.io.InputStream;
import java.util.Iterator;

/**
 *
 * @author faramir
 */
public class LargeByteArrayInputStream extends InputStream {

    private final Iterator<byte[]> iterator;
    private int index = 0;
    private byte[] bytes;

    public LargeByteArrayInputStream(LargeByteArray largeByteArray) {
        iterator = largeByteArray.getBytesList().iterator();
        index = 0;
        bytes = new byte[0];

    }

    @Override
    public int read() {
        if (index < bytes.length) {
            return ((int)bytes[index++])&0xFF;
        }
        while (iterator.hasNext()) {
            bytes = iterator.next();
            index = 0;
            if (index < bytes.length) {
                return ((int)bytes[index++])&0xFF;
            }
        }
        return -1;
    }
}
