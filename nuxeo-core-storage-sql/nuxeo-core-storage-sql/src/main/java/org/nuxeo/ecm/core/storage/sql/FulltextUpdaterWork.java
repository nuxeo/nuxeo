/*
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 *     Stephane Lacoin
 */
package org.nuxeo.ecm.core.storage.sql;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.core.storage.sql.coremodel.SQLDocument;
import org.nuxeo.ecm.core.work.PrioritizedWork;
import org.nuxeo.ecm.core.work.api.WorkManager;

/**
 * Work task that inserts the fulltext (extracted manually in
 * {@link SessionImpl#getFulltextSimpleWork} or through
 * {@link FulltextExtractorWork}) into the fulltext table.
 * <p>
 * This is done single-threaded through the use of a {@link WorkManager} queue
 * with only one thread.
 * <p>
 * Priority is used to update "simpletext" before "binarytext".
 *
 * @since 5.7
 */
public class FulltextUpdaterWork extends PrioritizedWork {

    private static final Log log = LogFactory.getLog(FulltextUpdaterWork.class);

    protected static final String CATEGORY = "fulltextUpdater";

    protected static final String TITLE = "Fulltext Updater";

    /**
     * Info about what should be updated in a fulltext index.
     * <p>
     * Either docId or jobId is set.
     *
     * @since 5.7
     */
    public static class FulltextUpdaterInfo {
        /** If set, then only this document is updated. */
        String docId;

        /** If set, then all the documents with this jobId are updated. */
        String jobId;

        /** The index to be updated. */
        String indexName;

        /** The text to set in the index. */
        String text;

        @Override
        public String toString() {
            return getClass().getSimpleName() + '('
                    + (jobId != null ? "job=" + jobId : "doc=" + docId)
                    + ", index=" + indexName + ")";
        }
    }

    protected boolean simpletext;

    protected String repositoryName;

    protected Collection<FulltextUpdaterInfo> infos;

    public FulltextUpdaterWork(boolean simpletext, String repositoryName,
            Collection<FulltextUpdaterInfo> infos) {
        // simpletext prioritized before binarytext
        super((simpletext ? "1" : "2") + "-" + System.currentTimeMillis());
        this.simpletext = simpletext;
        this.repositoryName = repositoryName;
        this.infos = infos;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public String getTitle() {
        return TITLE;
    }

    @Override
    public Collection<DocumentLocation> getDocuments() {
        List<DocumentLocation> docs = new ArrayList<DocumentLocation>(
                infos.size());
        for (FulltextUpdaterInfo info : infos) {
            DocumentRef ref = new IdRef(info.jobId != null ? info.jobId
                    : info.docId);
            DocumentLocation doc = new DocumentLocationImpl(repositoryName, ref);
            docs.add(doc);
        }
        return docs;
    }

    @Override
    public void work() throws Exception {
        if (infos.isEmpty()) {
            return;
        }

        initSession(repositoryName);
        // if the runtime has shut down (normally because tests are finished)
        // this can happen, see NXP-4009
        if (session.getPrincipal() == null) {
            return;
        }

        boolean save = false;
        int n = 0;
        setStatus("Updating");
        for (FulltextUpdaterInfo info : infos) {
            setProgress(new Progress(++n, infos.size()));

            Collection<DocumentModel> docs;
            if (info.jobId != null) {
                String query = String.format(
                        "SELECT * FROM Document WHERE ecm:fulltextJobId = '%s' AND ecm:isProxy = 0",
                        info.jobId);
                docs = session.query(query);
            } else {
                DocumentRef ref = new IdRef(info.docId);
                if (!session.exists(ref)) {
                    // doc is gone
                    continue;
                }
                DocumentModel doc = session.getDocument(ref);
                if (doc.isProxy()) {
                    // proxies don't have any fulltext attached, it's
                    // the target document that carries it
                    continue;
                }
                docs = Collections.singleton(doc);
            }
            for (DocumentModel doc : docs) {
                try {
                    DocumentRef ref = doc.getRef();
                    if (info.jobId != null) {
                        // reset job id
                        session.setDocumentSystemProp(ref,
                                SQLDocument.FULLTEXT_JOBID_SYS_PROP, null);
                    }
                    session.setDocumentSystemProp(ref,
                            getFulltextPropertyName(info.indexName), info.text);
                    save = true;
                } catch (DocumentException e) {
                    log.error("Could not set fulltext on: " + doc.getId(), e);
                    continue;
                }
            }
        }
        if (save) {
            setStatus("Saving");
            session.save();
        }
        setStatus(null);
    }

    protected String getFulltextPropertyName(String indexName) {
        String name = simpletext ? SQLDocument.SIMPLE_TEXT_SYS_PROP
                : SQLDocument.BINARY_TEXT_SYS_PROP;
        if (!Model.FULLTEXT_DEFAULT_INDEX.equals(indexName)) {
            name += '_' + indexName;
        }
        return name;
    }

    @Override
    public void cleanUp(boolean ok, Exception e) {
        super.cleanUp(ok, e);
        infos.clear();
        infos = null;
    }
}
