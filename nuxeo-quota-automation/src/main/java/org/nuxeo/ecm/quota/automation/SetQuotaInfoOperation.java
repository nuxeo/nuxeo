/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
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
    public Long run(DocumentModel doc) {
        QuotaAware qa = QuotaAwareDocumentFactory.make(doc);
        qa.setMaxQuota(targetSize);
        qa.save();
        return qa.getMaxQuota();
    }

    @OperationMethod()
    public Long run(DocumentRef docRef) {
        return run(session.getDocument(docRef));
    }

    @OperationMethod()
    public Long run() {
        return run(documentRef);
    }

}
