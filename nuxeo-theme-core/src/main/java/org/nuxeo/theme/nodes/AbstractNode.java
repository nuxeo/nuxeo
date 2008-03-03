/*
 * (C) Copyright 2006-2007 Nuxeo SAS <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jean-Marc Orliaguet, Chalmers
 *
 * $Id$
 */

package org.nuxeo.theme.nodes;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractNode implements Node {

    private Node parent;

    private List<Node> children = new ArrayList<Node>();

    public void clearParent() {
        parent = null;
    }

    public void setParent(Node parent) {
        if (this.parent != null) {
            List<Node> siblings = this.parent.getChildren();
            siblings.remove(this);
            this.parent.setChildren(siblings);
        }
        this.parent = parent;
    }

    public Node getParent() {
        return parent;
    }

    public Node addChild(Node node) {
        children.add(node);
        node.setParent(this);
        return node;
    }

    public void removeChild(Node node) {
        children.remove(node);
        node.setParent(null);
    }

    public List<Node> getChildren() {
        return children;
    }

    public void setChildren(List<Node> children) {
        this.children = children;
    }

    public abstract NodeTypeFamily getNodeTypeFamily();

    public boolean isLeaf() {
        return getNodeTypeFamily() == NodeTypeFamily.LEAF;
    }

    public Integer getOrder() {
        Integer order = null;
        if (parent != null) {
            order = parent.getChildren().indexOf(this);
        }
        return order;
    }

    public void setOrder(Integer order) {
        List<Node> siblings = parent.getChildren();
        siblings.add(order, this);
        parent.setChildren(siblings);
    }

    public void moveTo(Node container, Integer order) {
        setParent(container);
        setOrder(order);
    }

    public void insertAfter(Node node) {
        node.getParent().addChild(this);
        moveTo(node.getParent(), node.getOrder() + 1);
    }

    public boolean hasSiblings() {
        if (parent == null) {
            return false;
        }
        return parent.getChildren().size() > 1;
    }

    public Node getNextNode() {
        int order = getOrder();
        List<Node> siblings = parent.getChildren();
        if (order + 1 >= siblings.size()) {
            return null;
        }
        return siblings.get(order + 1);
    }

    public Node getPreviousNode() {
        int order = getOrder();
        if (order == 0) {
            return null;
        }
        List<Node> siblings = parent.getChildren();
        return siblings.get(order - 1);
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }

    public boolean isChildOf(Node node) {
        boolean res = false;
        Node parent = this.parent;
        while (parent != null) {
            if (parent == node) {
                res = true;
                break;
            }
            parent = parent.getParent();
        }
        return res;
    }

    public void removeDescendants() {
        for (Node child : children) {
            child.removeDescendants();
            child.clearParent();
        }
        children.clear();
    }

    public List<Node> getDescendants() {
        List<Node> descendants = new ArrayList<Node>();
        collectDescendants(descendants);
        return descendants;
    }

    public void collectDescendants(List<Node> nodes) {
        for (Node child : children) {
            nodes.add(child);
            child.collectDescendants(nodes);
        }
    }

}
