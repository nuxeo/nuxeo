/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.rendering.wiki;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wikimodel.wem.IWikiPrinter;

/**
 * Special writer used to split the serialization result in dynamic or static segments.
 * This way we can generate final output after parsing the entire file. This is needed for example to
 * generate TOC.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class WikiWriter implements IWikiPrinter, WikiText {

    protected static final Log log = LogFactory.getLog(WikiWriter.class);

    protected static final String LINE_SEP = System.getProperty("line.separator");

    protected WikiWriter parent;
    protected final List<String> segments = new ArrayList<String>();
    protected final List<WikiText> dynamicSegments = new ArrayList<WikiText>();
    protected final StringBuilder buf = new StringBuilder();


    public WikiWriter() {
    }

    public WikiWriter(WikiWriter parent) {
        this.parent = parent;
    }

    public void print(String str) {
        buf.append(str);
    }

    public void println() {
        buf.append(LINE_SEP);
    }

    public void println(String str) {
        buf.append(str);
        buf.append(LINE_SEP);
    }

    public void writeText(WikiText text) {
        segments.add(buf.toString());
        buf.setLength(0);
        dynamicSegments.add(text);
    }

    public WikiWriter getParent() {
        return parent;
    }

    public StringBuilder getBuffer() {
        return buf;
    }

    public void writeTo(WikiSerializerHandler handler, Writer writer) throws IOException {
        for (int i=0, len=segments.size(); i<len; i++) {
            writer.write(segments.get(i));
            dynamicSegments.get(i).writeTo(handler, writer);
        }
        writer.write(buf.toString());
    }

}
