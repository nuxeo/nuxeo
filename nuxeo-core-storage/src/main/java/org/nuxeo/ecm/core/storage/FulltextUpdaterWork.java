/*
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.storage;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.ecm.core.work.api.WorkManager;

/**
 * Work task that inserts the fulltext (extracted manually by the session at
 * save time, or through FulltextExtractorWork) into the fulltext table.
 * <p>
 * This is done single-threaded through the use of a {@link WorkManager} queue
 * with only one thread.
 */
public class FulltextUpdaterWork extends AbstractWork {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(FulltextUpdaterWork.class);

    public static final String SYSPROP_FULLTEXT_SIMPLE = "fulltextSimple";

    public static final String SYSPROP_FULLTEXT_BINARY = "fulltextBinary";

    public static final String SYSPROP_FULLTEXT_JOBID = "fulltextJobId";

    public static final String FULLTEXT_DEFAULT_INDEX = "default";

    protected static final String CATEGORY = "fulltextUpdater";

    protected static final String TITLE = "Fulltext Updater";

    /** Is this a simple text index or a binary text one. */
    protected final boolean isSimpleText;

    /** If true, then all the documents with the id as their jobId are updated. */
    protected final boolean isJob;

    /** The indexes and text to be updated. */
    protected final List<IndexAndText> indexesAndText;

    public static class IndexAndText {
        public String indexName;

        public String text;

        public IndexAndText(String indexName, String text) {
            this.indexName = indexName;
            this.text = text;
        }
    }

    public FulltextUpdaterWork(String repositoryName, String docId,
            boolean isSimpleText, boolean isJob,
            List<IndexAndText> indexesAndText) {
        super(); // random id, for unique job
        setDocument(repositoryName, docId);
        this.isSimpleText = isSimpleText;
        this.isJob = isJob;
        this.indexesAndText = indexesAndText;
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
    public int getRetryCount() {
        return 1;
    }

    @Override
    public void work() throws Exception {
        initSession();
        // if the runtime has shut down (normally because tests are finished)
        // this can happen, see NXP-4009
        if (session.getPrincipal() == null) {
            return;
        }

        setProgress(Progress.PROGRESS_0_PC);
        setStatus("Updating");
        update();
        setStatus("Saving");
        session.save();
        setStatus("Done");
    }

    protected void update() throws ClientException {
        Collection<DocumentModel> docs;
        if (isJob) {
            String query = String.format(
                    "SELECT * FROM Document WHERE ecm:fulltextJobId = '%s' AND ecm:isProxy = 0",
                    docId);
            docs = session.query(query);
        } else {
            DocumentRef ref = new IdRef(docId);
            if (!session.exists(ref)) {
                // doc is gone
                return;
            }
            DocumentModel doc = session.getDocument(ref);
            if (doc.isProxy()) {
                // proxies don't have any fulltext attached, it's
                // the target document that carries it
                return;
            }
            docs = Collections.singleton(doc);
        }
        for (DocumentModel doc : docs) {
            for (IndexAndText indexAndText : indexesAndText) {
                try {
                    session.setDocumentSystemProp(doc.getRef(),
                            getFulltextPropertyName(indexAndText.indexName),
                            indexAndText.text);
                } catch (DocumentException e) {
                    log.error("Could not set fulltext on: " + doc.getId(), e);
                    continue;
                }
            }
        }
        if (isJob) {
            // reset job id
            for (DocumentModel doc : docs) {
                try {
                    session.setDocumentSystemProp(doc.getRef(),
                            SYSPROP_FULLTEXT_JOBID, null);
                } catch (DocumentException e) {
                    log.error("Could not set fulltext on: " + doc.getId(), e);
                    continue;
                }
            }
        }
    }

    protected String getFulltextPropertyName(String indexName) {
        String name = isSimpleText ? SYSPROP_FULLTEXT_SIMPLE
                : SYSPROP_FULLTEXT_BINARY;
        if (!FULLTEXT_DEFAULT_INDEX.equals(indexName)) {
            name += '_' + indexName;
        }
        return name;
    }

}
