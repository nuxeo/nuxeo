/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.ecm.automation.core.rendering;

import java.io.InputStream;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

import org.mvel2.templates.CompiledTemplate;
import org.mvel2.templates.TemplateCompiler;
import org.mvel2.templates.TemplateRuntime;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.resource.ResourceService;

import edu.emory.mathcs.backport.java.util.Collections;

/**
 * MVEL rendering using a simple cache of compiled template.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class MvelRender implements Renderer {

    @SuppressWarnings("unchecked")
    protected Map<String,CompiledTemplate> cache = Collections.synchronizedMap(new Cache());

    @Override
    public String render(String uriOrContent, Map<String,Object> root) throws Exception {
        CompiledTemplate compiled = null;
        String content = null;
        if (uriOrContent.startsWith(Renderer.TEMPLATE_PREFIX)) {
            String name = uriOrContent.substring(Renderer.TEMPLATE_PREFIX.length());
            compiled = cache.get(name);
            if (compiled == null) {
                URL url = Framework.getService(ResourceService.class).getResource(name);
                if (url == null) {
                    throw new OperationException("Rendering resource not found: "+name);
                }
                InputStream in = url.openStream();
                try {
                    content = FileUtils.read(in);
                } finally {
                    in.close();
                }
                compiled = TemplateCompiler.compileTemplate(content);
                cache.put(name, compiled);
            }
        } else {
            content = uriOrContent;
            compiled = TemplateCompiler.compileTemplate(content);
        }


        Object obj = TemplateRuntime.execute(compiled,
                root);
        return obj == null ? "" : obj.toString();
    }

    @SuppressWarnings("serial")
    private static class Cache extends LinkedHashMap<String, CompiledTemplate> {

        protected int maxCachedItems;

        public Cache() {
            this (128);
        }

        public Cache(int maxCachedItems) {
            super(maxCachedItems, 1.0f, true);
            this.maxCachedItems = maxCachedItems;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, CompiledTemplate> eldest) {
            return size() > maxCachedItems;
        }

    }

}
