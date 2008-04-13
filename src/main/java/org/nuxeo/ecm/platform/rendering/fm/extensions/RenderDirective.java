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

import org.nuxeo.ecm.platform.rendering.api.RenderingContext;
import org.nuxeo.ecm.platform.rendering.api.RenderingException;
import org.nuxeo.ecm.platform.rendering.api.RenderingTransformer;
import org.nuxeo.ecm.platform.rendering.fm.FreemarkerEngine;
import org.nuxeo.ecm.platform.rendering.fm.adapters.RenderingContextModel;

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

	    RenderingContextModel ctxModel = FreemarkerEngine.getContextModel(env);
	    if (ctxModel == null) {
	        throw new TemplateModelException("Not in a nuxeo rendering context");
	    }

        String type = null;
        SimpleScalar scalar = (SimpleScalar)params.get("type");
        if (scalar != null) {
            type = scalar.getAsString();
        }

	    RenderingContext ctx = ctxModel.pushContext();
	    if (ctx != null) {
	        String uri = ctx.getTemplate();
	        Template temp = env.getConfiguration().getTemplate(uri);
	        try {
	            render(type, ctxModel, env, temp);
	        } finally {
	            ctxModel.popContext();
	        }
	    }
	}

	protected void render(String type, RenderingContextModel ctxModel, Environment env, Template temp) throws TemplateException, IOException {
	    if (type != null) {
	        RenderingTransformer tr = ctxModel.getEngine().getTransformer(type);
	        if (tr != null) {
	            StringWriter writer = new StringWriter();
	            Writer out = env.getOut();
	            out.flush();
	            env.setOut(writer);
	            try {
	                env.include(temp);
	                tr.transform(new StringReader(writer.toString()), out, ctxModel.getContext());
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
