package org.nuxeo.ecm.platform.ui.web.auth.simple;

import java.util.HashMap;
import java.util.Map;

import org.jboss.seam.mock.MockHttpServletResponse;

public class MockHttpResponse extends MockHttpServletResponse {

    protected final Map<String, String> headers = new HashMap<String, String>();

    @Override
    public void setHeader(String key, String value) {
        if (value == null) {
            headers.remove(value);
        } else {
            headers.put(key, value);
        }
    }

}
