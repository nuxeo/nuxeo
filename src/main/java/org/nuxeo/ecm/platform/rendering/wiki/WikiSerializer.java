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
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.rendering.api.RenderingContext;
import org.wikimodel.wem.WikiParserException;
import org.wikimodel.wem.WikiPrinter;
import org.wikimodel.wem.common.CommonWikiParser;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class WikiSerializer {

    public static final Log log = LogFactory.getLog(WikiSerializer.class);

    protected CommonWikiParser parser;

    protected HashMap<String, WikiMacro> macros = new HashMap<String, WikiMacro>();
    protected HashMap<String, WikiExpression> expressions = new HashMap<String, WikiExpression>();
    protected ArrayList<WikiFilter> filters = new ArrayList<WikiFilter>();


    public WikiSerializer() {
        parser = new CommonWikiParser();
    }

    public void registerMacro(WikiMacro macro) {
        macros.put(macro.getName(), macro);
    }

    public void registerExpression(WikiExpression expression) {
        expressions.put(expression.getName(), expression);
    }

    public void addFilter(WikiFilter filter) {
        filters.add(filter);
    }

    public void serialize(Reader reader, Writer writer) throws IOException, WikiParserException {
        serialize(reader, writer, null);
        WikiPrinter printer = new WikiPrinter();
        WikiSerializerHandler serializer = new WikiSerializerHandler(this, printer);
        parser.parse(reader, serializer);
        writer.write(printer.getBuffer().toString());
        writer.flush();
    }

    public void serialize(Reader reader, Writer writer, RenderingContext ctx) throws IOException, WikiParserException {
        WikiPrinter printer = new WikiPrinter();
        WikiSerializerHandler serializer = new WikiSerializerHandler(this, printer, ctx);
        parser.parse(reader, serializer);
        writer.write(printer.getBuffer().toString());
        writer.flush();
    }

}
