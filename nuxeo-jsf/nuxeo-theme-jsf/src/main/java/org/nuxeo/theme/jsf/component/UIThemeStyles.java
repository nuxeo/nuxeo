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
import java.util.HashMap;
import java.util.Map;

import javax.faces.component.UIOutput;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.servlet.http.HttpServletRequest;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.theme.html.Utils;
import org.nuxeo.theme.html.ui.ThemeStyles;
import org.nuxeo.theme.themes.ThemeManager;

public class UIThemeStyles extends UIOutput {

    private String cache;

    private String inline;

    private String theme;

    public String getCache() {
        return cache;
    }

    public void setCache(String cache) {
        this.cache = cache;
    }

    public String getInline() {
        return inline;
    }

    public void setInline(String inline) {
        this.inline = inline;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    @Override
    public void encodeAll(final FacesContext context) throws IOException {
        Map<String, Object> attributes = getAttributes();
        cache = (String) attributes.get("cache");
        inline = (String) attributes.get("inline");
        theme = (String) attributes.get("theme");

        final ResponseWriter writer = context.getResponseWriter();
        final ExternalContext externalContext = context.getExternalContext();

        Map<String, Object> requestMap = externalContext.getRequestMap();
        if (theme == null) {
            final URL themeUrl = (URL) requestMap.get("org.nuxeo.theme.url");
            theme = ThemeManager.getThemeNameByUrl(themeUrl);
        }

        Map<String, String> params = new HashMap<String, String>();

        params.put("themeName", theme);
        params.put("path", externalContext.getRequestContextPath());
        // FIXME: use configuration
        String basePath = Framework.getProperty("org.nuxeo.ecm.contextPath",
                "/nuxeo")
                + "/site";
        params.put("basepath", basePath);

        Boolean virtualHosting = Utils.isVirtualHosting((HttpServletRequest) externalContext.getRequest());
        writer.write(ThemeStyles.render(params, Boolean.parseBoolean(cache),
                Boolean.parseBoolean(inline), virtualHosting));
    }
}
