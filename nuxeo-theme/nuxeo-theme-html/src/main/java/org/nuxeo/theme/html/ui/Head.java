/*
 * (C) Copyright 2006-2007 Nuxeo SAS <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jean-Marc Orliaguet, Chalmers
 *
 * $Id$
 */

package org.nuxeo.theme.html.ui;

import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.elements.ElementFormatter;
import org.nuxeo.theme.elements.ThemeElement;
import org.nuxeo.theme.formats.widgets.Widget;

public class Head {

    private static final Log log = LogFactory.getLog(Head.class);

    public static String render(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();

        String themeName = params.get("themeName");
        final ThemeElement theme = Manager.getThemeManager().getThemeByName(
                themeName);
        final Widget widget = (Widget) ElementFormatter.getFormatFor(theme,
                "widget");

        if (widget == null) {
            log.warn("Theme " + themeName + " has no widget.");
        } else {
            final Properties properties = widget.getProperties();

            // Charset
            final String charset = properties.getProperty("charset", "utf-8");
            sb.append(String.format(
                    "<meta http-equiv=\"Content-Type\" content=\"text/html;charset=%s\"/>",
                    charset));

            // Site icon
            final String icon = properties.getProperty("icon", "/favicon.ico");
            sb.append(String.format(
                    "<link rel=\"icon\" href=\"%s\" type=\"image/x-icon\"/>",
                    icon));
            sb.append(String.format(
                    "<link rel=\"shortcut icon\" href=\"%s\" type=\"image/x-icon\"/>",
                    icon));
        }

        // Base URL
        final String baseUrl = params.get("baseUrl");
        if (baseUrl != null) {
            sb.append(String.format("<base href=\"%s\" />", baseUrl));
        }

        return sb.toString();
    }

}
