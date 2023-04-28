/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.ecm.platform.auth.saml.mock;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.nuxeo.ecm.core.io.DummyServletOutputStream;

/**
 * @since 2023.0
 */
public class MockHttpServletResponse {

    protected final HttpServletResponse mock;

    protected ByteArrayOutputStream responseOutputStream;

    protected List<Cookie> cookies;

    protected MockHttpServletResponse(HttpServletResponse request) {
        mock = request;
    }

    public static MockHttpServletResponse init() {
        var response = Mockito.mock(HttpServletResponse.class, RETURNS_DEEP_STUBS);
        return new MockHttpServletResponse(response);
    }

    public MockHttpServletResponse withOutputStream() {
        try {
            responseOutputStream = new ByteArrayOutputStream();
            when(mock.getOutputStream()).thenReturn(new DummyServletOutputStream(responseOutputStream));
            return this;
        } catch (IOException e) {
            throw new AssertionError("Unexpected error", e);
        }
    }

    public HttpServletResponse mock() {
        return mock;
    }

    public Cookie getCookie(String name) {
        if (cookies == null) {
            ArgumentCaptor<Cookie> captor = ArgumentCaptor.forClass(Cookie.class);
            verify(mock).addCookie(captor.capture());
            cookies = captor.getAllValues();
        }
        return cookies.stream().filter(c -> name.equals(c.getName())).findFirst().orElse(null);
    }

    public String getRedirect() {
        try {
            var argCaptor = ArgumentCaptor.forClass(String.class);
            verify(mock).sendRedirect(argCaptor.capture());
            return argCaptor.getValue();
        } catch (IOException e) {
            throw new AssertionError("Unexpected error", e);
        }
    }

    public String getResponseString() {
        if (responseOutputStream == null) {
            throw new AssertionError("The response writer wasn't initialized, consider using withOutputStream.");
        }
        return responseOutputStream.toString();
    }
}
