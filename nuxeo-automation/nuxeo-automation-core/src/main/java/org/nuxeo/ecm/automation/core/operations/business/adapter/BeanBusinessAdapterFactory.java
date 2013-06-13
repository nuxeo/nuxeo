package org.nuxeo.ecm.automation.core.operations.business.adapter;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.adapter.DocumentAdapterFactory;

/**
 * @since 5.7
 */
public class BeanBusinessAdapterFactory implements DocumentAdapterFactory {

    @Override
    public Object getAdapter(DocumentModel doc, Class<?> itf) {
        if ("File".equals(doc.getType())) {
            return new BeanBusinessAdapter(doc);
        } else {
            return null;
        }
    }
}
