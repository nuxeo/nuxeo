/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.rendition.service;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.runtime.transaction.TransactionHelper;

import java.util.Calendar;

@Operation(id = DummyDocToTxt.ID, category = Constants.CAT_CONVERSION, label = "Convert Doc To Txt", description = "very dummy just for tests !")
public class DummyDocToTxt {

    public static final String ID = "DummyDoc.ToTxt";

    @Context
    protected CoreSession session;

    @OperationMethod
    public Blob run(DocumentModel doc) throws Exception {
        DocumentRef docRef = doc.getRef();
        String content = doc.getTitle();
        String desc = "";
        Calendar issued = null;
        Boolean delayed = null;
        try {
            desc = (String) doc.getPropertyValue("dc:description");
            issued = (Calendar) doc.getPropertyValue("dc:issued");
            delayed = (Boolean) doc.getContextData("delayed");
        } catch (PropertyException ignored) {}
        if (StringUtils.isNotBlank(desc)) {
            content += String.format("%n" + desc);
        }
        if (TestRenditionService.LATCH_ISSUED_DATE.equals(issued)) {
            TestRenditionService.countDownLatch.await();
        }
        if (delayed != null) {
            // Sync #1
            TestRenditionService.RenditionThread.cyclicBarrier.await();

            // Sync #2
            TestRenditionService.RenditionThread.cyclicBarrier.await();
            nextTransaction();

            if (Boolean.TRUE.equals(delayed)) {

                // Delayed Sync #3
                TestRenditionService.RenditionThread.cyclicBarrier.await();
                nextTransaction();
            } else {

                doc = session.getDocument(docRef);
                desc = (String) doc.getPropertyValue("dc:description");
                if (StringUtils.isNotBlank(desc)) {
                    content += String.format("%n" + desc);
                }
            }
        }

        return Blobs.createBlob(content);
    }

    protected void nextTransaction() {
        if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
    }

}
