/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

/**
 * Renderer for HTML imports.
 *
 * @since 6.0
 */
public class HTMLImportRenderer extends AbstractResourceRenderer {

    /**
     * Resolve url either from src, looking up resource in the war, either from JSF resources, given a name (and
     * optional library).
     */
    protected String resolveUrl(FacesContext context, UIComponent component) throws IOException {
        Map<String, Object> attributes = component.getAttributes();
        String src = (String) attributes.get("src");
        String url;
        if (src != null) {
            url = resolveResourceFromSource(context, component, src);
        } else {
            String name = (String) attributes.get("name");
            String library = (String) attributes.get("library");
            url = resolveResourceUrl(context, component, library, name);
        }
        return url;
    }

    @Override
    public void encodeEnd(FacesContext context, UIComponent component) throws IOException {
        String url = resolveUrl(context, component);
        if (url != null) {
            ResponseWriter writer = context.getResponseWriter();
            startElement(writer, component);
            writer.writeURIAttribute("href", url, "href");
            endElement(writer);
        }
    }

    @Override
    protected void startElement(ResponseWriter writer, UIComponent component) throws IOException {
        writer.startElement("link", component);
        writer.writeAttribute("rel", "import", "rel");
    }

    @Override
    protected void endElement(ResponseWriter writer) throws IOException {
        writer.endElement("link");
    }

}
