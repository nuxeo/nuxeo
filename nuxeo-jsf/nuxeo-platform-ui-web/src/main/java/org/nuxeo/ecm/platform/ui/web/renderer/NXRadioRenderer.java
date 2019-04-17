/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.ui.web.renderer;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import javax.el.ELException;
import javax.faces.component.NamingContainer;
import javax.faces.component.UIComponent;
import javax.faces.component.UISelectOne;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.convert.Converter;
import javax.faces.model.SelectItem;

import com.sun.faces.renderkit.Attribute;
import com.sun.faces.renderkit.AttributeManager;
import com.sun.faces.renderkit.RenderKitUtils;
import com.sun.faces.renderkit.html_basic.RadioRenderer;
import com.sun.faces.util.RequestStateManager;

/**
 * Renderer that does not ignore the converter set on the component on submit
 *
 * @since 5.8
 */
public class NXRadioRenderer extends RadioRenderer {

    private static final Attribute[] ATTRIBUTES = AttributeManager.getAttributes(AttributeManager.Key.SELECTONERADIO);

    @SuppressWarnings("resource") // ResponseWriter not ours to close
    @Override
    protected void renderOption(FacesContext context, UIComponent component, Converter converter, SelectItem curItem,
            Object currentSelections, Object[] submittedValues, boolean alignVertical, int itemNumber,
            OptionComponentInfo optionInfo) throws IOException {

        ResponseWriter writer = context.getResponseWriter();
        assert (writer != null);

        UISelectOne selectOne = (UISelectOne) component;
        Object curValue = selectOne.getSubmittedValue();
        if (curValue == null) {
            curValue = selectOne.getValue();
            // XXX added for NXRadioRenderer
            if (converter != null) {
                curValue = converter.getAsString(context, component, curValue);
            }
        }

        Class<?> type = String.class;
        if (curValue != null) {
            type = curValue.getClass();
            if (type.isArray()) {
                curValue = ((Object[]) curValue)[0];
                if (null != curValue) {
                    type = curValue.getClass();
                }
            } else if (Collection.class.isAssignableFrom(type)) {
                Iterator<?> valueIter = ((Collection<?>) curValue).iterator();
                if (null != valueIter && valueIter.hasNext()) {
                    curValue = valueIter.next();
                    if (null != curValue) {
                        type = curValue.getClass();
                    }
                }
            }
        }
        Object itemValue = curItem.getValue();
        RequestStateManager.set(context, RequestStateManager.TARGET_COMPONENT_ATTRIBUTE_NAME, component);
        Object newValue;
        try {
            newValue = context.getApplication().getExpressionFactory().coerceToType(itemValue, type);
        } catch (ELException ele) {
            newValue = itemValue;
        } catch (IllegalArgumentException iae) {
            // If coerceToType fails, per the docs it should throw
            // an ELException, however, GF 9.0 and 9.0u1 will throw
            // an IllegalArgumentException instead (see GF issue 1527).
            newValue = itemValue;
        }

        boolean checked = null != newValue && newValue.equals(curValue);

        // FIXME: curItem.isNoSelectionOption() method not found
        // if (optionInfo.isHideNoSelection() && curItem.isNoSelectionOption()
        // && curValue != null && !checked) {
        // return;
        // }
        if (optionInfo.isHideNoSelection() && curValue != null && !checked) {
            return;
        }

        if (alignVertical) {
            writer.writeText("\t", component, null);
            writer.startElement("tr", component);
            writer.writeText("\n", component, null);
        }

        String labelClass;
        if (optionInfo.isDisabled() || curItem.isDisabled()) {
            labelClass = optionInfo.getDisabledClass();
        } else {
            labelClass = optionInfo.getEnabledClass();
        }
        writer.startElement("td", component);
        writer.writeText("\n", component, null);

        writer.startElement("input", component);
        writer.writeAttribute("type", "radio", "type");

        if (checked) {
            writer.writeAttribute("checked", Boolean.TRUE, null);
        }
        writer.writeAttribute("name", component.getClientId(context), "clientId");
        String idString = component.getClientId(context) + NamingContainer.SEPARATOR_CHAR
                + Integer.toString(itemNumber);
        writer.writeAttribute("id", idString, "id");

        writer.writeAttribute("value", (getFormattedValue(context, component, curItem.getValue(), converter)), "value");

        // Don't render the disabled attribute twice if the 'parent'
        // component is already marked disabled.
        if (!optionInfo.isDisabled()) {
            if (curItem.isDisabled()) {
                writer.writeAttribute("disabled", true, "disabled");
            }
        }
        // Apply HTML 4.x attributes specified on UISelectMany component to all
        // items in the list except styleClass and style which are rendered as
        // attributes of outer most table.
        RenderKitUtils.renderPassThruAttributes(context, writer, component, ATTRIBUTES,
                getNonOnClickSelectBehaviors(component));
        RenderKitUtils.renderXHTMLStyleBooleanAttributes(writer, component);

        RenderKitUtils.renderSelectOnclick(context, component, false);

        writer.endElement("input");
        writer.startElement("label", component);
        writer.writeAttribute("for", idString, "for");
        // if enabledClass or disabledClass attributes are specified, apply
        // it on the label.
        if (labelClass != null) {
            writer.writeAttribute("class", labelClass, "labelClass");
        }
        String itemLabel = curItem.getLabel();
        if (itemLabel != null) {
            writer.writeText(" ", component, null);
            if (!curItem.isEscape()) {
                // It seems the ResponseWriter API should
                // have a writeText() with a boolean property
                // to determine if it content written should
                // be escaped or not.
                writer.write(itemLabel);
            } else {
                writer.writeText(itemLabel, component, "label");
            }
        }
        writer.endElement("label");
        writer.endElement("td");
        writer.writeText("\n", component, null);
        if (alignVertical) {
            writer.writeText("\t", component, null);
            writer.endElement("tr");
            writer.writeText("\n", component, null);
        }
    }

}
