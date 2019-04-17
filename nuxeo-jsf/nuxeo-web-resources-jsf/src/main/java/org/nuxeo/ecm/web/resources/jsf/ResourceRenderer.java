/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.web.resources.jsf;

import java.io.IOException;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.web.resources.api.Resource;
import org.nuxeo.ecm.web.resources.api.ResourceType;
import org.nuxeo.ecm.web.resources.api.service.WebResourceManager;

/**
 * Renderer for JavaScript, CSS and HTML resources declared on the {@link WebResourceManager} extension point.
 *
 * @since 7.3
 */
public class ResourceRenderer extends AbstractResourceRenderer {

    private static final Log log = LogFactory.getLog(ResourceRenderer.class);

    @Override
    public void encodeEnd(FacesContext context, UIComponent component) throws IOException {
        Resource resource = null;
        String url = null;
        Map<String, Object> attributes = component.getAttributes();
        String name = (String) attributes.get("name");
        if (name != null) {
            resource = resolveNuxeoResource(context, component, name);
            if (resource == null) {
                log.error("Resource not found: " + name);
                return;
            }
            url = getUrlWithParams(context, component,
                    resolveNuxeoResourceUrl(context, component, resolveNuxeoResourcePath(resource)));
            url = resolveUrlWithTimestamp(component, url);
        }
        @SuppressWarnings("resource")
        ResponseWriter writer = context.getResponseWriter();
        if (ResourceType.css.matches(resource)) {
            writer.startElement("link", component);
            writer.writeAttribute("type", "text/css", "type");
            writer.writeAttribute("rel", "stylesheet", "rel");
            writer.writeURIAttribute("href", url, "href");
            writer.endElement("link");
        } else if (ResourceType.js.matches(resource)) {
            writer.startElement("script", component);
            writer.writeAttribute("type", "text/javascript", "type");
            writer.writeURIAttribute("src", url, "src");
            writer.endElement("script");
        } else if (ResourceType.html.matches(resource)) {
            writer.startElement("link", component);
            writer.writeAttribute("rel", "import", "rel");
            writer.writeURIAttribute("href", url, "href");
            writer.endElement("link");
        } else {
            log.error("Unhandled type for resource " + resource);
        }
    }

    @Override
    protected void startElement(ResponseWriter writer, UIComponent component) throws IOException {
        // NOOP
    }

    @Override
    protected void endElement(ResponseWriter writer) throws IOException {
        // NOOP
    }

}
