/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Class for representing tree structure of nodes.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class CommunicationTree {

    private Integer root;
    private Integer parent;
    private List<Integer> children;

    public CommunicationTree() {
        children = new ArrayList<>();
    }

    public Integer getRoot() {
        return root;
    }

    public void setRoot(Integer root) {
        this.root = root;
    }

    public Integer getParent() {
        return parent;
    }

    public void setParent(Integer parent) {
        this.parent = parent;
    }

    public List<Integer> getChildren() {
        return children;
    }

    public void setChildren(List<Integer> children) {
        this.children = children;
    }
}
