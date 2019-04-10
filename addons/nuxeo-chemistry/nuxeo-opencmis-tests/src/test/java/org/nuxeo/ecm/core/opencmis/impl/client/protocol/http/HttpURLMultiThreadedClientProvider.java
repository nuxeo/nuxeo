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

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;

public class HttpURLMultiThreadedClientProvider implements HttpURLClientProvider {

    HttpClient client = new HttpClient(new MultiThreadedHttpConnectionManager());

    @Override
    public HttpClient getClient() {
        return client;
    }

    @Override
    public void setClient(HttpClient client) {
        this.client = client;
    }

}
