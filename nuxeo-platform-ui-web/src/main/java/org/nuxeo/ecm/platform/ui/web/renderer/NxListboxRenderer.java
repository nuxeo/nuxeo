/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package org.nuxeo.ecm.platform.ui.web.renderer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.faces.application.ResourceDependencies;
import javax.faces.application.ResourceDependency;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.codehaus.jackson.map.ObjectMapper;

import com.sun.faces.renderkit.html_basic.ListboxRenderer;

/**
 * @since 5.9.6
 */
@ResourceDependencies({
        @ResourceDependency(library = "org.richfaces", name = "jquery.js"),
        @ResourceDependency(library = "org.nuxeo.select2", name = "select2.js") })
public class NxListboxRenderer extends ListboxRenderer {

    final String ENABLE_SELECT2_PROPERTY = "enableSelect2";

    @Override
    public void encodeEnd(FacesContext context, UIComponent component)
            throws IOException {

        super.encodeEnd(context, component);

        final String enableSelect2 = (String) component.getAttributes().get(
                ENABLE_SELECT2_PROPERTY);

        if (enableSelect2 != null && Boolean.parseBoolean(enableSelect2)) {
            ResponseWriter writer = context.getResponseWriter();
            writer.startElement("script", component);
            Map<String, String> params = new HashMap<String, String>();
            final String placeholder = (String) component.getAttributes().get(
                    "placeholder");
            if (placeholder != null) {
                params.put("placeholder", placeholder);
            }
            writer.write("jQuery(document).ready(function(){nuxeo.utils.select2ifyjSelect('"
                    + component.getClientId()
                    + "', "
                    + new ObjectMapper().writeValueAsString(params) + ")});");
            writer.endElement("script");
        }
    }

}
