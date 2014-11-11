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

package org.nuxeo.ecm.platform.rendering.fm.extensions;

import java.io.IOException;
import java.util.Map;

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
 *
 */
public class ExtendsDirective implements TemplateDirectiveModel {

    @SuppressWarnings("unchecked")
    public void execute(Environment env, Map params, TemplateModel[] loopVars,
            TemplateDirectiveBody body) throws TemplateException, IOException {

        if (body == null) {
            throw new TemplateModelException("Expecting a body");
        }

        String src = null;
        SimpleScalar scalar = (SimpleScalar) params.get("src");
        if (scalar != null) {
            src = scalar.getAsString();
        } else {
            throw new TemplateModelException("src attribute is not defined");
        }

        BlockWriter writer = (BlockWriter) env.getOut();
        writer.suppressOutput = true;
        body.render(writer);
        writer.suppressOutput = false;

        // now we should go into the base template and render it
        // String oldPath = writer.reg.path;
        // writer.reg.path = src;
        Template temp = env.getConfiguration().getTemplate(src);
        env.include(temp);
        // writer.reg.path = oldPath;
    }

}
