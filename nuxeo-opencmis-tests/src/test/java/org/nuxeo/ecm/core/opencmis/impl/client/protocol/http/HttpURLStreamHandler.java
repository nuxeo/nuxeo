/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Stephane Lacoin (aka matic)
 */
package org.nuxeo.ecm.core.opencmis.impl.client.protocol.http;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

public class HttpURLStreamHandler extends URLStreamHandler {

    final HttpURLClientProvider clientProvider;

    public HttpURLStreamHandler(HttpURLClientProvider provider) {
        this.clientProvider = provider;
    }

    @Override
    protected URLConnection openConnection(URL url) throws IOException {
        URLConnection connection = new HttpURLConnection(clientProvider, url);
        connection.connect();
        return connection;
    }

}
