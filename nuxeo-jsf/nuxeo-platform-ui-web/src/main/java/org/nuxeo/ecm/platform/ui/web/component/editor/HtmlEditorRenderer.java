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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: HtmlEditorRenderer.java 28610 2008-01-09 17:13:52Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.component.editor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.platform.ui.web.htmleditor.api.HtmlEditorPluginService;
import org.nuxeo.runtime.api.Framework;

import com.sun.faces.renderkit.html_basic.HtmlBasicInputRenderer;

/**
 * Renderer for html editor component.
 * <p>
 * Uses TinyMCE javascript editor.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class HtmlEditorRenderer extends HtmlBasicInputRenderer {

    private static Map<String, String> pluginsOptions;

    private static Map<String, String> toolbarPluginsOptions;

    @Override
    public void encodeBegin(FacesContext context, UIComponent component) throws IOException {
        if (!component.isRendered()) {
            return;
        }

        UIHtmlEditor editorComp = (UIHtmlEditor) component;
        ResponseWriter writer = context.getResponseWriter();
        Locale locale = context.getViewRoot().getLocale();

        if (pluginsOptions == null) {
            final HtmlEditorPluginService pluginService = Framework.getService(HtmlEditorPluginService.class);
            Map<String,String> options = new HashMap<>();
            options.put("plugins", pluginService.getFormattedPluginsNames());
            pluginsOptions = options;
        }
        if (toolbarPluginsOptions == null) {
            final HtmlEditorPluginService pluginService = Framework.getService(HtmlEditorPluginService.class);
            Map<String,String> options = new HashMap<>();
            options.put("toolbar", pluginService.getFormattedToolbarsButtonsNames());
            toolbarPluginsOptions = options;
        }

        String clientId = editorComp.getClientId(context);
        boolean disableHtmlInit = Boolean.TRUE.equals(editorComp.getDisableHtmlInit());

        // input text area
        writer.startElement("textarea", editorComp);
        writer.writeAttribute("id", clientId, null);
        writer.writeAttribute("name", clientId, null);
        String editorSelector = editorComp.getEditorSelector();
        if (Boolean.TRUE.equals(editorComp.getDisableHtmlInit())) {
            writer.writeAttribute("class", editorSelector + ",disableMCEInit", null);
        } else {
            writer.writeAttribute("class", editorSelector, null);
        }
        writer.writeAttribute("rows", editorComp.getRows(), null);
        writer.writeAttribute("cols", editorComp.getCols(), null);
        Object currentValue = getCurrentValue(editorComp);
        if (currentValue != null) {
            writer.writeText(currentValue, null);
        } else {
            writer.writeText("", null);
        }
        writer.endElement("textarea");

        if (!disableHtmlInit) {
            writer.startElement("script", editorComp);
            writer.writeAttribute("type", "text/javascript", null);
            String compConfiguration = editorComp.getConfiguration();
            if (StringUtils.isBlank(compConfiguration)) {
                compConfiguration = "{}";
            }
            // Since 5.7.3, use unique clientId instead of editorSelector value
            // so that tiny mce editors are initialized individually: no need
            // anymore to specify a class to know which one should or should
            // not be initialized
            String scriptContent = new StringBuilder().append("initTinyMCE(")
                                                      .append(editorComp.getWidth())
                                                      .append(", ")
                                                      .append(editorComp.getHeight())
                                                      .append(", '")
                                                      .append(clientId)
                                                      .append("', '")
                                                      .append(pluginsOptions.get("plugins"))
                                                      .append("', '")
                                                      .append(locale.getLanguage())
                                                      .append("', '")
                                                      .append(toolbarPluginsOptions.get("toolbar"))
                                                      .append("', '")
                                                      .append(compConfiguration)
                                                      .append("');")
                                                      .toString();
            writer.writeText(scriptContent, null);
            String ajaxScriptContent = "jsf.ajax.addOnEvent(function(data) {if (data.status == \"success\") {"
                    + scriptContent + "}});";
            writer.writeText(ajaxScriptContent, null);
            String scriptContent2 = "jQuery(document.getElementById('" + clientId
                    + "')).closest('form').bind('ajaxsubmit', function() { var editor = tinyMCE.editors['" + clientId
                    + "']; if (editor != undefined) {editor.save()};});";
            writer.writeText(scriptContent2, null);
            writer.endElement("script");
        }

        writer.flush();
    }

    protected static Object getCurrentValue(UIInput comp) {
        Object submitted = comp.getSubmittedValue();
        if (submitted != null) {
            return submitted;
        }
        return comp.getValue();
    }

    protected static String generateOptions(Map<String, String> options) {
        List<String> strOptions = new ArrayList<String>();
        for (Map.Entry<String, String> option : options.entrySet()) {
            strOptions.add(option.getKey() + ": \"" + option.getValue() + "\"");
        }
        StringBuilder res = new StringBuilder();
        res.append('{');
        res.append(StringUtils.join(strOptions.toArray(), ", "));
        res.append('}');
        return res.toString();
    }

}
