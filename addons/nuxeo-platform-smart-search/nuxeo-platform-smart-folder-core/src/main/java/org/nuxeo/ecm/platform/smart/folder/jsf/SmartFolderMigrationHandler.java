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
 *     Anahide Tchertchian
 */

package org.nuxeo.ecm.platform.smart.folder.jsf;

import static org.apache.commons.logging.LogFactory.getLog;
import static org.nuxeo.ecm.core.api.CoreSession.ALLOW_VERSION_WRITE;
import static org.nuxeo.ecm.core.query.sql.NXQL.ECM_UUID;
import static org.nuxeo.ecm.core.query.sql.NXQL.NXQL;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.repository.RepositoryInitializationHandler;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Migrate old Original picture view to {@code file:content}.
 * <p>
 * Does not copy it if {@code file:content} is not empty. When done, the Original picture view is removed.
 * <p>
 * It does not recompute the picture views.
 *
 * @since 7.2
 */
public class SmartFolderMigrationHandler extends RepositoryInitializationHandler {

    private static final Log log = getLog(SmartFolderMigrationHandler.class);

    public static final String MIGRATION_QUERY = "SELECT ecm:uuid FROM SmartFolder WHERE cvd:contentViewName IS NULL";

    public static final String CV_PROP = "cvd:contentViewName";

    public static final String CV_NAME = "nxql_incremental_smart_query";

    public static final int BATCH_SIZE = 50;

    public static final String DISABLE_QUOTA_CHECK_LISTENER = "disableQuotaListener";

    @Override
    public void doInitializeRepository(CoreSession session) {
        boolean txStarted = false;
        if (!TransactionHelper.isTransactionActive()) {
            txStarted = true;
        }

        try {
            doMigration(session);
        } finally {
            if (txStarted) {
                TransactionHelper.commitOrRollbackTransaction();
            }
        }
    }

    protected void doMigration(CoreSession session) {
        Set<String> pictureIds = getDocIdsToMigrate(session);
        if (pictureIds.isEmpty()) {
            return;
        }

        if (log.isWarnEnabled()) {
            log.warn(String.format("Started migration of %d documents with the 'Picture' facet", pictureIds.size()));
        }

        long pictureMigratedCount = 0;
        try {
            for (String pictureId : pictureIds) {
                if (migrateDocument(session, pictureId)) {
                    if (++pictureMigratedCount % BATCH_SIZE == 0) {
                        TransactionHelper.commitOrRollbackTransaction();
                        TransactionHelper.startTransaction();
                    }
                }
            }
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }

        if (log.isWarnEnabled()) {
            log.warn(String.format("Finished migration of %d/%d documents with the 'Picture' facet",
                    pictureMigratedCount, pictureIds.size()));
        }
    }

    protected Set<String> getDocIdsToMigrate(CoreSession session) {
        IterableQueryResult it = null;
        Set<String> ids = new HashSet<>();

        try {
            it = session.queryAndFetch(MIGRATION_QUERY, NXQL);

            for (Map<String, Serializable> map : it) {
                String id = (String) map.get(ECM_UUID);
                if (id != null) {
                    ids.add(id);
                }
            }
        } finally {
            if (it != null) {
                it.close();
            }
        }
        return ids;
    }

    protected boolean migrateDocument(CoreSession session, String docId) {
        DocumentModel doc = session.getDocument(new IdRef(docId));

        if (log.isDebugEnabled()) {
            log.debug(String.format("Migrating %s", doc));
        }

        doc.setPropertyValue(CV_PROP, CV_NAME);
        if (doc.isVersion()) {
            doc.putContextData(ALLOW_VERSION_WRITE, Boolean.TRUE);
        }
        // disable quota if installed
        doc.putContextData(DISABLE_QUOTA_CHECK_LISTENER, Boolean.TRUE);
        session.saveDocument(doc);
        return true;
    }

}
