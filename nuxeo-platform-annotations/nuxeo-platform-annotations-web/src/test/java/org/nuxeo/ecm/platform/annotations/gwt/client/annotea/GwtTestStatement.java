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

package org.nuxeo.ecm.platform.annotations.gwt.client.annotea;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.NodeList;
import com.google.gwt.xml.client.XMLParser;

/**
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 *
 */
public class GwtTestStatement extends GWTTestCase {

    @Override
    public String getModuleName() {
        return "org.nuxeo.ecm.platform.annotations.gwt.AnnotationPanel";
    }

    public void testStatement() {
        int x = 0;
        Document doc = XMLParser.parse(nodeStatements);
        NodeList nodes = doc.getChildNodes().item(0).getChildNodes();
        while (nodes.item(x).getNodeName().equals("#text")) {
            x++;
        }
        Statement s = new Statement(nodes.item(x));
        assertEquals(RDFConstant.D_TITLE, s.getPredicate());
        assertEquals("Annotation of Sample Page 1", s.getObject());
        x++;
        while (nodes.item(x).getNodeName().equals("#text")) {
            x++;
        }
        s = new Statement(nodes.item(x));
        assertEquals(RDFConstant.A_CONTEXT, s.getPredicate());
        assertEquals(
                "http://serv1.example.com/some/page.html#xpointer(id(\"Main\")/p[2])",
                s.getObject());
        x++;
        while (nodes.item(x).getNodeName().equals("#text")) {
            x++;
        }
        s = new Statement(nodes.item(x));
        assertEquals(RDFConstant.A_BODY, s.getPredicate());
        x++;
        while (nodes.item(x).getNodeName().equals("#text")) {
            x++;
        }
        s = new Statement(nodes.item(x));
        assertEquals(RDFConstant.A_BODY, s.getPredicate());
    }

    private static final String nodeStatements = ""
            + " <r:RDF xmlns:r=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" "
            + "    xmlns:a=\"http://www.w3.org/2000/10/annotation-ns#\" "
            + "    xmlns:d=\"http://purl.org/dc/elements/1.1/\"> "
            + "        <d:title>Annotation of Sample Page 1</d:title>"
            + "        <a:context>"
            + "            http://serv1.example.com/some/page.html#xpointer(id(\"Main\")/p[2])"
            + "        </a:context>"
            + "        <a:body r:datatype=\"http://www.w3.org/1999/02/22-r-syntax-ns#XMLLiteral\">fqsdfqsdf</a:body>"
            + "        <a:body r:resource=\"http://annotea.example.org/Annotation/body/3ACF6D754\" />"
            + "</r:RDF>";
}
