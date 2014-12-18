/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 */

package org.nuxeo.elasticsearch.work;

import static org.nuxeo.elasticsearch.ElasticSearchConstants.REINDEX_BUCKET_WRITE_PROPERTY;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.elasticsearch.api.ElasticSearchIndexing;
import org.nuxeo.elasticsearch.commands.IndexingCommand;
import org.nuxeo.runtime.api.Framework;

/**œ
 * Worker to index a bucket of documents
 *
 * @since 7.1
 */
public class BucketIndexingWorker extends BaseIndexingWorker implements Work {
    private static final Log log = LogFactory.getLog(BucketIndexingWorker.class);

    private static final long serialVersionUID = 1L;

    private static final String DEFAULT_BUCKET_SIZE = "50";

    private final boolean isLast;

    private final int documentCount;

    public BucketIndexingWorker(String repositoryName, List<String> docIds, boolean isLast) {
        super();
        setDocuments(repositoryName, (List<String>) docIds);
        documentCount = docIds.size();
        this.isLast = isLast;
    }

    @Override
    public String getTitle() {
        String title = " ElasticSearch bucket indexer size " + documentCount;
        if (isLast) {
            title = title + " last worker";
        }
        return title;
    }

    @Override
    protected void doWork() {
        ElasticSearchIndexing esi = Framework.getLocalService(ElasticSearchIndexing.class);
        CoreSession session = initSession(repositoryName);
        int bucketSize = Math.min(documentCount, getBucketSize());
        List<String> ids = new ArrayList<>(bucketSize);
        for (DocumentLocation doc : getDocuments()) {
            ids.add(doc.getIdRef().value);
            if ((ids.size() % bucketSize) == 0) {
                esi.indexNonRecursive(getIndexingCommands(session, ids));
                ids.clear();
            }
        }
        if (!ids.isEmpty()) {
            esi.indexNonRecursive(getIndexingCommands(session, ids));
            ids.clear();
        }
        if (isLast) {
            log.warn(String.format("Re-indexing job: %s completed.", getSchedulePath().getParentPath()));
        }
    }

    private List<IndexingCommand> getIndexingCommands(CoreSession session, List<String> ids) {
        List<IndexingCommand> ret = new ArrayList<>(ids.size());
        for (DocumentModel doc : fetchDocuments(session, ids)) {
            IndexingCommand cmd = new IndexingCommand(doc, false, false);
            ret.add(cmd);
        }
        return ret;
    }

    private List<DocumentModel> fetchDocuments(CoreSession session, List<String> ids) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM Document, Relation WHERE ecm:uuid IN (");
        for (int i = 0; i < ids.size(); i++) {
            sb.append(NXQL.escapeString(ids.get(i)));
            if (i < ids.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append(")");
        // read invalidation
        session.save();
        return session.query(sb.toString());
    }

    protected int getBucketSize() {
        String value = Framework.getProperty(REINDEX_BUCKET_WRITE_PROPERTY, DEFAULT_BUCKET_SIZE);
        return Integer.parseInt(value);
    }

}
