/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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

import org.apache.commons.lang.StringUtils;

import com.sun.faces.RIConstants;
import com.sun.faces.renderkit.Attribute;
import com.sun.faces.renderkit.RenderKitUtils;
import com.sun.faces.renderkit.html_basic.ImageRenderer;

/**
 * Renderer that does not display an empty "img" tag as well as empty width and
 * height attributes (as it's an issue for IE)
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
            attr("onkeydown", "keydown"), attr("onkeypress", "keypress"),
            attr("onkeyup", "keyup"), attr("onmousedown", "mousedown"),
            //
            attr("onmousemove", "mousemove"), attr("onmouseout", "mouseout"),
            attr("onmouseover", "mouseover"), attr("onmouseup", "mouseup"),
            //
            attr("role"), attr("style"), attr("title"), attr("usemap")
    //
    // attr("width")
    );

    public static final String RENDERER_TYPE = "javax.faces.NXImage";

    @Override
    public void encodeEnd(FacesContext context, UIComponent component)
            throws IOException {

        rendererParamsNotNull(context, component);

        if (!shouldEncode(component)) {
            return;
        }

        ResponseWriter writer = context.getResponseWriter();

        // do not even render tag if url is empty
        String src = src(context, component);
        if (StringUtils.isBlank(src)) {
            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER,
                        "Do not render empty img tag with empty src value for component "
                                + component.getId());
            }
        } else {

            writer.startElement("img", component);
            writeIdAttributeIfNecessary(context, writer, component);
            writer.writeURIAttribute("src", src, "value");

            Map<String, Object> attrs = component.getAttributes();

            // if we're writing XHTML and we have a null alt attribute
            if (writer.getContentType().equals(RIConstants.XHTML_CONTENT_TYPE)
                    && null == attrs.get("alt")) {
                // write out an empty alt
                writer.writeAttribute("alt", "", "alt");
            }

            RenderKitUtils.renderPassThruAttributes(context, writer, component,
                    ATTRIBUTES);
            // add back height and width attributes if any
            String width = (String) attrs.get("width");
            String height = (String) attrs.get("height");
            if (!StringUtils.isBlank(width)) {
                writer.writeAttribute("width", width, "width");
            }
            if (!StringUtils.isBlank(height)) {
                writer.writeAttribute("height", height, "height");
            }

            RenderKitUtils.renderXHTMLStyleBooleanAttributes(writer, component);

            String styleClass = (String) attrs.get("styleClass");
            if (!StringUtils.isBlank(styleClass)) {
                writer.writeAttribute("class", styleClass, "styleClass");
            }

            writer.endElement("img");
        }

        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER,
                    "End encoding component " + component.getId());
        }

    }

    protected static String src(FacesContext context, UIComponent component) {
        String value = (String) ((UIGraphic) component).getValue();
        if (value == null) {
            return "";
        }
        value = context.getApplication().getViewHandler().getResourceURL(
                context, value);
        return (context.getExternalContext().encodeResourceURL(value));
    }

}
