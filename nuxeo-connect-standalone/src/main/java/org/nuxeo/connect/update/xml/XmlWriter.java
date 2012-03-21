/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.connect.update.xml;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class XmlWriter {

    protected final String tab;

    protected StringBuilder sb;

    protected String indent;

    protected int depth;

    /**
     * Pretty print with a tab of 2 space characters.
     */
    public XmlWriter() {
        this("  ");
    }

    public XmlWriter(String tab) {
        this.tab = tab == null ? "" : tab;
        reset();
    }

    public void reset() {
        depth = 0;
        this.indent = "";
        this.sb = new StringBuilder(1024);
    }

    public void writeXmlDecl() {
        sb.append("<?xml version=\"1.0\"?>").append("\n\n");
    }

    protected void updateIndent() {
        if (depth <= 0) {
            indent = "";
            return;
        }
        int len = tab.length();
        if (len > 0) {
            StringBuilder buf = new StringBuilder(len * depth);
            for (int i = 0; i < depth; i++) {
                buf.append(tab);
            }
            indent = buf.toString();
        }
    }

    protected final void inc() {
        depth++;
        updateIndent();
    }

    protected final void dec() {
        depth--;
        updateIndent();
    }

    public final void start(String name) {
        sb.append(indent).append('<').append(name);
    }

    public final void end(String name) {
        dec();
        sb.append(indent).append("</").append(name).append(">\n");
    }

    public final void startContent() {
        sb.append(">\n");
        inc();
    }

    public final void end() {
        sb.append("/>\n");
    }

    public final void crlf() {
        sb.append("\n");
    }

    public final void attr(String name, String value) {
        if (value != null) {
            sb.append(" ").append(name).append("=\"").append(value).append("\"");
        }
    }

    protected void text(String text) {
        sb.append(text);
    }

    public void element(String name, String value) {
        if (value != null) {
            sb.append(indent).append('<').append(name).append('>').append(value).append(
                    "</").append(name).append(">\n");
        }
    }

    public StringBuilder getBuffer() {
        return sb;
    }

    @Override
    public String toString() {
        return sb.toString();
    }
}
