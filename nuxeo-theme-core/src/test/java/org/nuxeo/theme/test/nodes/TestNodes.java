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

import java.util.List;

import junit.framework.TestCase;

import org.nuxeo.theme.nodes.Node;
import org.nuxeo.theme.test.DummyNode;

public class TestNodes extends TestCase {

    public void testOrder() {
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

    public void testParent() {
        Node node0 = new DummyNode();
        Node node1 = new DummyNode();
        assertNull(node0.getParent());
        assertNull(node1.getParent());
        node0.setParent(node1);
        assertSame(node1, node0.getParent());
    }

    public void testChildren() {
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

    public void testMove() {
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

    public void testInsertAfter() {
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

    public void testHasSiblings() {
        Node container = new DummyNode();
        Node node1 = new DummyNode();
        Node node2 = new DummyNode();
        container.addChild(node1);
        assertFalse(node1.hasSiblings());
        container.addChild(node2);
        assertTrue(node2.hasSiblings());
    }

    public void testGetNextNode() {
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

    public void testGetPreviousNode() {
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

    public void testHasChildren() {
        Node node0 = new DummyNode();
        Node container = new DummyNode();
        assertFalse(container.hasChildren());
        container.addChild(node0);
        assertTrue(container.hasChildren());
    }

    public void testIsChildOf() {
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

    public void testRemoveDescendants() {
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

    public void testGetDescendants() {
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
