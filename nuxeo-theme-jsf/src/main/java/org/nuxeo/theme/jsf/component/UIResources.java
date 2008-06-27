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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.resources.ResourceManager;
import org.nuxeo.theme.resources.ResourceType;
import org.nuxeo.theme.types.TypeRegistry;
import org.nuxeo.theme.types.TypeFamily;

public class UIResources extends UIOutput {

    private static final Log log = LogFactory.getLog(UIResources.class);

    @Override
    public void encodeAll(final FacesContext context) throws IOException {
        final ResponseWriter writer = context.getResponseWriter();
        final String resourcePath = "/nuxeo/nxthemes-lib/";
        final TypeRegistry typeRegistry = Manager.getTypeRegistry();

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
            ResourceType resource = (ResourceType) typeRegistry.lookup(
                    TypeFamily.RESOURCE, resourceName);
                    if (resource == null) {
                        log.error(String.format("Resource '%s' not registered.",resourceName));
                        continue;
                    }
            String url = resource.getUrl();
            if (resourceName.endsWith(".css")) {
                if (url == null) {
                    combinedStyles.append(resourceName).append(",");
                    hasStyles = true;
                    } else {
                    writer.startElement("link", this);
                    writer.writeAttribute("type", "text/css", null);
                    writer.writeAttribute("rel", "stylesheet", null);
                    writer.writeAttribute("media", "all", null);
                    writer.writeAttribute("href", url, null);
                    writer.endElement("link");
                    }
            } else if (resourceName.endsWith(".js")) {
                if (url == null) {
                    combinedScripts.append(resourceName).append(",");
                    hasScripts = true;
                    } else {
                    writer.startElement("script", this);
                    writer.writeAttribute("type", "text/javascript", null);
                    writer.writeAttribute("src", url, null);
                    writer.endElement("script");
                    }
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
