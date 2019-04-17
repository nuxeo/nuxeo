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

import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;

/**
 * Overrides JSF stylesheet renderer to allow specifying resources from the war using "src" attribute.
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
        return ComponentUtils.verifyTarget(toVerify, "head");
    }

    @Override
    public void encodeEnd(FacesContext context, UIComponent component) throws IOException {
        String url = resolveUrl(context, component);
        if (url != null) {
            @SuppressWarnings("resource")
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
