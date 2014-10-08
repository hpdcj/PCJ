/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.utils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * Class contains useful static method for checking if address
 * is an local address.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class NetworkUtils {

    public static boolean isLocalAddress(String host) {
        try {
            InetAddress ia = InetAddress.getByName(host);

            if (ia.isAnyLocalAddress() || ia.isLoopbackAddress()) {
                return true;
            }

            return NetworkInterface.getByInetAddress(ia) != null;
        } catch (UnknownHostException | SocketException ex) {
            return false;
        }
    }
}
