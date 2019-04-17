/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package org.nuxeo.ecm.platform.ui.web.renderer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.faces.renderkit.html_basic.ListboxRenderer;

/**
 * @since 6.0
 */
public class NxListboxRenderer extends ListboxRenderer {

    public static final String RENDERER_TYPE = "org.nuxeo.NxListboxRenderer";

    public static final String DISABLE_SELECT2_PROPERTY = "disableSelect2";

    @Override
    public void encodeEnd(FacesContext context, UIComponent component) throws IOException {

        super.encodeEnd(context, component);

        final boolean disableSelect2 = Boolean.parseBoolean((String) component.getAttributes().get("disableSelect2"));

        if (!disableSelect2) {
            @SuppressWarnings("resource")
            ResponseWriter writer = context.getResponseWriter();
            writer.startElement("script", component);
            Map<String, String> params = new HashMap<>();
            final String placeholder = (String) component.getAttributes().get("placeholder");
            final String width = (String) component.getAttributes().get("width");
            if (placeholder != null) {
                params.put("placeholder", placeholder);
            }
            if (width != null) {
                params.put("width", width);
            }
            writer.write("jQuery(document).ready(function(){nuxeo.utils.select2ifySelect('" + component.getClientId()
                    + "', " + new ObjectMapper().writeValueAsString(params) + ")});");
            writer.endElement("script");
        }
    }

}
