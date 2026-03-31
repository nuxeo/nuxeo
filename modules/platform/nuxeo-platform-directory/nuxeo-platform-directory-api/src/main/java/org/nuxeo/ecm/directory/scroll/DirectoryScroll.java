/*
 * (C) Copyright 2025-2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard
 */
package org.nuxeo.ecm.directory.scroll;

import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.scroll.Scroll;
import org.nuxeo.ecm.core.api.scroll.ScrollRequest;
import org.nuxeo.ecm.core.query.scroll.AbstractQueryBuilderScroll;
import org.nuxeo.ecm.core.query.scroll.QueryBuilderScrollRequest;
import org.nuxeo.ecm.core.query.sql.SQLQueryParser;
import org.nuxeo.ecm.core.query.sql.model.OrderByExprs;
import org.nuxeo.ecm.core.query.sql.model.QueryBuilder;
import org.nuxeo.ecm.core.scroll.GenericScrollRequest;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.login.NuxeoLoginContext;

/**
 * Scrolls directory entry ids.
 * <p>
 * The scroll query is expressed in NXQL e.g "SELECT * FROM continent". Where clauses are ignored and results are
 * ordered by directory id field ascendant.
 *
 * @since 2025.1
 */
public class DirectoryScroll extends AbstractQueryBuilderScroll.Query implements Scroll {

    public static final String SCROLL_NAME = "directory";

    protected NuxeoLoginContext loginContext;

    protected Session session;

    @Override
    public void init(ScrollRequest request, Map<String, String> options) {
        String directoryName;
        if (request instanceof QueryBuilderScrollRequest scrollRequest) {
            super.init(request, options);
            if (scrollRequest.getFroms().size() != 1) {
                throw new IllegalArgumentException(
                        "Scroll on multiple directories is not supported, command: " + scrollRequest.getQueryBuilder());
            }
            directoryName = scrollRequest.getFroms().getFirst();
        } else if (request instanceof GenericScrollRequest scrollRequest) {
            // usage of this type of request is deprecated since 2025.18
            hasNext = true;
            var sqlQuery = SQLQueryParser.parse(scrollRequest.getQuery());
            if (sqlQuery.getFromClause().count() != 1) {
                throw new IllegalArgumentException(
                        "Scroll on multiple directories is not supported, command: " + scrollRequest.getQuery());
            }
            queryBuilder = new QueryBuilder().offset(0L).limit(scrollRequest.getSize() + 1);
            size = scrollRequest.getSize();
            directoryName = sqlQuery.getFromClause().get(0);
        } else {
            throw new IllegalArgumentException(
                    "Request: " + request.getClass().getCanonicalName() + " is not supported");
        }
        DirectoryService ds = Framework.getService(DirectoryService.class);
        queryBuilder.order(OrderByExprs.asc(ds.getDirectoryIdField(directoryName)));
        loginContext = Framework.loginSystem();
        session = ds.open(directoryName);
    }

    @Override
    public void close() {
        if (session != null) {
            session.close();
            session = null;
        }
        if (loginContext != null) {
            loginContext.close();
            loginContext = null;
        }
    }

    @Override
    protected List<String> queryIds(QueryBuilder queryBuilder) {
        return session.queryIds(queryBuilder);
    }
}
