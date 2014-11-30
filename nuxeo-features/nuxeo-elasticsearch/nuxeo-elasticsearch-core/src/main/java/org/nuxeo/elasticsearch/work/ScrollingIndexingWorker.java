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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Worker to reindex a large amount of document
 *
 * @since 7.1
 */
public class ScrollingIndexingWorker extends BaseIndexingWorker implements Work {
    private static final Log log = LogFactory
            .getLog(ScrollingIndexingWorker.class);
    private static final long serialVersionUID = 1L;
    private static final String DEFAULT_BUCKET_SIZE = "500";
    protected final String nxql;
    protected WorkManager workManager;
    private long docCount = 0;

    public ScrollingIndexingWorker(String nxql) {
        super();
        this.nxql = nxql;
    }

    @Override
    public String getTitle() {
        return "ElasticSearch scrolling indexer: " + nxql + ", processed "
                + docCount;
    }

    @Override
    protected void doWork() {
        CoreSession session = initSession(repositoryName);
        IterableQueryResult res = session.queryAndFetch(nxql, NXQL.NXQL);
        int bucketCount = 0;
        try {
            Iterator<Map<String, Serializable>> it = res.iterator();
            int bucketSize = getBucketSize();
            Set<String> ids = new HashSet<>(bucketSize);
            while (it.hasNext()) {
                docCount += 1;
                ids.add((String) it.next().get(NXQL.ECM_UUID));
                if (ids.size() == bucketSize) {
                    scheduleBucketWorker(ids, false);
                    ids = new HashSet<>(bucketSize);
                    bucketCount += 1;
                }
            }
            scheduleBucketWorker(ids, true);
            bucketCount += 1;
        } finally {
            res.close();
            log.warn(String.format(
                    "%d documents submitted in %d bucket worker", docCount,
                    bucketCount));
        }
    }

    protected void scheduleBucketWorker(Set<String> bucket, boolean isLast) {
        BucketIndexingWorker subWorker = new BucketIndexingWorker(bucket,
                isLast);
        getWorkManager().schedule(subWorker);
    }

    protected WorkManager getWorkManager() {
        if (workManager == null) {
            workManager = Framework.getLocalService(WorkManager.class);
        }
        return workManager;
    }

    protected int getBucketSize() {
        String value = Framework.getProperty(REINDEX_BUCKET_READ_PROPERTY,
                DEFAULT_BUCKET_SIZE);
        return Integer.parseInt(value);
    }

}
