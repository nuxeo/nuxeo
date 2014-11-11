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

        // tiny mce scripts
        writer.startElement("script", editorComp);
        writer.writeAttribute("type", "text/javascript", null);
        writer.writeAttribute("src", "tiny_mce/tiny_mce.js", null);
        // force the script tag to be opened and then closed to avoid IE bug.
        writer.write(" ");
        writer.endElement("script");

        String editorSelector = editorComp.getEditorSelector();

        writer.startElement("script", editorComp);
        writer.writeAttribute("type", "text/javascript", null);
        writer.writeText(String.format("var lang = \"%s\";",
                locale.getLanguage()), null);
        writer.writeText(String.format("var editorSelector = \"%s\";",
                editorSelector), null);
        writer.writeText(String.format("var width = \"%s\";",
                editorComp.getWidth()), null);
        writer.writeText(String.format("var height = \"%s\";",
                editorComp.getHeight()), null);

        // plugins registration
        if (pluginsOptions == null) {
            final HtmlEditorPluginService pluginService = Framework.getLocalService(HtmlEditorPluginService.class);
            pluginsOptions = new HashMap<String, String>();
            pluginsOptions.put("plugins",
                    pluginService.getFormattedPluginsNames());
            toolbarPluginsOptions = pluginService.getToolbarsButtons();
        }
        writer.writeText(String.format("var plugins = \"%s\";",
                pluginsOptions.get("plugins")), null);
        writer.writeText(generateToolbarOptions(toolbarPluginsOptions), null);
        writer.endElement("script");

        writer.startElement("script", editorComp);
        writer.writeAttribute("type", "text/javascript", null);

        writer.writeAttribute("src", "tiny_mce/tiny_mce_init.js", null);
        writer.write(" ");
        writer.endElement("script");

        // input text area
        String clientId = editorComp.getClientId(context);
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

    protected static String generateToolbarOptions(Map<String, String> options) {
        StringBuilder res = new StringBuilder();
        res.append("var toolbarOptions = new Array();");
        for (Map.Entry<String, String> option : options.entrySet()) {
            res.append("toolbarOptions['" + option.getKey() + "'] = '"
                    + option.getValue() + "';");
        }
        return res.toString();
    }

}
