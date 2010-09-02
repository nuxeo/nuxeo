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

    private Node parentNode;

    private List<Node> childrenNodes = new ArrayList<Node>();

    public void clearParent() {
        parentNode = null;
    }

    public void setParent(Node parent) throws NodeException {
        if (equals(parent)) {
            throw new NodeException(String.format(
                    "A node cannot be made a parent of itself: %s.", this));
        }
        if (parent != null && parent.isChildOf(this)) {
            throw new NodeException(String.format(
                    "Cycle detected while trying to make %s a parent of %s.",
                    parent, this));
        }
        if (parentNode != null) {
            List<Node> siblings = parentNode.getChildren();
            siblings.remove(this);
            parentNode.setChildren(siblings);
        }
        parentNode = parent;
    }

    public Node getParent() {
        return parentNode;
    }

    public Node addChild(Node node) throws NodeException {
        if (equals(node)) {
            throw new NodeException(String.format(
                    "A node cannot be made a child of itself: %s.", this));
        }
        if (isChildOf(node)) {
            throw new NodeException(String.format(
                    "Cycle detected while trying to add child %s to %s.", node,
                    this));
        }
        childrenNodes.add(node);
        node.setParent(this);
        return node;
    }

    public void removeChild(Node node) throws NodeException {
        if (!childrenNodes.contains(node)) {
            throw new NodeException(String.format(
                    "Trying to remove unexisting child %s of %s", node, this));
        }
        childrenNodes.remove(node);
        node.setParent(null);
    }

    public List<Node> getChildren() {
        return childrenNodes;
    }

    public void setChildren(List<Node> children) throws NodeException {
        for (Node child : children) {
            if (equals(child)) {
                throw new NodeException(String.format(
                        "Node %s cannot be made a child of itself", child));
            }
            if (isChildOf(child)) {
                throw new NodeException(String.format(
                        "Cycle detected while trying to set children of %s.",
                        this));
            }
        }
        childrenNodes = children;
    }

    public abstract NodeTypeFamily getNodeTypeFamily();

    public boolean isLeaf() {
        return getNodeTypeFamily() == NodeTypeFamily.LEAF;
    }

    public Integer getOrder() {
        Integer order = null;
        if (parentNode != null) {
            order = parentNode.getChildren().indexOf(this);
        }
        return order;
    }

    public void setOrder(Integer order) throws NodeException {
        if (order == null) {
            throw new NodeException(String.format(
                    "Cannot set node order to null on %s", this));
        }
        if (parentNode == null) {
            throw new NodeException(String.format(
                    "Cannot set order on node %s unless it has a parent", this));
        }
        List<Node> siblings = parentNode.getChildren();
        siblings.remove(this);
        if (order < 0 || (order > 0 && order > siblings.size())) {
            throw new NodeException(String.format(
                    "Incorrect node order value (%s) for %s", order, this));
        }
        siblings.add(order, this);
        parentNode.setChildren(siblings);
    }

    public void moveTo(Node container, Integer order) throws NodeException {
        setParent(container);
        setOrder(order);
    }

    public void insertAfter(Node node) throws NodeException {
        node.getParent().addChild(this);
        moveTo(node.getParent(), node.getOrder() + 1);
    }

    public boolean hasSiblings() {
        if (parentNode == null) {
            return false;
        }
        return parentNode.getChildren().size() > 1;
    }

    public Node getNextNode() {
        int order = getOrder();
        List<Node> siblings = parentNode.getChildren();
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
        List<Node> siblings = parentNode.getChildren();
        return siblings.get(order - 1);
    }

    public boolean hasChildren() {
        return !childrenNodes.isEmpty();
    }

    public boolean isChildOf(Node node) {
        boolean res = false;
        Node parent = parentNode;
        while (parent != null) {
            if (parent == node) {
                res = true;
                break;
            }
            parent = parent.getParent();
        }
        return res;
    }

    public void removeDescendants() throws NodeException {
        for (Node child : childrenNodes) {
            child.removeDescendants();
            child.clearParent();
        }
        childrenNodes.clear();
    }

    public List<Node> getDescendants() {
        List<Node> descendants = new ArrayList<Node>();
        collectDescendants(descendants);
        return descendants;
    }

    public void collectDescendants(List<Node> nodes) {
        for (Node child : childrenNodes) {
            nodes.add(child);
            child.collectDescendants(nodes);
        }
    }

}
