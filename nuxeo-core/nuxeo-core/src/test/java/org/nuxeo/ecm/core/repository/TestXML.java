/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.core.repository;

import java.io.ByteArrayInputStream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class TestXML {

    @Test
    public void testToString() throws Exception {
        String xmlInput = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<doc>\n"
                + "    <head>text text text</head>\n"
                + "    <body id=\"theBody\">aaaaaa<a>nnn</a>\n"
                + "    </body>\n"
                + "</doc>\n";
        DocumentBuilderFactory builderFactory = XML.getBuilderFactory();
        Document doc = builderFactory.newDocumentBuilder().parse(new ByteArrayInputStream(xmlInput.getBytes()));

        assertEquals(xmlInput, XML.toString(doc));

        NodeList body = doc.getElementsByTagName("body");
        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<body id=\"theBody\">aaaaaa<a>nnn</a>\n"
                + "</body>\n";
        assertEquals(expected, XML.toString((Element) body.item(0)));
    }

}
