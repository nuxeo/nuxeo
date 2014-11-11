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

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;

/**
 * Testing the mime type icon updater listener. This listener should update mime
 * type of a blob when this one is dirty (updated). When the blob is about is on
 * file:content, it also updates the common:icon field setting the right icon
 * according to the mime type
 *
 * @author Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 * @author Benjamin Jalon <bjalon@nuxeo.com>
 */
public class TestMimetypeIconUpdater extends SQLRepositoryTestCase {

    @Override
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

    /**
     * Testing a mime type and icon update (only done on file:content)
     *
     * @throws Exception
     */
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
    }

    /**
     * Testing mime type update with a schema without prefix.
     * https://jira.nuxeo.org/browse/NXP-3972 <a
     * href="https://jira.nuxeo.org/browse/NXP-3972">NXP-3972</a>
     *
     * @throws Exception
     */
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
    public void testMimeTypeUpdaterWithPrefix() throws Exception {
        DocumentModel doc = createWithPrefixBlobDocument(false);
        Blob blob = (Blob) doc.getProperty("simpleblob", "blob");
        assertNotNull(blob);
        String mt = blob.getMimeType();
        assertNotNull(mt);
        assertEquals("application/pdf", mt);
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

}
