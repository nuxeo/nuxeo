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

import static org.nuxeo.ecm.core.api.event.CoreEventConstants.CHANGED_ACL_NAME;
import static org.nuxeo.ecm.core.api.event.CoreEventConstants.DOCUMENT_REFS;
import static org.nuxeo.ecm.core.api.event.CoreEventConstants.REPOSITORY_NAME;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.ACE_STATUS_UPDATED;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.time.FastDateFormat;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.security.ACE;
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

    public static final String CATEGORY = "updateACEStatus";

    public static final String QUERY = "SELECT ecm:uuid, ecm:acl/*1/principal, ecm:acl/*1/permission,"
            + " ecm:acl/*1/grant, ecm:acl/*1/creator, ecm:acl/*1/begin, ecm:acl/*1/end, ecm:acl/*1/name FROM Document"
            + " WHERE (ecm:acl/*1/status = 0 AND ecm:acl/*1/begin <= TIMESTAMP '%s')"
            + " OR (ecm:acl/*1/status = 1 AND ecm:acl/*1/end <= TIMESTAMP '%s')";

    public static final FastDateFormat FORMATTER = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss");

    protected int batchSize = DEFAULT_BATCH_SIZE;

    @Override
    public void work() {
        setStatus("Updating ACE status");
        openSystemSession();

        Date now = new Date();
        String formattedDate = FORMATTER.format(now);

        IterableQueryResult result = session.queryAndFetch(String.format(QUERY, formattedDate, formattedDate),
                NXQL.NXQL);
        Map<String, List<ACE>> docIdsToACEs = new HashMap<>();
        try {
            for (Map<String, Serializable> map : result) {
                String docId = (String) map.get("ecm:uuid");
                List<ACE> aces = docIdsToACEs.get(docId);
                if (aces == null) {
                    aces = new ArrayList<>();
                    docIdsToACEs.put(docId, aces);
                }

                String username = (String) map.get("ecm:acl/*1/principal");
                String permission = (String) map.get("ecm:acl/*1/permission");
                Boolean grant = (Boolean) map.get("ecm:acl/*1/grant");
                String creator = (String) map.get("ecm:acl/*1/creator");
                Calendar begin = (Calendar) map.get("ecm:acl/*1/begin");
                Calendar end = (Calendar) map.get("ecm:acl/*1/end");
                String aclName = (String) map.get("ecm:acl/*1/name");
                Map<String, Serializable> contextData = new HashMap<>();
                contextData.put(CHANGED_ACL_NAME, aclName);
                ACE ace = ACE.builder(username, permission)
                        .isGranted(grant)
                        .creator(creator)
                        .begin(begin)
                        .end(end)
                        .contextData(contextData)
                        .build();
                aces.add(ace);
            }
        } finally {
            result.close();
        }

        int acpUpdatedCount = 0;
        Map<DocumentRef, List<ACE>> processedRefToACEs = new HashMap<>();
        for (Map.Entry<String, List<ACE>> entry : docIdsToACEs.entrySet()) {
            try {
                DocumentRef ref = new IdRef(entry.getKey());
                ACP acp = session.getACP(ref);
                // re-set the ACP to actually write the new status
                session.setACP(ref, acp, true);
                acpUpdatedCount++;
                processedRefToACEs.put(ref, entry.getValue());
                if (acpUpdatedCount % batchSize == 0) {
                    fireACEStatusUpdatedEvent(processedRefToACEs);
                    commitOrRollbackTransaction();
                    startTransaction();
                    processedRefToACEs.clear();
                }
            } catch (TransactionRuntimeException e) {
                if (e.getMessage().contains("Transaction timeout")) {
                    batchSize = 1;
                }
                throw e;
            }
        }
        fireACEStatusUpdatedEvent(processedRefToACEs);

        setStatus(null);
    }

    protected void fireACEStatusUpdatedEvent(Map<DocumentRef, List<ACE>> refToACEs) {
        EventContext eventContext = new EventContextImpl(session, session.getPrincipal());
        eventContext.setProperty(DOCUMENT_REFS, (Serializable) refToACEs);
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
