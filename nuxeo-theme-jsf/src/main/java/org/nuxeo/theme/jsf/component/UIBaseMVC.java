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
import java.util.HashMap;
import java.util.Map;

import javax.faces.component.UIOutput;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.nuxeo.theme.html.ui.MVCElement;

public abstract class UIBaseMVC extends UIOutput {

    private String resource;

    private String url;

    public abstract String getClassName();

    @Override
    public void encodeBegin(final FacesContext context) throws IOException {
        final ResponseWriter writer = context.getResponseWriter();

        final Map<String, Object> attributes = getAttributes();
        final Map<String, String> params = new HashMap<String, String>();
        params.put("className", getClassName());
        params.put("resource", (String) attributes.get("resource"));
        params.put("url", (String) attributes.get("url"));
        writer.write(MVCElement.render(params));
    }

    public String getResource() {
        return resource;
    }

    public void setResource(final String resource) {
        this.resource = resource;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

}
