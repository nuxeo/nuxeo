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

package org.nuxeo.ecm.platform.rendering.wiki.extensions;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import org.nuxeo.ecm.platform.rendering.wiki.WikiMacro;
import org.nuxeo.ecm.platform.rendering.wiki.WikiSerializerHandler;
import org.wikimodel.wem.WikiParameters;

import freemarker.core.Environment;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class FreemarkerMacro implements WikiMacro {

    @Override
    public String getName() {
        return "freemarker";
    }

    @Override
    public void eval(WikiParameters params, String content, WikiSerializerHandler serializer) throws IOException,
            TemplateException {
        Environment env = serializer.getEnvironment();
        if (env != null) {
            Template tpl = new Template("inline", new StringReader(content), env.getConfiguration(),
                    env.getTemplate().getEncoding());
            @SuppressWarnings("resource") // not ours to close
            Writer oldw = env.getOut();
            Writer neww = new StringWriter();
            try {
                env.setOut(neww);
                env.include(tpl);
            } finally {
                env.setOut(oldw);
            }
            serializer.getWriter().print(neww.toString());
        }
    }

    @Override
    public void evalInline(WikiParameters params, String content, WikiSerializerHandler serializer) throws IOException,
            TemplateException {
        eval(params, content, serializer);
    }

}
