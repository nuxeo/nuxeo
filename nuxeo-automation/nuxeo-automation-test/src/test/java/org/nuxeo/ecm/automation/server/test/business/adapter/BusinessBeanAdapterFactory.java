package org.nuxeo.ecm.automation.server.test.business.adapter;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.adapter.DocumentAdapterFactory;

public class BusinessBeanAdapterFactory implements DocumentAdapterFactory {

    @Override
    public Object getAdapter(DocumentModel doc, Class<?> itf) {
        if ("File".equals(doc.getType())) {
            return new BusinessBeanAdapter(doc);
        } else {
            return null;
        }
    }
}
