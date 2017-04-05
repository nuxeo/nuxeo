/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and contributors.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.platform.ui.web.renderer;

import java.io.IOException;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.UIOutput;
import javax.faces.component.html.HtmlInputFile;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import com.sun.faces.renderkit.Attribute;
import com.sun.faces.renderkit.AttributeManager;
import com.sun.faces.renderkit.RenderKitUtils;
import com.sun.faces.renderkit.html_basic.TextRenderer;

/**
 * Override default {@code TextRenderer} to handle HTML5 attributes.
 *
 * @since 5.7
 */
public class NXTextRenderer extends TextRenderer {

    private static final Attribute[] INPUT_ATTRIBUTES = AttributeManager.getAttributes(AttributeManager.Key.INPUTTEXT);

    private static final Attribute[] OUTPUT_ATTRIBUTES = AttributeManager.getAttributes(AttributeManager.Key.OUTPUTTEXT);

    @Override
    protected void getEndTextToRender(FacesContext context,
            UIComponent component, String currentValue) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        assert(writer != null);
        boolean shouldWriteIdAttribute = false;
        boolean isOutput = false;

        String style = (String) component.getAttributes().get("style");
        String styleClass = (String) component.getAttributes().get("styleClass");
        String dir = (String) component.getAttributes().get("dir");
        String lang = (String) component.getAttributes().get("lang");
        String title = (String) component.getAttributes().get("title");
        String placeholder = (String) component.getAttributes().get(
                "placeholder");
        Map<String, Object> passthroughAttributes = component.getPassThroughAttributes(false);
        boolean hasPassthroughAttributes = null != passthroughAttributes && !passthroughAttributes.isEmpty();
        if (component instanceof UIInput) {
            writer.startElement("input", component);
            writeIdAttributeIfNecessary(context, writer, component);

            if (component instanceof HtmlInputFile) {
                writer.writeAttribute("type", "file", null);
            }
            else {
                writer.writeAttribute("type", "text", null);
            }
            writer.writeAttribute("name", (component.getClientId(context)),
                    "clientId");

            // only output the autocomplete attribute if the value
            // is 'off' since its lack of presence will be interpreted
            // as 'on' by the browser
            if ("off".equals(component.getAttributes().get("autocomplete"))) {
                writer.writeAttribute("autocomplete",
                        "off",
                        "autocomplete");
            }

            // render default text specified
            if (currentValue != null) {
                writer.writeAttribute("value", currentValue, "value");
            }
            if (null != styleClass) {
                writer.writeAttribute("class", styleClass, "styleClass");
            }

            // HTML5 attributes not handled by default TextRenderer
            if (null != placeholder) {
                writer.writeAttribute("placeholder", placeholder, "placeholder");
            }

            // style is rendered as a passthur attribute
            RenderKitUtils.renderPassThruAttributes(context,
                    writer,
                    component,
                    INPUT_ATTRIBUTES,
                    getNonOnChangeBehaviors(component));
            RenderKitUtils.renderXHTMLStyleBooleanAttributes(writer, component);

            RenderKitUtils.renderOnchange(context, component, false);


            writer.endElement("input");

        } else if (isOutput = (component instanceof UIOutput)) {
            if (styleClass != null
                    || style != null
                    || dir != null
                    || lang != null
                    || title != null
                    || hasPassthroughAttributes
                    || (shouldWriteIdAttribute = shouldWriteIdAttribute(component))) {
                writer.startElement("span", component);
                writeIdAttributeIfNecessary(context, writer, component);
                if (null != styleClass) {
                    writer.writeAttribute("class", styleClass, "styleClass");
                }
                // style is rendered as a passthru attribute
                RenderKitUtils.renderPassThruAttributes(context,
                        writer,
                        component,
                        OUTPUT_ATTRIBUTES);

            }
            if (currentValue != null) {
                Object val = component.getAttributes().get("escape");
                if ((val != null) && Boolean.valueOf(val.toString())) {
                    writer.writeText(currentValue, component, "value");
                } else {
                    writer.write(currentValue);
                }
            }
        }
        if (isOutput && (styleClass != null
                || style != null
                || dir != null
                || lang != null
                || title != null
                || hasPassthroughAttributes
                || (shouldWriteIdAttribute))) {
            writer.endElement("span");
        }
    }

}
