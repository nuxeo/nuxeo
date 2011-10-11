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

import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;

public class TestDocumentModel extends SQLRepositoryTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        openSession();
    }

    @Override
    public void tearDown() throws Exception {
        closeSession();
        super.tearDown();
    }

    /**
     * Tests on a DocumentModel that hasn't been created in the session yet.
     */
    public void testDocumentModelNotYetCreated() throws ClientException {
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        assertTrue(doc.isCheckedOut());
        assertEquals("0.0", doc.getVersionLabel());
        doc.refresh();
    }

    public void testContextDataOfCreatedDocument() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        doc.putContextData("key", "value");
        doc = session.createDocument(doc);
        assertEquals(doc.getContextData("key"), "value");
    }
}
