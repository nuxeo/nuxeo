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
 *
 * $Id$
 */

package org.nuxeo.ecm.core.url.nxobj;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ObjectURLConnection extends URLConnection {

    protected Object obj;

    public ObjectURLConnection(URL url) {
        super(url);
    }

    @Override
    public void connect() throws IOException {
        if (connected) {
            return;
        }
        obj = ObjectURL.getObject(url);
        if (obj == null) {
            throw new FileNotFoundException("Object was not found for url: " + url.toString());
        }
        connected = true;
    }

    @Override
    public long getLastModified() {
        try {
            connect();
            return lastModified();
        } catch (Exception e) {
            return -1L;
        }
    }

    @Override
    public InputStream getInputStream() throws IOException {
        connect();
        return new FilterInputStream(openStream()) {
            @Override
            // FIXME: this method recurses infinitely
            public void close() throws IOException {
                super.close();
                try {
                    close();
                } catch (Exception e) {
                }
            }
        };
    }

    protected long lastModified() throws IOException {
        return -1L;
    }

    protected InputStream openStream() throws IOException {
        if (obj instanceof InputStream) {
            return (InputStream) obj;
        }
        return new ByteArrayInputStream(obj.toString().getBytes());
    }

    protected void close() {
        ObjectURL.removeURL(url);
        obj = null;
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            close();
        } finally {
            super.finalize();
        }
    }

}
