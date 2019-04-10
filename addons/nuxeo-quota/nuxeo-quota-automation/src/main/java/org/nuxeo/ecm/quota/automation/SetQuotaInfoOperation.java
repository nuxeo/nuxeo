package org.nuxeo.ecm.quota.automation;

import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.quota.size.QuotaAware;
import org.nuxeo.ecm.quota.size.QuotaAwareDocumentFactory;

@Operation(id = SetQuotaInfoOperation.ID, category = "Quotas", label = "Set max Quota size for the target DocumentModel", description = "Set the maximum size of the target DocumentModel, use -1 to make Quota checks innative")
public class SetQuotaInfoOperation {

    public static final String ID = "Quotas.SetMaxSize";

    @Context
    protected CoreSession session;

    @Param(name = "targetSize", required = true)
    protected Long targetSize;

    @Param(name = "documentRef", required = false)
    protected DocumentRef documentRef;

    @OperationMethod()
    public Long run(DocumentModel doc) throws Exception {
        QuotaAware qa = doc.getAdapter(QuotaAware.class);
        if (qa == null) {
            qa = QuotaAwareDocumentFactory.make(doc, false);
        }
        qa.setMaxQuota(targetSize, true);
        return qa.getMaxQuota();
    }

    @OperationMethod()
    public Long run(DocumentRef docRef) throws Exception {
        return run(session.getDocument(docRef));
    }

    @OperationMethod()
    public Long run() throws Exception {
        return run(documentRef);
    }

}
