/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.utils;

/**
 * Class that reads version of the library from the Manifest.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class Version {

    public static final String version;
    public static final String builtDate;

    static {
        Package p = Version.class.getPackage();
        version = p.getImplementationVersion() == null ? "UNKNOWN" : p.getImplementationVersion();
        builtDate = p.getImplementationTitle() == null ? "UNKNOWN" : p.getImplementationTitle();
    }
}
