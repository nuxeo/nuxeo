/*
 * Copyright (c) 2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.theme.jsf;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.faces.context.ExternalContext;

import org.nuxeo.theme.ResourceResolver;

/**
 * Resolver for resources that checks the servlet context (through FacesContext)
 * first.
 *
 * @since 5.5
 */
public class FacesResourceResolver extends ResourceResolver {

    public final ExternalContext externalContext;

    public FacesResourceResolver(ExternalContext externalContext) {
        this.externalContext = externalContext;
    }

    @Override
    public URL getResource(String path) {
        try {
            URL url = externalContext.getResource("/" + path);
            if (url != null) {
                return url;
            }
        } catch (MalformedURLException e) {
            // continue
        }
        return super.getResource(path);
    }

    @Override
    public InputStream getResourceAsStream(String path) {
        InputStream is = externalContext.getResourceAsStream("/" + path);
        if (is != null) {
            return is;
        }
        return super.getResourceAsStream(path);
    }

}
