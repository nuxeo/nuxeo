/*
 * (C) Copyright 2014-2018 Nuxeo (http://nuxeo.com/) and others.
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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

@Operation(id = DummyDocToTxt.ID, category = Constants.CAT_CONVERSION, label = "Convert Doc To Txt", description = "very dummy just for tests !")
public class DummyDocToTxt {

    public static final String ID = "DummyDoc.ToTxt";

    private static final Log log = LogFactory.getLog(DummyDocToTxt.class);

    @Context
    protected CoreSession session;

    @OperationMethod
    public Blob run(DocumentModel doc) throws Exception {
        DocumentRef docRef = doc.getRef();
        String description = "";
        Calendar issued = null;
        Boolean delayed = null;
        try {
            description = (String) doc.getPropertyValue("dc:description");
            issued = (Calendar) doc.getPropertyValue("dc:issued");
            delayed = (Boolean) doc.getContextData("delayed");
        } catch (PropertyException ignored) {
        }
        String content = getContent(doc.getTitle(), description, issued);
        if (description != null && description.startsWith(TestRenditionService.CYCLIC_BARRIER_DESCRIPTION)) {
            for (int i = 0; i < 3; i++) {
                if (log.isDebugEnabled()) {
                    log.debug(formatLogEntry(docRef, content, description, issued) + " before barrier " + i);
                }
                TestRenditionService.CYCLIC_BARRIERS[i].await();
            }
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
                description = (String) doc.getPropertyValue("dc:description");
                if (StringUtils.isNotBlank(description)) {
                    content += String.format("%n" + description);
                }
            }
        }

        Blob blob = Blobs.createBlob(content);
        blob.setDigest(DigestUtils.md5Hex(content));
        return blob;
    }

    public static String formatLogEntry(DocumentRef docRef, String content, String desc, Calendar issued) {
        return String.format("Doc with id '%s', content '%s', description '%s', issued '%s'", docRef,
                StringUtils.defaultString(content), StringUtils.defaultString(desc),
                issued == null ? "" : new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(issued.getTime()));
    }

    /**
     * @since 9.3
     */
    public static String getDigest(DocumentModel doc) {
        String description = (String) doc.getPropertyValue("dc:description");
        Calendar issued = (Calendar) doc.getPropertyValue("dc:issued");
        String content = getContent(doc.getTitle(), description, issued);
        return DigestUtils.md5Hex(content);
    }

    protected static String getContent(String title, String description, Calendar issued) {
        StringBuilder sb = new StringBuilder(title);
        if (StringUtils.isNotBlank(description)) {
            sb.append(String.format("%n" + description));
        }
        if (issued != null) {
            sb.append(String.format("%n" + new Date(issued.getTimeInMillis())));
        }
        return sb.toString();
    }

    protected void nextTransaction() {
        if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
    }

}
