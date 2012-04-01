/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Dragos Mihalache
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.version.test;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.facet.VersioningDocument;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.runtime.api.Framework;

public class TestVersioningDocumentAdapter extends SQLRepositoryTestCase {

    protected VersioningService service;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        openSession();
        service = Framework.getService(VersioningService.class);
        assertNotNull(service);
    }

    @After
    public void tearDown() throws Exception {
        closeSession();
        super.tearDown();
    }

    @Test
    public void testVersionLabel() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "testfile1",
                "File");
        doc = session.createDocument(doc);
        VersioningDocument vdoc = doc.getAdapter(VersioningDocument.class);
        assertNotNull(vdoc);
        assertTrue(doc.isCheckedOut());
        assertEquals("0.0", vdoc.getVersionLabel());

        doc.checkIn(VersioningOption.MINOR, "");
        assertFalse(doc.isCheckedOut());
        assertEquals("0.1", vdoc.getVersionLabel());

        doc.checkOut();
        assertTrue(doc.isCheckedOut());
        assertEquals("0.1+", vdoc.getVersionLabel());
    }

}
