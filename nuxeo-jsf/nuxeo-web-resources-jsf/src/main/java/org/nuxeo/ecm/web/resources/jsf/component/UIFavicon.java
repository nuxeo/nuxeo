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

/**
 * Component rendering a favicon link.
 *
 * @since 7.3
 */
public class UIFavicon extends UIComponentBase {

    public static final String COMPONENT_TYPE = UIFavicon.class.getName();

    // local cache of icons
    protected static Map<String, String> iconsMime = new HashMap<>();

    public static enum PropertyKeys {
        name, src, mimetype
    }

    @Override
    public String getFamily() {
        return UIOutput.COMPONENT_FAMILY;
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
        String src = getSrc();
        String mt = getMimetype();
        if (StringUtils.isBlank(mt)) {
            mt = getMimetype(src);
        }
        writer.startElement("link", this);
        writer.writeAttribute("rel", getName(), "rel");
        writer.writeAttribute("type", mt, "rel");
        writer.writeURIAttribute("href", src, "href");
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
