/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
