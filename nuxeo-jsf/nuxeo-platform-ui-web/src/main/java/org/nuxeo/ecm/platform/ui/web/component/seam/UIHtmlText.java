/*
 * (C) Copyright 2008 JBoss and others.
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
 *     Original file from org.jboss.seam.pdf.ui.UIHtmlText.java in jboss-seam-pdf
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.ui.web.component.seam;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;

import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ui.util.JSF;

import com.lowagie.text.html.simpleparser.HTMLWorker;
import com.lowagie.text.html.simpleparser.StyleSheet;

/**
 * Overrides basic p:html tag to use {@link NuxeoITextImageProvider} to resolve image resources.
 *
 * @since 5.4.2
 */
public class UIHtmlText extends org.jboss.seam.pdf.ui.UIHtmlText {

    private static final Log log = LogFactory.getLog(UIHtmlText.class);

    @Override
    public void encodeChildren(FacesContext context) throws IOException {
        @SuppressWarnings("resource")
        ResponseWriter writer = context.getResponseWriter();

        StringWriter stringWriter = new StringWriter();
        @SuppressWarnings("resource")
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

    private void addFromHtml(String html, FacesContext context) {
        HashMap<String, Object> interfaceProps = new HashMap<>();
        interfaceProps.put("img_provider", new NuxeoITextImageProvider(
                (HttpServletRequest) context.getExternalContext().getRequest()));

        try {
            for (Object o : HTMLWorker.parseToList(new StringReader(html), getStyle(), interfaceProps)) {
                addToITextParent(o);
            }
        } catch (IOException e) {
            // XXX avoid crash when rendering an image with resource not found
            log.error("Error converting HTML to PDF", e);
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
