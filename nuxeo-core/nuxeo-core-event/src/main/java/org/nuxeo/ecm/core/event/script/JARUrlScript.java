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
package org.nuxeo.ecm.core.event.script;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class JARUrlScript extends Script {

    protected final URL url;
    protected final URL jar;

    public JARUrlScript(URL jar, URL url) {
        this.url = url;
        this.jar = jar;
    }

    @Override
    public String getExtension() {
        return getExtension(url.getPath());
    }

    @Override
    public String getLocation() {
        return url.toExternalForm();
    }

    @Override
    public Reader getReader() throws IOException {
        return new InputStreamReader(url.openStream());
    }

    @Override
    public Reader getReaderIfModified() throws IOException {
        long tm = jar.openConnection().getLastModified();
        if (tm > lastModified) {
            synchronized (this) {
                if (tm > lastModified) {
                    lastModified = tm;
                    return new InputStreamReader(url.openStream());
                }
            }
        }
        return null;
    }

}
