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
 *     Max Stepanov
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.domsync.core;

import java.text.MessageFormat;
import java.util.Stack;
import java.util.StringTokenizer;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Max Stepanov
 *
 */
public class DOMUtil {

    private DOMUtil() {
    }

    public static String computeNodeXPath(Node base, Node node) {
        if (base.isSameNode(node)) {
            return "/";
        }
        short nodeType = node.getNodeType();
        String subpath;
        if(nodeType == Node.ELEMENT_NODE || nodeType == Node.TEXT_NODE) {
            String localName = node.getLocalName().toLowerCase();
            int pos = 0;
            Node sibling = node.getPreviousSibling();
            while (sibling != null) {
                if (sibling.getNodeType() == nodeType) {
                    if(localName.equals(sibling.getLocalName().toLowerCase())) {
                        ++pos;
                    }
                }
                sibling = sibling.getPreviousSibling();
            }
            if (nodeType == Node.TEXT_NODE) {
                localName = "text()";
            }

            if(pos == 0) {
                subpath = localName;
            } else {
                subpath = MessageFormat.format("{0}[{1}]", localName, new Integer(pos));
            }
        } else {
            System.err.println("Unsupported type "+nodeType);
            subpath = "unknown";
        }
        Node parent = node.getParentNode();
        if (base.isSameNode(parent)) {
            return '/' + subpath;
        } else {
            return computeNodeXPath(base, parent) + '/' + subpath;
        }
    }

    public static Node findNodeByXPath(Node base, String xpath) {
        if("/".equals(xpath)) {
            return base;
        }
        Node node = base;
        StringTokenizer tok = new StringTokenizer(xpath, "/");
        while(tok.hasMoreTokens()) {
            String subpath = tok.nextToken();
            String localName;
            int nodePos = 0;
            int index = subpath.indexOf('[');
            if(index > 0) {
                localName = subpath.substring(0, index).toLowerCase();
                nodePos = Integer.parseInt(subpath.substring(index+1, subpath.indexOf(']')));
            } else {
                localName = subpath.toLowerCase();
            }
            short nodeType = Node.ELEMENT_NODE;
            if ("text()".equals(localName)) {
                nodeType = Node.TEXT_NODE;
                localName = "";
            }
            node = node.getFirstChild();
            int pos = 0;
            while(node != null) {
                if (node.getNodeType() == nodeType
                        && localName.equals(node.getLocalName().toLowerCase())) {
                    if(pos == nodePos) {
                        break;
                    }
                    ++pos;
                }
                node = node.getNextSibling();
            }
        }
        return node;
        /*
        try {
            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();
            XPathExpression expr = xpath.compile(path);
            return (Node) expr.evaluate(document, XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (DOMException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        */

    }

    public static Node getNodeAtPosition(Node parent, int offset) {
        if(offset <0) {
            return null;
        } else if (offset == 0) {
            return parent.getFirstChild();
        }
        NodeList nodes = parent.getChildNodes();
        int count = nodes.getLength();
        if(offset < count) {
            return nodes.item(offset);
        }
        return null;
    }

    public static int getNodePosition(Node node) {
        int pos = -1;
        while(node != null) {
            ++pos;
            node = node.getPreviousSibling();
        }
        return pos;
    }

    public static String getElementOuterNoChildren(Element element) {
        StringBuilder sb = new StringBuilder();
        String tagName = element.getTagName();
        sb.append('<').append(tagName);
        NamedNodeMap attrs = element.getAttributes();
        int count = attrs.getLength();
        for(int i = 0; i < count; ++i) {
            Attr attr = (Attr) attrs.item(i);
            if (attr.getSpecified()) {
                sb.append(' ').append(attr.getName()).append("=\"")
                        .append(attr.getValue()).append('"');
            }
        }
        if("br".equals(tagName.toLowerCase())) {
            sb.append("/>");
        } else {
            sb.append("></").append(tagName).append('>');
        }
        return sb.toString();
    }

    public static String dumpTree(Node node) {
        StringBuilder sb = new StringBuilder();
        Stack<Node> stack = new Stack<Node>();
        int level = 0;
        while(node != null || !stack.isEmpty()) {
            if (node == null) {
                do {
                    node = stack.pop();
                    --level;
                } while(node == null && !stack.isEmpty());
                continue;
            }
            for(int i = 0; i < level; ++i) {
                sb.append(' ');
            }
            sb.append(node.getNodeName()).append(" <").append(node.getNodeValue()).append(">\n");
            stack.push(node.getNextSibling());
            node = node.getFirstChild();
            ++level;
        }
        return sb.toString();
    }
}
