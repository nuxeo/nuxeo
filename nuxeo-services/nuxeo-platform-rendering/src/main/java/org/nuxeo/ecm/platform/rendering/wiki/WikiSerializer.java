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
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wikimodel.wem.WikiParserException;
import org.wikimodel.wem.common.CommonWikiParser;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class WikiSerializer {

    public static final Log log = LogFactory.getLog(WikiSerializer.class);

    protected final CommonWikiParser parser;

    protected final Map<String, WikiMacro> macros = new HashMap<String, WikiMacro>();
    protected final Map<String, WikiExpression> expressions = new HashMap<String, WikiExpression>();
    protected final List<WikiFilter> filters = new ArrayList<WikiFilter>();

    public WikiSerializer() {
        parser = new CommonWikiParser();
        registerMacro(new TocMacro());
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
        WikiSerializerHandler serializer = new WikiSerializerHandler(this);
        parser.parse(reader, serializer);
        serializer.getWriter().writeTo(serializer, writer);
        writer.flush();
    }

}
