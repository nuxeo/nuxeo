/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 */
package org.nuxeo.ecm.platform.queue.core;

import java.net.URI;
import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.queue.api.QueueContent;
import org.nuxeo.ecm.platform.queue.api.QueueItem;
import org.nuxeo.runtime.api.Framework;

/**
 * Unit Testing the nuxeo queue persister.
 * 
 * @author Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 * 
 */
public class TestNuxeoQueuePersister extends SQLRepositoryTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.platform.heartbeat.api");
        deployBundle("org.nuxeo.ecm.platform.heartbeat");
        deployBundle("org.nuxeo.ecm.platform.queue.api");
        deployBundle("org.nuxeo.ecm.platform.queue");
        super.fireFrameworkStarted();
    }

    public void testNuxeoQueuePersister() throws Exception {
        URI owner = new URI("nxprincipal:test");

        NuxeoQueuePersister persister = new NuxeoQueuePersister();
        QueueContent content = new QueueContent(owner, "myQueue", "myQueueItem");
        content.setDelay(800);
        content.setComments("test content");
        MockAdditionalInfo info = new MockAdditionalInfo("test add info");
        content.setAdditionalInfo(info);

        persister.saveContent(content);

        // Testing the document is in the nuxeo repo
        TestRunner runner = new TestRunner(Framework.getLocalService(
                RepositoryManager.class).getDefaultRepository().getName());
        runner.runUnrestricted();

        // testing retrieving content from the persister
        List<QueueItem> items = persister.listKnownItems("myQueue");
        // additional info
        MockAdditionalInfo newInfo = (MockAdditionalInfo) items.get(0).getHandledContent().getAdditionalInfo();
        assertEquals("Additional info is", "test add info", newInfo.info);
        // owner
        assertEquals("Owner is", "nxprincipal:test",
                items.get(0).getHandledContent().getOwner().toASCIIString());
    }

    class TestRunner extends UnrestrictedSessionRunner {

        public TestRunner(String repository) {
            super(repository);
        }

        @Override
        public void run() throws ClientException {
            assertNotNull(session.getDocument(new PathRef("/queues/myQueue")));
            DocumentModel document = session.getDocument(new PathRef(
                    "/queues/myQueue/myQueueItem"));
            assertNotNull(document);
            assertNotNull("server id is not there", document.getProperty(
                    NuxeoQueueConstants.QUEUEITEM_SCHEMA,
                    NuxeoQueueConstants.QUEUEITEM_SERVERID));
        }
    }

}
