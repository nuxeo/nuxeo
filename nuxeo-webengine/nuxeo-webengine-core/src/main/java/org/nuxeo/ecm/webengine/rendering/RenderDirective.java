/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.webengine.rendering;

import java.io.IOException;
import java.util.Map;

import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.model.impl.AbstractWebContext;
import org.nuxeo.ecm.webengine.scripting.ScriptFile;

import freemarker.core.Environment;
import freemarker.template.SimpleScalar;
import freemarker.template.Template;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class RenderDirective implements TemplateDirectiveModel {

    @Override
    public void execute(Environment env, @SuppressWarnings("rawtypes") Map params, TemplateModel[] loopVars,
            TemplateDirectiveBody body) throws TemplateException, IOException {
        int size = params.size();
        if (size != 1) {
            throw new TemplateModelException("Invalid number of arguments for render(...) method");
        }

        SimpleScalar val = (SimpleScalar) params.get("src");
        if (val == null) {
            throw new TemplateModelException("src attribute is required");
        }
        String src = val.getAsString();

        WebContext ctx = WebEngine.getActiveContext();
        if (ctx != null) {
            ScriptFile script = ctx.getFile(src);
            Template tpl = env.getConfiguration().getTemplate(script.getURL());
            try {
                ((AbstractWebContext) ctx).pushScriptFile(script.getFile());
                env.include(tpl);
            } finally {
                ((AbstractWebContext) ctx).popScriptFile();
            }
        } else {
            throw new IllegalStateException("Not In a Web Context");
        }
    }

}
