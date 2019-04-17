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

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.render.Renderer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.i18n.I18NUtils;
import org.nuxeo.ecm.platform.ui.web.util.BaseURL;

/**
 * Renders many listboxes for the MultiListbox component
 *
 * @deprecated : renderer is useless (not declared correctly in deployment-fragment.xml and bugg) should be refactored
 *             instead
 */
@Deprecated
public class ChainSelectMultiListboxRenderer extends Renderer {

    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(ChainSelectMultiListboxRenderer.class);

    @Override
    public void decode(FacesContext facesContext, UIComponent component) {
    }

    @SuppressWarnings("resource") // ResponseWriter not ours to close
    @Override
    public void encodeBegin(FacesContext context, UIComponent component) throws IOException {
        ChainSelectMultiListboxComponent comp = (ChainSelectMultiListboxComponent) component;
        ResponseWriter writer = context.getResponseWriter();
        Boolean displayValueOnly = comp.getChain().getBooleanProperty("displayValueOnly", false);
        if (displayValueOnly) {
            return;
        }
        encodeInput(context, writer, comp);
    }

    private void encodeInput(FacesContext context, ResponseWriter writer, ChainSelectMultiListboxComponent mcomp)
            throws IOException {

        ChainSelectListboxComponent[] sComps = mcomp.createSingleComponents();
        for (ChainSelectListboxComponent component : sComps) {
            encodeInput(context, writer, component);

            // mcomp.getChildren()
            // mcomp.encodeChildren(context);
        }
    }

    private static void encodeInput(FacesContext context, ResponseWriter writer, ChainSelectListboxComponent comp)
            throws IOException {
        String id = comp.getClientId(context);
        ChainSelect chain = comp.getChain();
        String cssStyleClass = comp.getStringProperty("cssStyleClass", null);
        String cssStyle = comp.getStringProperty("cssStyle", null);
        String onchange = comp.getStringProperty("onchange", null);

        String id1 = comp.getClientId(context).split(":")[0];
        String id2 = comp.getClientId(context);
        onchange = "A4J.AJAX.Submit('_viewRoot','" + id1 + "',event,{'parameters':{'" + id2 + "':'" + id2
                + "'},'actionUrl':'" + BaseURL.getContextPath() + "/documents/tabs/document_externe_edit.faces'})";

        boolean multiSelect = comp.getBooleanProperty("multiSelect", false);
        String size = comp.getStringProperty("size", null);
        boolean displayIdAndLabel = comp.getBooleanProperty("displayIdAndLabel", false);
        String displayIdAndLabelSeparator = comp.getStringProperty("displayIdAndLabelSeparator", " ");
        boolean localize = comp.getBooleanProperty("localize", false);
        String display = comp.getStringProperty("display", "");

        if (chain.getBooleanProperty("displayValueOnly", false)) {
            return;
        }

        writer.startElement("select", comp);
        writer.writeAttribute("id", id, "id");
        writer.writeAttribute("name", id, "name");
        if (onchange != null) {
            writer.writeAttribute("onchange", onchange, "onchange");
        }
        if (cssStyleClass != null) {
            writer.writeAttribute("class", cssStyleClass, "class");
        }
        if (cssStyle != null) {
            writer.writeAttribute("style", cssStyle, "style");
        }
        if (multiSelect) {
            writer.writeAttribute("multiple", "true", "multiple");
        }
        if (size != null && Integer.valueOf(size) > 0) {
            writer.writeAttribute("size", size, "size");
        }

        // <select id="j_id202:theme7" name="j_id202:theme7"
        // onchange="A4J.AJAX.Submit('_viewRoot','j_id202',event,{'parameters':{'j_id202:j_id286':'j_id202:j_id286'},'actionUrl':'/nuxeo/documents/tabs/document_externe_edit.faces'})"
        // multiple="true" size="4"><option value="">Select a value</option>
        // writer.writeAttribute("onchange","onchange");

        List<String> valueList = new ArrayList<>();

        int index = comp.getIndex();
        Selection[] selections = chain.getSelections();
        if (selections != null) {
            for (Selection selection : selections) {
                valueList.add(selection.getColumnValue(index));
            }
        }

        String optionsLabel = translate(context, "label.vocabulary.selectValue");

        writer.startElement("option", comp);
        writer.writeAttribute("value", "", "value");
        writer.writeText(optionsLabel, null);
        writer.endElement("option");
        writer.write("\n");

        Map<String, DirectorySelectItem> options = comp.getOptions();
        if (options != null) {
            for (DirectorySelectItem item : options.values()) {
                String optionId = (String) item.getValue();
                String optionLabel = item.getLabel();

                writer.startElement("option", comp);
                writer.writeAttribute("value", optionId, "value");
                if (valueList.contains(optionId)) {
                    writer.writeAttribute("selected", "true", "selected");
                }
                if (localize) {
                    optionLabel = translate(context, optionLabel);
                }
                writer.writeText(DirectoryHelper.getOptionValue(optionId, optionLabel, display, displayIdAndLabel,
                        displayIdAndLabelSeparator), null);
                writer.endElement("option");
            }
        }
        writer.endElement("select");
        writer.write("\n");
    }

    protected static String translate(FacesContext context, String label) {
        String bundleName = context.getApplication().getMessageBundle();
        Locale locale = context.getViewRoot().getLocale();
        label = I18NUtils.getMessageString(bundleName, label, null, locale);
        return label;
    }
}
