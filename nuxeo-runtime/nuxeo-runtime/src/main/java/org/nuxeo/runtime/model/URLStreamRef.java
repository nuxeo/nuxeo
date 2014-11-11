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
package org.nuxeo.runtime.model;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class URLStreamRef implements StreamRef {

    protected final URL url;

    protected final String name;

    public URLStreamRef(URL url) {
        this(url, url.toString());
    }

    public URLStreamRef(URL url, String name) {
        this.url = url;
        this.name = name;
    }

    @Override
    public String getId() {
        return url.toString();
    }

    @Override
    public InputStream getStream() throws IOException {
        return url.openStream();
    }

    @Override
    public URL asURL() {
        return url;
    }

}
