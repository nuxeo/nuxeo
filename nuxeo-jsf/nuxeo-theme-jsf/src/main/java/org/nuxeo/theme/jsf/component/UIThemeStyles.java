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

import org.nuxeo.theme.html.ui.ThemeStyles;
import org.nuxeo.theme.themes.ThemeManager;

public class UIThemeStyles extends UIOutput {

    private String cache;

    private String inline;

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

    @Override
    public void encodeAll(final FacesContext context) throws IOException {
        Map<String, Object> attributes = getAttributes();
        cache = (String) attributes.get("cache");
        inline = (String) attributes.get("inline");

        final ResponseWriter writer = context.getResponseWriter();
        final ExternalContext externalContext = context.getExternalContext();

        final URL themeUrl = (URL) externalContext.getRequestMap().get(
                "org.nuxeo.theme.url");
        Map<String, String> params = new HashMap<String, String>();

        params.put("themeName", ThemeManager.getThemeNameByUrl(themeUrl));
        params.put("path", externalContext.getRequestContextPath());

        writer.write(ThemeStyles.render(params, Boolean.parseBoolean(cache),
                Boolean.parseBoolean(inline)));
    }
}
