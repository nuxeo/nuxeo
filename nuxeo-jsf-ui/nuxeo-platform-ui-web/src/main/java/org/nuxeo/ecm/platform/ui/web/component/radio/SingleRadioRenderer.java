/*
 * (C) Copyright 2014 JBoss RichFaces and others.
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

package org.nuxeo.ecm.platform.ui.web.component.radio;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlSelectOneRadio;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.richfaces.component.util.InputUtils;
import org.richfaces.renderkit.RenderKitUtils;
import org.richfaces.renderkit.RendererBase;

/**
 * Renderer for a single radio button, given a target original radio component and index for original button attributes
 * retrieval.
 *
 * @since 6.0
 */
public class SingleRadioRenderer extends RendererBase {

    public static final String RENDERER_TYPE = SingleRadioRenderer.class.getName();

    private String convertToString(Object obj) {
        return (obj == null ? "" : obj.toString());
    }

    @Override
    protected void doEncodeEnd(ResponseWriter writer, FacesContext context, UIComponent _component) throws IOException {
        UIRadio component = (UIRadio) _component;
        java.lang.String clientId = component.getClientId(context);
        final UIComponent targetComponent = getUtils().findComponentFor(component, component.getFor());
        final javax.faces.model.SelectItem item = component.getSelectItem(context, targetComponent);
        boolean checked = false;
        if (targetComponent instanceof javax.faces.component.UIOutput) {
            final Object currentValue = ((javax.faces.component.UIOutput) targetComponent).getValue();
            final Object itemValue = item.getValue();
            checked = itemValue == null ? currentValue == null : itemValue.equals(currentValue);
        }

        writer.startElement("input", component);
        writer.writeAttribute("id", clientId, "id");
        writer.writeAttribute("name", getUtils().clientId(context, targetComponent), "name");
        writer.writeAttribute("type", "radio", "type");
        writer.writeAttribute("value", InputUtils.getConvertedStringValue(context, targetComponent, item.getValue()),
                "value");
        if (checked) {
            writer.writeAttribute("checked", "checked", "checked");
        }
        if (isDisabled(targetComponent) || isReadonly(targetComponent)) {
            writer.writeAttribute("disabled", "disabled", "disabled");
        }

        String targetOnchange = convertToString(RenderKitUtils.getAttributeAndBehaviorsValue(context, targetComponent,
                RenderKitUtils.attributes().generic("onchange", "onchange", "change", "valueChange").first()));
        String onchange = convertToString(RenderKitUtils.getAttributeAndBehaviorsValue(context, component,
                RenderKitUtils.attributes().generic("onchange", "onchange", "change", "valueChange").first()));
        if (targetOnchange != null && !targetOnchange.trim().isEmpty()) {
            onchange = onchange == null ? targetOnchange : targetOnchange + ";" + onchange;
        }
        if (onchange != null && onchange.trim().length() > 0) {
            writer.writeAttribute("onchange", onchange, "onchange");
        }
        getUtils().encodeAttributesFromArray(
                context,
                component,
                new String[] { "accept", "accesskey", "align", "alt", "checked", "dir", "disabled", "lang",
                        "maxlength", "onblur", "onclick", "ondblclick", "onfocus", "onkeydown", "onkeypress",
                        "onkeyup", "onmousedown", "onmousemove", "onmouseout", "onmouseover", "onmouseup", "onselect",
                        "readonly", "size", "src", "style", "tabindex", "title", "usemap", "xml:lang" });

        writer.endElement("input");
        writer.startElement("label", component);
        writer.writeAttribute("for", clientId, "for");
        writer.writeText(convertToString(item.getLabel()), null);
        writer.endElement("label");
    }

    @Override
    protected Class<? extends UIComponent> getComponentClass() {
        return UIRadio.class;
    }

    private boolean isDisabled(UIComponent targetComponent) {
        if (targetComponent instanceof HtmlSelectOneRadio) {
            return ((HtmlSelectOneRadio) targetComponent).isDisabled();
        } else {
            final Object disabled = targetComponent.getAttributes().get("disabled");
            return Boolean.TRUE.equals(disabled);
        }
    }

    private boolean isReadonly(UIComponent targetComponent) {
        if (targetComponent instanceof HtmlSelectOneRadio) {
            return ((HtmlSelectOneRadio) targetComponent).isReadonly();
        } else {
            final Object readonly = targetComponent.getAttributes().get("readonly");
            return Boolean.TRUE.equals(readonly);
        }
    }
}
