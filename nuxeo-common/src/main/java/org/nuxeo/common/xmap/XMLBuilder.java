/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.common.xmap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class XMLBuilder {

    private static final Log log = LogFactory.getLog(XMLBuilder.class);

    private XMLBuilder() {
    }

    public static String saveToXML(Object object, Element root,
            XAnnotatedObject xao) {
        try {
            toXML(object, root, xao);
            return DOMSerializer.toString(root);
        } catch (Exception e) {
            log.error(e, e);
        }
        return null;
    }

    public static void toXML(Object o, Element parent, XAnnotatedObject xao)
            throws Exception {
        // XPath xpath = XPathFactory.newInstance().newXPath();
        Element currentNode = parent;
        String path = xao.getPath().toString();
        if (path.length() > 0) {
            currentNode = parent.getOwnerDocument().createElement(path);
            parent.appendChild(currentNode);
        }
        // process annotated members
        for (XAnnotatedMember m : xao.members) {
            if (m instanceof XAnnotatedMap) {
                m.toXML(o, currentNode);
            } else if (m instanceof XAnnotatedList) {
                m.toXML(o, currentNode);
            } else if (m instanceof XAnnotatedContent) {
                m.toXML(o, currentNode);
            } else if (m instanceof XAnnotatedParent) {

            } else {
                m.toXML(o, currentNode);
            }
        }
    }

    // TODO use xpath for that ?
    public static Element getOrCreateElement(Element root, Path path) {
        Element e = root;
        for (String segment : path.segments) {
            e = getOrCreateElement(e, segment);
        }
        return e;
    }

    public static Element addElement(Element root, Path path) {
        Element e = root;
        int len = path.segments.length - 1;
        for (int i = 0; i < len; i++) {
            e = getOrCreateElement(e, path.segments[i]);
        }
        return addElement(e, path.segments[len]);
    }

    private static Element getOrCreateElement(Element parent, String segment) {
        NodeList list = parent.getChildNodes();
        for (int i = 0, len = list.getLength(); i < len; i++) {
            Element e = (Element) list.item(i);
            if (segment.equals(e.getNodeName())) {
                return e;
            }
        }
        // node not found, create one
        return addElement(parent, segment);
    }

    public static Element addElement(Element parent, String segment) {
        Element e = parent.getOwnerDocument().createElement(segment);
        parent.appendChild(e);
        return e;
    }

    public static void fillField(Element element, String value, String attribute) {
        if (attribute != null) {
            element.setAttribute(attribute, value);
        } else {
            element.setTextContent(value);
        }
    }

}
