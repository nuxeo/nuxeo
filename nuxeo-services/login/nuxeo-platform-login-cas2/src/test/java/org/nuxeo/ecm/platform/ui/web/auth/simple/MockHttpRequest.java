/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.ui.web.auth.simple;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.jboss.seam.mock.MockHttpServletRequest;
import org.jboss.seam.util.IteratorEnumeration;

public class MockHttpRequest extends MockHttpServletRequest {

    protected final Map<String, String[]> headers = new HashMap<String, String[]>();

    public MockHttpRequest(HttpSession session) {
        super(session);
    }

    protected void setHeaderParam(String key, String[] value) {
        if (value == null) {
            headers.remove(value);
        } else {
            headers.put(key, value);
        }
    }

    @SuppressWarnings("unchecked")
    public void setParameter(String key, String[] value) {
        if (value == null) {
            getParameterMap().remove(key);
        } else {
            getParameterMap().put(key, value);
        }
    }

    @Override
    public String getHeader(String header) {
        String[] values = headers.get(header);
        return values == null || values.length == 0 ? null : values[0];
    }

    @Override
    public long getDateHeader(String arg0) {
        return super.getDateHeader(arg0);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Enumeration getHeaderNames() {
        return new IteratorEnumeration(headers.keySet().iterator());
    }

    @Override
    public int getIntHeader(String header) {
        return super.getIntHeader(header);
    }

    @Override
    public Map<String, String[]> getHeaders() {
        return headers;
    }

}
