/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

/**
 * Renderer for HTML imports.
 *
 * @since 6.0
 */
public class HTMLImportRenderer extends AbstractResourceRenderer {

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
