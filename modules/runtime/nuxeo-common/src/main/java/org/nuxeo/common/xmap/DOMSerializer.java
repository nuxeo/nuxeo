/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.common.xmap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.ranges.DocumentRange;
import org.w3c.dom.ranges.Range;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public final class DOMSerializer {

    private static final DocumentBuilderFactory BUILDER_FACTORY = DocumentBuilderFactory.newInstance();

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
    private DOMSerializer() {
    }

    /**
     * @return the builderFactory
     */
    public static DocumentBuilderFactory getBuilderFactory() {
        return BUILDER_FACTORY;
    }

    public static String toString(Element element) throws IOException {
        return toString(element, DEFAULT_FORMAT);
    }

    public static String toStringOmitXml(Element element) throws IOException {
        OutputFormat of = new OutputFormat();
        of.setOmitXMLDeclaration(true);
        return toString(element, of);
    }

    public static String toString(Element element, OutputFormat format) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        write(element, format, baos);
        return baos.toString();
    }

    public static String toString(DocumentFragment fragment) throws IOException {
        return toString(fragment, DEFAULT_FORMAT);
    }

    public static String toString(DocumentFragment fragment, OutputFormat format) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        write(fragment, format, baos);
        return baos.toString();
    }

    public static String toString(Document doc) throws IOException {
        return toString(doc, DEFAULT_FORMAT);
    }

    public static String toString(Document doc, OutputFormat format) throws IOException {
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

    public static void write(DocumentFragment fragment, OutputStream out) throws IOException {
        write(fragment, DEFAULT_FORMAT, out);
    }

    public static void write(DocumentFragment fragment, OutputFormat format, OutputStream out) throws IOException {
        XMLSerializer serializer = new XMLSerializer(out, format);
        serializer.asDOMSerializer().serialize(fragment);
    }

    public static void write(Document doc, OutputStream out) throws IOException {
        write(doc, DEFAULT_FORMAT, out);
    }

    public static void write(Document doc, OutputFormat format, OutputStream out) throws IOException {
        XMLSerializer serializer = new XMLSerializer(out, format);
        serializer.asDOMSerializer().serialize(doc);
    }

    public static DocumentFragment getContentAsFragment(Element element) {
        Node node = element.getFirstChild();
        if (node == null) {
            return null; // no content
        }
        Range range = ((DocumentRange) element.getOwnerDocument()).createRange();
        range.setStartBefore(node);
        range.setEndAfter(element.getLastChild());
        return range.cloneContents();
    }

}
