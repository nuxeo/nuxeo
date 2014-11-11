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

import java.util.List;

public interface Node {

    NodeTypeFamily getNodeTypeFamily();

    void clearParent();

    void setParent(Node node) throws NodeException;

    Node getParent();

    Node addChild(Node node) throws NodeException;

    void removeChild(Node node) throws NodeException;

    List<Node> getChildren();

    void setChildren(List<Node> children) throws NodeException;

    Integer getOrder();

    void setOrder(Integer order) throws NodeException;

    void moveTo(Node container, Integer order) throws NodeException;

    boolean isLeaf();

    void insertAfter(Node node) throws NodeException;

    boolean hasSiblings();

    Node getNextNode();

    Node getPreviousNode();

    boolean hasChildren();

    boolean isChildOf(Node node);

    void removeDescendants() throws NodeException;

    List<Node> getDescendants();

    void collectDescendants(List<Node> nodes);

}
