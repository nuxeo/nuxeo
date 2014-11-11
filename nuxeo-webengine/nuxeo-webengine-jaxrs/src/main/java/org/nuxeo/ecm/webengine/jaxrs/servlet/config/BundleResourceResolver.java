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

import org.osgi.framework.Bundle;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class BundleResourceResolver implements ResourceResolver {

    protected Bundle bundle;
    protected String prefix;

    public BundleResourceResolver(Bundle bundle, String prefix) {
        this.bundle = bundle;
        this.prefix = prefix.endsWith("/") ? prefix.substring(0, prefix.length()-1) : prefix;
    }

    @Override
    public URL getResource(String name) {
        return bundle.getEntry(name.startsWith("/") ? prefix+name : prefix+'/'+name);
    }

}
