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
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

import org.nuxeo.ecm.platform.rendering.api.RenderingException;
import org.nuxeo.ecm.platform.rendering.api.RenderingTransformer;
import org.nuxeo.ecm.platform.rendering.fm.FreemarkerEngine;
import org.nuxeo.ecm.platform.rendering.fm.adapters.ContextDocumentTemplate;
import org.nuxeo.ecm.platform.rendering.fm.adapters.RootContextModel;

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
public class RenderDirective implements TemplateDirectiveModel {

	@SuppressWarnings("unchecked")
	public void execute(Environment env, Map params, TemplateModel[] loopVars,
			TemplateDirectiveBody body) throws TemplateException, IOException {
	    if (body != null) {
	        throw new TemplateModelException("Didn't expect a body");
	    }

	    RootContextModel ctx = FreemarkerEngine.getRootContext(env);
	    if (ctx == null) {
	        throw new TemplateModelException("Not in a nuxeo rendering context");
	    }

        String type = null;
        SimpleScalar scalar = (SimpleScalar)params.get("type");
        if (scalar != null) {
            type = scalar.getAsString();
        }

	    ContextDocumentTemplate doc = ctx.pushContext();
	    if (doc != null) {
	        String uri = doc.getContext().getTemplate();
	        Template temp = env.getConfiguration().getTemplate(uri);
	        try {
	            render(type, ctx, env, temp);
	        } finally {
	            ctx.popContext();
	        }
	    }
	}

	protected void render(String type, RootContextModel ctx, Environment env, Template temp) throws TemplateException, IOException {
	    if (type != null) {
	        RenderingTransformer tr = ctx.getEngine().getTransformer(type);
	        if (tr != null) {
	            StringWriter writer = new StringWriter();
	            Writer out = env.getOut();
	            out.flush();
	            env.setOut(writer);
	            try {
	                env.include(temp);
	                tr.transform(new StringReader(writer.toString()), out, ctx.getThisContext());
	            } catch (RenderingException e) {
	                throw new TemplateException("Failed to transform rendering result using transformer: "+type, e, env);
	            } finally {
	                env.setOut(out);
	            }
	        } else {
	            throw new TemplateException("Unknown rendering transformer: "+type, env);
	        }
	    } else { // no transformer specified
	        env.include(temp);
	    }
	}


}
