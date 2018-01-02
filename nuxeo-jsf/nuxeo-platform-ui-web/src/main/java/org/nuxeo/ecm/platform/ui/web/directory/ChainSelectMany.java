/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     George Lefter
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.ui.web.directory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.faces.component.UICommand;
import javax.faces.component.UIGraphic;
import javax.faces.component.html.HtmlInputHidden;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 */
public class ChainSelectMany extends ChainSelectBase {

    public static final String ADD_BUTTON = "addButton";

    private static final String REMOVE_BUTTON = "removeButton";

    /* a hidden input set if the value of the component is empty */
    private static final String EMPTY_VALUE_MARKER = "emptyValueMarker";

    /* a hidden input to hold the remove entry id */
    private static final String REMOVE_HIDDEN = "removeHidden";

    /* a hidden input set if the user click the add button */
    private static final String ADD_HIDDEN = "addHidden";

    public ChainSelectMany() {
        FacesContext context = FacesContext.getCurrentInstance();

        HtmlInputHidden emptyValueMarker = new HtmlInputHidden();
        emptyValueMarker.setId("emptyValueMarker");
        emptyValueMarker.setValue("true");
        getFacets().put(EMPTY_VALUE_MARKER, emptyValueMarker);

        UICommand addButton = (UICommand) context.getApplication().createComponent("org.ajax4jsf.ajax.CommandButton");
        addButton.getAttributes().put("id", "addButton");
        addButton.getAttributes().put("value", "add");
        getFacets().put(ADD_BUTTON, addButton);

        HtmlInputHidden addHidden = new HtmlInputHidden();
        addHidden.setId("addHidden");
        getFacets().put(ADD_HIDDEN, addHidden);

        UICommand removeButton = (UICommand) context.getApplication().createComponent("org.ajax4jsf.ajax.CommandLink");
        UIGraphic image = new UIGraphic();
        image.setValue("/icons/delete.png");
        removeButton.getAttributes().put("id", "removeButton");
        removeButton.getChildren().add(image);
        getFacets().put(REMOVE_BUTTON, removeButton);

        HtmlInputHidden removeHidden = new HtmlInputHidden();
        removeHidden.setId("removeHidden");
        getFacets().put(REMOVE_HIDDEN, removeHidden);
    }

    @Override
    public void decode(FacesContext context) {
        if (getDisplayValueOnly()) {
            return;
        }
        decodeSelection(context);
        decodeValue(context);
        setValid(true);
    }

    private void decodeValue(FacesContext context) {
        String emptyValueMarkerClientId = getFacet(EMPTY_VALUE_MARKER).getClientId(context);
        String removeEntryClientId = getFacet(REMOVE_HIDDEN).getClientId(context);
        String addEntryClientId = getFacet(ADD_HIDDEN).getClientId(context);
        Map<String, String> map = context.getExternalContext().getRequestParameterMap();

        String removeEntryId = map.get(removeEntryClientId);
        boolean addButtonClicked = "true".equals(map.get(addEntryClientId));
        String allValues = map.get(emptyValueMarkerClientId);

        String[] oldValue = StringUtils.split(allValues, ",");

        List<String> valueList = new ArrayList<String>(Arrays.asList(oldValue));

        if (addButtonClicked) {
            String[] selection = getSelection();
            if (validateEntry(context, selection)) {
                valueList.add(StringUtils.join(selection, getKeySeparator()));
            }
        }

        if (!StringUtils.isEmpty(removeEntryId)) {
            valueList.remove(removeEntryId);
        }

        String[] newValue = valueList.toArray(new String[valueList.size()]);
        setSubmittedValue(newValue);
    }

    @Override
    public String[] getSelection() {
        String clientId = getClientId(FacesContext.getCurrentInstance());
        String[] selection = selectionMap.get(clientId);
        if (selection == null) {
            selection = new String[0];
            selectionMap.put(clientId, selection);
        }
        return selection;
    }

    @Override
    public void encodeBegin(FacesContext context) throws IOException {
        if (getDisplayValueOnly()) {
            encodeReadOnly(context);
        } else {
            encodeReadWrite(context);
        }
    }

    public void encodeReadWrite(FacesContext context) throws IOException {
        ResponseWriter writer = context.getResponseWriter();

        getChildren().clear();
        writer.startElement("div", this);
        writer.writeAttribute("id", getClientId(context), "id");
        String style = getStyle();
        if (style != null) {
            writer.writeAttribute("style", style, "style");
        }
        String styleClass = getStyleClass();
        if (styleClass != null) {
            writer.writeAttribute("class", styleClass, "class");
        }

        String[] selectedKeys = getSelection();
        for (int level = 0; level < getDepth(); level++) {
            encodeListbox(context, level, selectedKeys);
        }

        encodeAddButton(context);
        encodeValue(context);
        writer.endElement("div");
    }

    public void encodeReadOnly(FacesContext context) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        String[] values = (String[]) getSubmittedValue();
        if (values == null) {
            values = (String[]) getValue();
        }
        if (values != null) {
            writer.startElement("div", this);
            for (String value : values) {
                String[] keys = StringUtils.split(value, getKeySeparator());
                List<DirectoryEntry> nodes = resolveKeys(keys);
                List<String> labels = new ArrayList<String>();
                for (DirectoryEntry node : nodes) {
                    String itemValue = node.getId();
                    String itemLabel = node.getLabel();
                    itemLabel = computeItemLabel(context, itemValue, itemLabel);
                    labels.add(itemLabel);
                }
                String concatenatedLabel = StringUtils.join(labels.iterator(), getKeySeparator());

                writer.startElement("div", this);
                writer.write(concatenatedLabel);
                writer.endElement("div");
            }
            writer.endElement("div");
        }
    }

    private void encodeAddButton(FacesContext context) throws IOException {
        UICommand addButton = (UICommand) getFacet(ADD_BUTTON);
        HtmlInputHidden addHidden = (HtmlInputHidden) getFacet(ADD_HIDDEN);
        String addJs = String.format("document.getElementById('%s').value = '%s'", addHidden.getClientId(context),
                "true");
        addButton.getAttributes().put("onclick", addJs);
        addHidden.setValue("");

        String reRender = getReRender();
        if (reRender == null) {
            reRender = getId();
        }
        addButton.getAttributes().put("reRender", reRender);
        ComponentUtils.encodeComponent(context, addButton);
        ComponentUtils.encodeComponent(context, addHidden);
    }

    @Override
    public boolean getRendersChildren() {
        return true;
    }

    @Override
    public void encodeChildren(FacesContext context) throws IOException {
    }

    private void encodeValue(FacesContext context) throws IOException {
        ResponseWriter writer = context.getResponseWriter();

        String[] values = (String[]) getSubmittedValue();
        if (values == null) {
            values = (String[]) getValue();
        }

        String compValue = StringUtils.join(values, ",");
        compValue = compValue == null ? "" : compValue;
        HtmlInputHidden emptyValueMarker = (HtmlInputHidden) getFacet(EMPTY_VALUE_MARKER);
        emptyValueMarker.setValue(compValue);
        ComponentUtils.encodeComponent(context, emptyValueMarker);

        if (values != null) {
            HtmlInputHidden removeHidden = (HtmlInputHidden) getFacet(REMOVE_HIDDEN);
            removeHidden.setValue("");
            ComponentUtils.encodeComponent(context, removeHidden);
            UICommand removeButton = (UICommand) getFacet(REMOVE_BUTTON);
            String reRender = getReRender();
            if (reRender == null) {
                reRender = getId();
            }
            removeButton.getAttributes().put("reRender", reRender);
            writer.startElement("div", this);
            for (String value : values) {
                String[] keys = StringUtils.split(value, getKeySeparator());
                List<DirectoryEntry> nodes = resolveKeys(keys);
                List<String> labels = new ArrayList<String>();
                for (DirectoryEntry node : nodes) {
                    String itemValue = node.getId();
                    String itemLabel = node.getLabel();
                    itemLabel = computeItemLabel(context, itemValue, itemLabel);
                    labels.add(itemLabel);
                }
                String concatenatedLabel = StringUtils.join(labels.iterator(), getKeySeparator());

                writer.startElement("div", this);
                String removeJs = String.format("document.getElementById('%s').value = '%s'",
                        removeHidden.getClientId(context), value);
                removeButton.getAttributes().put("onclick", removeJs);
                ComponentUtils.encodeComponent(context, removeButton);
                writer.write(concatenatedLabel);
                writer.endElement("div");
            }
            writer.endElement("div");
        }
    }

    /*
     * the number of visible components
     */
    public int getCurrentDepth() {
        return getDepth();
    }

    @Override
    public String getFamily() {
        return "nxdirectory.ChainSelectMany";
    }

    @Override
    public String getRendererType() {
        return null;
    }

    @Override
    public Object saveState(FacesContext context) {
        Object[] values = new Object[1];
        values[0] = super.saveState(context);
        return values;
    }

    @Override
    public void restoreState(FacesContext context, Object state) {
        Object[] values = (Object[]) state;
        super.restoreState(context, values[0]);
    }

}
