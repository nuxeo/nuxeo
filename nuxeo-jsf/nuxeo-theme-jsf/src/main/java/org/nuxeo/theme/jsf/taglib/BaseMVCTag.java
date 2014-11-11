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

package org.nuxeo.theme.jsf.taglib;

import javax.faces.component.UIComponent;
import javax.faces.webapp.UIComponentELTag;

public abstract class BaseMVCTag extends UIComponentELTag {

    private String resource;

    private String url;

    @Override
    public abstract String getComponentType();

    @Override
    public String getRendererType() {
        // null means the component renders itself
        return null;
    }

    @Override
    protected void setProperties(UIComponent component) {
        super.setProperties(component);

        if (null != url && null != resource) {
            throw new IllegalArgumentException(
                    "Cannot specify both a URL and a resource.");
        }

        if (null != resource) {
            component.getAttributes().put("resource", resource);
        }

        if (null != url) {
            if (!url.startsWith("/")) {
                throw new IllegalArgumentException("The URL must begin with /");
            }
            component.getAttributes().put("url", url);
        }

    }

    @Override
    public void release() {
        super.release();
        resource = null;
        url = null;
    }

    /* property accessors */
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

}
