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
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.web.resources.api.Resource;
import org.nuxeo.ecm.web.resources.api.ResourceContextImpl;
import org.nuxeo.ecm.web.resources.api.ResourceType;
import org.nuxeo.ecm.web.resources.api.service.WebResourceManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Renderer for resource bundles, handling several types of resources (css, js, html for now).
 *
 * @since 7.3
 */
public class ResourceBundleRenderer extends AbstractResourceRenderer {

    public static final String RENDERER_TYPE = "org.nuxeo.ecm.web.resources.jsf.ResourceBundle";

    private static final Log log = LogFactory.getLog(ResourceBundleRenderer.class);

    @Override
    public void encodeEnd(FacesContext context, UIComponent component) throws IOException {
        Map<String, Object> attributes = component.getAttributes();
        String name = (String) attributes.get("name");
        String type = (String) attributes.get("type");
        WebResourceManager wrm = Framework.getService(WebResourceManager.class);
        if (StringUtils.isBlank(type)) {
            log.error("Cannot encode bundle with empty type at %s" + component.getClientId());
            return;
        }
        if (!ResourceType.css.equals(type) && !ResourceType.js.equals(type) && !ResourceType.html.equals(type)) {
            log.error("Unsupported type '" + type + "' to encode page '" + name + "' at " + component.getClientId());
            return;
        }
        List<Resource> rs = wrm.getResources(new ResourceContextImpl(), name, type);
        if (rs != null && !rs.isEmpty()) {
            if (ResourceType.css.equals(type)) {
                encodeEnd(context, component, ResourceType.css, BUNDLE_ENDPOINT_PATH + name + ".css");
            } else if (ResourceType.js.equals(type)) {
                encodeEnd(context, component, ResourceType.js, BUNDLE_ENDPOINT_PATH + name + ".js");
            } else if (ResourceType.html.equals(type)) {
                for (Resource r : rs) {
                    encodeEnd(context, component, ResourceType.html, COMPONENTS_PATH + r.getPath());
                }
            }
        }
        super.encodeEnd(context, component);
    }

    protected void encodeEnd(FacesContext context, UIComponent component, ResourceType type, String base)
            throws IOException {
        String url = resolveNuxeoResourceUrl(context, component, base);
        url = resolveUrlWithTimestamp(component, url);
        ResponseWriter writer = context.getResponseWriter();
        if (ResourceType.css.equals(type)) {
            writer.startElement("link", component);
            writer.writeAttribute("type", "text/css", "type");
            writer.writeAttribute("rel", "stylesheet", "rel");
            writer.writeURIAttribute("href", url, "href");
            writer.endElement("link");
        } else if (ResourceType.js.equals(type)) {
            writer.startElement("script", component);
            writer.writeAttribute("type", "text/javascript", "type");
            writer.writeURIAttribute("src", url, "src");
            writer.endElement("script");
        } else if (ResourceType.html.equals(type)) {
            writer.startElement("link", component);
            writer.writeAttribute("rel", "import", "rel");
            writer.writeURIAttribute("href", url, "href");
            writer.endElement("link");
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
