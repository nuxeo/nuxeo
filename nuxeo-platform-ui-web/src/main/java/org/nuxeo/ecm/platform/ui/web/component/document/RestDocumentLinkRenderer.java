/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.platform.ui.web.component.document;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.logging.Level;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.nuxeo.ecm.platform.ui.web.rest.RestHelper;
import org.nuxeo.ecm.platform.ui.web.tag.fn.Functions;

import com.sun.faces.renderkit.RenderKitUtils;
import com.sun.faces.renderkit.html_basic.OutputLinkRenderer;

/**
 * Overrides default output link renderer so that URL parameters passed through
 * f:param tags are not added twice, since the component already takes them
 * into account when building the URL.
 *
 * @see RestDocumentLink
 * @since 5.4.2
 */
public class RestDocumentLinkRenderer extends OutputLinkRenderer {

    /**
     * Returns an empty parameters list because parameters are already taken
     * care of in the computed URL.
     */
    protected Param[] getParamList(UIComponent command) {
        return new Param[0];
    }

    protected static final String[] PASSTHROUGHATTRIBUTES = { "accesskey",
            "charset", "coords", "dir", "hreflang", "lang", "onblur",
            "onclick", "ondblclick", "onfocus", "onkeydown", "onkeypress",
            "onkeyup", "onmousedown", "onmousemove", "onmouseout",
            "onmouseover", "onmouseup", "rel", "rev", "shape", "style",
            "tabindex", "title", "type", };

    protected void renderAsActive(FacesContext context, UIComponent component)
            throws IOException {

        String hrefVal = getCurrentValue(context, component);

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Value to be rendered " + hrefVal);
        }

        // suppress rendering if "rendered" property on the output is
        // false
        if (!component.isRendered()) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("End encoding component " + component.getId()
                        + " since " + "rendered attribute is set to false ");
            }
            return;
        }
        ResponseWriter writer = context.getResponseWriter();
        assert (writer != null);
        writer.startElement("a", component);
        String writtenId = writeIdAttributeIfNecessary(context, writer,
                component);
        if (null != writtenId) {
            writer.writeAttribute("name", writtenId, "name");
        }
        // render an empty value for href if it is not specified
        if (null == hrefVal || 0 == hrefVal.length()) {
            hrefVal = "";
        }

        // Write Anchor attributes

        Param paramList[] = getParamList(component);
        StringBuffer sb = new StringBuffer();
        sb.append(hrefVal);
        boolean paramWritten = false;
        for (int i = 0, len = paramList.length; i < len; i++) {
            String pn = paramList[i].name;
            if (pn != null && pn.length() != 0) {
                String pv = paramList[i].value;
                sb.append((paramWritten) ? '&' : '?');
                sb.append(URLEncoder.encode(pn, "UTF-8"));
                sb.append('=');
                if (pv != null && pv.length() != 0) {
                    sb.append(URLEncoder.encode(pv, "UTF-8"));
                }
                paramWritten = true;
            }
        }

        String urlNewConversation = context.getExternalContext().encodeResourceURL(
                sb.toString());
        String urlCurrentConversation = RestHelper.addCurrentConversationParameters(urlNewConversation);

        writer.writeURIAttribute("href", urlNewConversation, "href");

        Boolean isNewConversation = ((RestDocumentLink) component).getNewConversation();
        if (!Boolean.TRUE.equals(isNewConversation)) {
            String onclickJS = "if(!(event.ctrlKey||event.shiftKey||event.metaKey||event.button==1)){this.href='"
                    + Functions.javaScriptEscape(urlCurrentConversation) + "'}";
            writer.writeAttribute("onclick", onclickJS, "onclick");
        }

        RenderKitUtils.renderPassThruAttributes(writer, component,
                PASSTHROUGHATTRIBUTES);
        RenderKitUtils.renderXHTMLStyleBooleanAttributes(writer, component);

        String target = (String) component.getAttributes().get("target");
        if (target != null && target.trim().length() != 0) {
            writer.writeAttribute("target", target, "target");
        }

        writeCommonLinkAttributes(writer, component);

        writer.flush();

    }
}
