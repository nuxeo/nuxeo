/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.webengine.jaxrs.servlet.config;

import java.net.URL;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.osgi.framework.Bundle;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject("resources")
public class ResourcesDescriptor {

    @XNode("@servlet")
    protected String servlet;

    @XNode("@path")
    protected String path;

    private Bundle bundle;

    private String id;

    private ResourceResolver resolver;

    void setBundle(Bundle bundle) {
        this.bundle = bundle;
        if (path.startsWith("file:")) {
            resolver = new FileResourceResolver(path.substring("file:".length()));
        } else {
            resolver = new BundleResourceResolver(bundle, path);
        }
        id = bundle.getSymbolicName()+":"+servlet+":"+path;
    }

    public void setResolver(ResourceResolver resolver) {
        this.resolver = resolver;
    }

    public Bundle getBundle() {
        return bundle;
    }

    public String getPath() {
        return path;
    }

    public String getServlet() {
        return servlet;
    }

    public String getId() {
        return id;
    }

    public URL getResource(String name) {
        return resolver.getResource(name);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ResourcesDescriptor) {
            return id.equals(((ResourcesDescriptor)obj).id);
        }
        return false;
    }

    @Override
    public String toString() {
        return id;
    }
}
