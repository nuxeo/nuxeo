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
