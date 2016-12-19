/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.mvel2.templates.CompiledTemplate;
import org.mvel2.templates.TemplateCompiler;
import org.mvel2.templates.TemplateRuntime;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.resource.ResourceService;

/**
 * MVEL rendering using a simple cache of compiled template.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class MvelRender implements Renderer {

    protected Map<String, CompiledTemplate> cache = Collections.synchronizedMap(new Cache());

    @Override
    public String render(String uriOrContent, Map<String, Object> root) throws OperationException, IOException {
        CompiledTemplate compiled;
        String content;
        if (uriOrContent.startsWith(Renderer.TEMPLATE_PREFIX)) {
            String name = uriOrContent.substring(Renderer.TEMPLATE_PREFIX.length());
            compiled = cache.get(name);
            if (compiled == null) {
                URL url = Framework.getService(ResourceService.class).getResource(name);
                if (url == null) {
                    throw new OperationException("Rendering resource not found: " + name);
                }
                try (InputStream in = url.openStream()) {
                    content = IOUtils.toString(in, Charsets.UTF_8);
                }
                compiled = TemplateCompiler.compileTemplate(content);
                cache.put(name, compiled);
            }
        } else {
            content = uriOrContent;
            compiled = TemplateCompiler.compileTemplate(content);
        }

        Object obj = TemplateRuntime.execute(compiled, root);
        return obj == null ? "" : obj.toString();
    }

    @SuppressWarnings("serial")
    private static class Cache extends LinkedHashMap<String, CompiledTemplate> {

        protected int maxCachedItems;

        private Cache() {
            this(128);
        }

        private Cache(int maxCachedItems) {
            super(maxCachedItems, 1.0f, true);
            this.maxCachedItems = maxCachedItems;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, CompiledTemplate> eldest) {
            return size() > maxCachedItems;
        }

    }

}
