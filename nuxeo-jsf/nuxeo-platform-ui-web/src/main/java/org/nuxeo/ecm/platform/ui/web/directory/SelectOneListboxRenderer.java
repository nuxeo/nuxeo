/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.ui.web.directory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.ValueHolder;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.model.SelectItem;
import javax.faces.render.Renderer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.i18n.I18NUtils;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 */
public class SelectOneListboxRenderer extends Renderer {

    private static final Log log = LogFactory.getLog(SelectOneListboxRenderer.class);

    @Override
    public void decode(FacesContext facesContext, UIComponent component) {
        // String compId = component.getClientId(facesContext);
        SelectOneListboxComponent comp = (SelectOneListboxComponent) component;
        if (comp.getDisplayValueOnly()) {
            return;
        }

        String id = comp.getClientId(facesContext);

        Map<String, String> requestMap = facesContext.getExternalContext().getRequestParameterMap();
        String value = requestMap.get(id);
        // use empty string, not null, so that required/validate() works
        if (value == null) {
            value = "";
        }
        ((UIInput) component).setSubmittedValue(value);
    }

    @Override
    public void encodeBegin(FacesContext context, UIComponent component)
            throws IOException {
        SelectOneListboxComponent comp = (SelectOneListboxComponent) component;
        ResponseWriter writer = context.getResponseWriter();
        boolean displayValueOnly = comp.getDisplayValueOnly();
        if (displayValueOnly) {
            encodeInputAsText(context, writer, comp);
        } else {
            encodeInput(context, writer, comp);
        }
    }

    private static void encodeInputAsText(FacesContext context,
            ResponseWriter writer, SelectOneListboxComponent comp)
            throws IOException {
        String displayValueOnlyStyle = comp.getDisplayValueOnlyStyle();
        String displayValueOnlyStyleClass = comp.getDisplayValueOnlyStyleClass();
        Boolean displayIdAndLabel = comp.getDisplayIdAndLabel();
        String display = comp.getDisplay();
        // default value
        display = display == null ? "" : display;

        String id = comp.getStringProperty("id", null);
        writer.startElement("div", comp);
        String selectedOption = getCurrentSelectedValue(comp);

        if (id != null) {
            writer.writeAttribute("id", id, "id");
        }
        if (displayValueOnlyStyle != null) {
            writer.writeAttribute("style", displayValueOnlyStyle, "style");
        }
        if (displayValueOnlyStyleClass != null) {
            writer.writeAttribute("class", displayValueOnlyStyleClass, "class");
        }

        if (selectedOption != null) {
            Map<String, SelectItem> options = comp.getOptions();
            SelectItem item = options.get(selectedOption);
            if (item == null) {
                log.warn("option not found: " + selectedOption);
            } else {
                String optionId = (String) item.getValue();
                String optionLabel = item.getLabel();
                String displayValue = DirectoryHelper.getOptionValue(optionId,
                        optionLabel, display, displayIdAndLabel, " ");
                writer.writeText(displayValue, null);
            }
        }

        writer.endElement("div");
    }

    private static void encodeInput(FacesContext context,
            ResponseWriter writer, SelectOneListboxComponent comp)
            throws IOException {
        String cssStyleClass = comp.getStringProperty("cssStyleClass", null);
        String cssStyle = comp.getStringProperty("cssStyle", null);
        String id = comp.getClientId(context);
        Boolean displayIdAndLabel = comp.getDisplayIdAndLabel();
        String display = comp.getStringProperty("display", "");
        // default value
        display = display == null ? "" : display;
        String size = comp.getSize();

        writer.startElement("select", comp);
        writer.writeAttribute("name", id, "name");
        writer.writeAttribute("id", id, "id");
        if (size != null) {
            writer.writeAttribute("size", size, "size");
        }
        if (cssStyleClass != null) {
            writer.writeAttribute("class", cssStyleClass, "class");
        }
        if (cssStyle != null) {
            writer.writeAttribute("style", cssStyle, "style");
        }

        String onchange = comp.getOnchange();
        if (onchange != null) {
            writer.writeAttribute("onchange", onchange, "onchange");
        }
        String onclick = comp.getOnclick();
        if (onclick != null) {
            writer.writeAttribute("onclick", onclick, "onclick");
        }
        String onselect = comp.getOnselect();
        if (onselect != null) {
            writer.writeAttribute("onselect", onselect, "onselect");
        }

        Map<String, SelectItem> options = comp.getOptions();
        String value = getCurrentSelectedValue(comp);

        if (options != null) {
            List<SelectItem> newOptions = new ArrayList<SelectItem>();
            for (SelectItem item : options.values()) {
                String optionId = (String) item.getValue();
                String optionLabel = item.getLabel();
                SelectItem newItem = new SelectItem(optionId, optionLabel);
                newOptions.add(newItem);
            }
            if (!comp.getNotDisplayDefaultOption()) {
                displayDefaultOption(context, writer);
            }
            for (SelectItem item : newOptions) {
                String optionId = (String) item.getValue();
                String optionLabel = item.getLabel();
                String displayValue = DirectoryHelper.getOptionValue(optionId,
                        optionLabel, display, displayIdAndLabel, " ");

                writer.startElement("option", comp);
                writer.writeAttribute("value", optionId, "value");
                if (optionId.equals(value)) {
                    writer.writeAttribute("selected", "true", "selected");
                }
                writer.writeText(displayValue, null);
                writer.endElement("option");
            }
        }
        writer.endElement("select");
    }

    /**
     * Gets value to be rendered.
     */
    protected static String getCurrentSelectedValue(UIComponent component) {
        if (component instanceof EditableValueHolder) {
            Object submittedValue = ((EditableValueHolder) component).getSubmittedValue();
            if (submittedValue != null) {
                return (String) submittedValue;
            }
        }
        if (component instanceof ValueHolder) {
            Object value = ((ValueHolder) component).getValue();
            return (String) value;
        }
        return null;
    }

    private static void displayDefaultOption(FacesContext context,
            ResponseWriter writer) throws IOException {
        String defaultLabel = translate(context, "label.vocabulary.selectValue");
        writer.startElement("option", null);
        writer.writeAttribute("value", "", "value");
        writer.writeText(defaultLabel, null);
        writer.endElement("option");
    }

    protected static String translate(FacesContext context, String label) {
        String bundleName = context.getApplication().getMessageBundle();
        Locale locale = context.getViewRoot().getLocale();
        label = I18NUtils.getMessageString(bundleName, label, null, locale);
        return label;
    }

}
