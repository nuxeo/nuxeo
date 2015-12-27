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

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.platform.annotations.gwt.client.AnnotationConstant;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.SpanElement;

/**
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 */
public class XPathUtil {
    public String getXPath(Node node) {
        Log.debug("XPathUtil] node: " + node.getNodeName() + " parent node: " + node.getParentNode().getNodeName());
        Document document = node.getOwnerDocument();
        Node current = node;
        StringBuilder xpath = new StringBuilder();
        while (!current.equals(document)) {
            int counter = 1;
            String name = current.getNodeName();
            while (current.getPreviousSibling() != null) {
                if (current.getPreviousSibling().getNodeName().equalsIgnoreCase(name)
                        && !isIgnored(current.getPreviousSibling())) {
                    counter++;
                }
                current = current.getPreviousSibling();
            }
            xpath.insert(0, "/" + name.toLowerCase() + "[" + counter + "]");
            current = current.getParentNode();
        }
        Log.debug("XPathUtil] xpath: " + xpath);
        return xpath.toString();
    }

    public String getSelectionXPointer(Range range) {
        int start = range.getStartOffset();
        Node clickedNode = range.getStartContainer();
        Node parentNode = clickedNode.getParentNode();

        if (clickedNode.getNodeType() == Node.TEXT_NODE) {
            TextGrabberVisitor processor = new TextGrabberVisitor();
            Visitor visitor = new Visitor(processor);
            visitor.process(parentNode, clickedNode);
            start += processor.getText().length();
        }
        Log.debug("getSelectionXPointer; start: " + start);
        return "#xpointer(string-range(" + getXPath(parentNode) + ",\"\"," + start + ","
                + getShortLength(range.getSelectedText()) + "))";
    }

    public int getShortLength(String selectedText) {
        for (String removed : new String[] { "\n", "\r" }) {
            selectedText = selectedText.replace(removed, "");
        }
        return selectedText.length();
    }

    public List<Node> getNode(String xpath, Document document) {
        List<Node> nodes = new ArrayList<Node>();
        if (xpath.startsWith("//")) {
            xpath = xpath.substring(2);
            NodeList<Element> n = document.getElementsByTagName(xpath);
            for (int x = 0; x < n.getLength(); x++) {
                nodes.add(n.getItem(x));
            }
            return nodes;
        }
        Log.debug("XPathUtil#getNode -- xpath: " + xpath);
        String[] paths = xpath.split("/");
        Node result = document;
        for (String path : paths) {
            if ("".equals(path)) {
                continue;
            }
            NodeList<Node> nodeList = result.getChildNodes();
            String name = path.substring(0, path.indexOf("["));
            int index = Integer.parseInt(path.substring(path.indexOf("[") + 1, path.indexOf("]")));
            int counter = 1;
            for (int x = 0; x < nodeList.getLength(); x++) {
                Node node = nodeList.getItem(x);
                if (node.getNodeName().equalsIgnoreCase(name)) {
                    if (isIgnored(node)) {// considered as text node
                        continue;
                    }
                    if (counter == index) {
                        result = node;
                        break;
                    }
                    counter++;
                }
            }

        }
        nodes.add(result);
        Log.debug("XPathUtil#getNode -- end function: ");
        return nodes;
    }

    private boolean isIgnored(Node node) {
        int nodeType = node.getNodeType();
        if (nodeType != Node.ELEMENT_NODE && nodeType != Node.TEXT_NODE) {
            return true; // ignore non element and non text node
        }
        if (node.getNodeName().equalsIgnoreCase("span")) {
            SpanElement spanElement = SpanElement.as(node).cast();
            String name = spanElement.getClassName();
            if (name == null) {
                return false;
            }
            return name.contains(AnnotationConstant.IGNORED_ELEMENT);
        } else if (node.getNodeName().equalsIgnoreCase("div")) {
            DivElement divElement = DivElement.as(node).cast();
            return divElement.getClassName().equals(AnnotationConstant.IGNORED_ELEMENT);
        }
        return false;
    }

    public boolean isChildOfIgnored(Node node) {
        boolean ignored = false;
        while (node != null && !ignored) {
            ignored = isIgnored(node);
            node = node.getParentNode();
        }
        return ignored;
    }

    public static String toIdableName(String xpath) {
        xpath = "X" + xpath;
        xpath = xpath.replace("/", "-");
        xpath = xpath.replace("[", "_");
        return xpath.replace("]", ":");
    }

    public static String fromIdableName(String xpath) {
        xpath = xpath.substring(1);
        xpath = xpath.replace("-", "/");
        xpath = xpath.replace("_", "[");
        return xpath.replace(":", "]");
    }
}
