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
 * $Id: InputDateTimeRenderer.java 30751 2008-02-28 11:02:31Z cbaican $
 */

package org.nuxeo.ecm.platform.ui.web.component.date;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.apache.commons.lang.StringUtils;

import com.sun.faces.renderkit.html_basic.HtmlBasicInputRenderer;

/**
 * Renderer for input date time component.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class InputDateTimeRenderer extends HtmlBasicInputRenderer {

    @Override
    public void encodeBegin(FacesContext context, UIComponent component)
            throws IOException {
        if (!component.isRendered()) {
            return;
        }

        UIInputDateTime dateTimeComp = (UIInputDateTime) component;
        ResponseWriter writer = context.getResponseWriter();
        String localeString = dateTimeComp.getLocale();
        if (localeString == null) {
            // get local string
            Locale locale = context.getViewRoot().getLocale();
            localeString = locale.getLanguage();
        }

        // localization script
        writer.startElement("script", dateTimeComp);
        writer.writeAttribute("type", "text/javascript", null);
        String scriptUrl = generateResourceUrl(context, String.format(
                "/jscalendar/lang/calendar-%s.js", localeString), null);
        writer.writeAttribute("src", scriptUrl, null);
        // force the script tag to be opened and then closed to avoid IE bug.
        writer.write(" ");
        writer.endElement("script");

        String inputTextId = dateTimeComp.getClientId(context);
        String triggerButtonId = inputTextId + ":trigger";

        // input text
        writer.startElement("input", dateTimeComp);
        writer.writeAttribute("type", "text", null);
        String onchange = dateTimeComp.getOnchange();
        if (onchange != null) {
            writer.writeAttribute("onchange", onchange, "onchange");
        }
        String onclick = dateTimeComp.getOnclick();
        if (onclick != null) {
            writer.writeAttribute("onclick", onclick, "onclick");
        }
        String onselect = dateTimeComp.getOnselect();
        if (onselect != null) {
            writer.writeAttribute("onselect", onselect, "onselect");
        }
        writer.writeAttribute("id", inputTextId, null);
        writer.writeAttribute("name", inputTextId, null);
        Object currentValue = getCurrentValue(dateTimeComp);
        writer.writeAttribute("value", getFormattedValue(context, component,
                currentValue), null);
        String styleClass = (String) component.getAttributes().get("styleClass");
        if (styleClass != null) {
            writer.writeAttribute("class", styleClass, "styleClass");
        }
        writer.endElement("input");

        // trigger button
        writer.startElement("img", dateTimeComp);
        writer.writeAttribute("id", triggerButtonId, null);
        writer.writeAttribute("name", triggerButtonId, null);
        String imgUrl = generateResourceUrl(context,
                dateTimeComp.getTriggerImg(),
                dateTimeComp.getDefaultTriggerImg());
        writer.writeAttribute("src", imgUrl, null);
        writer.writeAttribute("alt", dateTimeComp.getTriggerLabel(), null);
        writer.writeAttribute("title", dateTimeComp.getTriggerLabel(), null);
        String triggerStyleClass = (String) component.getAttributes().get(
                "triggerStyleClass");
        if (triggerStyleClass != null) {
            writer.writeAttribute("class", triggerStyleClass, "styleClass");
        } else {
            writer.writeAttribute("class", "calendarTrigger", "styleClass");
        }
        writer.endElement("img");

        // javascript calendar
        writer.startElement("script", dateTimeComp);
        writer.writeAttribute("type", "text/javascript", null);
        Map<String, String> options = new HashMap<String, String>();
        options.put("inputField", inputTextId);
        options.put("button", triggerButtonId);
        /* in javascript: empty string == false, non empty string == true */
        String showsTime = dateTimeComp.getShowsTime() ? "true" : "";
        options.put("showsTime", showsTime);
        options.put("ifFormat", convertFormat(dateTimeComp.getFormat()));
        String calendarSetup = String.format("Calendar.setup(%s);",
                generateOptions(options));
        writer.writeText(calendarSetup, null);
        writer.endElement("script");

        writer.flush();
    }

    protected static Object getCurrentValue(UIInputDateTime comp) {
        Object submitted = comp.getSubmittedValue();
        if (submitted != null) {
            return submitted;
        }
        return comp.getValue();
    }

    /**
     * Converts format as used by the converter to the javascript syntax. TODO:
     * translation is basic for now...
     */
    protected static String convertFormat(String format) {
        String jsFormat = format;
        // this method is now OK for english, us, french, russian, italian,
        // deutsh and arabic locals

        // days
        jsFormat = jsFormat.replace("dd", "%d");
        if (!jsFormat.contains("%d")) {
            jsFormat = jsFormat.replace("d", "%e");
        }
        // month
        jsFormat = jsFormat.replace("MMM", "%b");
        jsFormat = jsFormat.replace("MM", "%m");
        if (!jsFormat.contains("%M")) {
            jsFormat = jsFormat.replace("M", "%m");
        }

        // year
        jsFormat = jsFormat.replace("yyyy", "%Y");
        jsFormat = jsFormat.replace("yy", "%y");

        // minutes
        jsFormat = jsFormat.replace("mm", "%M");

        // hours
        jsFormat = jsFormat.replace("HH", "%H");
        jsFormat = jsFormat.replace("hh", "%I");
        if (!jsFormat.contains("%H")) {
            jsFormat = jsFormat.replace("H", "%H");
        }
        if (!jsFormat.contains("%h")) {
            jsFormat = jsFormat.replace("h", "%I");
        }
        // AM-PM token
        jsFormat = jsFormat.replace("a", "%p");

        return jsFormat;
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

    protected static String generateResourceUrl(FacesContext context,
            String resource, String defaultResource) {
        // avoid empty url
        if (resource == null || "".equals(resource.trim())) {
            return defaultResource;
        }
        resource = context.getApplication().getViewHandler().getResourceURL(
                context, resource);
        return (context.getExternalContext().encodeResourceURL(resource));
    }

}
