/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc <kleturc@nuxeo.com>
 *
 */
package org.nuxeo.ecm.platform.audit.api;

import org.nuxeo.ecm.core.query.sql.model.OrderByExprs;
import org.nuxeo.ecm.core.query.sql.model.QueryBuilder;

/**
 * Query builder for querying audit.
 *
 * @since 9.3
 */
public class AuditQueryBuilder extends QueryBuilder {

    @Override
    public QueryBuilder defaultOrder() {
        return orders(OrderByExprs.desc(BuiltinLogEntryData.LOG_EVENT_DATE));
    }

}
