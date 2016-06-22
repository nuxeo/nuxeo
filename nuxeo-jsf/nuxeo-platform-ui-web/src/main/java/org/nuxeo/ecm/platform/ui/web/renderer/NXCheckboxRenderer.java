/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.ui.web.renderer;

import java.io.IOException;

import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.component.ValueHolder;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sun.faces.renderkit.html_basic.CheckboxRenderer;
import com.sun.faces.util.RequestStateManager;

/**
 * Renderer that does not ignore the converter set on the component on submit
 *
 * @author Anahide Tchertchian
 */
public class NXCheckboxRenderer extends CheckboxRenderer {

    private static final Log log = LogFactory.getLog(NXCheckboxRenderer.class);

    public static final String RENDERER_TYPE = "javax.faces.NXCheckbox";

    @Override
    public Object getConvertedValue(FacesContext context, UIComponent component, Object submittedValue)
            throws ConverterException {

        String newValue = null;
        if (submittedValue instanceof Boolean) {
            newValue = ((Boolean) submittedValue).toString();
        } else if (submittedValue instanceof String) {
            newValue = (String) submittedValue;
        } else if (submittedValue != null) {
            log.error("Unsupported submitted value, should be a string or boolean: '" + submittedValue
                    + "' => using false");
        }

        Converter converter = null;
        // If there is a converter attribute, use it to to ask application
        // instance for a converter with this identifer.
        if (component instanceof ValueHolder) {
            converter = ((ValueHolder) component).getConverter();
        }

        if (converter != null) {
            // If the conversion eventually falls to needing to use EL type
            // coercion,
            // make sure our special ConverterPropertyEditor knows about this
            // value.
            RequestStateManager.set(context, RequestStateManager.TARGET_COMPONENT_ATTRIBUTE_NAME, component);
            return converter.getAsObject(context, component, newValue);
        } else {
            return Boolean.valueOf(newValue);
        }
    }

    @Override
    protected void getEndTextToRender(FacesContext context, UIComponent component, String currentValue)
            throws IOException {
        String val = currentValue;
        // hack to make sure checkbox state is restored correctly from bound value on ajax request
        if (component instanceof EditableValueHolder && context.getPartialViewContext().isAjaxRequest()) {
            Object reset = component.getAttributes().get("resetOnAjax");
            if ((reset instanceof Boolean && Boolean.TRUE.equals(reset))
                    || (reset instanceof String && Boolean.parseBoolean((String) reset))) {
                EditableValueHolder c = (EditableValueHolder) component;
                c.resetValue();
                val = getCurrentValue(context, component);
            }
        }
        super.getEndTextToRender(context, component, val);
    }

}
