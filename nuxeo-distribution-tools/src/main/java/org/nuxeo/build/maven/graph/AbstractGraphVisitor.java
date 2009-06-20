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
 *     bstefanescu
 */
package org.nuxeo.build.maven.graph;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class AbstractGraphVisitor implements GraphVisitor {

    protected Set<Node> visitedNodes;

    public AbstractGraphVisitor() {
        visitedNodes = new HashSet<Node>();
    }

    public void process(Graph graph) {
        process(graph.getRoots());
    }

    public void process(Collection<Node> roots) {
        for (Node root : roots) {
            visit(root);
        }
    }

    public boolean visit(Node node) {
        if (visitedNodes.contains(node.id)) {
            return false;
        }
        visitedNodes.add(node);
        if (visitNode(node)) {
            visitEdges(node);
        }
        return true;
    }

    public void visitEdges(Node node) {
        for (Edge edge : node.getEdgesOut()) {
            if (visitEdge(edge)) {
                visit(edge.dst);
            }
        }
    }

    public abstract boolean visitNode(Node node);

    public abstract boolean visitEdge(Edge edge);

}
