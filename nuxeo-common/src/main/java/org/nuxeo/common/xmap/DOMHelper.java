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

package org.nuxeo.common.xmap;

import java.util.Collection;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public final class DOMHelper {

    // Utility class.
    private DOMHelper() {
    }

    /**
     * Gets the value of the node at the given path
     * relative to the given base element.
     * <p>
     * For element nodes the value is the text content and for
     * the attributes node the attribute value.
     *
     * @param base
     * @param path
     * @return the node value or null if no such node was found
     */
    public static String getNodeValue(Element base, Path path) {
        Node node = getElementNode(base, path);
        if (node != null) {
            if (path.attribute != null) {
                Node at = node.getAttributes()
                    .getNamedItem(path.attribute);
                return at != null ? at.getNodeValue() : null;
            } else {
                return node.getTextContent();
            }
        }
        return null;
    }

    /**
     * Visits the nodes selected by the given path using the given visitor.
     *
     * @param path
     * @param base
     * @param visitor
     */
    public static void visitNodes(Context ctx, XAnnotatedList xam,
            Element base, Path path,
            NodeVisitor visitor, Collection<Object> result) {
        Node el = base;
        int len = path.segments.length - 1;
        for (int i = 0; i < len; i++) {
            el = getElementNode(el, path.segments[i]);
            if (el == null) {
                return;
            }
        }
        String name = path.segments[len];

        if (path.attribute != null) {
            visitAttributes(ctx, xam, el, name, path.attribute, visitor, result);
        } else {
            visitElements(ctx, xam, el, name, visitor, result);
        }
    }

    public static void visitAttributes(Context ctx, XAnnotatedList xam, Node base,
            String name, String attrName, NodeVisitor visitor, Collection<Object> result) {
        Node p = base.getFirstChild();
        while (p != null) {
            if (p.getNodeType() == Node.ELEMENT_NODE) {
                if (name.equals(p.getNodeName())) {
                    Node at = p.getAttributes().getNamedItem(attrName);
                    if (at != null) {
                        visitor.visitNode(ctx, xam, at, result);
                    }
                }
            }
            p = p.getNextSibling();
        }
    }

    public static void visitElements(Context ctx, XAnnotatedList xam, Node base,
            String name, NodeVisitor visitor, Collection<Object> result) {
        Node p = base.getFirstChild();
        while (p != null) {
            if (p.getNodeType() == Node.ELEMENT_NODE) {
                if (name.equals(p.getNodeName())) {
                    visitor.visitNode(ctx, xam, p, result);
                }
            }
            p = p.getNextSibling();
        }
    }

    public static void visitMapNodes(Context ctx, XAnnotatedMap xam,
            Element base, Path path,
            NodeMapVisitor visitor, Map<String, Object> result) {
        Node el = base;
        int len = path.segments.length - 1;
        for (int i = 0; i < len; i++) {
            el = getElementNode(el, path.segments[i]);
            if (el == null) {
                return;
            }
        }
        String name = path.segments[len];

        if (path.attribute != null) {
            visitMapAttributes(ctx, xam, el, name, path.attribute, visitor, result);
        } else {
            visitMapElements(ctx, xam, el, name, visitor, result);
        }
    }

    public static void visitMapAttributes(Context ctx, XAnnotatedMap xam, Node base,
            String name, String attrName, NodeMapVisitor visitor, Map<String, Object> result) {
        Node p = base.getFirstChild();
        while (p != null) {
            if (p.getNodeType() == Node.ELEMENT_NODE) {
                if (name.equals(p.getNodeName())) {
                    Node at = p.getAttributes().getNamedItem(attrName);
                    if (at != null) {
                        String key = getNodeValue((Element) p, xam.key);
                        if (key != null) {
                            visitor.visitNode(ctx, xam, at, key, result);
                        }
                    }
                }
            }
            p = p.getNextSibling();
        }
    }

    public static void visitMapElements(Context ctx, XAnnotatedMap xam, Node base,
            String name, NodeMapVisitor visitor, Map<String, Object> result) {
        Node p = base.getFirstChild();
        while (p != null) {
            if (p.getNodeType() == Node.ELEMENT_NODE) {
                if (name.equals(p.getNodeName())) {
                    String key = getNodeValue((Element) p, xam.key);
                    if (key != null) {
                        visitor.visitNode(ctx, xam, p, key, result);
                    }
                }
            }
            p = p.getNextSibling();
        }
    }

    /**
     * Gets the first child element node having the given name.
     *
     * @param base
     * @param name
     * @return
     */
    public static Node getElementNode(Node base, String name) {
        Node node = base.getFirstChild();
        while (node != null) {
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                if (name.equals(node.getNodeName())) {
                    return node;
                }
            }
            node = node.getNextSibling();
        }
        return null;
    }

    public static Node getElementNode(Node base, Path path) {
        Node el = base;
        int len = path.segments.length;
        for (int i = 0; i < len; i++) {
            el = getElementNode(el, path.segments[i]);
            if (el == null) {
                return null;
            }
        }
        return el;
    }

    public static interface NodeVisitor {

        void visitNode(Context ctx, XAnnotatedMember xam,
                Node node, Collection<Object> result);

    }

    public static interface NodeMapVisitor {

        void visitNode(Context ctx, XAnnotatedMember xam,
                Node node, String key, Map<String, Object> result);

    }

}
