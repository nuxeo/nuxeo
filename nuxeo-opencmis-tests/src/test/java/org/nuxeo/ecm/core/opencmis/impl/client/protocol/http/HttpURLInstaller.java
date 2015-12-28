/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

    private HttpURLInstaller() {;
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

    public void installSelf() {
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
