/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pcj.internal;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author faramir
 */
public class LargeByteArray {

    private final List<byte[]> bytesList;
    private long length;

    public LargeByteArray() {
        length = 0;

        bytesList = new ArrayList<>();
    }

    public long getLength() {
        return length;
    }

    public void addBytes(byte[] bytes) {
        bytesList.add(bytes);
        length += bytes.length;
    }

    public List<byte[]> getBytesList() {
        return bytesList;
    }
}
