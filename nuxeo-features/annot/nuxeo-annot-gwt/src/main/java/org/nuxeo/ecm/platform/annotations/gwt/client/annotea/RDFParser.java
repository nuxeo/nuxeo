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

package org.nuxeo.ecm.platform.annotations.gwt.client.annotea;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.platform.annotations.gwt.client.model.Annotation;
import org.nuxeo.ecm.platform.annotations.gwt.client.model.Container;
import org.nuxeo.ecm.platform.annotations.gwt.client.util.XPointer;
import org.nuxeo.ecm.platform.annotations.gwt.client.util.XPointerFactory;

import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.NodeList;
import com.google.gwt.xml.client.XMLParser;

/**
 * @author Alexandre Russel
 */
public class RDFParser {

    private static final String r = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";

    private static final String nx = "http://www.nuxeo.org/document/uid/";

    public List<Annotation> getAnnotationList(String response) {
        List<Annotation> result = new ArrayList<Annotation>();
        Document document = XMLParser.parse(response);
        NodeList rdfNodes = document.getElementsByTagName("RDF");
        if (rdfNodes.getLength() == 0) {
            rdfNodes = document.getElementsByTagName("rdf:RDF");
        }
        Node root = rdfNodes.item(0);
        NodeList nodeList = root.getChildNodes();
        for (int x = 0; x < nodeList.getLength(); x++) {
            Annotation annotation = processAnnotation(nodeList.item(x));
            if (annotation != null) {
                result.add(annotation);
            }
        }
        return result;
    }

    public Annotation processAnnotation(Node item) {
        if (item == null || item.getNamespaceURI() == null || item.getNodeName() == null) {
            return null;
        }
        if (item.getNamespaceURI().equals(r) && item.getNodeName().endsWith(":Description")) {
            String about = item.getAttributes().item(0).getNodeValue();
            String annotationUUID = about.substring(about.lastIndexOf(":") + 1);
            return getAnnotation(annotationUUID, item.getChildNodes());
        }
        return null;
    }

    public Annotation getAnnotation(String annotationUUID, NodeList list) {
        Annotation annotation = new Annotation(annotationUUID);
        Map<String, Statement> map = new HashMap<String, Statement>();
        Map<String, String> fields = new HashMap<String, String>();
        for (int x = 0; x < list.getLength(); x++) {
            Node node = list.item(x);
            if (node.getNodeName().equals("#text") || node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            if (node.getNamespaceURI().equals(nx)) {
                String nodeName = node.getNodeName();
                String name = nodeName.substring(nodeName.indexOf(":") + 1);
                if ("startContainer".equals(name)) {
                    annotation.setStartContainer(Container.fromString(node.getFirstChild().getNodeValue()));
                    continue;
                }
                if ("endContainer".equals(name)) {
                    annotation.setEndContainer(Container.fromString(node.getFirstChild().getNodeValue()));
                    continue;
                }
                fields.put(name, node.getFirstChild().getNodeValue());
                continue;
            }
            Statement statement = new Statement(node);
            if (statement.getObject().equals("http://www.w3.org/2000/10/annotation-ns#Annotation")) {
                continue;
            }
            map.put(statement.getPredicate(), statement);
        }
        annotation.setType(map.get(RDFConstant.R_TYPE).getObject());
        XPointer xpointer = XPointerFactory.getXPointer(map.get(RDFConstant.A_CONTEXT).getObject());

        annotation.setXpointer(xpointer);
        if (map.containsKey(RDFConstant.H_BODY)) {
            Statement s = map.get(RDFConstant.H_BODY);
            annotation.setBody(parseXMLLiteralForBody(s.getObject()));
        } else {
            Statement bodyStatement = map.get(RDFConstant.A_BODY);
            annotation.setBody(bodyStatement.getObject());
            if (bodyStatement.isResource()) {
                annotation.setBodyUrl(true);
            }
        }

        if (map.get(RDFConstant.D_CREATOR) != null && map.get(RDFConstant.D_CREATOR).getObject() != null) {
            annotation.setAuthor(map.get(RDFConstant.D_CREATOR).getObject());
        }
        if (map.get(RDFConstant.D_DATE) != null && map.get(RDFConstant.D_DATE).getObject() != null) {
            annotation.setStringDate(map.get(RDFConstant.D_DATE).getObject());
        }

        for (Map.Entry<String, Statement> entry : map.entrySet()) {
            fields.put(entry.getKey(), entry.getValue().getObject());
        }
        annotation.setFields(fields);

        return annotation;
    }

    private String parseXMLLiteralForBody(String html) {
        if (html.contains("<body>")) {
            String body = "<body>";
            int beginIndex = html.indexOf(body) + body.length();
            int endIndex = html.indexOf("</body>");
            return html.substring(beginIndex, endIndex);
        } else if (html.contains("&lt;body&gt;")) {
            String body = "&lt;body&gt;";
            int beginIndex = html.indexOf(body) + body.length();
            int endIndex = html.indexOf("&lt;/body&gt;");
            return html.substring(beginIndex, endIndex);
        }
        return "";
    }

}
