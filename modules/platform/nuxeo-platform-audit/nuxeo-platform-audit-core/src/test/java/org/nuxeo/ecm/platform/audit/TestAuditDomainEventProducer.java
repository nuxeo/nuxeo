/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *    bdelbosc
 */

package org.nuxeo.ecm.platform.audit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.audit.api.AuditReader;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.lib.stream.log.LogLag;
import org.nuxeo.lib.stream.log.Name;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.stream.StreamService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * @since 11.4
 */
@Deploy("org.nuxeo.ecm.platform.audit.tests:test-domain-event-producer-contrib.xml")
@RunWith(FeaturesRunner.class)
@Features(AuditFeature.class)
public class TestAuditDomainEventProducer {
    protected static final Log log = LogFactory.getLog(TestAuditDomainEventProducer.class);

    @Inject
    protected CoreSession repo;

    @Inject
    protected StreamService streamService;

    @Inject
    protected TransactionalFeature txFeature;

    public void waitForAsyncCompletion() {
        txFeature.nextTransaction(Duration.ofSeconds(20));
    }

    @Before
    public void isInjected() {
        assertNotNull(repo);
    }

    @Test
    public void testAuditDomainEventProducer() {
        LogLag lag = streamService.getLogManager().getLag(Name.ofUrn("source/audit"), Name.ofUrn("test/reader"));
        // Initializing the repository emits some audit events
        assertTrue(lag.lag() > 0);

        // generate events
        DocumentModel doc = repo.createDocumentModel("/", "a-file", "File");
        doc = repo.createDocument(doc);
        waitForAsyncCompletion();

        // test audit trail
        AuditReader reader = Framework.getService(AuditReader.class);
        List<LogEntry> trail = reader.getLogEntriesFor(doc.getId(), repo.getRepositoryName());
        assertNotNull(trail);
        assertEquals(1, trail.size());

        // test we have one more event
        LogLag lag2 = streamService.getLogManager().getLag(Name.ofUrn("source/audit"), Name.ofUrn("test/reader"));
        assertEquals(lag.lag() + 1, lag2.lag());
    }

}
