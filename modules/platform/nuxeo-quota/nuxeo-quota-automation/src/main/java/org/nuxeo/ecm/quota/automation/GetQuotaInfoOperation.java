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
import org.nuxeo.ecm.automation.jaxrs.JsonAdapter;
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
    public JsonAdapter run(DocumentModel doc) {
        QuotaAware qa = doc.getAdapter(QuotaAware.class);
        if (qa == null) {
            return new TestableJsonAdapter(new SimpleQuotaInfo());
        } else {
            return new TestableJsonAdapter(new SimpleQuotaInfo(qa.getQuotaInfo()));
        }
    }

    @OperationMethod()
    public JsonAdapter run(DocumentRef docRef) {
        return run(session.getDocument(docRef));
    }

    @OperationMethod()
    public JsonAdapter run() {
        return run(documentRef);
    }

}
