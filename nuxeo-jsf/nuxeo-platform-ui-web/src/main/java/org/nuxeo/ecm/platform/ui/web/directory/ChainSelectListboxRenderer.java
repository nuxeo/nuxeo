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

import com.sun.faces.renderkit.RenderKitUtils;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 */
public class ChainSelectListboxRenderer extends Renderer {

    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(ChainSelectListboxRenderer.class);

    @Override
    public void decode(FacesContext facesContext, UIComponent component) {
    }

    @SuppressWarnings("resource") // ResponseWriter not ours to close
    @Override
    public void encodeBegin(FacesContext context, UIComponent component) throws IOException {
        ChainSelectListboxComponent comp = (ChainSelectListboxComponent) component;
        ResponseWriter writer = context.getResponseWriter();
        Boolean displayValueOnly = comp.getChain().getBooleanProperty("displayValueOnly", false);
        if (displayValueOnly) {
            return;
        }
        encodeInput(context, writer, comp);
    }

    private static void encodeInput(FacesContext context, ResponseWriter writer, ChainSelectListboxComponent comp)
            throws IOException {
        String id = comp.getClientId(context);
        ChainSelect chain = comp.getChain();
        String cssStyleClass = comp.getStringProperty("cssStyleClass", null);
        String cssStyle = comp.getStringProperty("cssStyle", null);
        String onchange = comp.getStringProperty("onchange", null);
        // XXX hack: add onchange set on the chain select
        String chainOnchange = chain.getOnchange();
        if (chainOnchange != null) {
            if (onchange == null) {
                onchange = chainOnchange;
            } else {
                if (onchange.endsWith(";")) {
                    onchange += chainOnchange;
                } else {
                    onchange += "; " + chainOnchange;
                }
            }
        }

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
        RenderKitUtils.renderOnchange(context, comp, false);

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
                writer.writeText(
                        DirectoryHelper.instance().getOptionValue(optionId, optionLabel, display, displayIdAndLabel,
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
