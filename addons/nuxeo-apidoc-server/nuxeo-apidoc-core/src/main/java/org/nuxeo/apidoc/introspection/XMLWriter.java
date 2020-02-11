/*
 * Copyright 2001-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Bogdan Stefanescu
 */
package org.nuxeo.apidoc.introspection;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import javax.xml.namespace.QName;

/**
 * This file contains code from org.apache.commons.betwixt.XMLUtils
 */
public class XMLWriter {

    protected static final String CRLF = System.getProperty("line.separator");

    protected int indent;

    protected Writer writer;

    protected String crlf;

    protected boolean emitHeader = true;

    protected String encoding;

    protected ArrayList<String> globalNsMap;

    protected Element element; // current element

    protected int depth = -1;

    public XMLWriter(Writer writer) {
        this(writer, 0);
    }

    public XMLWriter(Writer writer, int indent) {
        this(writer, indent, CRLF);
    }

    public XMLWriter(Writer writer, int indent, String crlf) {
        this.writer = writer;
        this.indent = indent;
        this.crlf = crlf;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public String getEncoding() {
        return encoding;
    }

    public void putXmlns(String uri) {
        putXmlns("", uri);
    }

    public void putXmlns(String prefix, String uri) {
        if (globalNsMap == null) {
            globalNsMap = new ArrayList<>();
        }
        globalNsMap.add(uri);
        globalNsMap.add(prefix);
    }

    public String getXmlNs(String uri) {
        if (globalNsMap != null) {
            for (int i = 0, len = globalNsMap.size(); i < len; i += 2) {
                if (uri.equals(globalNsMap.get(i))) {
                    return globalNsMap.get(i + 1);
                }
            }
        }
        return null;
    }

    public void setIndent(int indent) {
        this.indent = indent;
    }

    public int getIndent() {
        return indent;
    }

    public void setCRLF(String crlf) {
        this.crlf = crlf;
    }

    public String getCRLF() {
        return crlf;
    }

    public void setWriter(Writer writer) {
        this.writer = writer;
    }

    public Writer getWriter() {
        return writer;
    }

    public void setEmitHeader(boolean emitHeader) {
        this.emitHeader = emitHeader;
    }

    public boolean isEmitHeader() {
        return emitHeader;
    }

    public void flush() throws IOException {
        writer.flush();
    }

    public void close() throws IOException {
        writer.close();
    }

    protected void done() throws IOException {
        writer.flush();
        // TODO check for errors
    }

    public XMLWriter write(String text) throws IOException {
        writer.write(text);
        return this;
    }

    public void indent(String text) throws IOException {
        if (indent > 0) {
            writer.write(crlf);
            char[] buf = new char[depth * indent];
            Arrays.fill(buf, ' ');
            writer.write(buf);
        }
        writer.write(text);
    }

    public XMLWriter element(String name) throws IOException {
        if (element != null && !element.isContainer) { // a non closed sibling -
                                                       // close it
            pop();
            writer.write("/>");
        }
        indent("<");
        writer.write(name);
        if (element == null) { // the first element - write any global ns
            if (globalNsMap != null) {
                for (int i = 0, len = globalNsMap.size(); i < len; i += 2) {
                    String prefix = globalNsMap.get(i + 1);
                    String uri = globalNsMap.get(i);
                    writer.write(" xmlns");
                    if (prefix != null && prefix.length() > 0) {
                        writer.write(":");
                        writer.write(prefix);
                    }
                    writer.write("=\"");
                    writer.write(uri);
                    writer.write("\"");
                }
            }
        }
        push(name); // push myself to the stack
        return this;
    }

    public XMLWriter start() throws IOException {
        depth++;
        if (element == null) { // the root
            if (emitHeader) {
                if (encoding != null) {
                    writer.write("<?xml version=\"1.0\" encoding=" + encoding + "?>");
                } else {
                    writer.write("<?xml version=\"1.0\"?>");
                }
                writer.write(crlf);
            }
        } else {
            element.isContainer = true;
            writer.write(">");
        }
        return this;
    }

    public XMLWriter end() throws IOException {
        depth--;
        if (element == null) {
            done();
        } else {
            if (!element.isContainer) { // a child element - close it
                pop();
                writer.write("/>");
            }
            Element myself = pop(); // close myself
            indent("</");
            writer.write(myself.name);
            writer.write(">");
        }
        return this;
    }

    public XMLWriter content(String text) throws IOException {
        start();
        depth--;
        writer.write(text);
        Element elem = pop(); // close myself
        writer.write("</");
        writer.write(elem.name);
        writer.write(">");
        return this;
    }

    public XMLWriter econtent(String text) throws IOException {
        return content(escapeBodyValue(text));
    }

    public XMLWriter content(boolean value) throws IOException {
        return content(value ? "true" : "false");
    }

    public XMLWriter content(Date value) throws IOException {
        return content(DateTimeFormat.abderaFormat(value));
    }

    public XMLWriter text(String text) throws IOException {
        indent(text);
        return this;
    }

    public XMLWriter etext(String text) throws IOException {
        return text(escapeBodyValue(text));
    }

    public XMLWriter attr(String name, Object value) throws IOException {
        writer.write(" ");
        writer.write(name);
        writer.write("=\"");
        writer.write(value.toString());
        writer.write("\"");
        return this;
    }

    public XMLWriter eattr(String name, Object value) throws IOException {
        return attr(name, escapeAttributeValue(value));
    }

    public XMLWriter xmlns(String value) throws IOException {
        attr("xmlns", value);
        element.putXmlns("", value);
        return this;
    }

    public XMLWriter xmlns(String name, String value) throws IOException {
        attr("xmlns:" + name, value);
        element.putXmlns(name, value);
        return this;
    }

    public XMLWriter attr(String name) throws IOException {
        writer.write(" ");
        writer.write(name);
        writer.write("=\"");
        return this;
    }

    public XMLWriter string(String value) throws IOException {
        writer.write(value);
        writer.write("\"");
        return this;
    }

    public XMLWriter object(Object value) throws IOException {
        writer.write(value.toString());
        writer.write("\"");
        return this;
    }

    public XMLWriter date(Date value) throws IOException {
        writer.write(format(value));
        writer.write("\"");
        return this;
    }

    public XMLWriter integer(long value) throws IOException {
        writer.write(Long.toString(value));
        writer.write("\"");
        return this;
    }

    public XMLWriter number(double value) throws IOException {
        writer.write(Double.toString(value));
        writer.write("\"");
        return this;
    }

    public XMLWriter bool(String name, boolean value) throws IOException {
        attr(name, value ? "true" : "false");
        return this;
    }

    public XMLWriter integer(String name, long value) throws IOException {
        return attr(name, Long.toString(value));
    }

    public XMLWriter number(String name, double value) throws IOException {
        return attr(name, Double.toString(value));
    }

    public XMLWriter date(String name, Date value) throws IOException {
        return attr(name, value.toString());
    }

    public String resolve(QName name) {
        String prefix = null;
        String uri = name.getNamespaceURI();
        if (element != null) {
            prefix = element.getXmlNs(uri);
            if (prefix == null) {
                prefix = getXmlNs(uri);
            }
        } else {
            prefix = getXmlNs(uri);
        }
        if (prefix == null) {
            return name.toString();
        }
        if (prefix.length() == 0) {
            return name.getLocalPart();
        }
        return prefix + ":" + name.getLocalPart();
    }

    public XMLWriter element(QName name) throws IOException {
        return element(resolve(name));
    }

    public XMLWriter attr(QName name, Object value) throws IOException {
        return attr(resolve(name), value);
    }

    public XMLWriter eattr(QName name, Object value) throws IOException {
        return eattr(resolve(name), value);
    }

    public XMLWriter attr(QName name) throws IOException {
        return attr(resolve(name));
    }

    public XMLWriter bool(QName name, boolean value) throws IOException {
        return bool(resolve(name), value);
    }

    public XMLWriter integer(QName name, long value) throws IOException {
        return integer(resolve(name), value);
    }

    public XMLWriter number(QName name, double value) throws IOException {
        return number(resolve(name), value);
    }

    public XMLWriter date(QName name, Date value) throws IOException {
        return date(resolve(name), value);
    }

    public static String format(Date date) {
        StringBuilder sb = new StringBuilder();
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        c.setTime(date);
        sb.append(c.get(Calendar.YEAR));
        sb.append('-');
        int f = c.get(Calendar.MONTH);
        if (f < 9) {
            sb.append('0');
        }
        sb.append(f + 1);
        sb.append('-');
        f = c.get(Calendar.DATE);
        if (f < 10) {
            sb.append('0');
        }
        sb.append(f);
        sb.append('T');
        f = c.get(Calendar.HOUR_OF_DAY);
        if (f < 10) {
            sb.append('0');
        }
        sb.append(f);
        sb.append(':');
        f = c.get(Calendar.MINUTE);
        if (f < 10) {
            sb.append('0');
        }
        sb.append(f);
        sb.append(':');
        f = c.get(Calendar.SECOND);
        if (f < 10) {
            sb.append('0');
        }
        sb.append(f);
        sb.append('.');
        f = c.get(Calendar.MILLISECOND);
        if (f < 100) {
            sb.append('0');
        }
        if (f < 10) {
            sb.append('0');
        }
        sb.append(f);
        sb.append('Z');
        return sb.toString();
    }

    public static final String LESS_THAN_ENTITY = "&lt;";

    public static final String GREATER_THAN_ENTITY = "&gt;";

    public static final String AMPERSAND_ENTITY = "&amp;";

    public static final String APOSTROPHE_ENTITY = "&apos;";

    public static final String QUOTE_ENTITY = "&quot;";

    /**
     * <p>
     * Escape the <code>toString</code> of the given object. For use as body text.
     * </p>
     *
     * @param value escape <code>value.toString()</code>
     * @return text with escaped delimiters
     */
    public static final String escapeBodyValue(Object value) {
        StringBuilder sb = new StringBuilder(value.toString());
        for (int i = 0, size = sb.length(); i < size; i++) {
            switch (sb.charAt(i)) {
            case '<':
                sb.replace(i, i + 1, LESS_THAN_ENTITY);
                size += 3;
                i += 3;
                break;
            case '>':
                sb.replace(i, i + 1, GREATER_THAN_ENTITY);
                size += 3;
                i += 3;
                break;
            case '&':
                sb.replace(i, i + 1, AMPERSAND_ENTITY);
                size += 4;
                i += 4;
                break;
            }
        }
        return sb.toString();
    }

    /**
     * <p>
     * Escape the <code>toString</code> of the given object. For use in an attribute value.
     * </p>
     *
     * @param value escape <code>value.toString()</code>
     * @return text with characters restricted (for use in attributes) escaped
     */
    public static final String escapeAttributeValue(Object value) {
        StringBuilder sb = new StringBuilder(value.toString());
        for (int i = 0, size = sb.length(); i < size; i++) {
            switch (sb.charAt(i)) {
            case '<':
                sb.replace(i, i + 1, LESS_THAN_ENTITY);
                size += 3;
                i += 3;
                break;
            case '>':
                sb.replace(i, i + 1, GREATER_THAN_ENTITY);
                size += 3;
                i += 3;
                break;
            case '&':
                sb.replace(i, i + 1, AMPERSAND_ENTITY);
                size += 4;
                i += 4;
                break;
            case '\'':
                sb.replace(i, i + 1, APOSTROPHE_ENTITY);
                size += 5;
                i += 5;
                break;
            case '\"':
                sb.replace(i, i + 1, QUOTE_ENTITY);
                size += 5;
                i += 5;
                break;
            }
        }
        return sb.toString();
    }

    Element push(String name) {
        element = new Element(name);
        return element;
    }

    Element pop() {
        Element el = element;
        if (el != null) {
            element = el.parent;
        }
        return el;
    }

    class Element {
        final String name;

        final Element parent;

        ArrayList<String> nsMap;

        boolean isContainer;

        Element(String name) {
            this.name = name;
            parent = element;
        }

        void putXmlns(String prefix, String uri) {
            if (nsMap == null) {
                nsMap = new ArrayList<>();
            }
            nsMap.add(uri);
            nsMap.add(prefix);
        }

        String getXmlNs(String uri) {
            if (nsMap != null) {
                for (int i = 0, len = nsMap.size(); i < len; i += 2) {
                    if (uri.equals(nsMap.get(i))) {
                        return nsMap.get(i + 1);
                    }
                }
            }
            if (parent != null) {
                return parent.getXmlNs(uri);
            }
            return null;
        }
    }

}
