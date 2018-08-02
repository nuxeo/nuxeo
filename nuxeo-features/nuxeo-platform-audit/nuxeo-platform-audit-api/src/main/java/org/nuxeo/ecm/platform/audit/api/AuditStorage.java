/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 *
 */

package org.nuxeo.ecm.platform.audit.api;

import java.util.List;

import org.nuxeo.ecm.core.api.ScrollResult;
import org.nuxeo.ecm.core.query.sql.model.QueryBuilder;

/**
 * Audit storage interface to append and scroll {@link LogEntry} represented as JSON. The {@link LogEntry} has to be
 * serialized to JSON with {@link BuiltinLogEntryData} field names.
 *
 * @since 9.3
 */
public interface AuditStorage {

    void append(List<String> jsonEntries);

    ScrollResult<String> scroll(QueryBuilder queryBuilder, int batchSize, int keepAliveSeconds);

    ScrollResult<String> scroll(String scrollId);

}
