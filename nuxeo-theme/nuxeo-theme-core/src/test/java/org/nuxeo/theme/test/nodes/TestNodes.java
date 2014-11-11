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

package org.nuxeo.theme.test.nodes;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import static org.junit.Assert.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.theme.nodes.Node;
import org.nuxeo.theme.nodes.NodeException;
import org.nuxeo.theme.test.DummyNode;

public class TestNodes {

    private static final Log log = LogFactory.getLog(TestNodes.class);

    @Test
    public void testOrder() throws NodeException {
        Node node0 = new DummyNode();
        Node node1 = new DummyNode();
        Node node2 = new DummyNode();
        assertNull(node1.getOrder());
        assertNull(node2.getOrder());
        node0.addChild(node1);
        node0.addChild(node2);
        assertEquals(0, (int) node1.getOrder());
        assertEquals(1, (int) node2.getOrder());
        node1.setOrder(1);
        node2.setOrder(0);
        assertEquals(1, (int) node1.getOrder());
        assertEquals(0, (int) node2.getOrder());
    }

    @Test
    public void testSetOrderBeyondLimits() throws NodeException {
        Node node0 = new DummyNode();
        try {
            node0.setOrder(0);
        } catch (NodeException e) {
            log.warn(e);
        }
        Node container = new DummyNode();
        container.addChild(node0);
        try {
            node0.setOrder(1);
        } catch (NodeException e) {
            log.warn(e);
        }
        try {
            node0.setOrder(-1);
        } catch (NodeException e) {
            log.warn(e);
        }
    }

    @Test
    public void testSetParent() throws NodeException {
        Node node0 = new DummyNode();
        Node node1 = new DummyNode();
        assertNull(node0.getParent());
        assertNull(node1.getParent());
        node0.setParent(node1);
        assertSame(node1, node0.getParent());
    }

    @Test
    public void testSetParentWithCycle() throws NodeException {
        Node node0 = new DummyNode();
        Node node1 = new DummyNode();
        node0.setParent(node1);
        try {
            node1.setParent(node0);
        } catch (NodeException e) {
            log.warn(e);
        }
        assertSame(node1, node0.getParent());
    }

    @Test
    public void testChildren() throws NodeException {
        Node node0 = new DummyNode();
        Node node1 = new DummyNode();
        assertFalse(node0.getChildren().contains(node1));
        node0.addChild(node1);
        assertTrue(node0.getChildren().contains(node1));
        assertSame(node0, node1.getParent());
        node0.removeChild(node1);
        assertFalse(node0.getChildren().contains(node1));
        assertNull(node1.getParent());
    }

    @Test
    public void testChildrenWithCycle() throws NodeException {
        Node node0 = new DummyNode();
        Node node1 = new DummyNode();
        try {
            node0.addChild(node0);
        } catch (NodeException e) {
            log.warn(e);
        }
        assertFalse(node0.hasChildren());
        node0.addChild(node1);
        assertTrue(node0.getChildren().contains(node1));
        try {
            node1.addChild(node0);
        } catch (NodeException e) {
            log.warn(e);
        }
        assertFalse(node1.hasChildren());
    }

    @Test
    public void testSetChildren() throws NodeException {
        Node container = new DummyNode();
        Node node1 = new DummyNode();
        Node node2 = new DummyNode();
        Node node3 = new DummyNode();
        List<Node> children = new ArrayList<Node>();
        children.add(node1);
        children.add(node2);
        children.add(node3);
        container.setChildren(children);
        assertTrue(container.getChildren().contains(node1));
        assertTrue(container.getChildren().contains(node2));
        assertTrue(container.getChildren().contains(node3));
    }

    @Test
    public void testSetChildrenWithCycle() throws NodeException {
        Node container = new DummyNode();
        Node node1 = new DummyNode();
        Node node2 = new DummyNode();
        Node node3 = new DummyNode();
        container.setParent(node2);
        List<Node> children = new ArrayList<Node>();
        children.add(node1);
        children.add(node2);
        children.add(node3);
        try {
            container.setChildren(children);
        } catch (NodeException e) {
            log.warn(e);
        }
        assertFalse(container.hasChildren());
    }

    @Test
    public void testMove() throws NodeException {
        Node container1 = new DummyNode();
        Node container2 = new DummyNode();
        Node node = new DummyNode();
        container1.addChild(node);
        assertTrue(container1.getChildren().contains(node));
        assertFalse(container2.getChildren().contains(node));
        assertSame(container1, node.getParent());
        node.moveTo(container2, 0);
        assertFalse(container1.getChildren().contains(node));
        assertTrue(container2.getChildren().contains(node));
        assertSame(container2, node.getParent());
    }

    @Test
    public void testMoveEdgeCase() throws NodeException {
        Node node = new DummyNode();
        Node container = new DummyNode();
        container.addChild(node);
        assertSame(container, node.getParent());
        try {
            node.moveTo(node, 0);
        } catch (NodeException e) {
            log.warn(e);
        }
        assertSame(container, node.getParent());
        assertTrue(node.getChildren().isEmpty());
    }

    @Test
    public void testInsertAfter() throws NodeException {
        Node node0 = new DummyNode();
        Node node1 = new DummyNode();
        Node node2 = new DummyNode();
        Node node3 = new DummyNode();
        Node node4 = new DummyNode();
        Node container = new DummyNode();
        container.addChild(node0);
        node1.insertAfter(node0);
        assertSame(node0, container.getChildren().get(0));
        assertSame(node1, container.getChildren().get(1));
        node2.insertAfter(node1);
        assertSame(node2, container.getChildren().get(2));
        node3.insertAfter(node0);
        assertSame(node0, container.getChildren().get(0));
        assertSame(node3, container.getChildren().get(1));
        assertSame(node1, container.getChildren().get(2));
        assertSame(node2, container.getChildren().get(3));
        node4.insertAfter(node3);
        assertSame(node0, container.getChildren().get(0));
        assertSame(node3, container.getChildren().get(1));
        assertSame(node4, container.getChildren().get(2));
        assertSame(node1, container.getChildren().get(3));
        assertSame(node2, container.getChildren().get(4));
    }

    @Test
    public void testHasSiblings() throws NodeException {
        Node container = new DummyNode();
        Node node1 = new DummyNode();
        Node node2 = new DummyNode();
        container.addChild(node1);
        assertFalse(node1.hasSiblings());
        container.addChild(node2);
        assertTrue(node2.hasSiblings());
    }

    @Test
    public void testGetNextNode() throws NodeException {
        Node node0 = new DummyNode();
        Node node1 = new DummyNode();
        Node node2 = new DummyNode();
        Node container = new DummyNode();
        container.addChild(node0);
        container.addChild(node1);
        container.addChild(node2);
        assertSame(node1, node0.getNextNode());
        assertSame(node2, node1.getNextNode());
        assertNull(node2.getNextNode());
    }

    @Test
    public void testGetPreviousNode() throws NodeException {
        Node node0 = new DummyNode();
        Node node1 = new DummyNode();
        Node node2 = new DummyNode();
        Node container = new DummyNode();
        container.addChild(node0);
        container.addChild(node1);
        container.addChild(node2);
        assertSame(node1, node2.getPreviousNode());
        assertSame(node0, node1.getPreviousNode());
        assertNull(node0.getPreviousNode());
    }

    @Test
    public void testHasChildren() throws NodeException {
        Node node0 = new DummyNode();
        Node container = new DummyNode();
        assertFalse(container.hasChildren());
        container.addChild(node0);
        assertTrue(container.hasChildren());
    }

    @Test
    public void testIsChildOf() throws NodeException {
        Node node0 = new DummyNode();
        Node node1 = new DummyNode();
        Node parentOfNode0 = new DummyNode();
        Node parentOfNode1 = new DummyNode();

        assertFalse(node0.isChildOf(parentOfNode0));
        assertFalse(node1.isChildOf(parentOfNode1));

        parentOfNode0.addChild(node0);
        parentOfNode1.addChild(node1);

        assertTrue(node0.isChildOf(parentOfNode0));
        assertTrue(node1.isChildOf(parentOfNode1));
        assertFalse(node0.isChildOf(parentOfNode1));
        assertFalse(node1.isChildOf(parentOfNode0));
        assertFalse(parentOfNode1.isChildOf(parentOfNode0));
        assertFalse(parentOfNode0.isChildOf(parentOfNode1));
        assertFalse(parentOfNode0.isChildOf(node0));
        assertFalse(parentOfNode1.isChildOf(node1));

        parentOfNode0.addChild(parentOfNode1);

        assertTrue(node0.isChildOf(parentOfNode0));
        assertTrue(node1.isChildOf(parentOfNode1));
        assertFalse(node0.isChildOf(parentOfNode1));
        assertTrue(node1.isChildOf(parentOfNode0));
        assertTrue(parentOfNode1.isChildOf(parentOfNode0));
        assertFalse(parentOfNode0.isChildOf(parentOfNode1));
        assertFalse(parentOfNode0.isChildOf(node0));
        assertFalse(parentOfNode1.isChildOf(node1));
    }

    @Test
    public void testRemoveDescendants() throws NodeException {
        Node node0 = new DummyNode();
        Node node1 = new DummyNode();
        Node node2 = new DummyNode();
        Node node3 = new DummyNode();
        Node node4 = new DummyNode();
        node0.addChild(node1);
        node1.addChild(node2);
        node1.addChild(node3);
        node2.addChild(node4);

        node1.removeDescendants();

        assertTrue(node0.getChildren().contains(node1));
        assertTrue(node1.getChildren().isEmpty());
        assertTrue(node2.getChildren().isEmpty());

        assertSame(node0, node1.getParent());
        assertNull(node2.getParent());
        assertNull(node3.getParent());
        assertNull(node4.getParent());
    }

    @Test
    public void testGetDescendants() throws NodeException {
        Node node0 = new DummyNode();
        Node node1 = new DummyNode();
        Node node2 = new DummyNode();
        Node node3 = new DummyNode();
        Node node4 = new DummyNode();
        node0.addChild(node1);
        node1.addChild(node2);
        node1.addChild(node3);
        node2.addChild(node4);

        List<Node> descendants = node1.getDescendants();
        assertFalse(descendants.contains(node0));
        assertFalse(descendants.contains(node1));
        assertTrue(descendants.contains(node2));
        assertTrue(descendants.contains(node3));
        assertTrue(descendants.contains(node4));
    }
}
