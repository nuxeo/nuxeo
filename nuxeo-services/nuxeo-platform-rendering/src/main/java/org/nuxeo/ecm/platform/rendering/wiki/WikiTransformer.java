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

package org.nuxeo.ecm.platform.rendering.wiki;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.Map;

import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.platform.rendering.api.RenderingException;
import org.nuxeo.ecm.platform.rendering.fm.FreemarkerEngine;
import org.nuxeo.ecm.platform.rendering.fm.adapters.ComplexPropertyTemplate;
import org.nuxeo.ecm.platform.rendering.wiki.extensions.FreemarkerMacro;

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
public class WikiTransformer implements TemplateDirectiveModel {

    protected final WikiSerializer serializer;

    public WikiTransformer() {
        this(new WikiSerializer());
    }

    public WikiTransformer(WikiSerializer serializer) {
        this.serializer = serializer;
        this.serializer.registerMacro(new FreemarkerMacro());
        //TODO implement and register a JEXL extension
    }

    public WikiSerializer getSerializer() {
        return serializer;
    }

    public void transform(Reader reader, Writer writer)
            throws RenderingException {
        try {
            serializer.serialize(reader, writer);
        } catch (Exception e) {
            throw new RenderingException(e);
        }
    }

    public void transform(URL url, Writer writer)
            throws RenderingException {
        Reader reader = null;
        try {
            InputStream in = url.openStream();
            reader = new BufferedReader(new InputStreamReader(in));
            transform(reader, writer);
        } catch (Exception e) {
            throw new RenderingException(e);
        } finally {
            if (reader != null) {
                try { reader.close(); } catch (Exception e) {}
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void execute(Environment env, Map params, TemplateModel[] loopVars,
            TemplateDirectiveBody body) throws TemplateException, IOException {

        // TODO: not used for now.
        String syntax = null;
        SimpleScalar scalar = (SimpleScalar)params.get("syntax");
        if (scalar != null) {
            syntax = scalar.getAsString();
        }

        scalar = (SimpleScalar) params.get("src");
        String src = null;
        if (scalar != null) {
            src = scalar.getAsString();
        }

        ComplexPropertyTemplate complex = (ComplexPropertyTemplate) params.get("property");
        Property property = null;
        if (complex != null) {
            property = (Property)complex.getAdaptedObject(null);
        }

        FreemarkerEngine engine = (FreemarkerEngine)env.getCustomAttribute(FreemarkerEngine.RENDERING_ENGINE_KEY);
        if (engine == null) {
            throw new TemplateModelException("Not in a nuxeo rendering context");
        }

        try {
            if (property != null) {
                //TODO XXX implement property support (with caching)
                throw new UnsupportedOperationException("Not Yet Implemented");
//                URL url = PropertyURL.getURL(ctxModel.getDocument(), property.getPath());
//                tr.transform(url, env.getOut(), ctxModel.getContext());
            } else if (src == null) {
                if (body == null) {
                    throw new TemplateModelException(
                            "Transform directive must have either a content either a valid 'src' attribute");
                }
                // render body to get back the result
                StringWriter writer = new StringWriter();
                body.render(writer);
                String content = writer.getBuffer().toString();
                transform(new StringReader(content), env.getOut());
            } else {
                if (src.contains(":/")) {
                    URL url = engine.getResourceLocator().getResourceURL(src);
                    if (url != null) {
                        transform(url, env.getOut());
                    } else {
                        throw new IllegalArgumentException("Cannot resolve the src attribute: "+src);
                    }
                } else {
                    File file = engine.getResourceLocator().getResourceFile(src);
                    if (file != null) {
                        transform(file.toURI().toURL(), env.getOut());
                    } else {
                        throw new IllegalArgumentException("Cannot resolve the src attribute: "+src);
                    }
                }
            }
        } catch (RenderingException e) {
            throw new TemplateException("Running wiki transformer failed", e, env);
        }
    }

}
