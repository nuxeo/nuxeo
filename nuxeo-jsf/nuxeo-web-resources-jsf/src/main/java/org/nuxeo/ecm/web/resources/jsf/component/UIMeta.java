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
package org.nuxeo.ecm.web.resources.jsf.component;

import java.io.IOException;

import javax.faces.component.UIComponentBase;
import javax.faces.component.UIOutput;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

/**
 * Component rendering a "meta" HTML tag.
 *
 * @since 7.3
 */
public class UIMeta extends UIComponentBase {

    public static final String COMPONENT_TYPE = UIMeta.class.getName();

    public static enum PropertyKeys {
        charset
    }

    protected String charset;

    @Override
    public String getFamily() {
        return UIOutput.COMPONENT_FAMILY;
    }

    public String getCharset() {
        return (String) getStateHelper().eval(PropertyKeys.charset, "utf-8");
    }

    public void setCharset(String charset) {
        getStateHelper().put(PropertyKeys.charset, charset);
    }

    public String getRendererType() {
        return null;
    }

    @Override
    public void encodeEnd(FacesContext context) throws IOException {
        if (context == null) {
            throw new NullPointerException();
        }
        if (!isRendered()) {
            popComponentFromEL(context);
            return;
        }

        ResponseWriter writer = context.getResponseWriter();
        writer.startElement("meta", this);
        writer.writeAttribute("http-equiv", "Content-Type", "http-equiv");
        writer.writeAttribute("content", String.format("text/html;charset=%s", getCharset()), "content");
        writer.endElement("meta");

        popComponentFromEL(context);
    }

}
