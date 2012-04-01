/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.api;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;

public class TestDocumentModel extends SQLRepositoryTestCase {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        openSession();
    }

    @After
    public void tearDown() throws Exception {
        closeSession();
        super.tearDown();
    }

    /**
     * Tests on a DocumentModel that hasn't been created in the session yet.
     */
    @Test
    public void testDocumentModelNotYetCreated() throws ClientException {
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        assertTrue(doc.isCheckedOut());
        assertEquals("0.0", doc.getVersionLabel());
        doc.refresh();
    }

    @Test
    public void testContextDataOfCreatedDocument() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        doc.putContextData("key", "value");
        doc = session.createDocument(doc);
        assertEquals(doc.getContextData("key"), "value");
    }

    @Test
    public void testDetachAttach() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        doc = session.createDocument(doc);
        String sid = doc.getSessionId();
        assertNotNull(sid);
        assertEquals("project", doc.getCurrentLifeCycleState());

        doc.detach(false);
        doc.prefetchCurrentLifecycleState(null);
        assertNull(doc.getSessionId());
        assertNull(doc.getCurrentLifeCycleState());

        doc.attach(sid);
        session.saveDocument(doc);
        assertEquals("project", doc.getCurrentLifeCycleState());

        try {
            doc.attach("fakesid");
            fail("Should not allow attach");
        } catch (ClientException e) {
            // ok
        }
    }

}
