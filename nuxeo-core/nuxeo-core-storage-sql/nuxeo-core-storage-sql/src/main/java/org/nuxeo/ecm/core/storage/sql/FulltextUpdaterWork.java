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

import java.util.Collection;
import java.util.Collections;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.storage.sql.coremodel.SQLDocument;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.ecm.core.work.api.WorkManager;

/**
 * Work task that inserts the fulltext (extracted manually in
 * {@link SessionImpl#getFulltextSimpleWork} or through
 * {@link FulltextExtractorWork}) into the fulltext table.
 * <p>
 * This is done single-threaded through the use of a {@link WorkManager} queue
 * with only one thread.
 *
 * @since 5.7
 */
public class FulltextUpdaterWork extends AbstractWork {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(FulltextUpdaterWork.class);

    protected static final String CATEGORY = "fulltextUpdater";

    protected static final String TITLE = "Fulltext Updater";

    /** The index to be updated. */
    protected final String indexName;

    /** Is this a simple text index or a binary text one. */
    protected final boolean isSimpleText;

    /** The text to set in the index. */
    protected final String text;

    /** If true, then all the documents with the id as their jobId are updated. */
    protected final boolean isJob;

    public FulltextUpdaterWork(String repositoryName, String docId,
            String indexName, boolean isSimpleText, String text, boolean isJob) {
        super(); // random id, for unique job
        setDocument(repositoryName, docId);
        this.indexName = indexName;
        this.isSimpleText = isSimpleText;
        this.text = text;
        this.isJob = isJob;
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
    public void retryableWork() throws Exception {
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
        String fulltextPropertyName = getFulltextPropertyName();
        for (DocumentModel doc : docs) {
            try {
                DocumentRef ref = doc.getRef();
                if (isJob) {
                    // reset job id
                    session.setDocumentSystemProp(ref,
                            SQLDocument.FULLTEXT_JOBID_SYS_PROP, null);
                }
                session.setDocumentSystemProp(ref, fulltextPropertyName, text);
            } catch (DocumentException e) {
                log.error("Could not set fulltext on: " + doc.getId(), e);
                continue;
            }
        }
    }

    protected String getFulltextPropertyName() {
        String name = isSimpleText ? SQLDocument.SIMPLE_TEXT_SYS_PROP
                : SQLDocument.BINARY_TEXT_SYS_PROP;
        if (!Model.FULLTEXT_DEFAULT_INDEX.equals(indexName)) {
            name += '_' + indexName;
        }
        return name;
    }

}
