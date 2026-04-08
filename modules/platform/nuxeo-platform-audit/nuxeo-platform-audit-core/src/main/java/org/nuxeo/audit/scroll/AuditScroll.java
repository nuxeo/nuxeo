/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.audit.scroll;

import java.time.Duration;
import java.util.Map;

import org.nuxeo.audit.api.LogEntryConstants;
import org.nuxeo.audit.service.AuditBackend;
import org.nuxeo.audit.service.AuditService;
import org.nuxeo.ecm.core.api.ScrollResult;
import org.nuxeo.ecm.core.api.scroll.ScrollRequest;
import org.nuxeo.ecm.core.query.scroll.AbstractQueryBuilderScroll;
import org.nuxeo.ecm.core.query.scroll.QueryBuilderScrollRequest;
import org.nuxeo.ecm.core.query.sql.model.OrderByExprs;
import org.nuxeo.ecm.core.query.sql.model.QueryBuilder;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 2025.18
 */
public class AuditScroll extends AbstractQueryBuilderScroll.Scroll {

    /** @since 2025.19 */
    public static final String SCROLL_NAME = "audit";

    protected AuditBackend backend;

    @Override
    public void init(ScrollRequest request, Map<String, String> options) {
        super.init(request, options);
        var queryBuilderRequest = (QueryBuilderScrollRequest) request;
        if (queryBuilderRequest.getFroms().size() != 1) {
            throw new IllegalArgumentException(
                    "Scroll on multiple backends is not supported, command: " + request.getReference());
        }
        if (queryBuilder.orders().isEmpty()) {
            queryBuilder.order(OrderByExprs.asc(LogEntryConstants.LOG_ID));
        }
        backend = Framework.getService(AuditService.class).getAuditBackend(queryBuilderRequest.getFroms().getFirst());
    }

    @Override
    protected ScrollResult<String> scrollIds(QueryBuilder queryBuilder, int batchSize, Duration keepAlive) {
        return backend.scrollLogIds(queryBuilder, batchSize, keepAlive);
    }

    @Override
    protected ScrollResult<String> scrollIds(String scrollId) {
        return backend.scrollLogIds(scrollId);
    }

    @Override
    protected void clearScroll(String scrollId) {
        backend.clearScrollLogIds(scrollId);
    }
}
