/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.network;

/**
 * Enum with types of possible changes in socket interest for
 * Selector.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public enum ChangeRequestType {

    REGISTER,
    CHANGEOPS;
}
