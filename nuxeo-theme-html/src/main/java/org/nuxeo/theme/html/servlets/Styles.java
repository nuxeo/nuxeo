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

package org.nuxeo.theme.html.servlets;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.zip.GZIPOutputStream;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.theme.ApplicationType;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.formats.styles.Style;
import org.nuxeo.theme.html.CSSUtils;
import org.nuxeo.theme.html.Utils;
import org.nuxeo.theme.resources.ResourceBank;
import org.nuxeo.theme.themes.ThemeDescriptor;
import org.nuxeo.theme.themes.ThemeException;
import org.nuxeo.theme.themes.ThemeManager;
import org.nuxeo.theme.types.TypeFamily;

public final class Styles extends HttpServlet implements Serializable {

    private static final Log log = LogFactory.getLog(Styles.class);

    private static final long serialVersionUID = 1L;

    private static final boolean RESOLVE_PRESETS = true;

    private static final boolean IGNORE_VIEW_NAME = false;

    private static final boolean IGNORE_CLASSNAME = false;

    private static final boolean INDENT = false;

    @Override
    protected void doGet(final HttpServletRequest request,
            final HttpServletResponse response) throws IOException {
        doPost(request, response);
    }

    @Override
    protected void doPost(final HttpServletRequest request,
            final HttpServletResponse response) throws IOException {

        // headers
        response.addHeader("content-type", "text/css");

        final String themeName = request.getParameter("theme");
        if (themeName == null) {
            response.sendError(404);
            log.error("Theme name not set");
            return;
        }

        // Load theme if needed
        ThemeDescriptor themeDescriptor = ThemeManager.getThemeDescriptorByThemeName(themeName);
        if (themeDescriptor == null) {
            throw new IOException("Theme not found: " + themeName);
        }
        if (!themeDescriptor.isLoaded()) {
            ThemeManager.loadTheme(themeDescriptor);
        }

        // cache control
        final String applicationPath = request.getParameter("path");
        if (applicationPath != null) {
            final ApplicationType application = (ApplicationType) Manager.getTypeRegistry().lookup(
                    TypeFamily.APPLICATION, applicationPath);
            if (application != null) {
                Utils.setCacheHeaders(response, application.getStyleCaching());
            }
        }

        // compression
        OutputStream os = response.getOutputStream();
        if (Utils.supportsGzip(request)) {
            response.setHeader("Content-Encoding", "gzip");
            // Needed by proxy servers
            response.setHeader("Vary", "Accept-Encoding");
            os = new GZIPOutputStream(os);
        }

        final String basePath = request.getParameter("basepath");

        final ThemeManager themeManager = Manager.getThemeManager();
        String rendered = themeManager.getCachedStyles(themeName, basePath);

        if (rendered == null) {
            final StringBuilder sb = new StringBuilder();
            for (Style style : themeManager.getNamedStyles(themeName)) {
                sb.append(CSSUtils.styleToCss(style,
                        style.getSelectorViewNames(), RESOLVE_PRESETS,
                        IGNORE_VIEW_NAME, IGNORE_CLASSNAME, INDENT));
            }
            for (Style style : themeManager.getStyles(themeName)) {
                sb.append(CSSUtils.styleToCss(style,
                        style.getSelectorViewNames(), RESOLVE_PRESETS,
                        IGNORE_VIEW_NAME, IGNORE_CLASSNAME, INDENT));
            }
            rendered = sb.toString();

            if (basePath != null) {
                rendered = rendered.replaceAll("\\$\\{basePath\\}",
                        Matcher.quoteReplacement(basePath));
            }

            // Replace images from resource banks
            String resourceBankName = themeDescriptor.getResourceBankName();
            if (resourceBankName != null) {
                ResourceBank resourceBank;
                try {
                    resourceBank = ThemeManager.getResourceBank(resourceBankName);
                    for (String path : resourceBank.getImages()) {
                        rendered = rendered.replace(path, String.format(
                                "'/nuxeo/nxthemes-images/%s/%s'",
                                resourceBankName, path));
                    }
                } catch (ThemeException e) {
                    log.warn("Could not get the list of bank images in: "
                            + resourceBankName);
                }
            }
            themeManager.setCachedStyles(themeName, basePath, rendered);
        }

        os.write(rendered.getBytes());
        os.close();
    }
}
