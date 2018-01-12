/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.tests;

import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.Map;

import org.apache.chemistry.opencmis.client.bindings.spi.BindingSession;
import org.apache.chemistry.opencmis.client.bindings.spi.http.DefaultHttpInvoker;
import org.apache.chemistry.opencmis.client.bindings.spi.http.HttpInvoker;
import org.apache.chemistry.opencmis.client.bindings.spi.http.Output;
import org.apache.chemistry.opencmis.client.bindings.spi.http.Response;
import org.apache.chemistry.opencmis.commons.impl.UrlBuilder;

/**
 * HTTP Invoker that notes the last status returned.
 *
 * @since 7.1
 */
public class StatusLoggingDefaultHttpInvoker implements HttpInvoker {

    protected final DefaultHttpInvoker invoker;

    protected Method invokeMethod;

    public StatusLoggingDefaultHttpInvoker() {
        // we delegate instead of subclassing because the method we're
        // interested in overriding (invoke) is private...
        invoker = new DefaultHttpInvoker();
        for (Method m : invoker.getClass().getDeclaredMethods()) {
            if (m.getName().equals("invoke")) {
                invokeMethod = m;
                invokeMethod.setAccessible(true);
                break;
            }
        }
    }

    public Response invokeGET(UrlBuilder url, BindingSession session) {
        return invoke(url, "GET", null, null, null, session, null, null);
    }

    public Response invokeGET(UrlBuilder url, BindingSession session, BigInteger offset, BigInteger length) {
        return invoke(url, "GET", null, null, null, session, offset, length);
    }

    public Response invokePOST(UrlBuilder url, String contentType, Output writer, BindingSession session) {
        return invoke(url, "POST", contentType, null, writer, session, null, null);
    }

    public Response invokePUT(UrlBuilder url, String contentType, Map<String, String> headers, Output writer,
            BindingSession session) {
        return invoke(url, "PUT", contentType, headers, writer, session, null, null);
    }

    public Response invokeDELETE(UrlBuilder url, BindingSession session) {
        return invoke(url, "DELETE", null, null, null, session, null, null);
    }

    protected Response invoke(UrlBuilder url, String method, String contentType, Map<String, String> headers,
            Output writer, BindingSession session, BigInteger offset, BigInteger length) {
        Response response;
        try {
            response = (Response) invokeMethod.invoke(invoker, url, method, contentType, headers, writer, session,
                    offset, length);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        return response;
    }

}
