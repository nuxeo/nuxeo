/*
 * (C) Copyright 2015-2016 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.platform.picture;

import static org.apache.commons.logging.LogFactory.getLog;
import static org.nuxeo.ecm.core.api.CoreSession.ALLOW_VERSION_WRITE;
import static org.nuxeo.ecm.core.query.sql.NXQL.ECM_UUID;
import static org.nuxeo.ecm.core.query.sql.NXQL.NXQL;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.repository.RepositoryInitializationHandler;
import org.nuxeo.ecm.platform.picture.api.PictureView;
import org.nuxeo.ecm.platform.picture.api.adapters.MultiviewPicture;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;
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
public class PictureMigrationHandler extends RepositoryInitializationHandler {

    private static final Log log = getLog(PictureMigrationHandler.class);

    public static final String PICTURES_TO_MIGRATE_QUERY = "SELECT ecm:uuid FROM Document "
            + "WHERE ecm:mixinType = 'Picture' AND ecm:isProxy = 0 AND views/*/title = 'Original' "
            + "AND content/data IS NULL";

    public static final String ORIGINAL_VIEW_TITLE = "Original";

    public static final String FILE_CONTENT_PROPERTY = "file:content";

    public static final int BATCH_SIZE = 50;

    public static final String DISABLE_QUOTA_CHECK_LISTENER = "disableQuotaListener";

    @Override
    public void doInitializeRepository(CoreSession session) {
        if (Framework.getService(ConfigurationService.class).isBooleanPropertyTrue("nuxeo.picture.migration.enabled")) {
            if (log.isInfoEnabled()) {
                log.info(
                        "Starting picture migration handler (this may take some time depending on the number of documents)");
            }
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
    }

    protected void doMigration(CoreSession session) {
        Set<String> pictureIds = getPictureIdsToMigrate(session);
        if (pictureIds.isEmpty()) {
            return;
        }

        if (log.isInfoEnabled()) {
            log.info(String.format("Started migration of %d documents with the 'Picture' facet", pictureIds.size()));
        }

        long pictureMigratedCount = 0;
        try {
            for (String pictureId : pictureIds) {
                if (migratePicture(session, pictureId)) {
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

        if (log.isInfoEnabled()) {
            log.info(String.format("Finished migration of %d/%d documents with the 'Picture' facet",
                    pictureMigratedCount, pictureIds.size()));
        }
    }

    protected Set<String> getPictureIdsToMigrate(CoreSession session) {
        IterableQueryResult it = null;
        Set<String> pictureIds = new HashSet<>();

        try {
            it = session.queryAndFetch(PICTURES_TO_MIGRATE_QUERY, NXQL);

            for (Map<String, Serializable> map : it) {
                String id = (String) map.get(ECM_UUID);
                if (id != null) {
                    pictureIds.add(id);
                }
            }
        } finally {
            if (it != null) {
                it.close();
            }
        }
        return pictureIds;
    }

    protected boolean migratePicture(CoreSession session, String docId) {
        DocumentModel picture = session.getDocument(new IdRef(docId));

        if (log.isDebugEnabled()) {
            log.debug(String.format("Migrating %s", picture));
        }

        MultiviewPicture multiviewPicture = picture.getAdapter(MultiviewPicture.class);
        PictureView originalView = multiviewPicture.getView(ORIGINAL_VIEW_TITLE);
        Blob blob = originalView.getBlob();
        if (blob == null) {
            if (log.isWarnEnabled()) {
                log.warn(String.format("No Original view Blob found for %s", picture));
            }
            return false;
        }
        String filename = blob.getFilename();
        filename = StringUtils.defaultString(filename).replaceAll("^Original_", "");
        blob.setFilename(filename);
        picture.setPropertyValue(FILE_CONTENT_PROPERTY, (Serializable) blob);
        multiviewPicture.removeView(ORIGINAL_VIEW_TITLE);
        if (picture.isVersion()) {
            picture.putContextData(ALLOW_VERSION_WRITE, Boolean.TRUE);
        }
        // disable quota if installed
        picture.putContextData(DISABLE_QUOTA_CHECK_LISTENER, Boolean.TRUE);
        session.saveDocument(picture);
        return true;
    }

}
