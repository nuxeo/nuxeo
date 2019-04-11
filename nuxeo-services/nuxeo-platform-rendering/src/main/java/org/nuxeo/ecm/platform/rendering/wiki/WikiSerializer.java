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
 */
public class WikiSerializer {

    public static final Log log = LogFactory.getLog(WikiSerializer.class);

    protected final CommonWikiParser parser;

    protected final Map<String, WikiMacro> macros = new HashMap<>();

    protected final List<WikiFilter> filters = new ArrayList<>();

    public WikiSerializer() {
        parser = new CommonWikiParser();
        registerMacro(new TocMacro());
    }

    public void registerMacro(WikiMacro macro) {
        macros.put(macro.getName(), macro);
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
