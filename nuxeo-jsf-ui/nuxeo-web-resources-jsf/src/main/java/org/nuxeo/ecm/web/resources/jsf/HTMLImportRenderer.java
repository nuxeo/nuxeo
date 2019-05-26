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
            @SuppressWarnings("resource")
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
