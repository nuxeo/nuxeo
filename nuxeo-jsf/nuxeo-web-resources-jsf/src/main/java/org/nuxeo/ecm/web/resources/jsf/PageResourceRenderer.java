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
package org.nuxeo.ecm.web.resources.jsf;

import java.io.IOException;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.web.resources.api.ResourceType;

/**
 * Renderer for pages, handling several types of resources (css and js for now).
 *
 * @since 7.10
 */
public class PageResourceRenderer extends ResourceBundleRenderer {

    public static final String RENDERER_TYPE = "org.nuxeo.ecm.web.resources.jsf.PageResource";

    private static final Log log = LogFactory.getLog(PageResourceRenderer.class);

    @Override
    public void encodeEnd(FacesContext context, UIComponent component) throws IOException {
        Map<String, Object> attributes = component.getAttributes();
        String name = (String) attributes.get("name");
        String type = (String) attributes.get("type");
        if (StringUtils.isBlank(type)) {
            log.error("Cannot encode page with empty type at %s" + component.getClientId());
            return;
        }
        if (!ResourceType.css.equals(type) && !ResourceType.js.equals(type)) {
            log.error("Unsupported type '" + type + "' to encode page '" + name + "' at " + component.getClientId());
            return;
        }
        if (ResourceType.css.equals(type)) {
            encodeEnd(context, component, ResourceType.css, PAGE_ENDPOINT_PATH + name + ".css");
        } else if (ResourceType.js.equals(type)) {
            encodeEnd(context, component, ResourceType.js, PAGE_ENDPOINT_PATH + name + ".js");
        }
        super.encodeEnd(context, component);
    }

}
