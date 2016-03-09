/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 *     Stephane Lacoin
 */
package org.nuxeo.ecm.core.storage;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.ecm.core.work.api.WorkManager;

/**
 * Work task that inserts the fulltext (extracted manually by the session at save time, or through
 * FulltextExtractorWork) into the fulltext table.
 * <p>
 * This is done single-threaded through the use of a {@link WorkManager} queue with only one thread.
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

    public static class IndexAndText implements Serializable {
        private static final long serialVersionUID = 1L;

        public String indexName;

        public String text;

        public IndexAndText(String indexName, String text) {
            this.indexName = indexName;
            this.text = text;
        }
    }

    public FulltextUpdaterWork(String repositoryName, String docId, boolean isSimpleText, boolean isJob,
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
    public void work() {
        openSystemSession();
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

    protected void updateWithSession(CoreSession session) {
        CoreSession tmp = this.session;
        this.session = session;
        update();
        this.session = tmp;
    }

    protected void update() {
        Collection<DocumentModel> docs;
        if (isJob) {
            String query = String.format("SELECT * FROM Document WHERE ecm:fulltextJobId = '%s' AND ecm:isProxy = 0",
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
                session.setDocumentSystemProp(doc.getRef(), getFulltextPropertyName(indexAndText.indexName),
                        indexAndText.text);
            }
        }
        if (isJob) {
            // reset job id
            for (DocumentModel doc : docs) {
                session.setDocumentSystemProp(doc.getRef(), SYSPROP_FULLTEXT_JOBID, null);
            }
        }
    }

    protected String getFulltextPropertyName(String indexName) {
        String name = isSimpleText ? SYSPROP_FULLTEXT_SIMPLE : SYSPROP_FULLTEXT_BINARY;
        if (!FULLTEXT_DEFAULT_INDEX.equals(indexName)) {
            name += '_' + indexName;
        }
        return name;
    }

}
