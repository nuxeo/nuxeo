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

import org.nuxeo.ecm.web.resources.api.Resource;
import org.nuxeo.ecm.web.resources.api.service.WebResourceManager;

/**
 * Renderer for HTML resources declared on the {@link WebResourceManager} extension point.
 *
 * @since 7.3
 */
public class ResourceImportRenderer extends HTMLImportRenderer {

    @Override
    protected String resolveUrl(FacesContext context, UIComponent component) throws IOException {
        Map<String, Object> attributes = component.getAttributes();
        String name = (String) attributes.get("name");
        if (name != null) {
            Resource r = resolveNuxeoResource(context, component, name);
            if (r != null) {
                String url = getUrlWithParams(context, component, r.getURI());
                url = resolveUrlWithTimestamp(component, url);
                return url;
            }
        }
        return null;
    }

}
