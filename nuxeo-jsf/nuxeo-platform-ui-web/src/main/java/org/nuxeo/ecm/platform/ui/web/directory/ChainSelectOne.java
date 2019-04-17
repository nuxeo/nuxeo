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
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.apache.commons.lang3.StringUtils;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 */
public class ChainSelectOne extends ChainSelectBase {

    @Override
    public void decode(FacesContext context) {
        if (getDisplayValueOnly()) {
            return;
        }
        decodeSelection(context);

        setValid(true);
        setSubmittedValue(getValueAsString(getSelection()));
    }

    @Override
    public String[] getSelection() {
        String clientId = getClientId(FacesContext.getCurrentInstance());
        String[] selection = selectionMap.get(clientId);
        if (selection == null) {
            selection = getValueAsArray((String) getValue());
            selectionMap.put(clientId, selection);
        }
        return selection;
    }

    @Override
    public void encodeBegin(FacesContext context) throws IOException {
        getChildren().clear();

        if (getDisplayValueOnly()) {
            encodeReadOnly(context);
        } else {
            encodeReadWrite(context);
        }
    }

    public void encodeReadOnly(FacesContext context) throws IOException {
        @SuppressWarnings("resource")
        ResponseWriter writer = context.getResponseWriter();
        String value = (String) getSubmittedValue();
        if (value == null) {
            value = (String) getValue();
        }
        if (value != null) {
            String[] keys = StringUtils.split(value, getKeySeparator());
            List<DirectoryEntry> nodes = resolveKeys(keys);
            List<String> labels = new ArrayList<>();
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
    }

    public void encodeReadWrite(FacesContext context) throws IOException {
        @SuppressWarnings("resource")
        ResponseWriter writer = context.getResponseWriter();
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
        writer.endElement("div");
    }

    @Override
    public void validateValue(FacesContext context, Object newValue) {
        super.validateValue(context, newValue);
        if (newValue == null || !isValid()) {
            return;
        }
        String[] keys = getValueAsArray((String) newValue);
        validateEntry(context, keys);
    }

    @Override
    public String getFamily() {
        return "nxdirectory.ChainSelectOne";
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
