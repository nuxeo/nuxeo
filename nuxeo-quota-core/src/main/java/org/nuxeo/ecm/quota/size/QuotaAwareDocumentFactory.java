package org.nuxeo.ecm.quota.size;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.adapter.DocumentAdapterFactory;

public class QuotaAwareDocumentFactory implements DocumentAdapterFactory {

    public static QuotaAwareDocument make(DocumentModel doc, boolean save)
            throws ClientException {
        if (!doc.hasFacet(QuotaAwareDocument.DOCUMENTS_SIZE_STATISTICS_FACET)) {
            doc.addFacet(QuotaAwareDocument.DOCUMENTS_SIZE_STATISTICS_FACET);
            if (save) {
                doc = doc.getCoreSession().saveDocument(doc);
            }
        }
        return (QuotaAwareDocument) doc.getAdapter(QuotaAware.class);
    }

    @Override
    public Object getAdapter(DocumentModel doc, Class<?> adapter) {
        if (doc.hasFacet(QuotaAwareDocument.DOCUMENTS_SIZE_STATISTICS_FACET)) {
            return adapter.cast(new QuotaAwareDocument(doc));
        }
        return null;
    }
}
