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
package org.nuxeo.ecm.platform.ui.web.component.seam;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;

import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.servlet.ServletRequest;

import org.jboss.seam.ui.util.JSF;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;

import com.lowagie.text.html.simpleparser.HTMLWorker;
import com.lowagie.text.html.simpleparser.StyleSheet;

/**
 * Overrides basic p:html tag to pass the application server url. Useful for
 * image resolutions.
 *
 * @since 5.4.1
 */
public class UIHtmlText extends org.jboss.seam.pdf.ui.UIHtmlText {

    @Override
    public void encodeChildren(FacesContext context) throws IOException {
        ResponseWriter writer = context.getResponseWriter();

        StringWriter stringWriter = new StringWriter();
        ResponseWriter cachingResponseWriter = writer.cloneWithWriter(stringWriter);
        context.setResponseWriter(cachingResponseWriter);
        JSF.renderChildren(context, this);
        context.setResponseWriter(writer);

        String output = stringWriter.getBuffer().toString();
        addFromHtml(output, context);
    }

    @Override
    public void encodeEnd(FacesContext context) throws IOException {
        Object value = getValue();
        if (value != null) {
            addFromHtml(convert(context, value), context);
        }

        super.encodeEnd(context);
    }

    private void addFromHtml(String html, FacesContext context)
            throws IOException {
        // XXX: fill in the server url for images resolution
        HashMap<String, Object> interfaceProps = new HashMap<String, Object>();
        String base = VirtualHostHelper.getServerURL(
                (ServletRequest) context.getExternalContext().getRequest(),
                false);
        if (base != null && base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        interfaceProps.put("img_baseurl", base);
        for (Object o : HTMLWorker.parseToList(new StringReader(html),
                getStyle(), interfaceProps)) {
            addToITextParent(o);
        }
    }

    /**
     * XXX - this needs some work
     */
    private StyleSheet getStyle() {
        StyleSheet styles = new StyleSheet();
        styles.loadTagStyle("body", "leading", "16,0");
        return styles;
    }

}
