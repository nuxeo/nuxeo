package org.nuxeo.ecm.platform.ec.notification;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.adapter.DocumentAdapterFactory;

public class SubscriptionAdapterFactory implements DocumentAdapterFactory {

    @Override
    public Object getAdapter(DocumentModel doc, Class<?> itf) {
        if (!doc.hasFacet(SubscriptionAdapter.NOTIFIABLE_FACET)) {
            doc.addFacet(SubscriptionAdapter.NOTIFIABLE_FACET);
        }
        return new SubscriptionAdapter(doc);
    }

}
