/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.gwt.client.util;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;

/**
 * @author Alexandre Russel
 *
 */
public class Visitor {
    private final NodeProcessor processor;

    private boolean processing = true;

    private boolean partialVisit = false;

    private Node startNode;

    private Node endNode;

    public Visitor(NodeProcessor visitor) {
        this.processor = visitor;
    }

    /**
     * process all the children of this node.
     *
     * @param node
     */
    public void process(Node node) {
        visit(node);
    }

    /**
     * process all the node of this document.
     *
     * @param document
     */
    public void process(Document document) {
        visit(document);
    }

    /**
     * process all the node from startNode to endNode.
     *
     * @param startNode
     * @param endNode
     */
    public void process(Node startNode, Node endNode) {
        this.processing = false;
        partialVisit = true;
        this.startNode = startNode;
        this.endNode = endNode;
        visit(startNode.getOwnerDocument());
    }

    public void visit(Node node) {
        if (startNode == node) {
            processing = true;
        } else if (endNode == node) {
            processing = false;
        }
        if (processor.doBreak()) {
            return;
        }

        NodeList list = node.getChildNodes();
        if (list == null || list.getLength() == 0) {
            processIf(node);
        } else {
            int length = list.getLength();
            Node[] nodes = new Node[list.getLength()];
            for (int x = 0; x < length; x++) {
                nodes[x] = list.getItem(x);
            }
            processIf(node);
            for (int x = 0; x < length; x++) {
                visit(nodes[x]);
            }
        }

    }

    private void processIf(Node node) {
        if (processing || !partialVisit) {
            processor.process(node);
        }
    }

}
