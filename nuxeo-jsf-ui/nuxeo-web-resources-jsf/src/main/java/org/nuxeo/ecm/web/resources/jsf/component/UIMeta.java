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
package org.nuxeo.ecm.web.resources.jsf.component;

import java.io.IOException;

import javax.faces.component.UIComponentBase;
import javax.faces.component.UIOutput;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.apache.commons.lang3.StringUtils;

import com.sun.faces.config.FaceletsConfiguration;
import com.sun.faces.config.WebConfiguration;

/**
 * Component rendering a "meta" HTML tag.
 *
 * @since 7.4
 */
public class UIMeta extends UIComponentBase {

    public static final String COMPONENT_TYPE = UIMeta.class.getName();

    public enum PropertyKeys {
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

    @Override
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

        @SuppressWarnings("resource")
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
