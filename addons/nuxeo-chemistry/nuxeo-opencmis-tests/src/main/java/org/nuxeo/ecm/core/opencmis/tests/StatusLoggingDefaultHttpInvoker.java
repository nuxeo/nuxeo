/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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

    public static int lastStatus;

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
        lastStatus = response.getResponseCode();
        return response;
    }

}
