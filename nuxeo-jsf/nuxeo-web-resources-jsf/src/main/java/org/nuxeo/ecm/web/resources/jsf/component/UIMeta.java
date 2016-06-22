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

import org.apache.commons.lang.StringUtils;

import com.sun.faces.config.FaceletsConfiguration;
import com.sun.faces.config.WebConfiguration;

/**
 * Component rendering a "meta" HTML tag.
 *
 * @since 7.4
 */
public class UIMeta extends UIComponentBase {

    public static final String COMPONENT_TYPE = UIMeta.class.getName();

    public static enum PropertyKeys {
        charset, content, httpequiv, name
    }

    @Override
    public String getFamily() {
        return UIOutput.COMPONENT_FAMILY;
    }

    public String getCharset() {
        return (String) getStateHelper().eval(PropertyKeys.charset);
    }

    public void setCharset(String charset) {
        getStateHelper().put(PropertyKeys.charset, charset);
    }

    public String getContent() {
        return (String) getStateHelper().eval(PropertyKeys.content);
    }

    public void setContent(String content) {
        getStateHelper().put(PropertyKeys.content, content);
    }

    public String getHttpequiv() {
        return (String) getStateHelper().eval(PropertyKeys.httpequiv);
    }

    public void setHttpequiv(String httpequiv) {
        getStateHelper().put(PropertyKeys.httpequiv, httpequiv);
    }

    public String getName() {
        return (String) getStateHelper().eval(PropertyKeys.name);
    }

    public void setName(String name) {
        getStateHelper().put(PropertyKeys.name, name);
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
        WebConfiguration webConfig = WebConfiguration.getInstance(context.getExternalContext());
        FaceletsConfiguration faceletsConfig = webConfig.getFaceletsConfiguration();
        if (faceletsConfig.isOutputHtml5Doctype(context.getViewRoot().getViewId())) {
            String charset = getCharset();
            if (!StringUtils.isBlank(charset)) {
                writer.writeAttribute("charset", charset, "charset");
            }
            String httpEquiv = getHttpequiv();
            if (!StringUtils.isBlank(httpEquiv)) {
                writer.writeAttribute("http-equiv", httpEquiv, "http-equiv");
            }
            String content = getContent();
            if (!StringUtils.isBlank(content)) {
                writer.writeAttribute("content", content, "content");
            }
        } else {
            writer.writeAttribute("http-equiv", "Content-Type", "http-equiv");
            writer.writeAttribute("content", "text/html;charset=" + getCharset(), "content");
        }
        String name = getName();
        if (!StringUtils.isBlank(name)) {
            writer.writeAttribute("name", name, "name");
        }
        writer.endElement("meta");

        popComponentFromEL(context);
    }

}
