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
        String collectionName = params.get("collection");

        String cssPath = VirtualHostHelper.getContextPathProperty()
                + "/nxthemes-css";

        // Load theme if needed
        ThemeDescriptor themeDescriptor = ThemeManager.getThemeDescriptorByThemeName(themeName);
        if (themeDescriptor == null) {
            log.warn(String.format(
                    "Could not resolve theme descriptor for name '%s'",
                    themeName));
        }
        if (themeDescriptor != null && !themeDescriptor.isLoaded()) {
            ThemeManager.loadTheme(themeDescriptor);
        }

        if (inline) {
            boolean includeDate = true;
            String includeDateParam = params.get("includeDate");
            if (includeDateParam != null) {
                includeDate = Boolean.TRUE.equals(includeDateParam);
            }
            return String.format(
                    "<style type=\"text/css\">\n%s\n</style>",
                    generateThemeStyles(themeName, themeDescriptor, basePath,
                            collectionName, includeDate));
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
                "<link rel=\"stylesheet\" type=\"text/css\" media=\"all\" href=\"%s/%s-styles.css?theme=%s&amp;path=%s&amp;basepath=%s&amp;collection=%s&amp;timestamp=%s\" />",
                cssPath, themeName, themeName, path, basePath, collectionName,
                Long.valueOf(timestamp));
    }

    public static String generateThemeStyles(String themeName,
            ThemeDescriptor themeDescriptor, String basePath,
            String collectionName, boolean includeDate) {

        final StringBuilder sb = new StringBuilder();

        final ThemeManager themeManager = Manager.getThemeManager();
        // Named styles sorted to preserve dependencies in CSS
        for (Style style : themeManager.getSortedNamedStyles(themeName)) {
            sb.insert(0, CSSUtils.styleToCss(style,
                    style.getSelectorViewNames(), IGNORE_VIEW_NAME,
                    IGNORE_CLASSNAME, INDENT));
            sb.insert(
                    0,
                    String.format("\n\n/* CSS styles named '%s' */\n",
                            style.getName()));
        }

        // add generation comment on top of file
        if (includeDate) {
            sb.insert(0, String.format("/* CSS styles for theme '%s' (%s) */\n",
                    themeName, new Date()));
        } else {
            sb.insert(0, String.format("/* CSS styles for theme '%s' */\n",
                    themeName));
        }

        // Local theme styles
        for (Style style : themeManager.getStyles(themeName)) {
            sb.append(CSSUtils.styleToCss(style, style.getSelectorViewNames(),
                    IGNORE_VIEW_NAME, IGNORE_CLASSNAME, INDENT));
        }

        final String collectionCssMarker = ThemeManager.getCollectionCssMarker();
        String rendered = sb.toString();
        if (collectionName != null) {
            // Preprocessing: replace the collection name
            rendered = rendered.replaceAll(collectionCssMarker, collectionName);
        }
        rendered = CSSUtils.expandVariables(rendered, basePath, collectionName,
                themeDescriptor);
        return rendered;
    }
}
