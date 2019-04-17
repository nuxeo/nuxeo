/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Jean-Marc Orliaguet, Chalmers
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.web.resources.jsf.component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.faces.component.UIComponentBase;
import javax.faces.component.UIOutput;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.theme.styling.service.descriptors.IconDescriptor;

/**
 * Component rendering a favicon link.
 *
 * @since 7.4
 */
public class UIFavicon extends UIComponentBase {

    public static final String COMPONENT_TYPE = UIFavicon.class.getName();

    // local cache of icons
    protected static Map<String, String> iconsMime = new HashMap<>();

    public enum PropertyKeys {
        value, name, src, mimetype, sizes
    }

    @Override
    public String getFamily() {
        return UIOutput.COMPONENT_FAMILY;
    }

    public IconDescriptor getValue() {
        return (IconDescriptor) getStateHelper().eval(PropertyKeys.value);
    }

    public void setValue(IconDescriptor value) {
        getStateHelper().put(PropertyKeys.value, value);
    }

    public String getName() {
        return (String) getStateHelper().eval(PropertyKeys.name);
    }

    public void setName(String name) {
        getStateHelper().put(PropertyKeys.name, name);
    }

    public String getSrc() {
        return (String) getStateHelper().eval(PropertyKeys.src);
    }

    public void setSrc(String src) {
        getStateHelper().put(PropertyKeys.src, src);
    }

    public String getMimetype() {
        return (String) getStateHelper().eval(PropertyKeys.mimetype);
    }

    public void setMimetype(String mimetype) {
        getStateHelper().put(PropertyKeys.mimetype, mimetype);
    }

    public String getSizes() {
        return (String) getStateHelper().eval(PropertyKeys.sizes);
    }

    public void setSizes(String sizes) {
        getStateHelper().put(PropertyKeys.sizes, sizes);
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

        String src;
        String mt = null;
        String name;
        String sizes;
        IconDescriptor icon = getValue();
        if (icon != null) {
            src = icon.getValue();
            name = icon.getName();
            sizes = icon.getSizes();
        } else {
            src = getSrc();
            mt = getMimetype();
            name = getName();
            sizes = getSizes();
        }
        if (StringUtils.isBlank(mt)) {
            mt = getMimetype(src);
        }

        @SuppressWarnings("resource")
        ResponseWriter writer = context.getResponseWriter();
        writer.startElement("link", this);
        writer.writeAttribute("rel", name, "rel");
        writer.writeAttribute("type", mt, "rel");
        String encodedSrc = context.getApplication().getViewHandler().getResourceURL(context, src);
        writer.writeURIAttribute("href", encodedSrc, "href");
        if (StringUtils.isBlank(sizes)) {
            writer.writeAttribute("sizes", sizes, "sizes");
        }
        writer.endElement("link");

        popComponentFromEL(context);
    }

    protected static String getMimetype(String ico) {
        String mt = null;
        if (iconsMime.containsKey(ico)) {
            mt = iconsMime.get(ico);
        } else {
            mt = resolveMimetype(ico);
            if (!Framework.isDevModeSet()) {
                // cache value
                iconsMime.put(ico, mt);
            }
        }
        return mt;
    }

    protected static String resolveMimetype(String ico) {
        int index = ico.lastIndexOf(".");
        if (index > 0) {
            // Handle only gif and png
            String ext = ico.substring(1 + index);
            switch (ext) {
            case "gif":
                return "image/gif";
            case "png":
                return "image/png";
            }
        }
        return "image/x-icon";
    }

}
