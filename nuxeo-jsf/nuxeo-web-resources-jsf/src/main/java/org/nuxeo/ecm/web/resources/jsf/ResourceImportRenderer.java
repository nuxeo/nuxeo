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

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.nuxeo.ecm.web.resources.api.service.WebResourceManager;

/**
 * Renderer for HTML resources declared on the {@link WebResourceManager} extension point.
 *
 * @since 7.3
 */
public class ResourceImportRenderer extends AbstractResourceRenderer {

    @Override
    protected void startElement(ResponseWriter writer, UIComponent component) throws IOException {
        // NOOP
    }

    @Override
    protected void endElement(ResponseWriter writer) throws IOException {
        // NOOP
    }

    @Override
    protected void encodeEnd(FacesContext context, UIComponent component, String src) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        writer.startElement("link", component);
        writer.writeAttribute("rel", "import", "rel");
        writer.writeURIAttribute("href", src, "href");
        writer.endElement("link");
    }

}
