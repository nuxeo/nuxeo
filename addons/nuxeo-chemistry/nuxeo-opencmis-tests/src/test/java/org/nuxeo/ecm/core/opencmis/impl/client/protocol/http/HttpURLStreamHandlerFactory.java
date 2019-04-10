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

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

public class HttpURLStreamHandlerFactory implements URLStreamHandlerFactory {

    HttpURLClientProvider provider = new HttpURLMultiThreadedClientProvider();

    protected HttpURLStreamHandlerFactory(HttpURLClientProvider provider) {
        this.provider = provider;
    }

    @Override
    public URLStreamHandler createURLStreamHandler(String protocol) {
        if ("http".equals(protocol)) {
            return new HttpURLStreamHandler(provider);
        }
        return null;
    }

}
