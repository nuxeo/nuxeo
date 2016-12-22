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
 *     bstefanescu
 */
package org.nuxeo.ecm.webengine.app.jersey;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;

import org.nuxeo.ecm.webengine.jaxrs.Activator;
import org.nuxeo.ecm.webengine.jaxrs.servlet.ApplicationServlet;

/**
 * WebEngine integration with OSGi JAX-RS model from ECR.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class WebEngineServlet extends ApplicationServlet {

    protected final String MIME_TYPE = "X-File-Type";

    private final class DefaultContentTypeRequestWrapper extends HttpServletRequestWrapper {

        protected final Hashtable<String, String[]> headers;

        protected final String lCONTENT_TYPE = HttpHeaders.CONTENT_TYPE.toLowerCase();

        protected final String lFILE_TYPE = MIME_TYPE.toLowerCase();

        protected DefaultContentTypeRequestWrapper(HttpServletRequest request, boolean patchCType, boolean patchMType) {
            super(request);
            headers = patchHeaders(request, patchCType, patchMType);
        }

        protected Hashtable<String, String[]> patchHeaders(HttpServletRequest request, boolean patchCType,
                boolean patchMType) {
            Hashtable<String, String[]> headers = new Hashtable<String, String[]>();
            // collect headers from request
            Enumeration<String> eachNames = request.getHeaderNames();
            while (eachNames.hasMoreElements()) {
                String name = eachNames.nextElement().toLowerCase();
                List<String> values = new LinkedList<String>();
                Enumeration<String> eachValues = request.getHeaders(name);
                while (eachValues.hasMoreElements()) {
                    values.add(eachValues.nextElement());
                }
                headers.put(name, values.toArray(new String[values.size()]));
            }
            if (patchCType) {
                // patch content type
                String ctype = request.getContentType();
                if (ctype == null || ctype.isEmpty()) {
                    String[] ctypes = new String[] { "application/octet-stream" };
                    headers.put(lCONTENT_TYPE, ctypes);
                } else {
                    patchContentTypes(headers.get(lCONTENT_TYPE));
                }
            }
            if (patchMType) {
                patchContentTypes(headers.get(lFILE_TYPE));
            }
            return headers;
        }

        protected void patchContentTypes(String[] ctypes) {
            for (int index = 0; index < ctypes.length; ++index) {
                String value = ctypes[index];
                if (value.isEmpty()) {
                    ctypes[index] = "application/octet-stream";
                } else if (!value.contains("/")) {
                    ctypes[index] = "application/".concat(value);
                }
            }
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            return headers.keys();
        }

        @Override
        public String getHeader(String name) {
            String lname = name.toLowerCase();
            if (!headers.containsKey(lname)) {
                return null;
            }
            return headers.get(lname)[0];
        }

        @Override
        public Enumeration<String> getHeaders(final String name) {
            final String lname = name.toLowerCase();
            if (!headers.containsKey(lname)) {
                return Collections.emptyEnumeration();
            }
            return new Enumeration<String>() {
                String[] values = headers.get(lname);

                int index = 0;

                @Override
                public boolean hasMoreElements() {
                    return index < values.length;
                }

                @Override
                public String nextElement() {
                    if (index >= values.length) {
                        throw new NoSuchElementException(index + " is higher than " + values.length);
                    }
                    return values[index++];
                }
            };
        }

    }

    private static final long serialVersionUID = 1L;

    @Override
    public void init(ServletConfig config) throws ServletException {
        bundle = Activator.getInstance().getContext().getBundle();
        super.init(config);
    }

    @Override
    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        containerService(request, response);
    }

    @Override
    protected void containerService(HttpServletRequest request, HttpServletResponse response) throws ServletException,
            IOException {
        if (isDirty) {
            reloadContainer();
        }
        String method = request.getMethod().toUpperCase();
        if (!"GET".equals(method)) {
            // force reading properties because jersey is consuming one
            // character
            // from the input stream - see WebComponent.isEntityPresent.
            request.getParameterMap();
        }
        final String ctype = request.getHeader(HttpHeaders.CONTENT_TYPE);
        final String mtype = request.getHeader(MIME_TYPE);
        boolean patchCType = ctype == null || ctype.length() == 0 || !ctype.contains("/");
        boolean patchMMType = mtype != null && !mtype.contains("/");
        if (patchCType || patchMMType) {
            request = new DefaultContentTypeRequestWrapper(request, patchCType, patchMMType);
        }
        container.service(request, response);
    }
}
