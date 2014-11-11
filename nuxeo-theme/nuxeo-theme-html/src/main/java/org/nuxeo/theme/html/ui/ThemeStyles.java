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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.formats.styles.Style;
import org.nuxeo.theme.html.CSSUtils;
import org.nuxeo.theme.themes.ThemeDescriptor;
import org.nuxeo.theme.themes.ThemeManager;

public class ThemeStyles {
    private static final Log log = LogFactory.getLog(ThemeStyles.class);

    private static final boolean IGNORE_VIEW_NAME = false;

    private static final boolean IGNORE_CLASSNAME = false;

    private static final boolean INDENT = false;

    public static String render(Map<String, String> params, boolean cache,
            boolean inline, boolean virtualHosting) {
        String themeName = params.get("themeName");
        String path = params.get("path");
        String basePath = params.get("basepath");

        String cssPath = VirtualHostHelper.getContextPathProperty()
                + "/nxthemes-css";

        // Load theme if needed
        ThemeDescriptor themeDescriptor = ThemeManager.getThemeDescriptorByThemeName(themeName);
        if (themeDescriptor != null && !themeDescriptor.isLoaded()) {
            ThemeManager.loadTheme(themeDescriptor);
        }

        if (inline) {
            final StringBuilder sb = new StringBuilder();

            final ThemeManager themeManager = Manager.getThemeManager();
            for (Style style : themeManager.getNamedStyles(themeName)) {
                sb.append(CSSUtils.styleToCss(style,
                        style.getSelectorViewNames(), IGNORE_VIEW_NAME,
                        IGNORE_CLASSNAME, INDENT));
            }
            for (Style style : themeManager.getStyles(themeName)) {
                sb.append(CSSUtils.styleToCss(style,
                        style.getSelectorViewNames(), IGNORE_VIEW_NAME,
                        IGNORE_CLASSNAME, INDENT));
            }

            String rendered = sb.toString();
            rendered = CSSUtils.expandVariables(rendered, basePath,
                    themeDescriptor);
            return String.format("<style type=\"text/css\">%s</style>",
                    rendered);
        }

        long timestamp = 0;
        if (cache) {
            if (themeDescriptor != null) {
                timestamp = themeDescriptor.getLastModified().getTime();
            }
        } else {
            timestamp = new Date().getTime();
        }
        return String.format(
                "<link rel=\"stylesheet\" type=\"text/css\" media=\"all\" href=\"%s/%s-styles.css?theme=%s&amp;path=%s&amp;basepath=%s&amp;timestamp=%s\" />",
                cssPath, themeName, themeName, path, basePath, timestamp);
    }
}
