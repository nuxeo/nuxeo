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
 *     tdelprat
 *     bdelbosc
 */

package org.nuxeo.elasticsearch.api;

import org.elasticsearch.index.query.QueryBuilder;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;

/**
 * Interface to search on documents
 *
 * @since 5.9.3
 */
public interface ElasticSearchService {

    /**
     * Returns a document list using an {@link NxQueryBuilder}.
     *
     * @since 5.9.5
     */
    DocumentModelList query(NxQueryBuilder queryBuilder) throws ClientException;

    /**
     * Returns documents and aggregates.
     *
     * @since 5.9.6
     */
    EsResult queryAndAggregate(NxQueryBuilder queryBuilder)
            throws ClientException;

    /**
     * Returns a document list using an NXQL query.
     *
     * Fetch documents from the VCS repository.
     *
     * @since 5.9.3
     * @deprecated since 5.9.6, use query with NxQueryBuilder
     */
    @Deprecated
    DocumentModelList query(CoreSession session, String nxql, int limit,
            int offset, SortInfo... sortInfos) throws ClientException;

    /**
     * Returns a document list using an ElasticSearch {@link QueryBuilder}.
     *
     * Fetch documents from the VCS repository.
     *
     * @since 5.9.3
     * @deprecated since 5.9.6, use query with NxQueryBuilder
     */
    @Deprecated
    DocumentModelList query(CoreSession session, QueryBuilder queryBuilder,
            int limit, int offset, SortInfo... sortInfos)
            throws ClientException;

}
