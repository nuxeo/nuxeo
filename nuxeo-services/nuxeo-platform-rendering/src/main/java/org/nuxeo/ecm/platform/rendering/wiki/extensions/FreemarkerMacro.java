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

package org.nuxeo.ecm.platform.rendering.wiki.extensions;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import org.nuxeo.ecm.platform.rendering.wiki.WikiMacro;
import org.nuxeo.ecm.platform.rendering.wiki.WikiSerializerHandler;
import org.wikimodel.wem.WikiParameters;

import freemarker.core.Environment;
import freemarker.template.Template;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class FreemarkerMacro implements WikiMacro {

    public String getName() {
        return "freemarker";
    }

    public void eval(WikiParameters params, String content, WikiSerializerHandler serializer) throws Exception {
        Environment env = serializer.getEnvironment();
        if (env != null) {
            Template tpl = new Template("inline", new StringReader(content),
                    env.getConfiguration(), env.getTemplate().getEncoding());
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

    public void evalInline(WikiParameters params, String content,
            WikiSerializerHandler serializer) throws Exception {
        eval(params, content, serializer);
    }

}
