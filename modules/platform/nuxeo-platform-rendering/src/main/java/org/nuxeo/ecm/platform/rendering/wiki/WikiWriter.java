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
 * Special writer used to split the serialization result in dynamic or static segments. This way we can generate final
 * output after parsing the entire file. This is needed for example to generate TOC.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class WikiWriter implements IWikiPrinter, WikiText {

    protected static final Log log = LogFactory.getLog(WikiWriter.class);

    protected static final String LINE_SEP = System.getProperty("line.separator");

    protected WikiWriter parent;

    protected final List<String> segments = new ArrayList<>();

    protected final List<WikiText> dynamicSegments = new ArrayList<>();

    protected final StringBuilder sb = new StringBuilder();

    public WikiWriter() {
    }

    public WikiWriter(WikiWriter parent) {
        this.parent = parent;
    }

    @Override
    public void print(String str) {
        sb.append(str);
    }

    public void println() {
        sb.append(LINE_SEP);
    }

    @Override
    public void println(String str) {
        sb.append(str);
        sb.append(LINE_SEP);
    }

    public void writeText(WikiText text) {
        segments.add(sb.toString());
        sb.setLength(0);
        dynamicSegments.add(text);
    }

    public WikiWriter getParent() {
        return parent;
    }

    public StringBuilder getBuffer() {
        return sb;
    }

    @Override
    public void writeTo(WikiSerializerHandler handler, Writer writer) throws IOException {
        for (int i = 0, len = segments.size(); i < len; i++) {
            writer.write(segments.get(i));
            dynamicSegments.get(i).writeTo(handler, writer);
        }
        writer.write(sb.toString());
    }

}
