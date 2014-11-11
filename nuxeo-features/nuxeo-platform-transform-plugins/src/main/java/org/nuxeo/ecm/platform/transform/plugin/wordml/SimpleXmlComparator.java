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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.transform.plugin.wordml;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Simple XML documents comparator. Iterates through document children and
 * checks for equivalent elements in the second doc.
 * <p>
 * It works only with docs that for any given node doesn't have more than one
 * element of a specific type.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
public final class SimpleXmlComparator {

    private static final Log log = LogFactory.getLog(SimpleXmlComparator.class);

    // Utility class.
    private SimpleXmlComparator() {
    }

    private static class DiffInfo {
        boolean equal;

        private final List<String> comparations = new ArrayList<String>();

        private final String path;

        DiffInfo(String path) {
            this.path = path;
        }

        public void add(String info) {
            comparations.add(path + ": " + info);
        }

        public void append(DiffInfo info) {
            comparations.addAll(info.comparations);
        }

        public String getInfo() {
            StringBuilder buf = new StringBuilder();
            for (String comp : comparations) {
                buf.append(comp);
                buf.append('\n');
            }
            return buf.toString();
        }
    }

    public static boolean compareXmlDocs(InputStream isDoc1, InputStream isDoc2)
            throws SAXException, IOException, ParserConfigurationException {
        // for each attribute from the first check it exists in the second
        DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder();
        Document doc1 = docBuilder.parse(isDoc1);
        Document doc2 = docBuilder.parse(isDoc2);

        return compareXmlDocs(doc1, doc2);
    }

    private static boolean compareXmlDocs(Document doc1, Document doc2) {
        DiffInfo info = compareNodes("/", doc1, doc2);
        if (log.isDebugEnabled()) {
            log.debug(info.getInfo());
        }
        return info.equal;
    }

    private static DiffInfo compareNodes(String path, Node n1, Node n2) {
        DiffInfo info = new DiffInfo(path);

        String currentPath = path + '/' + n1.getNodeName();

        String compInfo = "compare nodes: " + n1.getNodeName() + "::"
                + n2.getNodeName();

        if (!n1.getNodeName().equals(n2.getNodeName())) {
            compInfo += "; different node names";
            info.add(compInfo);
            return info;
        }

        NamedNodeMap attribs1 = n1.getAttributes();
        NamedNodeMap attribs2 = n2.getAttributes();

        DiffInfo attrDiffInfo = compareNodeAttributes(currentPath, attribs1, attribs2);
        info.append(attrDiffInfo);
        if (!attrDiffInfo.equal) {
            compInfo += "; different attributes";
            info.add(compInfo);
            info.equal = false;
            return info;
        }

        compInfo += "; found matching nodes";
        info.add(compInfo);

        // compare child nodes

        NodeList children1 = n1.getChildNodes();
        NodeList children2 = n2.getChildNodes();

        if (children1.getLength() == 1) {
            // we have only one child - need to compare values
            if (children2.getLength() != 1) {
                info.add("not same number of children");
                info.equal = false;
                return info;
            }
            Node child1 = children1.item(0);
            Node child2 = children2.item(0);
            if (child1.getNodeType() == Node.TEXT_NODE
                    && child2.getNodeType() == Node.TEXT_NODE) {
                // skip it;
                if (!child1.getTextContent().equals(child2.getTextContent())) {
                    info.add("different text contents: "
                            + child1.getTextContent() + " != "
                            + child2.getTextContent());
                    info.equal = false;
                    return info;
                }
            }
        }

        for (int i = 0; i < children1.getLength(); i++) {
            Node child1 = children1.item(i);

            if (child1.getNodeType() == Node.TEXT_NODE) {
                // skip it;
                info.add("skipped (text node) " + n1.getNodeType());
                continue;
            }


            // check it exist in the second
            boolean exist = false;
            for (int j = 0; j < children2.getLength(); j++) {
                Node child2 = children2.item(j);
                DiffInfo childDiffInfo = compareNodes(currentPath, child1,
                        child2);
                info.append(childDiffInfo);
                if (childDiffInfo.equal) {
                    exist = true;
                    break;
                }
            }
            if (!exist) {
                compInfo += "equivalent node not existing for: " + currentPath
                        + '/' + child1.getNodeName();
                info.add(compInfo);
                info.equal = false;
                return info;
            }
        }
        info.equal = true;
        return info;
    }

    private static  DiffInfo compareNodeAttributes(String path,
            NamedNodeMap attribs1, NamedNodeMap attribs2) {
        DiffInfo info = new DiffInfo(path);
        if (attribs1 == null) {
            info.add("no attributes for node: " + path);
            if (attribs2 != null) {
                info.add("attribs2 not null");
                info.equal = false;
                return info;
            }

            info.equal = true;
            return info;
        }
        if (attribs2 == null) {
            info.add("no attributes2 for node: " + path);
            info.equal = false;
            return info;
        }

        // compare attributes
        info.add("compare attributes");

        for (int i = 0; i < attribs1.getLength(); i++) {
            Attr attr1 = (Attr) attribs1.item(i);
            String attr1Name = attr1.getName();

            info.add("check " + attr1Name);

            Attr attr2 = (Attr) attribs2.getNamedItem(attr1Name);

            if (attr2 == null) {
                info.add("Attribute " + attr1Name + "not found in second");
                info.equal = false;
                return info;
            }

            if (!attr1.getValue().equals(attr2.getValue())) {
                info.add("Values not match (" + attr1.getValue() + " != "
                        + attr2.getValue() + ')');
                info.equal = false;
                return info;
            }

            info.add("attributes matched: " + attr1);
        }

        info.equal = true;
        return info;
    }

}
