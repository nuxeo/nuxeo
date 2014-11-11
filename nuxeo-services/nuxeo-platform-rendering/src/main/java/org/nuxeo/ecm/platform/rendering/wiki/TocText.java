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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.rendering.wiki;

import java.io.IOException;
import java.io.Writer;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class TocText implements WikiText {

    protected String title = "Table of Contents";

    public TocText(String title) {
        if (title != null) {
            title = title.trim();
            if (title.length() > 0) {
                this.title = title;
            }
        }
    }

    public void writeTo(WikiSerializerHandler handler, Writer writer)
            throws IOException {
        printToc(handler, writer);
    }

    public void printToc(WikiSerializerHandler serializer, Writer writer) throws IOException {
        printTocHeader(serializer,  writer, title);
        Toc.Entry h = serializer.toc.head.firstChild;
        if (h != null) {
            prinEntry(serializer, writer, h);
        }
        printTocFooter(serializer, writer);
    }

    private void prinEntry(WikiSerializerHandler serializer, Writer writer, Toc.Entry entry) throws IOException {
        printHeading(serializer, writer, entry);
        if (entry.firstChild != null) {
            writer.write("<ol>"+WikiWriter.LINE_SEP);
            prinEntry(serializer, writer, entry.firstChild);
            writer.write("</ol>"+WikiWriter.LINE_SEP);
        }
        if (entry.next != null) {
            prinEntry(serializer, writer, entry.next);
        }
    }

    protected void printTocHeader(WikiSerializerHandler serializer, Writer writer, String title) throws IOException {
        writer.write("<table class=\"toc\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">");
        writer.write(WikiWriter.LINE_SEP);
        writer.write("<tr><td>");
        writer.write("<div class=\"tocTitle\">"+title+"</div>");
        writer.write("</td></tr>");
        writer.write(WikiWriter.LINE_SEP);
        writer.write("<tr><td>");
        writer.write(WikiWriter.LINE_SEP);
        writer.write("<ol class=\"contentToc\">");
        writer.write(WikiWriter.LINE_SEP);
    }

    protected void printTocFooter(WikiSerializerHandler serializer, Writer writer) throws IOException {
        writer.write("</ol>");
        writer.write(WikiWriter.LINE_SEP);
        writer.write("</td></tr>");
        writer.write(WikiWriter.LINE_SEP);
        writer.write("</table>");
        writer.write(WikiWriter.LINE_SEP);
    }

    protected void printHeading(WikiSerializerHandler serializer, Writer writer, Toc.Entry entry) throws IOException {
        writer.write("<li><a href=\"#heading_"+entry.id+"\">"+entry.title+"</a></li>"+WikiWriter.LINE_SEP);
    }

}
