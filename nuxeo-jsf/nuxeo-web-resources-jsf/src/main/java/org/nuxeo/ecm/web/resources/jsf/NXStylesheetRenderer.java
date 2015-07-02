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
 * Overrides JSF script renderer to allow specifying resources from the war using "src" attribute.
 *
 * @since 7.4
 */
public class NXStylesheetRenderer extends AbstractResourceRenderer {

    @Override
    protected void startElement(ResponseWriter writer, UIComponent component) throws IOException {
        writer.startElement("style", component);
        writer.writeAttribute("type", "text/css", "type");
    }

    @Override
    protected void endElement(ResponseWriter writer) throws IOException {
        writer.endElement("style");
    }

    @Override
    protected String verifyTarget(String toVerify) {
        return "head";
    }

    @Override
    public void encodeEnd(FacesContext context, UIComponent component) throws IOException {
        String url = resolveUrl(context, component);
        if (url != null) {
            ResponseWriter writer = context.getResponseWriter();
            writer.startElement("link", component);
            writer.writeAttribute("type", "text/css", "type");
            writer.writeAttribute("rel", "stylesheet", "rel");
            writer.writeURIAttribute("href", url, "href");
            Map<String, Object> attributes = component.getAttributes();
            String media = (String) attributes.get("media");
            if (media != null) {
                writer.writeAttribute("media", media, "media");
            }
            writer.endElement("link");
        }
        super.encodeEnd(context, component);
    }

    @Override
    public void encodeChildren(FacesContext context, UIComponent component) throws IOException {
        encodeChildren(context, component, true);
    }

}
