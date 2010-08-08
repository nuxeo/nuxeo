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

import org.nuxeo.ecm.platform.ui.web.util.BaseURL;
import org.nuxeo.theme.html.Utils;
import org.nuxeo.theme.html.ui.Resources;

public class UIResources extends UIOutput {

    @Override
    public void encodeAll(final FacesContext context) throws IOException {
        final ResponseWriter writer = context.getResponseWriter();
        final ExternalContext externalContext = context.getExternalContext();

        Map<String, String> params = new HashMap<String, String>();

        Map<String, Object> requestMap = externalContext.getRequestMap();
        URL themeUrl = (URL) requestMap.get("org.nuxeo.theme.url");
        final Map<String, Object> attributes = getAttributes();

        String contextPath = BaseURL.getContextPath();
        params.put("contextPath", contextPath);
        params.put("themeUrl", themeUrl.toString());
        params.put("path", contextPath);
        params.put("ignoreLocal", (String) attributes.get("ignoreLocal"));

        String basePath =  contextPath + "/site";
        params.put("basepath", basePath);


        Boolean virtualHosting = Utils.isVirtualHosting((HttpServletRequest) externalContext.getRequest());
        writer.write(Resources.render(params, virtualHosting));
    }
}
