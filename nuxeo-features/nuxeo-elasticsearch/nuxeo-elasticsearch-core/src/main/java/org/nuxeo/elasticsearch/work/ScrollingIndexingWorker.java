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
 *     Delbosc Benoit
 */

package org.nuxeo.elasticsearch.work;

import static org.nuxeo.elasticsearch.ElasticSearchConstants.REINDEX_BUCKET_READ_PROPERTY;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.elasticsearch.core.IndexingMonitor;
import org.nuxeo.runtime.api.Framework;

/**
 * Worker to reindex a large amount of document
 *
 * @since 7.1
 */
public class ScrollingIndexingWorker extends BaseIndexingWorker implements Work {
    private static final Log log = LogFactory.getLog(ScrollingIndexingWorker.class);

    private static final long serialVersionUID = 1L;

    private static final String DEFAULT_BUCKET_SIZE = "500";

    private static final long WARN_DOC_COUNT = 500;

    protected final String nxql;

    protected WorkManager workManager;

    protected long documentCount = 0;

    public ScrollingIndexingWorker(IndexingMonitor monitor, String repositoryName, String nxql) {
        super(monitor);
        this.repositoryName = repositoryName;
        this.nxql = nxql;
    }

    @Override
    public String getTitle() {
        return "Elasticsearch scrolling indexer: " + nxql + ", processed " + documentCount;
    }

    @Override
    protected void doWork() {
        String jobName = getSchedulePath().getPath();
        if (log.isDebugEnabled()) {
            log.debug(String.format("Re-indexing job: %s started, NXQL: %s on repository: %s", jobName, nxql, repositoryName));
        }
        CoreSession session = initSession(repositoryName);
        IterableQueryResult res = session.queryAndFetch(nxql, NXQL.NXQL);
        int bucketCount = 0;
        boolean warnAtEnd = false;
        try {
            Iterator<Map<String, Serializable>> it = res.iterator();
            int bucketSize = getBucketSize();
            List<String> ids = new ArrayList<>(bucketSize);
            while (it.hasNext()) {
                documentCount += 1;
                ids.add((String) it.next().get(NXQL.ECM_UUID));
                if (ids.size() == bucketSize) {
                    scheduleBucketWorker(ids, false);
                    ids = new ArrayList<>(bucketSize);
                    bucketCount += 1;
                }
            }
            if (documentCount > WARN_DOC_COUNT) {
              warnAtEnd = true;
            }
            scheduleBucketWorker(ids, warnAtEnd);
            if (!ids.isEmpty()) {
                bucketCount += 1;
            }
        } finally {
            res.close();
            if (warnAtEnd || log.isDebugEnabled()) {
                String message = String.format("Re-indexing job: %s has submited %d documents in %d bucket workers",
                        jobName, documentCount, bucketCount);
                if (warnAtEnd) {
                    log.warn(message);
                } else {
                    log.debug(message);
                }
            }
        }
    }

    protected void scheduleBucketWorker(List<String> bucket, boolean isLast) {
        if (bucket.isEmpty()) {
            return;
        }
        BucketIndexingWorker subWorker = new BucketIndexingWorker(monitor, repositoryName, bucket, isLast);
        getWorkManager().schedule(subWorker);
    }

    protected WorkManager getWorkManager() {
        if (workManager == null) {
            workManager = Framework.getLocalService(WorkManager.class);
        }
        return workManager;
    }

    protected int getBucketSize() {
        String value = Framework.getProperty(REINDEX_BUCKET_READ_PROPERTY, DEFAULT_BUCKET_SIZE);
        return Integer.parseInt(value);
    }

}
