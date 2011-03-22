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

package org.nuxeo.ecm.core.url.nxdoc;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class Handler extends URLStreamHandler {

    private static Handler instance;

    public static Handler getInstance() {
        if (instance == null) {
            instance = new Handler();
        }
        return instance;
    }

    @Override
    protected URLConnection openConnection(URL u) throws IOException {
        String protocol = u.getProtocol();
        if ("nxdoc".equals(protocol)) {
            return new PropertyURLConnection(u);
        } else if ("nxobj".equals(protocol)) {
            return new LocalPropertyURLConnection(u);
        } else {
            throw new IOException("Unsupported URL protocol");
        }
    }

}
