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
    public void encodeBegin(FacesContext context, UIComponent component)
            throws IOException {
        if (!component.isRendered()) {
            return;
        }

        UIHtmlEditor editorComp = (UIHtmlEditor) component;
        ResponseWriter writer = context.getResponseWriter();
        Locale locale = context.getViewRoot().getLocale();

        // tiny MCE generic scripts now included in every page header

        // script to actually init tinyMCE with configured options
        String editorSelector = editorComp.getEditorSelector();
        // plugins registration
        if (pluginsOptions == null) {
            final HtmlEditorPluginService pluginService = Framework.getLocalService(HtmlEditorPluginService.class);
            pluginsOptions = new HashMap<String, String>();
            pluginsOptions.put("plugins",
                    pluginService.getFormattedPluginsNames());
            toolbarPluginsOptions = new HashMap<String, String>();
            toolbarPluginsOptions.put("toolbar",
                    pluginService.getFormattedToolbarsButtonsNames());
        }

        String clientId = editorComp.getClientId(context);
        boolean disableHtmlInit = Boolean.TRUE.equals(editorComp.getDisableHtmlInit());

        // input text area
        writer.startElement("textarea", editorComp);
        writer.writeAttribute("id", clientId, null);
        writer.writeAttribute("name", clientId, null);
        if (Boolean.TRUE.equals(editorComp.getDisableHtmlInit())) {
            writer.writeAttribute("class", editorSelector + ",disableMCEInit",
                    null);
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
            // Since 5.7.3, use unique clientId instead of editorSelector value
            // so that tiny mce editors are initialized individually: no need
            // anymore to specify a class to know which one should or should
            // not be initialized
            String scriptContent = String.format(
                    "initTinyMCE(%s, %s, '%s', '%s', '%s', '%s');",
                    editorComp.getWidth(), editorComp.getHeight(), clientId,
                    pluginsOptions.get("plugins"), locale.getLanguage(),
                    toolbarPluginsOptions.get("toolbar"));
            writer.writeText(scriptContent, null);
            String ajaxScriptContent = String.format(
                    "jsf.ajax.addOnEvent(function(data) {if (data.status == \"success\") {%s}});",
                    scriptContent);
            writer.writeText(ajaxScriptContent, null);
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
            strOptions.add(String.format("%s : \"%s\"", option.getKey(),
                    option.getValue()));
        }
        StringBuilder res = new StringBuilder();
        res.append('{');
        res.append(StringUtils.join(strOptions.toArray(), ", "));
        res.append('}');
        return res.toString();
    }

}
