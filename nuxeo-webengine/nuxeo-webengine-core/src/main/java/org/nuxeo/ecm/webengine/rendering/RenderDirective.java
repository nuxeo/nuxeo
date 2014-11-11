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

    public void execute(Environment env, Map params, TemplateModel[] loopVars,
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
