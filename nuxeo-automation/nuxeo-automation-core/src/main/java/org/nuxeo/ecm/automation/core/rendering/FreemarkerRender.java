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
 */
package org.nuxeo.ecm.automation.core.rendering;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.Map;

import org.nuxeo.ecm.platform.rendering.api.ResourceLocator;
import org.nuxeo.ecm.platform.rendering.fm.FreemarkerEngine;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.resource.ResourceService;

import freemarker.core.Environment;
import freemarker.template.Template;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class FreemarkerRender extends FreemarkerEngine implements Renderer {

    public FreemarkerRender() {
        setResourceLocator(new ResourceLocator() {
            public URL getResourceURL(String key) {
                try {
                    if (key.startsWith(Renderer.TEMPLATE_PREFIX)) {
                        return Framework.getService(ResourceService.class).getResource(
                                key.substring(Renderer.TEMPLATE_PREFIX.length()));
                    } else {
                        return new URL(key);
                    }
                } catch (Exception e) {
                    return null;
                }
            }

            public File getResourceFile(String key) {
                return null;
            }
        });
    }

    public void renderContent(String content, Object ctx, Writer writer)
            throws Exception {
        StringReader reader = new StringReader(content);
        Template tpl = new Template("@inline", reader, getConfiguration(),
                "UTF-8");
        Environment env = tpl.createProcessingEnvironment(ctx, writer,
                getObjectWrapper());
        env.process();
    }

    public String render(String uriOrContent, Map<String, Object> root) throws Exception {
        if (root.get("Document") != null) {
            // mvel wrapper not supported in freemarker
            root.put("Document", root.get("This"));
        }
        StringWriter result = new StringWriter();
        if (uriOrContent.startsWith(Renderer.TEMPLATE_PREFIX)) {
            render(uriOrContent, root, result);
        } else {
            renderContent(uriOrContent, root, result);
        }
        return result.getBuffer().toString();
    }
}
