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

import java.util.Date;
import java.util.Map;

import org.nuxeo.theme.Manager;
import org.nuxeo.theme.formats.styles.Style;
import org.nuxeo.theme.html.Utils;
import org.nuxeo.theme.themes.ThemeManager;

public class ThemeStyles {

    private static final boolean RESOLVE_PRESETS = true;

    private static final boolean IGNORE_VIEW_NAME = false;

    private static final boolean IGNORE_CLASSNAME = false;

    private static final boolean INDENT = false;

    public static String render(Map<String, String> params, boolean cache,
            boolean inline, boolean virtualHosting) {
        String themeName = params.get("themeName");
        String path = params.get("path");
        
        String cssPath = "/nuxeo/nxthemes-css";
        if (virtualHosting) {
            cssPath = path + "/nxthemes-css";
        }

        if (inline) {
            final StringBuilder sb = new StringBuilder();
            sb.append("<style type=\"text/css\">");
            final ThemeManager themeManager = Manager.getThemeManager();
            for (Style style : themeManager.getNamedStyles(themeName)) {
                sb.append(Utils.styleToCss(style, style.getSelectorViewNames(),
                        RESOLVE_PRESETS, IGNORE_VIEW_NAME, IGNORE_CLASSNAME,
                        INDENT));
            }
            for (Style style : themeManager.getStyles(themeName)) {
                sb.append(Utils.styleToCss(style, style.getSelectorViewNames(),
                        RESOLVE_PRESETS, IGNORE_VIEW_NAME, IGNORE_CLASSNAME,
                        INDENT));
            }
            sb.append("</style>");
            return sb.toString();
        }
        if (cache) {
            return String.format(
                    "<link rel=\"stylesheet\" type=\"text/css\" media=\"all\" href=\""
                            + cssPath + "/?theme=%s&amp;path=%s\" />",
                    themeName, path);
        } else {
            return String.format(
                    "<link rel=\"stylesheet\" type=\"text/css\" media=\"all\" href=\""
                            + cssPath
                            + "/?theme=%s&amp;path=%s&amp;timestamp=%s\" />",
                    themeName, path, new Date().getTime());
        }
    }
}
