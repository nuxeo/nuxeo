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
 *     Estelle Giuly <egiuly@nuxeo.com>
 */

package org.nuxeo.audit.storage.impl;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CursorService;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.ScrollResult;
import org.nuxeo.ecm.core.query.sql.model.MultiExpression;
import org.nuxeo.ecm.core.query.sql.model.Operator;
import org.nuxeo.ecm.core.query.sql.model.Predicate;
import org.nuxeo.ecm.core.query.sql.model.QueryBuilder;
import org.nuxeo.ecm.core.query.sql.model.StringLiteral;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.audit.api.AuditStorage;
import org.nuxeo.runtime.api.Framework;

/**
 * Audit storage implementation for an Audit directory.
 *
 * @since 9.10
 */
public class DirectoryAuditStorage implements AuditStorage {

    private static final Log log = LogFactory.getLog(DirectoryAuditStorage.class);

    public static final String NAME = "DirectoryAuditStorage";

    public static final String DIRECTORY_NAME = "auditStorage";

    public static final String ID_COLUMN = "id";

    public static final String JSON_COLUMN = "entry";

    protected CursorService<Iterator<String>, String, String> cursorService = new CursorService<>(Function.identity());

    protected Directory getAuditDirectory() {
        return Framework.getService(DirectoryService.class).getDirectory(DIRECTORY_NAME);
    }

    /**
     * Insert entries as Json in the Audit directory.
     */
    @Override
    public void append(List<String> jsonEntries) {
        try (Session session = getAuditDirectory().getSession()) {
            for (String jsonEntry : jsonEntries) {
                Framework.doPrivileged(() -> session.createEntry(Collections.singletonMap(JSON_COLUMN, jsonEntry)));
            }
        }
    }

    /**
     * Scroll log entries in the Audit directory, given a scroll Id.
     */
    @Override
    public ScrollResult<String> scroll(String scrollId) {
        return cursorService.scroll(scrollId);
    }

    /**
     * Scroll log entries in the Audit directory, given an audit query builder.
     */
    @Override
    public ScrollResult<String> scroll(QueryBuilder queryBuilder, int batchSize, int keepAlive) {
        cursorService.checkForTimedOutScroll();
        List<String> logEntries = queryLogs(queryBuilder);
        String scrollId = cursorService.registerCursor(logEntries.iterator(), batchSize, keepAlive);
        return scroll(scrollId);
    }

    /**
     * Query log entries in the Audit directory, given an audit query builder. Does not support literals other than
     * StringLiteral: see {@link Session#query(Map, Set, Map, boolean, int, int)}.
     */
    protected List<String> queryLogs(QueryBuilder queryBuilder) {
        // Get the predicates filter map from the query builder.
        Map<String, Serializable> filter = new HashMap<>();
        Set<String> fulltext = null;
        MultiExpression predicate = (MultiExpression) queryBuilder.predicate();
        @SuppressWarnings("unchecked")
        List<Predicate> predicateList = (List<Predicate>) ((List<?>) predicate.values);
        for (Predicate p : predicateList) {
            String rvalue;
            if (p.rvalue instanceof StringLiteral) {
                rvalue = ((StringLiteral) p.rvalue).asString();
            } else {
                rvalue = p.rvalue.toString();
                log.warn(String.format(
                        "Scrolling audit logs with a query builder containing non-string literals is not supported: %s.",
                        rvalue));
            }
            filter.put(p.lvalue.toString(), rvalue);

            if (fulltext == null && Arrays.asList(Operator.LIKE, Operator.ILIKE).contains(p.operator)) {
                fulltext = Collections.singleton(JSON_COLUMN);
            }
        }

        // Get the orderBy map from the query builder.
        Map<String, String> orderBy = queryBuilder.orders().stream().collect(
                Collectors.toMap(o -> o.reference.name, o -> o.isDescending ? "desc" : "asc"));

        // Get the limit and offset from the query builder.
        int limit = (int) queryBuilder.limit();
        int offset = (int) queryBuilder.offset();

        // Query the Json Entries via the directory session.
        Directory directory = getAuditDirectory();
        try (Session session = directory.getSession()) {
            DocumentModelList jsonEntriesDocs = session.query(filter, fulltext, orderBy, false, limit, offset);

            // Build a list of Json Entries from the document model list.
            String auditPropertyName = directory.getSchema() + ":" + JSON_COLUMN;
            return jsonEntriesDocs.stream()
                                  .map(doc -> doc.getPropertyValue(auditPropertyName))
                                  .map(String::valueOf)
                                  .collect(Collectors.toList());
        }
    }

}
