/*
 * (C) Copyright 2016-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.elasticsearch;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.adapter.impl.DefaultSyncRootFolderItem;
import org.nuxeo.drive.adapter.impl.ScrollDocumentModelList;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.api.EsScrollResult;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;
import org.nuxeo.runtime.api.Framework;

/**
 * Elasticsearch implementation of a {@link DefaultSyncRootFolderItem}.
 *
 * @since 8.3
 */
public class ESSyncRootFolderItem extends DefaultSyncRootFolderItem {

    private static final long serialVersionUID = 1020938498677864484L;

    private static final Log log = LogFactory.getLog(ESSyncRootFolderItem.class);

    public ESSyncRootFolderItem(String factoryName, FolderItem parentItem, DocumentModel doc) {
        super(factoryName, parentItem, doc);
    }

    public ESSyncRootFolderItem(String factoryName, FolderItem parentItem, DocumentModel doc,
            boolean relaxSyncRootConstraint) {
        super(factoryName, parentItem, doc, relaxSyncRootConstraint);
    }

    public ESSyncRootFolderItem(String factoryName, FolderItem parentItem, DocumentModel doc,
            boolean relaxSyncRootConstraint, boolean getLockInfo) {
        super(factoryName, parentItem, doc, relaxSyncRootConstraint, getLockInfo);
    }

    protected ESSyncRootFolderItem() {
        // Needed for JSON deserialization
    }

    @Override
    protected ScrollDocumentModelList getScrollBatch(String scrollId, int batchSize, CoreSession session,
            long keepAlive) {

        ElasticSearchService ess = Framework.getService(ElasticSearchService.class);

        StringBuilder sb = new StringBuilder(
                String.format("SELECT * FROM Document WHERE ecm:ancestorId = '%s'", docId));
        sb.append(" AND ecm:isTrashed = 0");
        sb.append(" AND ecm:mixinType != 'HiddenInNavigation'");
        sb.append(" AND ecm:isVersion = 0");
        // Let's order by path to make it easier for Drive as it isn't that expensive with Elasticsearch
        sb.append(" ORDER BY ecm:path");
        String query = sb.toString();
        NxQueryBuilder queryBuilder = new NxQueryBuilder(session).nxql(query).limit(batchSize);

        EsScrollResult res;
        if (StringUtils.isEmpty(scrollId)) {
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "Executing Elasticsearch initial search request to scroll through the descendants of %s with batchSize = %d and keepAlive = %d: %s",
                        docPath, batchSize, keepAlive, query));
            }
            res = ess.scroll(queryBuilder, keepAlive);
        } else {
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "Scrolling through the descendants of %s with scrollId = %s, batchSize = %s and keepAlive = %s",
                        docPath, scrollId, batchSize, keepAlive));
            }
            res = ess.scroll(new EsScrollResult(queryBuilder, scrollId, keepAlive));
        }
        return new ScrollDocumentModelList(res.getScrollId(), res.getDocuments());
    }

}
