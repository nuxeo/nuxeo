/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.repository.jcr;


import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.ItemImpl;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeImpl;
import org.apache.jackrabbit.name.MalformedPathException;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.name.Path.PathElement;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public final class ModelAdapter {

    // This is a utility class.
    private ModelAdapter() {
    }

    public static boolean isDocument(Node node) throws RepositoryException {
        return node.getPrimaryNodeType().isNodeType(NodeConstants.ECM_NT_DOCUMENT.rawname);
    }

    public static boolean isFolder(Node node) throws RepositoryException {
        return node.getPrimaryNodeType().isNodeType(NodeConstants.ECM_NT_FOLDER.rawname);
    }

    public static Node getContainerNode(Node node) throws RepositoryException {
        return node.getNode(NodeConstants.ECM_CHILDREN.rawname);
    }

    public static boolean isContainerNode(Node node) throws RepositoryException {
        return ((NodeImpl) node).getQName().equals(NodeConstants.ECM_CHILDREN.qname);
    }

    public static String getChildPath(String name) {
        return new StringBuffer(64).append(NodeConstants.ECM_CHILDREN.rawname)
                .append('/').append(name).toString();
    }

    public static Node getParentNode(Node node) throws RepositoryException {
        return node.getParent().getParent();
    }

    public static Node addChild(Node node, String name, String type) throws RepositoryException {
        return node.addNode(getChildPath(name), TypeAdapter.docType2NodeType(type));
    }

    public static Node addPropertyNode(Node node, String name, String type) throws RepositoryException {
        return node.addNode(name, TypeAdapter.fieldType2NodeType(type));
    }

    public static Node getChild(Node node, String name) throws RepositoryException {
        return node.getNode(getChildPath(name));
    }

    public static boolean hasChild(Node docNode, String name) throws RepositoryException {
        return docNode.hasNode(getChildPath(name));
    }

    public static boolean hasChildren(Node docNode) throws RepositoryException {
        return getContainerNode(docNode).hasNodes();
    }

    /**
     * Returns the JCR type name without the prefix.
     *
     * @param node
     * @return
     * @throws RepositoryException
     */
    public static String getLocalTypeName(Node node) throws RepositoryException {
        return ((NodeTypeImpl) node.getPrimaryNodeType()).getQName().getLocalName();
        // which method is better? both have same result.
        // is jackrabbit getName() costly than a cast? I think it is ...

       // return jcr2DocType(node.getPrimaryNodeType().getName());
    }

    public static String getTypeName(Node node) throws RepositoryException {
        return node.getPrimaryNodeType().getName();
    }

    public static String getPath(String wsName, Node node)
            throws MalformedPathException, RepositoryException {
        Path path = ((ItemImpl) node).getPrimaryPath().getCanonicalPath();
        PathElement[] elements = path.getElements();
        if (elements.length < 3) {
            return "/"; // the root
        }
        StringBuilder buf = new StringBuilder(256);
        if (wsName != null) {
            buf.append('/').append(wsName);
        }
        for (int i = 3; i < elements.length; i += 2) { // skip ecm:root and ecm:children elements
            buf.append('/').append(elements[i].getName().getLocalName());
        }
        return buf.toString();
    }

    public static boolean hasField(Node node, String path)
            throws RepositoryException {
        if (!node.hasProperty(path)) {
            return node.hasNode(path);
        }
        return true;
    }

    public static String path2Jcr(org.nuxeo.common.utils.Path path) {
        StringBuilder buf = new StringBuilder();
        String[] segments = path.segments();
        for (int i = 0; i < segments.length; i++) {
            if (i != 0) {
                buf.append('/');
            }
            String seg = segments[i];
            if (seg.contentEquals("..")) {
                buf.append("../..");
            } else {
                buf.append(NodeConstants.ECM_CHILDREN.rawname).append('/').append(
                        seg);
            }
        }
        return buf.toString();
    }

    /**
     * Marks the given node as an unstructured node.
     *
     * @param node the node to mark
     * @throws RepositoryException
     */
    public static void setUnstructured(Node node) throws RepositoryException {
        node.addMixin(NodeConstants.ECM_MIX_UNSTRUCTURED.rawname);
    }

}
