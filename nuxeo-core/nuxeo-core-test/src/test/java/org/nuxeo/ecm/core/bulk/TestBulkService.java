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
import static org.junit.Assert.assertNotNull;
import static org.nuxeo.ecm.core.bulk.BulkComponent.BULK_LOG_MANAGER_NAME;
import static org.nuxeo.ecm.core.bulk.BulkStatus.State.COMPLETED;
import static org.nuxeo.ecm.core.bulk.BulkStatus.State.RUNNING;

import java.math.BigInteger;
import java.time.Duration;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.LogRecord;
import org.nuxeo.lib.stream.log.LogTailer;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.stream.StreamService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features({ CoreBulkFeature.class, CoreFeature.class })
@Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-repo-core-types-contrib.xml")
@Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/bulk-count-action-tests.xml")
@RepositoryConfig(init = DocumentSetRepositoryInit.class)
public class TestBulkService {

    @Inject
    public BulkService service;

    @Inject
    public CoreSession session;

    @Test
    public void testRunBulkAction() throws Exception {

        DocumentModel model = session.getDocument(new PathRef("/default-domain/workspaces/test"));
        String nxql = String.format("SELECT * from Document where ecm:parentId='%s'", model.getId());

        String commandId = service.submit(new BulkCommand().withUsername(session.getPrincipal().getName())
                                                           .withRepository(session.getRepositoryName())
                                                           .withQuery(nxql)
                                                           .withAction("count"));
        assertNotNull(commandId);

        LogManager manager = Framework.getService(StreamService.class).getLogManager(BULK_LOG_MANAGER_NAME);
        try (LogTailer<Record> tailer = manager.createTailer("scroll", "documentSet")) {
            tailer.read(Duration.ofSeconds(1));
        }
        try (LogTailer<Record> tailer = manager.createTailer("counter", "output")) {
            LogRecord<Record> logRecord = tailer.read(Duration.ofSeconds(1));
            assertEquals(10, new BigInteger(logRecord.message().getData()).intValue());
        }

        BulkStatus status = service.getStatus(commandId);
        assertNotNull(status);
        // TODO change RUNNING state when we'll be able to detect end
        assertEquals(RUNNING, status.getState());
        assertNotNull(status.getCount());
        assertEquals(10, status.getCount().longValue());
    }

    @Test
    public void testSetPropertyBulkOperation() throws Exception {

        DocumentModel model = session.getDocument(new PathRef("/default-domain/workspaces/test"));
        String nxql = String.format("SELECT * from Document where ecm:parentId='%s'", model.getId());

        String title = "test title";
        String description = "test description";

        String commandId = service.submit(new BulkCommand().withRepository(session.getRepositoryName())
                                                           .withUsername(session.getPrincipal().getName())
                                                           .withQuery(nxql)
                                                           .withAction("setProperties")
                                                           .withParam("dc:title", title)
                                                           .withParam("dc:description", description));

        LogManager manager = Framework.getService(StreamService.class).getLogManager("bulk");
        // TODO remove the use of tailers when we'll be able to detect end
        try (LogTailer<Record> tailer = manager.createTailer("setProperties", "setProperties")) {
            for (int i = 0; i <= 10; i++) {
                tailer.read(Duration.ofSeconds(1));
            }
        }

        try (LogTailer<Record> tailer = manager.createTailer("counter", "counter")) {
            tailer.read(Duration.ofSeconds(1));
        }

        try (LogTailer<Record> tailer = manager.createTailer("kvwriter", "keyValueWriter")) {
            tailer.read(Duration.ofSeconds(1));
        }

        BulkStatus status = service.getStatus(commandId);
        assertNotNull(status);

        assertEquals(COMPLETED, status.getState());

        assertNotNull(status.getProcessed());
        assertEquals(10, status.getProcessed().longValue());

        for (DocumentModel child : session.getChildren(model.getRef())) {
            assertEquals(title, child.getTitle());
            assertEquals(description, child.getPropertyValue("dc:description"));
        }

    }
}
