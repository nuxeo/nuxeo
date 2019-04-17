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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.ui.web.component.holder;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.convert.ConverterException;

import com.sun.faces.renderkit.RenderKitUtils;
import com.sun.faces.renderkit.html_basic.HiddenRenderer;

/**
 * Extend hidden renderer to provide client behaviours (onchange events management).
 *
 * @since 6.0
 */
public class ValueHolderRenderer extends HiddenRenderer {

    @Override
    public Object getConvertedValue(FacesContext context, UIComponent component, Object submittedValue)
            throws ConverterException {
        // make sure submitted value is converted to String first
        String submitted = null;
        if (submittedValue != null) {
            submitted = submittedValue.toString();
        }
        return super.getConvertedValue(context, component, submitted);
    }

    @Override
    protected void getEndTextToRender(FacesContext context, UIComponent component, String currentValue)
            throws IOException {

        @SuppressWarnings("resource")
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
