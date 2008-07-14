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
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.nuxeo.theme.html.ui.Resources;

public class UIResources extends UIOutput {

    @Override
    public void encodeAll(final FacesContext context) throws IOException {
        final ResponseWriter writer = context.getResponseWriter();

        Map<String, String> params = new HashMap<String, String>();
        URL themeUrl = (URL) context.getExternalContext().getRequestMap().get(
                "org.nuxeo.theme.url");
        params.put("themeUrl", themeUrl.toString());
        params.put("path", context.getExternalContext().getRequestContextPath());

        writer.write(Resources.render(params));
    }
}
