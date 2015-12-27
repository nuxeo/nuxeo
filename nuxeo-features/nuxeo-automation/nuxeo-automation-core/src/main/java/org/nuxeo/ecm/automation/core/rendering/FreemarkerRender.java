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
 */
package org.nuxeo.ecm.automation.core.rendering;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.nuxeo.ecm.platform.rendering.api.RenderingException;
import org.nuxeo.ecm.platform.rendering.api.ResourceLocator;
import org.nuxeo.ecm.platform.rendering.fm.FreemarkerEngine;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.resource.ResourceService;

import freemarker.core.Environment;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class FreemarkerRender extends FreemarkerEngine implements Renderer {

    public FreemarkerRender() {
        setResourceLocator(new ResourceLocator() {
            @Override
            public URL getResourceURL(String key) {
                try {
                    if (key.startsWith(Renderer.TEMPLATE_PREFIX)) {
                        return Framework.getService(ResourceService.class).getResource(
                                key.substring(Renderer.TEMPLATE_PREFIX.length()));
                    } else {
                        return new URL(key);
                    }
                } catch (MalformedURLException e) {
                    return null;
                }
            }

            @Override
            public File getResourceFile(String key) {
                return null;
            }
        });
    }

    public void renderContent(String content, Object ctx, Writer writer) throws IOException, TemplateException {
        StringReader reader = new StringReader(content);
        Template tpl = new Template("@inline", reader, getConfiguration(), "UTF-8");
        Environment env = tpl.createProcessingEnvironment(ctx, writer, getObjectWrapper());
        env.process();
    }

    @Override
    public String render(String uriOrContent, Map<String, Object> root) throws RenderingException, TemplateException,
            IOException {
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
