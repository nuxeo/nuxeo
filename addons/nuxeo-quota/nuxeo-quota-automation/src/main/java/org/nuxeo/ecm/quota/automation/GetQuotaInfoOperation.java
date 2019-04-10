package org.nuxeo.ecm.quota.automation;

import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.server.jaxrs.JsonAdapter;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.quota.size.QuotaAware;

@Operation(id = GetQuotaInfoOperation.ID, category = "Quotas", label = "Get Quota info", description = "Returns the Quota Infos (innerSize, totalSize and maxQuota) for a DocumentModel")
public class GetQuotaInfoOperation {

    public static final String ID = "Quotas.GetInfo";

    @Context
    protected CoreSession session;

    @Param(name = "documentRef", required = false)
    protected DocumentRef documentRef;

    @OperationMethod()
    public JsonAdapter run(DocumentModel doc) throws Exception {
        QuotaAware qa = doc.getAdapter(QuotaAware.class);
        if (qa == null) {
            return new TestableJsonAdapter(new SimpleQuotaInfo());
        } else {
            return new TestableJsonAdapter(new SimpleQuotaInfo(
                    qa.getQuotaInfo()));
        }
    }

    @OperationMethod()
    public JsonAdapter run(DocumentRef docRef) throws Exception {
        return run(session.getDocument(docRef));
    }

    @OperationMethod()
    public JsonAdapter run() throws Exception {
        return run(documentRef);
    }

}
