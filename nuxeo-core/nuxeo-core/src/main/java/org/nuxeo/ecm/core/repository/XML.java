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

package org.nuxeo.ecm.core.repository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public final class XML {

    private static final DocumentBuilderFactory builderFactory
            = DocumentBuilderFactory.newInstance();

    // Default output format which is : no xml declaration, no document type,
    // indent.
    private static final OutputFormat DEFAULT_FORMAT = new OutputFormat();

    static {
        DEFAULT_FORMAT.setOmitXMLDeclaration(false);
        DEFAULT_FORMAT.setIndenting(true);
        DEFAULT_FORMAT.setMethod("xml");
        DEFAULT_FORMAT.setEncoding("UTF-8");
    }

    // Utility class.
    private XML() {}

    public static DocumentBuilderFactory getBuilderFactory() {
        return builderFactory;
    }

    public static String toString(Element element) throws IOException {
        return toString(element, DEFAULT_FORMAT);
    }

    public static String toString(Element element, OutputFormat format) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        write(element, format, baos);
        return baos.toString();
    }

    public static String toString(Document doc)
            throws IOException {
        return toString(doc, DEFAULT_FORMAT);
    }

    public static String toString(Document doc, OutputFormat format)
            throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        write(doc, format, baos);
        return baos.toString();
    }

    public static void write(Element element, OutputStream out) throws IOException {
        write(element, DEFAULT_FORMAT, out);
    }

    public static void write(Element element, OutputFormat format, OutputStream out) throws IOException {
        XMLSerializer serializer = new XMLSerializer(out, format);
        serializer.asDOMSerializer().serialize(element);
    }

    public static void write(Document doc, OutputStream out) throws IOException {
        write(doc, DEFAULT_FORMAT, out);
    }

    public static void write(Document doc, OutputFormat format, OutputStream out) throws IOException {
        XMLSerializer serializer = new XMLSerializer(out, format);
        serializer.asDOMSerializer().serialize(doc);
    }

}
