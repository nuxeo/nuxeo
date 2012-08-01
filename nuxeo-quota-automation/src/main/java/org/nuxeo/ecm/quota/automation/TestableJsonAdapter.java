package org.nuxeo.ecm.quota.automation;

import org.nuxeo.ecm.automation.server.jaxrs.DefaultJsonAdapter;

public class TestableJsonAdapter extends DefaultJsonAdapter {

    public TestableJsonAdapter(Object object) {
        super(object);
    }

    public Object getObject() {
        return object;
    }

}
