/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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

import org.apache.commons.lang.StringUtils;
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

    public static enum PropertyKeys {
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
