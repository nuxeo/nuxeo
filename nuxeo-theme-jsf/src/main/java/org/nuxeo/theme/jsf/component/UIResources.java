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

import javax.faces.component.UIOutput;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.nuxeo.theme.Manager;
import org.nuxeo.theme.resources.ResourceManager;

public class UIResources extends UIOutput {

    @Override
    public void encodeAll(final FacesContext context) throws IOException {
        final ResponseWriter writer = context.getResponseWriter();
        final String resourcePath = "/nuxeo/nxthemes-lib/";

        final URL themeUrl = (URL) context.getExternalContext().getRequestMap().get(
                "nxthemesThemeUrl");

        final String path = context.getExternalContext().getRequestContextPath();
        final ResourceManager resourceManager = Manager.getResourceManager();

        final StringBuilder combinedStyles = new StringBuilder();
        final StringBuilder combinedScripts = new StringBuilder();
        combinedStyles.append(resourcePath);
        combinedScripts.append(resourcePath);

        boolean hasScripts = false;
        boolean hasStyles = false;

        for (String resourceName : resourceManager.getResourcesFor(themeUrl)) {
            if (resourceName.endsWith(".css")) {
                combinedStyles.append(resourceName).append(",");
                hasStyles = true;
            } else if (resourceName.endsWith(".js")) {
                combinedScripts.append(resourceName).append(",");
                hasScripts = true;
            }
        }

        combinedStyles.deleteCharAt(combinedStyles.length() - 1);
        combinedScripts.deleteCharAt(combinedScripts.length() - 1);
        combinedStyles.append("?path=").append(path);
        combinedScripts.append("?path=").append(path);

        // styles
        if (hasStyles) {
            writer.startElement("link", this);
            writer.writeAttribute("type", "text/css", null);
            writer.writeAttribute("rel", "stylesheet", null);
            writer.writeAttribute("media", "all", null);
            writer.writeAttribute("href", combinedStyles.toString(), null);
            writer.endElement("link");
        }

        // scripts
        if (hasScripts) {
            writer.startElement("script", this);
            writer.writeAttribute("type", "text/javascript", null);
            writer.writeAttribute("src", combinedScripts.toString(), null);
            writer.endElement("script");
        }
    }
}
