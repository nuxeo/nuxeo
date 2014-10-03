/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.filemanager.core.listener.tests;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.*;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;

/**
 * Testing the mime type icon updater listener. This listener should update
 * mime type of a blob when this one is dirty (updated). When the blob is about
 * is on file:content, it also updates the common:icon field setting the right
 * icon according to the mime type
 *
 * @author Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 * @author Benjamin Jalon <bjalon@nuxeo.com>
 */
public class TestMimetypeIconUpdater extends SQLRepositoryTestCase {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.platform.mimetype.api");
        deployBundle("org.nuxeo.ecm.platform.mimetype.core");
        deployBundle("org.nuxeo.ecm.platform.filemanager.api");
        deployBundle("org.nuxeo.ecm.platform.filemanager.core");
        deployBundle("org.nuxeo.ecm.platform.types.api");
        deployBundle("org.nuxeo.ecm.platform.types.core");
        deployContrib("org.nuxeo.ecm.platform.filemanager.core.listener",
                "OSGI-INF/filemanager-iconupdater-event-contrib.xml");
        deployContrib("org.nuxeo.ecm.platform.filemanager.core.listener.test",
                "OSGI-INF/core-type-contrib.xml");

        openSession();
    }

    @After
    public void tearDown() throws Exception {
        closeSession();
        super.tearDown();
    }

    /**
     * Testing a mime type and icon update (only done on file:content)
     *
     * @throws Exception
     */
    @Test
    public void testMimeTypeUpdater() throws Exception {
        DocumentModel file = createFileDocument(false);
        Blob blob = (Blob) file.getProperty("file", "content");
        assertNotNull(blob);
        String mt = blob.getMimeType();
        assertNotNull(mt);
        assertEquals("application/pdf", mt);

        String icon = (String) file.getProperty("common", "icon");
        assertNotNull(icon);
        assertEquals("/icons/pdf.png", icon);
        assertTrue((Long)file.getPropertyValue("common:size") > 0L);

        // removing blob
        removeMainBlob(file);
        icon = (String) file.getProperty("common", "icon");
        assertNotNull(icon);
        assertEquals("/icons/pdf.png", icon);
        assertEquals(0L, file.getPropertyValue("common:size"));
    }

    /**
     * Testing mime type update with a schema without prefix.
     * https://jira.nuxeo.org/browse/NXP-3972 <a
     * href="https://jira.nuxeo.org/browse/NXP-3972">NXP-3972</a>
     *
     * @throws Exception
     */
    @Test
    public void testMimeTypeUpdaterWithoutPrefix() throws Exception {
        DocumentModel doc = createWithoutPrefixBlobDocument(false);
        Blob blob = (Blob) doc.getProperty("wihtoutpref", "blob");
        assertNotNull(blob);
        String mt = blob.getMimeType();
        assertNotNull(mt);
        assertEquals("application/pdf", mt);
    }

    /**
     * Testing mime type update with a schema with prefix.
     * https://jira.nuxeo.org/browse/NXP-3972 <a
     * href="https://jira.nuxeo.org/browse/NXP-3972">NXP-3972</a>
     *
     * @throws Exception
     */
    @Test
    public void testMimeTypeUpdaterWithPrefix() throws Exception {
        DocumentModel doc = createWithPrefixBlobDocument(false);
        Blob blob = (Blob) doc.getProperty("simpleblob", "blob");
        assertNotNull(blob);
        String mt = blob.getMimeType();
        assertNotNull(mt);
        assertEquals("application/pdf", mt);
    }

    @Test
    public void testMimeTypeUpdaterFolderish() throws Exception {
        // Workspace is folderish and contains the file schema
        DocumentModel doc = createWorkspace();
        Blob blob = (Blob) doc.getProperty("file", "content");
        assertNotNull(blob);
        String mt = blob.getMimeType();
        assertNotNull(mt);
        assertEquals("application/pdf", mt);

        String icon = (String) doc.getProperty("common", "icon");
        assertNull(icon); // default icon, not overridden by mime type
    }

    protected CoreSession getCoreSession() {
        return session;
    }

    protected DocumentModel createWithoutPrefixBlobDocument(boolean setMimeType)
            throws ClientException {
        DocumentModel withoutPrefixBlobDoc = getCoreSession().createDocumentModel(
                "/", "testFile", "WithoutPrefixDocument");
        withoutPrefixBlobDoc.setProperty("dublincore", "title", "TestFile");

        Blob blob = new StringBlob("SOMEDUMMYDATA", null);
        blob.setFilename("test.pdf");
        if (setMimeType) {
            blob.setMimeType("application/pdf");
        }
        withoutPrefixBlobDoc.setProperty("wihtoutpref", "blob", blob);

        withoutPrefixBlobDoc = getCoreSession().createDocument(
                withoutPrefixBlobDoc);

        getCoreSession().saveDocument(withoutPrefixBlobDoc);
        getCoreSession().save();

        return withoutPrefixBlobDoc;
    }

    protected DocumentModel createWithPrefixBlobDocument(boolean setMimeType)
            throws ClientException {
        DocumentModel withoutPrefixBlobDoc = getCoreSession().createDocumentModel(
                "/", "testFile", "SimpleBlobDocument");
        withoutPrefixBlobDoc.setProperty("dublincore", "title", "TestFile");

        Blob blob = new StringBlob("SOMEDUMMYDATA", null);
        blob.setFilename("test.pdf");
        if (setMimeType) {
            blob.setMimeType("application/pdf");
        }
        withoutPrefixBlobDoc.setProperty("simpleblob", "blob", blob);

        withoutPrefixBlobDoc = getCoreSession().createDocument(
                withoutPrefixBlobDoc);

        getCoreSession().saveDocument(withoutPrefixBlobDoc);
        getCoreSession().save();

        return withoutPrefixBlobDoc;
    }

    protected DocumentModel createFileDocument(boolean setMimeType)
            throws ClientException {
        DocumentModel fileDoc = getCoreSession().createDocumentModel("/",
                "testFile", "File");
        fileDoc.setProperty("dublincore", "title", "TestFile");

        Blob blob = new StringBlob("SOMEDUMMYDATA", null);
        blob.setFilename("test.pdf");
        if (setMimeType) {
            blob.setMimeType("application/pdf");
        }
        fileDoc.setProperty("file", "content", blob);

        fileDoc = getCoreSession().createDocument(fileDoc);

        getCoreSession().saveDocument(fileDoc);
        getCoreSession().save();

        return fileDoc;
    }

    protected DocumentModel removeMainBlob(DocumentModel doc) throws ClientException {
        doc.setPropertyValue("file:content", null);
        doc = getCoreSession().saveDocument(doc);
        getCoreSession().save();
        return doc;
    }

    protected DocumentModel createWorkspace() throws ClientException {
        DocumentModel doc = getCoreSession().createDocumentModel("/",
                "testWorkspace", "Workspace");
        doc.setProperty("dublincore", "title", "TestWorkspace");
        Blob blob = new StringBlob("SOMEDUMMYDATA", null);
        blob.setFilename("test.pdf");
        // blob.setMimeType("application/pdf");
        doc.setProperty("file", "content", blob);
        doc = getCoreSession().createDocument(doc);
        getCoreSession().saveDocument(doc);
        getCoreSession().save();
        return doc;
    }

}
