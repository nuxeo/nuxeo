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

package org.nuxeo.theme.jsf.component;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import javax.faces.component.UIOutput;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.servlet.ServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.elements.ElementFormatter;
import org.nuxeo.theme.elements.ThemeElement;
import org.nuxeo.theme.formats.widgets.Widget;
import org.nuxeo.theme.jsf.URLUtils;

public class UIHead extends UIOutput {

    private static final Log log = LogFactory.getLog(UIHead.class);

    @Override
    public void encodeAll(final FacesContext context) throws IOException {
        final ResponseWriter writer = context.getResponseWriter();
        final ExternalContext externalContext = (ExternalContext) context.getExternalContext();
        final URL themeUrl = (URL) externalContext.getRequestMap().get(
                "nxthemesThemeUrl");
        final ThemeElement theme = Manager.getThemeManager().getThemeByUrl(
                themeUrl);
        final Widget widget = (Widget) ElementFormatter.getFormatFor(theme,
                "widget");

        if (widget == null) {
            log.warn("Theme " + themeUrl + " has no widget format.");
        } else {
            final Properties properties = widget.getProperties();

            // Charset
            final String charset = properties.getProperty("charset", "utf-8");
            writer.write(String.format(
                    "<meta http-equiv=\"Content-Type\" content=\"text/html;charset=%s\"/>",
                    charset));

            // Site icon
            final String icon = properties.getProperty("icon", "/favicon.ico");
            writer.write(String.format(
                    "<link rel=\"icon\" href=\"%s\" type=\"image/x-icon\"/>",
                    icon));
            writer.write(String.format(
                    "<link rel=\"shortcut icon\" href=\"%s\" type=\"image/x-icon\"/>",
                    icon));
        }

        // Styles
        final String contextPath = externalContext.getRequestContextPath();
        writer.write(String.format(
                "<link rel=\"stylesheet\" type=\"text/css\" media=\"all\" href=\"/nuxeo/nxthemes-css/?path=%s\"/>",
                contextPath));

        // Base URL
        final ServletRequest request = (ServletRequest) externalContext.getRequest();
        final String baseUrl = URLUtils.getBaseURL(request);
        if (baseUrl != null) {
            writer.write(String.format("<base href=\"%s\" />", baseUrl));
        }
    }
}
