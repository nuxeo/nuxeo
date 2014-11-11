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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.logging.Logger;

import javax.faces.component.UIOutput;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.nuxeo.theme.Manager;
import org.nuxeo.theme.jsf.Utils;
import org.nuxeo.theme.resources.ResourceType;
import org.nuxeo.theme.types.TypeFamily;

public abstract class UIBaseMVC extends UIOutput {

    private static final Logger log = Logger.getLogger("nxthemes.jsf.component");

    private String resource;

    private String url;

    private Object binding;

    public abstract String getClassName();

    @Override
    public void encodeBegin(final FacesContext context) throws IOException {
        final ResponseWriter writer = context.getResponseWriter();

        final Map attributes = getAttributes();
        resource = (String) attributes.get("resource");
        url = (String) attributes.get("url");
        binding = attributes.get("binding");

        writer.startElement("ins", this);
        writer.writeAttribute("class", getClassName(), null);

        /* insert the content from a file source */
        if (null != resource) {
            ResourceType resourceType = (ResourceType) Manager.getTypeRegistry().lookup(
                    TypeFamily.RESOURCE, resource);

            if (resourceType == null) {
                log.warning("Could not find resource: " + resource);
            } else {
                InputStream inStream = null;
                try {
                    inStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(
                            resourceType.getPath());
                    if (inStream != null) {
                        BufferedReader in = new BufferedReader(
                                new InputStreamReader(inStream));

                        String inputLine;
                        while ((inputLine = in.readLine()) != null) {
                            writer.write(inputLine);
                        }
                        in.close();
                    } else {
                        log.warning("Could not open resource file: "
                                + resourceType.getPath());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (inStream != null) {
                        try {
                            inStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        /* get the content from a url */
        if (null != url) {
            writer.writeAttribute("cite", url, null);
        }

        /* get the content from the value binding */
        if (null != binding) {
            final String text = Utils.toJson(binding);
            writer.write(text);
        }
    }

    @Override
    public void encodeEnd(final FacesContext context) throws IOException {
        final ResponseWriter writer = context.getResponseWriter();
        writer.endElement("ins");
    }

    public Object getBinding() {
        return binding;
    }

    public void setBinding(final Object binding) {
        this.binding = binding;
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
