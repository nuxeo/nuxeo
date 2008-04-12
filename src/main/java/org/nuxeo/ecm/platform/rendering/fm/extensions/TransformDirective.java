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
import java.net.URL;
import java.util.Map;

import org.nuxeo.ecm.core.url.nxdoc.PropertyURL;
import org.nuxeo.ecm.platform.rendering.api.RenderingException;
import org.nuxeo.ecm.platform.rendering.api.RenderingTransformer;
import org.nuxeo.ecm.platform.rendering.fm.FreemarkerEngine;
import org.nuxeo.ecm.platform.rendering.fm.adapters.RootContextModel;

import freemarker.core.Environment;
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class TransformDirective implements TemplateDirectiveModel {

	@SuppressWarnings("unchecked")
	public void execute(Environment env, Map params, TemplateModel[] loopVars,
			TemplateDirectiveBody body) throws TemplateException, IOException {

	    String name = null;
	    SimpleScalar scalar = (SimpleScalar)params.get("name");
	    if (scalar != null) {
	        name = scalar.getAsString();
	    }

	    if (name == null) {
	        throw new TemplateModelException("Transformer must have a name. Use name attribute for this");
	    }

	    String src = null;
	    scalar = (SimpleScalar)params.get("src");
	    if (scalar != null) {
	        src = scalar.getAsString();
	    }

	    String property = null;
	    scalar = (SimpleScalar)params.get("property");
	    if (scalar != null) {
	        property = scalar.getAsString();
	    }

	    RootContextModel ctx = (RootContextModel)env.getCustomAttribute(FreemarkerEngine.ROOT_CTX_KEY);
	    if (ctx == null) {
	        throw new TemplateModelException("Not in a nuxeo rendering context");
	    }

	    RenderingTransformer tr = ctx.getEngine().getTransformer(name);
	    if (tr == null) {
	        throw new TemplateModelException("Unknown Transformer: "+name);
	    }

	    try {
	        String content = null;
	        if (property != null) {
                URL url = PropertyURL.getURL(ctx.getDocument(), property);
                tr.transform(url, env.getOut(), ctx.getThisContext());
	        } else if (src == null) {
	            if (body == null) {
	                throw new TemplateModelException("Transform directive must have either a content either a valid 'src' attribute");
	            }
	            // render body to get back the result
	            StringWriter writer = new StringWriter();
	            body.render(writer);
	            content = writer.getBuffer().toString();
	            tr.transform(new StringReader(content), env.getOut(), ctx.getThisContext());
	        } else {
	            URL url = ctx.getEngine().getResourceLocator().getResource(src);
	            tr.transform(url, env.getOut(), ctx.getThisContext());
	        }
	    } catch (RenderingException e) {
	        throw new TemplateException("Running "+name+" transformer failed", e, env);
	    }
	}

}
