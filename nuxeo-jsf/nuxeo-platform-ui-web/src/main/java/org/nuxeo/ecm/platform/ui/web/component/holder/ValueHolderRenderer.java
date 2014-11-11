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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.ui.web.component.holder;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import com.sun.faces.renderkit.RenderKitUtils;
import com.sun.faces.renderkit.html_basic.HiddenRenderer;

/**
 * Extend hidden renderer to provide client behaviours (onchange events
 * management).
 *
 * @since 6.0
 */
public class ValueHolderRenderer extends HiddenRenderer {

    @Override
    protected void getEndTextToRender(FacesContext context,
            UIComponent component, String currentValue) throws IOException {

        ResponseWriter writer = context.getResponseWriter();
        assert (writer != null);

        writer.startElement("input", component);
        writeIdAttributeIfNecessary(context, writer, component);
        writer.writeAttribute("type", "hidden", "type");
        String clientId = component.getClientId(context);
        writer.writeAttribute("name", clientId, "clientId");

        // render default text specified
        if (currentValue != null) {
            writer.writeAttribute("value", currentValue, "value");
        }

        // Nuxeo patch
        RenderKitUtils.renderOnchange(context, component, false);

        writer.endElement("input");

    }

}
