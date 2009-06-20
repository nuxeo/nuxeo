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
 */
package org.nuxeo.build.ant.ftl;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;

import org.apache.tools.ant.filters.BaseFilterReader;
import org.apache.tools.ant.filters.ChainableReader;

import freemarker.cache.StringTemplateLoader;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class TemplateFilter extends BaseFilterReader implements ChainableReader {

    protected String content;
    protected int offset = 0;

    public TemplateFilter(Reader reader) {
        super(reader);
    }

    public Reader chain(Reader rdr) {
        TemplateFilter newFilter = new TemplateFilter(rdr);
        newFilter.setProject(getProject());
        return newFilter;
    }

    @Override
    public int read() throws IOException {
        if (content == null) {
            content = readFully();
            if (content == null || content.length() == 0) {
                return -1;
            }
            FreemarkerEngine engine = new FreemarkerEngine();
            StringTemplateLoader loader = new StringTemplateLoader();
            loader.putTemplate("content", content);
            engine.getConfiguration().setTemplateLoader(loader);
            StringWriter writer = new StringWriter();
            engine.process(getProject(), "content", writer);
            content = writer.getBuffer().toString();
        }
        if (offset >= content.length()) {
            return -1;
        }
        return content.charAt(offset++);
    }

}
