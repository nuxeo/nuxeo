/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Stephane Lacoin (aka matic)
 */
package org.nuxeo.ecm.core.opencmis.impl.client.protocol.http;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.nuxeo.common.utils.URLStreamHandlerFactoryInstaller;

public class HttpURLInstaller {

    private HttpURLInstaller() {
        ;
    }

    public static final HttpURLInstaller INSTANCE = new HttpURLInstaller();

    protected final HttpURLClientProvider clientProvider = new HttpURLMultiThreadedClientProvider();

    public HttpClient getClient() {
        return clientProvider.getClient();
    }

    public void setClient(HttpClient client) {
        clientProvider.setClient(client);
    }

    protected HttpURLStreamHandlerFactory shf;

    public void setCredentials(String host, int port, String username, String password) {
        Credentials defaultcreds = new UsernamePasswordCredentials(username, password);
        HttpClient client = clientProvider.getClient();
        client.getState().setCredentials(new AuthScope(host, port, AuthScope.ANY_REALM), defaultcreds);
        client.getParams().setAuthenticationPreemptive(true);
    }

    public void installSelf()  {
        shf = new HttpURLStreamHandlerFactory(clientProvider);
        try {
            URLStreamHandlerFactoryInstaller.installURLStreamHandlerFactory(shf);
        } catch (Exception e) {
            throw new Error("Cannot install http client as default url invoker backend");
        }
    }

    public static void install() {
        INSTANCE.installSelf();
    }

    public static void uninstall() {
        throw new UnsupportedOperationException("no API available");
    }

}
