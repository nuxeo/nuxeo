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
package org.nuxeo.ecm.web.resources.jsf;

import java.io.IOException;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.apache.commons.lang3.StringUtils;
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
