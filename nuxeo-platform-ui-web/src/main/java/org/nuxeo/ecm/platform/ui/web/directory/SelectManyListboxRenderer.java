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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
 *
 */
public class SelectManyListboxRenderer extends Renderer {

    private static final Log log = LogFactory.getLog(SelectManyListboxRenderer.class);

    public SelectManyListboxRenderer() {
        log.trace("renderer created");
    }

    @Override
    @SuppressWarnings("unchecked")
    public void decode(FacesContext facesContext, UIComponent component) {
        SelectManyListboxComponent comp = (SelectManyListboxComponent) component;
        String clientId = comp.getClientId(facesContext);

        if (componentReadonly(comp)) {
            return;
        }

        Map<String, String[]> requestMap = facesContext.getExternalContext().getRequestParameterValuesMap();
        String[] value;
        if (requestMap.containsKey(clientId)) {
            value = requestMap.get(clientId);
            // remove the empty value if selected - key for the "Please select a
            // value" message
            List<String> list = new ArrayList(Arrays.asList(value));
            list.remove("");
            value = list.toArray(new String[list.size()]);
        } else {
            // Use the empty array, not null, so that required/validate() works
            value = new String[0];
        }
        ((UIInput) component).setSubmittedValue(value);
    }

    private static boolean componentReadonly(SelectManyListboxComponent comp) {
        return comp.getDisplayValueOnly();
    }

    @Override
    public void encodeBegin(FacesContext context, UIComponent component)
            throws IOException {
        SelectManyListboxComponent comp = (SelectManyListboxComponent) component;
        ResponseWriter writer = context.getResponseWriter();
        boolean displayValueOnly = comp.getDisplayValueOnly();
        if (displayValueOnly) {
            encodeInputAsText(context, writer, comp);
        } else {
            encodeInput(context, writer, comp);
        }
    }

    private static void encodeInputAsText(FacesContext context,
            ResponseWriter writer, SelectManyListboxComponent comp)
            throws IOException {
        String displayValueOnlyStyle = comp.getDisplayValueOnlyStyle();
        String displayValueOnlyStyleClass = comp.getDisplayValueOnlyStyleClass();
        String displayValueOnlySeparator = comp.getDisplayValueOnlySeparator();
        Boolean displayIdAndLabel = comp.getDisplayIdAndLabel();
        String display = comp.getDisplay();

        String id = comp.getClientId(context);
        writer.startElement("div", comp);
        String[] selectedOptions = getCurrentSelectedValues(comp);

        if (id != null) {
            writer.writeAttribute("id", id, "id");
        }
        if (displayValueOnlyStyle != null) {
            writer.writeAttribute("style", displayValueOnlyStyle, "style");
        }
        if (displayValueOnlyStyleClass != null) {
            writer.writeAttribute("class", displayValueOnlyStyleClass, "class");
        }

        Map<String, SelectItem> options = comp.getOptions();
        if (selectedOptions != null) {
            int index = 0;
            for (String selectedOption : selectedOptions) {
                SelectItem item = options.get(selectedOption);
                if (item == null) {
                    // selected option not in underlying vocabulary
                    continue;
                }
                String optionId = (String) item.getValue();
                String optionLabel = item.getLabel();
                String displayValue = DirectoryHelper.getOptionValue(optionId,
                        optionLabel, display, displayIdAndLabel, " ");

                writer.startElement("div", comp);
                writer.writeText(displayValue, null);
                writer.endElement("div");
                index++;
                if (displayValueOnlySeparator != null
                        && index < selectedOptions.length) {
                    writer.writeText(displayValueOnlySeparator, null);
                }
            }
        }

        writer.endElement("div");
    }

    private static void encodeInput(FacesContext context,
            ResponseWriter writer, SelectManyListboxComponent comp)
            throws IOException {
        String cssStyleClass = comp.getStringProperty("cssStyleClass", null);
        String cssStyle = comp.getStringProperty("cssStyle", null);
        String id = comp.getClientId(context);
        Boolean displayIdAndLabel = comp.getDisplayIdAndLabel();
        String display = comp.getDisplay();
        // default value
        display = display == null ? "" : display;
        String size = comp.getSize();
        // default value
        size = size == null ? "5" : size;

        writer.startElement("select", comp);
        writer.writeAttribute("name", id, "name");
        writer.writeAttribute("size", size, "size");
        writer.writeAttribute("multiple", "true", "multiple");

        if (id != null) {
            writer.writeAttribute("id", id, "id");
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
        String[] selectedValues = getCurrentSelectedValues(comp);

        List<SelectItem> newOptions = new ArrayList<SelectItem>();
        if (!comp.getNotDisplayDefaultOption()) {
            displayDefaultOption(context, writer);
        }
        for (SelectItem item : options.values()) {
            String optionId = (String) item.getValue();
            String optionLabel = item.getLabel();
            SelectItem newItem = new SelectItem(optionId, optionLabel);
            newOptions.add(newItem);
        }

        Set<String> selectedValuesSet = new HashSet<String>();
        if (selectedValues != null) {
            selectedValuesSet.addAll(Arrays.asList(selectedValues));
        }

        for (SelectItem item : newOptions) {
            String optionId = (String) item.getValue();
            String optionLabel = item.getLabel();

            String displayValue = DirectoryHelper.getOptionValue(optionId,
                    optionLabel, display, displayIdAndLabel, " ");

            writer.startElement("option", comp);
            writer.writeAttribute("value", optionId, "value");

            boolean selected = selectedValuesSet.contains(optionId);
            if (selected) {
                writer.writeAttribute("selected", "true", "selected");
            }
            writer.writeText(displayValue, null);
            writer.endElement("option");
        }
        writer.endElement("select");
    }

    /**
     * Gets value to be rendered.
     */
    protected static String[] getCurrentSelectedValues(UIComponent component) {
        if (component instanceof EditableValueHolder) {
            Object submittedValue = ((EditableValueHolder) component).getSubmittedValue();
            if (submittedValue != null) {
                return toArray(submittedValue);
            }
        }
        if (component instanceof ValueHolder) {
            Object value = ((ValueHolder) component).getValue();
            return toArray(value);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static String[] toArray(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof String[]) {
            return (String[]) value;
        } else if (value instanceof Object[]) {
            Object[] ar = (Object[]) value;
            String[] ret = new String[ar.length];
            System.arraycopy(ar, 0, ret, 0, ar.length);
            return ret;
        } else {
            assert value instanceof List;
            List<String> list = (List<String>) value;
            return list.toArray(new String[list.size()]);
        }
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
