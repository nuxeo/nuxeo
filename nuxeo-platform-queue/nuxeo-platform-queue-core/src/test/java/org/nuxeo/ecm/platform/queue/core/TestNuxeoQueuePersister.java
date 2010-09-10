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
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.queue.api.QueueInfo;
import org.nuxeo.runtime.api.Framework;

/**
 * Unit Testing the nuxeo queue persister.
 *
 * @author Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 *
 */
public class TestNuxeoQueuePersister extends QueueTestCase {

    public void testNuxeoQueuePersister() throws Exception {
        NuxeoQueuePersister<FakeContent> persister = new NuxeoQueuePersister<FakeContent>("fake", FakeContent.class);
        persister.addContent(new URI("test"), new URI("test"), new FakeContent());

        // Testing the document is in the nuxeo repo
        TestRunner runner = new TestRunner(Framework.getLocalService(
                RepositoryManager.class).getDefaultRepository().getName());
        runner.runUnrestricted();

        // testing retrieving content from the persister
        List<QueueInfo<FakeContent>> items = persister.listKnownItems();
        assertEquals("Single content in queue", items.size(), 1);
        QueueInfo<FakeContent> info = items.get(0);

        // additional info
        assertEquals("Fake content is", "fake", info.getContent().fake);
        // owner
        assertEquals("Owner is", "test",info.getOwnerName().toASCIIString());
        List<QueueInfo<FakeContent>> ownedItems = persister.listByOwner(new URI("test"));
        assertEquals("owns", 1, ownedItems.size());
    }

    class TestRunner extends UnrestrictedSessionRunner {

        public TestRunner(String repository) {
            super(repository);
        }

        @Override
        public void run() throws ClientException {
            DocumentRef queueRef = new PathRef("/queues/fake");
            DocumentModel queueDoc = session.getDocument(queueRef);
            assertNotNull(queueDoc);
            DocumentModel contentDoc = session.getChild(queueRef, "test");
            assertNotNull(contentDoc);
            assertNotNull("server id is not there", contentDoc.getProperty(
                    NuxeoQueueConstants.QUEUEITEM_SCHEMA,
                    NuxeoQueueConstants.QUEUEITEM_SERVERID));
        }
    }

}
