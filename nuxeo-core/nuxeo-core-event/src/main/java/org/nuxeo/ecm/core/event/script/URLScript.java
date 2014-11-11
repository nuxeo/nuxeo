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
import java.net.URLConnection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class URLScript extends Script {

    private static final Log log = LogFactory.getLog(URLScript.class);

    protected final URL url;

    protected URLConnection conn;

    public URLScript(URL url) throws IOException {
        this.url = url;
        conn = url.openConnection();
    }

    public URLScript(String location) throws IOException {
        this(new URL(location));
    }

    public URLScript(Bundle bundle, String path) throws IOException {
        this(bundle.getEntry(path));
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
    public Reader getReaderIfModified() throws IOException {
        URLConnection conn = url.openConnection();
        long tm = conn.getLastModified();
        if (tm > lastModified) {
            synchronized (this) {
                if (tm > lastModified) {
                    lastModified = tm;
                    return new InputStreamReader(conn.getInputStream());
                }
            }
        }
        return null;
    }

    @Override
    public Reader getReader() throws IOException {
        try {
            return new InputStreamReader(conn.getInputStream());
        } catch (Exception e) {
            log.error(e, e);
        }
        return null;
    }

}
