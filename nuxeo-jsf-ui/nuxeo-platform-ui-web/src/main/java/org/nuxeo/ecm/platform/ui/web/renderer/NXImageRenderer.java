/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.ui.web.renderer;

import static com.sun.faces.renderkit.Attribute.attr;
import static com.sun.faces.util.CollectionsUtils.ar;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;

import javax.faces.component.UIComponent;
import javax.faces.component.UIGraphic;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.apache.commons.lang3.StringUtils;

import com.sun.faces.RIConstants;
import com.sun.faces.renderkit.Attribute;
import com.sun.faces.renderkit.RenderKitUtils;
import com.sun.faces.renderkit.html_basic.ImageRenderer;

/**
 * Renderer that does not display an empty "img" tag as well as empty width and height attributes (as it's an issue for
 * IE)
 *
 * @since 5.6
 */
public class NXImageRenderer extends ImageRenderer {

    // remove attributes height and width from default attributes to avoid
    // adding them when empty
    protected static final Attribute[] ATTRIBUTES = ar(
            //
            attr("alt"),
            //
            attr("dir"),
            //
            // attr("height"),
            //
            attr("lang"),
            //
            attr("longdesc"),
            //
            attr("onclick", "click"),
            attr("ondblclick", "dblclick"),
            //
            attr("onkeydown", "keydown"), attr("onkeypress", "keypress"), attr("onkeyup", "keyup"),
            attr("onmousedown", "mousedown"),
            //
            attr("onmousemove", "mousemove"), attr("onmouseout", "mouseout"), attr("onmouseover", "mouseover"),
            attr("onmouseup", "mouseup"),
            //
            attr("role"), attr("style"), attr("title"), attr("usemap")
    //
    // attr("width")
    );

    public static final String RENDERER_TYPE = "javax.faces.NXImage";

    @Override
    public void encodeEnd(FacesContext context, UIComponent component) throws IOException {

        rendererParamsNotNull(context, component);

        if (!shouldEncode(component)) {
            return;
        }

        @SuppressWarnings("resource")
        ResponseWriter writer = context.getResponseWriter();

        // do not even render tag if url is empty
        String src = src(context, component);
        if (StringUtils.isBlank(src)) {
            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER,
                        "Do not render empty img tag with empty src value for component " + component.getId());
            }
        } else {
            Map<String, Object> attrs = component.getAttributes();

            // Get the attributes
            String width = (String) attrs.get("width");
            String height = (String) attrs.get("height");
            String enableContainer = (String) attrs.get("enableContainer");

            boolean hasDivContainer = false;
            if (enableContainer != null && Boolean.parseBoolean(enableContainer) == true) {
                hasDivContainer = true;
            }

            if (hasDivContainer) {
                writer.startElement("div", component);
                writer.writeAttribute("class", "pictureContainer", "class");

                StringBuilder styleBuilder = new StringBuilder();
                if (!StringUtils.isEmpty(width)) {
                    styleBuilder.append("width:");
                    styleBuilder.append(width);
                    styleBuilder.append("px;");
                }
                if (!StringUtils.isEmpty(height)) {
                    styleBuilder.append("height:");
                    styleBuilder.append(height);
                    styleBuilder.append("px;");
                }
                if (styleBuilder.length() > 0) {
                    writer.writeAttribute("style", styleBuilder.toString(), "style");
                }
            }

            writer.startElement("img", component);
            writeIdAttributeIfNecessary(context, writer, component);
            writer.writeURIAttribute("src", src, "value");

            // if we're writing XHTML and we have a null alt attribute
            if (writer.getContentType().equals(RIConstants.XHTML_CONTENT_TYPE) && null == attrs.get("alt")) {
                // write out an empty alt
                writer.writeAttribute("alt", "", "alt");
            }

            RenderKitUtils.renderPassThruAttributes(context, writer, component, ATTRIBUTES);
            // If the container is not activated and the width and/or height are defined, these attributes are set on
            // the img directly
            if (!hasDivContainer) {
                if (!StringUtils.isBlank(width)) {
                    writer.writeAttribute("width", width, "width");
                }
                if (!StringUtils.isBlank(height)) {
                    writer.writeAttribute("height", height, "height");
                }
            }

            RenderKitUtils.renderXHTMLStyleBooleanAttributes(writer, component);

            String styleClass = (String) attrs.get("styleClass");
            if (!StringUtils.isBlank(styleClass)) {
                writer.writeAttribute("class", styleClass, "styleClass");
            }

            writer.endElement("img");

            if (hasDivContainer) {
                writer.endElement("div");
            }
        }

        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "End encoding component " + component.getId());
        }

    }

    protected static String src(FacesContext context, UIComponent component) {
        String value = (String) ((UIGraphic) component).getValue();
        if (value == null) {
            return "";
        }
        value = context.getApplication().getViewHandler().getResourceURL(context, value);
        return (context.getExternalContext().encodeResourceURL(value));
    }

}
