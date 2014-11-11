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
import java.util.Map;

import javax.faces.component.UIOutput;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.nuxeo.theme.Manager;

public class UIRequire extends UIOutput {

    private String resource;

    @Override
    public void encodeAll(final FacesContext context) throws IOException {
        final ExternalContext externalContext = context.getExternalContext();
        Map<String, Object> requestMap = externalContext.getRequestMap();
        URL themeUrl = (URL) requestMap.get("org.nuxeo.theme.url");

        Map<String, Object> attributes = getAttributes();
        String resourceName = (String) attributes.get("resource");

        // Register as a local resource
        final boolean local = true;
        Manager.getResourceManager().addResource(resourceName, themeUrl, local);
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

}
