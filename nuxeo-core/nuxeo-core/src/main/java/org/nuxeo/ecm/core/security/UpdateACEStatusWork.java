/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
        initSession();

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
        EventContext eventContext = new EventContextImpl(session);
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
