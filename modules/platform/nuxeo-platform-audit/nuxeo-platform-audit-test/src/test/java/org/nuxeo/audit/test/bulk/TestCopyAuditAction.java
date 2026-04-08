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
package org.nuxeo.audit.test.bulk;

import static org.junit.Assert.assertEquals;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_EVENT_ID;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_ID;
import static org.nuxeo.audit.bulk.CopyAuditAction.ACTION_NAME;
import static org.nuxeo.audit.bulk.CopyAuditAction.PARAMETER_BACKEND_NAME;
import static org.nuxeo.audit.service.AuditComponent.DEFAULT_AUDIT_BACKEND;
import static org.nuxeo.audit.test.MultiAuditFeature.OTHER_AUDIT_BACKEND;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.SYSTEM_USERNAME;

import java.util.Date;
import java.util.List;

import jakarta.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.audit.AuditCoreFeature;
import org.nuxeo.audit.api.AuditQueryBuilder;
import org.nuxeo.audit.api.LogEntry;
import org.nuxeo.audit.scroll.AuditScroll;
import org.nuxeo.audit.service.AuditService;
import org.nuxeo.audit.test.IgnoreIfNotAuditSequence;
import org.nuxeo.audit.test.MultiAuditFeature;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.query.sql.model.OrderByExprs;
import org.nuxeo.ecm.core.query.sql.model.Predicates;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.ConditionalIgnore;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * @since 2025.19
 */
@RunWith(FeaturesRunner.class)
@Features({ MultiAuditFeature.class, CoreFeature.class })
public class TestCopyAuditAction {

    @Inject
    protected AuditService auditService;

    @Inject
    protected BulkService bulkService;

    @Inject
    protected AuditCoreFeature auditCoreFeature;

    @Inject
    protected TransactionalFeature txFeature;

    @Test
    public void testCopyFromDefaultToOther() {
        // testing copy from default allows to test that we can scroll from any backend implementation
        testCopy(DEFAULT_AUDIT_BACKEND, OTHER_AUDIT_BACKEND);
    }

    @Test
    @SuppressWarnings("removal")
    @ConditionalIgnore(condition = IgnoreIfNotAuditSequence.class)
    public void testCopyFromOtherToDefault() {
        // testing copy to default allows to test that we can insert into any backend implementation
        testCopy(OTHER_AUDIT_BACKEND, DEFAULT_AUDIT_BACKEND);
    }

    protected void testCopy(String from, String to) {
        var fromBackend = auditService.getAuditBackend(from);
        var toBackend = auditService.getAuditBackend(to);

        // create some log entries to copy
        auditCoreFeature.generateLogEntries(from, 10,
                i -> LogEntry.builder("toCopyForTests", new Date()).comment("log n°" + i).build());

        // assert current state
        var queryBuilder = new AuditQueryBuilder().predicate(Predicates.eq(LOG_EVENT_ID, "toCopyForTests"))
                                                  .order(OrderByExprs.asc(LOG_ID));
        assertEquals(10, fromBackend.queryLogs(queryBuilder).size());
        assertEquals(0, toBackend.queryLogs(queryBuilder).size());

        // create additional log entries in both backends to test that the copy action is not in error when copying an
        // already existing entry
        auditCoreFeature.generateLogEntries(List.of(from, to), 10,
                i -> LogEntry.builder("toCopyForTests", new Date()).comment("log n°" + (i + 10)).build());

        // assert current state
        assertEquals(20, fromBackend.queryLogs(queryBuilder).size());
        assertEquals(10, toBackend.queryLogs(queryBuilder).size());

        var commandId = bulkService.submit(new BulkCommand.Builder(ACTION_NAME, "SELECT * FROM " + from,
                SYSTEM_USERNAME).useQueryBuilderScroller()
                                .scroller(AuditScroll.SCROLL_NAME)
                                .param(PARAMETER_BACKEND_NAME, to)
                                .build());
        txFeature.nextTransaction();

        // assert current state
        var defaultFinalEntries = fromBackend.queryLogs(queryBuilder);
        var otherFinalEntries = toBackend.queryLogs(queryBuilder);
        assertEquals(20, defaultFinalEntries.size());
        assertEquals(20, otherFinalEntries.size());

        var defaultEntryIds = defaultFinalEntries.stream().map(LogEntry::getId).toList();
        var otherEntryIds = otherFinalEntries.stream().map(LogEntry::getId).toList();
        assertEquals(defaultEntryIds, otherEntryIds);
        // check that the one previously existing has been skipped
        var status = bulkService.getStatus(commandId);
        assertEquals(10, status.getSkipCount());
    }
}
