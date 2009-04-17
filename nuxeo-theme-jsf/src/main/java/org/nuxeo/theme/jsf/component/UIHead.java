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
import javax.servlet.ServletRequest;

import org.nuxeo.theme.html.ui.Head;
import org.nuxeo.theme.jsf.URLUtils;
import org.nuxeo.theme.themes.ThemeManager;

public class UIHead extends UIOutput {

    @Override
    public void encodeAll(final FacesContext context) throws IOException {
        final ResponseWriter writer = context.getResponseWriter();
        final ExternalContext externalContext = context.getExternalContext();

        final URL themeUrl = (URL) externalContext.getRequestMap().get(
                "org.nuxeo.theme.url");
        Map<String, String> params = new HashMap<String, String>();

        params.put("themeName", ThemeManager.getThemeNameByUrl(themeUrl));
        params.put("path", externalContext.getRequestContextPath());
        String baseUrl = URLUtils.getBaseURL((ServletRequest) externalContext.getRequest());
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
        params.put("baseUrl", baseUrl);

        writer.write(Head.render(params));
    }
}
