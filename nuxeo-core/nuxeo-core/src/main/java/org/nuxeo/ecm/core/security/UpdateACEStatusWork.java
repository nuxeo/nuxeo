/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.core.security;

import static org.nuxeo.ecm.core.api.event.CoreEventConstants.DOCUMENT_REFS;
import static org.nuxeo.ecm.core.api.event.CoreEventConstants.REPOSITORY_NAME;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.ACE_STATUS_UPDATED;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionRuntimeException;

/**
 * Work updating ACE status.
 *
 * @since 7.4
 */
public class UpdateACEStatusWork extends AbstractWork {

    public static final int DEFAULT_BATCH_SIZE = 20;

    public static final String ID = "updateACEStatus";

    public static final String CATEGORY = "updateACEStatus";

    public static final String QUERY = "SELECT ecm:uuid FROM Document WHERE (ecm:acl/*1/status = 0 AND ecm:acl/*1/begin <= TIMESTAMP '%s') OR (ecm:acl/*1/status = 1 AND ecm:acl/*1/end <= TIMESTAMP '%s')";

    public static final SimpleDateFormat FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    protected int batchSize = DEFAULT_BATCH_SIZE;

    public UpdateACEStatusWork() {
        super(ID);
    }

    @Override
    public void work() {
        setStatus("Updating ACE status");
        openSystemSession();

        Date now = new Date();
        String formattedDate = FORMATTER.format(now);

        IterableQueryResult result = session.queryAndFetch(String.format(QUERY, formattedDate, formattedDate),
                NXQL.NXQL);
        List<String> docIds = new ArrayList<>();
        try {
            for (Map<String, Serializable> map : result) {
                docIds.add((String) map.get("ecm:uuid"));
            }
        } finally {
            result.close();
        }

        int acpUpdatedCount = 0;
        List<DocumentRef> processedDocIds = new ArrayList<>();
        for (String docId : docIds) {
            try {
                DocumentRef ref = new IdRef(docId);
                ACP acp = session.getACP(ref);
                session.setACP(ref, acp, true);
                acpUpdatedCount++;
                processedDocIds.add(ref);
                if (acpUpdatedCount % batchSize == 0) {
                    fireACEStatusUpdatedEvent(processedDocIds);
                    commitOrRollbackTransaction();
                    startTransaction();
                    processedDocIds.clear();
                }
            } catch (TransactionRuntimeException e) {
                if (e.getMessage().contains("Transaction timeout")) {
                    batchSize = 1;
                }
                throw e;
            }
        }
        fireACEStatusUpdatedEvent(processedDocIds);

        setStatus(null);
    }

    protected void fireACEStatusUpdatedEvent(List<DocumentRef> docRefs) {
        EventContext eventContext = new EventContextImpl(session, session.getPrincipal());
        eventContext.setProperty(DOCUMENT_REFS, (Serializable) docRefs);
        eventContext.setProperty(REPOSITORY_NAME, session.getRepositoryName());
        Framework.getService(EventService.class).fireEvent(ACE_STATUS_UPDATED, eventContext);
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public String getTitle() {
        return "Updating ACE status";
    }

    @Override
    public int getRetryCount() {
        return 10;
    }
}
