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

    void setParent(Node node);

    Node getParent();

    Node addChild(Node node);

    void removeChild(Node node);

    List<Node> getChildren();

    void setChildren(List<Node> children);

    Integer getOrder();

    void setOrder(Integer order);

    void moveTo(Node container, Integer order);

    boolean isLeaf();

    void insertAfter(Node node);

    boolean hasSiblings();

    Node getNextNode();

    Node getPreviousNode();

    boolean hasChildren();

    boolean isChildOf(Node node);

    void removeDescendants();

    List<Node> getDescendants();

    void collectDescendants(List<Node> nodes);

}
