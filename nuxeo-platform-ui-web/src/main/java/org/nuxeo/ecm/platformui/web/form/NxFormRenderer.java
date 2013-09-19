/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Guillaume Renard
 */
package org.nuxeo.ecm.platformui.web.form;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import com.sun.faces.renderkit.html_basic.FormRenderer;

/**
 * Nuxeo h:form renderer.
 *
 * @since 5.7.3
 */
public class NxFormRenderer extends FormRenderer {

    @Override
    public void encodeBegin(FacesContext context, UIComponent component)
            throws IOException {

        super.encodeBegin(context, component);

        if (component.isRendered()) {
            ResponseWriter writer = context.getResponseWriter();

            // The purpose of this code is to prevent user from  double submit a form.
            writer.startElement("script", component);
            writer.writeAttribute("type", "text/javascript", null);

            String clientId = component.getClientId(context);
            clientId.replace(":", "\\\\:");
            String scriptContent = String.format(
                    "jQuery(\"#%s\").preventDoubleSubmission();", clientId.replace(":", "\\\\:"));
            writer.writeText(scriptContent, null);
            writer.endElement("script");
        }

    }

}
