/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo
 */

package org.nuxeo.elasticsearch.api;

import java.util.List;

import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilder;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.SortInfo;

/**
 * Main service interface for using ElasticSearch
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 5.9.3
 */
public interface ElasticSearchService {

    /**
     * Retrieves the {@link Client} that can be used to access ElasticSearch API
     *
     * @return
     */
    Client getClient();

    /**
     * Returns a document list using an NXQL query.
     *
     */
    DocumentModelList query(CoreSession session, String nxql, int limit,
            int offset, SortInfo... sortInfos) throws ClientException;

    /**
     * Returns a document list using an ElasticSearch QueryBuilder.
     *
     */
    DocumentModelList query(CoreSession session, QueryBuilder queryBuilder,
            int limit, int offset, SortInfo... sortInfos)
            throws ClientException;

    /**
     * Returns the list of field names that use a fulltext analyzer.
     *
     */
    List<String> getFulltextFields();

}
