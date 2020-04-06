/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     pierre
 */
package org.nuxeo.ecm.core.bulk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.nuxeo.ecm.core.bulk.BulkServiceImpl.BULK_LOG_MANAGER_NAME;
import static org.nuxeo.ecm.core.bulk.BulkServiceImpl.DONE_STREAM_NAME;
import static org.nuxeo.ecm.core.bulk.message.BulkStatus.State.COMPLETED;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.LogRecord;
import org.nuxeo.lib.stream.log.LogTailer;
import org.nuxeo.lib.stream.log.Name;
import org.nuxeo.runtime.stream.StreamService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @since 10.3
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
public class TestBulkProcessor {
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(TestBulkProcessor.class);

    @Inject
    public BulkService service;

    @Inject
    public CoreSession session;

    @Inject
    public StreamService stream;

    @Test
    public void testEmptyQuery() throws InterruptedException {
        @SuppressWarnings("resource")
        LogManager logManager = stream.getLogManager(BULK_LOG_MANAGER_NAME);
        try (LogTailer<Record> tailer = logManager.createTailer(Name.ofUrn("test"), DONE_STREAM_NAME)) {
            tailer.toLastCommitted();

            String nxql = "SELECT * from Document where ecm:parentId='nonExistentId'";
            assertEquals(0, session.query(nxql).size());

            String commandId = service.submit(
                    new BulkCommand.Builder("setProperties", nxql, session.getPrincipal().getName()).repository(
                            session.getRepositoryName()).build());
            assertTrue("Bulk action didn't finish", service.await(Duration.ofSeconds(10)));

            BulkStatus status = service.getStatus(commandId);
            assertEquals(commandId, status.getId());
            assertEquals(COMPLETED, status.getState());
            assertEquals(0, status.getTotal());
            assertFalse(status.hasError());

            LogRecord<Record> record = tailer.read(Duration.ofSeconds(10));
            assertNotNull("No done status found", record);
            BulkStatus doneStatus = BulkCodecs.getStatusCodec().decode(record.message().getData());
            assertEquals(status, doneStatus);
        }
    }

    @Test
    public void testInvalidQuery() throws InterruptedException {
        // null query
        try {
            service.submit(new BulkCommand.Builder("setProperties", null, "user").build());
            fail("null query should raise error");
        } catch (IllegalArgumentException e) {
            // expected
        }

        // invalid query
        String nxql = "DROP DATABASE is not a valid NXQL command";
        String commandId = service.submit(new BulkCommand.Builder("setProperties", nxql, "user").build());
        assertTrue("Bulk action didn't finish", service.await(Duration.ofSeconds(10)));
        BulkStatus status = service.getStatus(commandId);
        assertEquals(commandId, status.getId());
        assertEquals(COMPLETED, status.getState());
        assertEquals(0, status.getTotal());
        assertTrue(status.hasError());

        // query with error
        nxql = "SELECT * FROM Document WHERE ecm:path = 'non/existing/path'";
        commandId = service.submit(new BulkCommand.Builder("setProperties", nxql, "user").build());
        assertTrue("Bulk action didn't finish", service.await(Duration.ofSeconds(10)));
        status = service.getStatus(commandId);
        assertEquals(commandId, status.getId());
        assertEquals(COMPLETED, status.getState());
        assertEquals(0, status.getTotal());
        assertTrue(status.hasError());
    }

    @Test
    public void testInvalidAction() {
        String nxql = "SELECT * FROM Document";
        // null action
        try {
            service.submit(new BulkCommand.Builder(null, nxql, "user").build());
            fail("null action should raise error");
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            service.submit(new BulkCommand.Builder("unknownAction", nxql, "user").build());
            fail("unknown action should raise error");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testInvalidUser() {
        // TODO test with an invalid user

        // TODO test as non admin using and other user
    }

    @Test
    public void testInvalidOptionalParam() {
        String nxql = "SELECT * FROM Document";
        try {
            service.submit(new BulkCommand.Builder("setProperties", nxql, "user").repository("UnknownRepo").build());
            fail("unknown repo should raise error");
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            service.submit(new BulkCommand.Builder("setProperties", nxql, "user").batch(-1).bucket(-1).build());
            fail("negative batch size should raise error");
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            service.submit(new BulkCommand.Builder("setProperties", nxql, "user").batch(10).bucket(1).build());
            fail("batch must be smaller or equals to bucket");
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            new BulkCommand.Builder("setProperties", nxql, "user").param(null, "foo").build();
            fail("param key cannot be null");
        } catch (IllegalArgumentException e) {
            // expected
        }
        Map<String, Serializable> params = new HashMap<>();
        params.put(null, "foo");
        try {
            new BulkCommand.Builder("setProperties", nxql, "user").params(params).build();
            fail("param key cannot be null");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/bulk-failure-contrib.xml")
    public void testPolicyOnFailure() throws Exception {
        // add a doc
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        session.createDocument(doc);
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        // run an action that fails with a continue on failure option
        String nxql = "SELECT * FROM Document";
        String commandId = service.submit(new BulkCommand.Builder("fail", nxql, "Administrator").build());
        assertTrue("Bulk action didn't finish", service.await(Duration.ofSeconds(60)));

        BulkStatus status = service.getStatus(commandId);
        assertNotNull(status);
        assertEquals(COMPLETED, status.getState());
        assertEquals(1, status.getProcessed());
        assertTrue(status.hasError());
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/bulk-sequential-contrib.xml")
    public void testSequentialCommand() throws Exception {
        final int nbDocs = 10;
        final int nbCommands = 10;
        // create some docs
        for (int i = 0; i < nbDocs; i++) {
            DocumentModel doc = session.createDocumentModel("/", "doc" + i, "File");
            session.createDocument(doc);
        }
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        // submits multiple commands
        List<String> commands = new ArrayList<>(nbCommands);
        for (int i = 0; i < nbCommands; i++) {
            commands.add(service.submit(
                    new BulkCommand.Builder("dummySequential", "SELECT * FROM File", "Administrator").build()));
            commands.add(service.submit(
                    new BulkCommand.Builder("dummyConcurrent", "SELECT * FROM File", "Administrator").build()));
        }
        // get the results
        assertTrue("Bulk action didn't finish", service.await(Duration.ofSeconds(60)));
        List<BulkStatus> results = commands.stream().map(service::getStatus).collect(Collectors.toList());
        // sequential commands should not overlap
        assertFalse(overlapCommands(
                results.stream().filter(r -> "dummySequential".equals(r.getAction())).collect(Collectors.toList()),
                true));
        // concurrent commands must overlap
        assertTrue(overlapCommands(
                results.stream().filter(r -> "dummyConcurrent".equals(r.getAction())).collect(Collectors.toList()),
                false));
    }

    @Test
    public void testScroller() throws InterruptedException {
        // use the default scroller, everything ok
        String nxql = "SELECT * FROM Document";
        String commandId = service.submit(
                new BulkCommand.Builder("setProperties", nxql, "user").build());
        assertTrue("Bulk action didn't finish", service.await(Duration.ofSeconds(10)));
        BulkStatus status = service.getStatus(commandId);
        assertEquals(COMPLETED, status.getState());
        assertEquals(0, status.getTotal());
        assertFalse(status.hasError());

        // explicit use of the repository scroller, everything ok
        nxql = "SELECT * FROM Document";
        commandId = service.submit(
                new BulkCommand.Builder("setProperties", nxql, "user").scroller("repository").build());
        assertTrue("Bulk action didn't finish", service.await(Duration.ofSeconds(10)));
        status = service.getStatus(commandId);
        assertEquals(COMPLETED, status.getState());
        assertEquals(0, status.getTotal());
        assertFalse(status.hasError());

        // use an unknown scroller, error expected
        try {
            service.submit(
                    new BulkCommand.Builder("setProperties", "whatever query", "user").scroller("unknown").build());
            fail("unknown scroller should raise error");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    protected boolean overlapCommands(List<BulkStatus> results, boolean logOverlap) {
        // Check for overlap on scrolling
        results.sort(Comparator.comparing(BulkStatus::getScrollStartTime));
        BulkStatus previous = null;
        for (BulkStatus current : results) {
            if (previous == null) {
                previous = current;
                continue;
            }
            if (overlapInstant(previous.getScrollStartTime(), previous.getScrollEndTime(), current.getScrollStartTime(),
                    current.getScrollEndTime())) {
                if (logOverlap) {
                    logOverlap("scroll", previous, current);
                }
                return true;
            }
        }
        // Check for overlap on processing
        results.sort(Comparator.comparing(BulkStatus::getProcessingStartTime));
        previous = null;
        for (BulkStatus current : results) {
            if (previous == null) {
                previous = current;
                continue;
            }
            if (overlapInstant(previous.getProcessingStartTime(), previous.getProcessingEndTime(),
                    current.getProcessingStartTime(), current.getProcessingEndTime())) {
                if (logOverlap) {
                    logOverlap("processing", previous, current);
                }
                return true;
            }
        }
        return false;
    }

    protected void logOverlap(String msg, BulkStatus previous, BulkStatus current) {
        log.warn(String.format("Overlap detected in %s:\n- %s\n- %s", msg, previous, current));
        log.warn(String.format("%s %s %s %s %s %s", previous.getId(), previous.getSubmitTime(),
                previous.getScrollStartTime(), previous.getScrollEndTime(), previous.getProcessingStartTime(),
                previous.getCompletedTime()));
        log.warn(String.format("%s %s %s %s %s %s", current.getId(), current.getSubmitTime(),
                current.getScrollStartTime(), current.getScrollEndTime(), current.getProcessingStartTime(),
                current.getCompletedTime()));
    }

    protected boolean overlapInstant(Instant aStart, Instant aEnd, Instant bStart, Instant bEnd) {
        return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
    }
}
