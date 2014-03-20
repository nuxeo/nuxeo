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

import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilder;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;

/**
 * Main service interface for using ElasticSearch
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 *
 */
public interface ElasticSearchService {

    /**
     * Retrieves the {@link Client} that can be used to acces ElasticSearch API
     *
     * @return
     */
    Client getClient();

    DocumentModelList query(CoreSession session, QueryBuilder queryBuilder,
            int pageSize, int pageIdx) throws ClientException;

    DocumentModelList queryAsNXQL(CoreSession session, String nxql,
            int pageSize, int pageIdx) throws ClientException;
}
