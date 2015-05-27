/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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

import org.nuxeo.ecm.web.resources.api.Resource;
import org.nuxeo.ecm.web.resources.api.ResourceBundle;
import org.nuxeo.ecm.web.resources.api.ResourceContextImpl;
import org.nuxeo.ecm.web.resources.api.ResourceType;
import org.nuxeo.ecm.web.resources.api.service.WebResourceManager;
import org.nuxeo.runtime.api.Framework;

import com.sun.faces.renderkit.html_basic.ScriptStyleBaseRenderer;

/**
 * @since 6.0
 */
public class ResourceBundleRenderer extends ScriptStyleBaseRenderer {

    @Override
    protected void startElement(ResponseWriter writer, UIComponent component) throws IOException {
        // NOOP
    }

    @Override
    protected void endElement(ResponseWriter writer) throws IOException {
        // NOOP
    }

    @Override
    public void encodeEnd(FacesContext context, UIComponent component) throws IOException {
        Map<String, Object> attributes = component.getAttributes();
        String name = (String) attributes.get("name");
        WebResourceManager wrm = Framework.getService(WebResourceManager.class);
        ResourceBundle bundle = wrm.getResourceBundle(name);
        if (bundle != null) {
            List<Resource> cssr = wrm.getResources(new ResourceContextImpl(), name, ResourceType.css);
            if (cssr != null && !cssr.isEmpty()) {
                encodeEnd(context, component, ResourceType.css, "/wapi/v1/resource/bundle/" + name + ".css");
            }
            List<Resource> jsr = wrm.getResources(new ResourceContextImpl(), name, ResourceType.html);
            if (jsr != null && !jsr.isEmpty()) {
                encodeEnd(context, component, ResourceType.js, "/wapi/v1/resource/bundle/" + name + ".js");
            }
            List<Resource> htmlr = wrm.getResources(new ResourceContextImpl(), name, ResourceType.html);
            if (htmlr != null && !htmlr.isEmpty()) {
                for (Resource rhtml : htmlr) {
                    encodeEnd(context, component, ResourceType.html, "/components/" + rhtml.getPath());
                }
            }
        }
        super.encodeEnd(context, component);
    }

    protected void encodeEnd(FacesContext context, UIComponent component, ResourceType type, String base)
            throws IOException {
        String value = context.getApplication().getViewHandler().getResourceURL(context, base);
        String url = context.getExternalContext().encodeResourceURL(value);
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

}
