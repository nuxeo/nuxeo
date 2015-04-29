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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * Testing the mime type icon updater listener. This listener should update mime type of a blob when this one is dirty
 * (updated). When the blob is about is on file:content, it also updates the common:icon field setting the right icon
 * according to the mime type
 *
 * @author Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 * @author Benjamin Jalon <bjalon@nuxeo.com>
 */
@RunWith(FeaturesRunner.class)
@Features({ TransactionalFeature.class, CoreFeature.class })
@Deploy({ "org.nuxeo.ecm.platform.filemanager.api", //
        "org.nuxeo.ecm.platform.filemanager.core", //
        "org.nuxeo.ecm.platform.types.api", //
        "org.nuxeo.ecm.platform.types.core", //
})
@LocalDeploy({ "org.nuxeo.ecm.platform.filemanager.core.listener:OSGI-INF/filemanager-iconupdater-event-contrib.xml",
        "org.nuxeo.ecm.platform.filemanager.core.listener.test:OSGI-INF/core-type-contrib.xml" })
public class TestMimetypeIconUpdater {

    @Inject
    protected CoreSession coreSession;

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
        assertTrue((Long) file.getPropertyValue("common:size") > 0L);

        // removing blob
        removeMainBlob(file);
        icon = (String) file.getProperty("common", "icon");
        assertNotNull(icon);
        assertEquals("/icons/pdf.png", icon);
        assertEquals(0L, file.getPropertyValue("common:size"));
    }

    /**
     * Testing mime type update with a schema without prefix. https://jira.nuxeo.org/browse/NXP-3972 <a
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
     * Testing mime type update with a schema with prefix. https://jira.nuxeo.org/browse/NXP-3972 <a
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

    protected DocumentModel createWithoutPrefixBlobDocument(boolean setMimeType) throws ClientException {
        DocumentModel withoutPrefixBlobDoc = coreSession.createDocumentModel("/", "testFile", "WithoutPrefixDocument");
        withoutPrefixBlobDoc.setProperty("dublincore", "title", "TestFile");

        Blob blob = Blobs.createBlob("SOMEDUMMYDATA", null, null, "test.pdf");
        if (setMimeType) {
            blob.setMimeType("application/pdf");
        }
        withoutPrefixBlobDoc.setProperty("wihtoutpref", "blob", blob);

        withoutPrefixBlobDoc = coreSession.createDocument(withoutPrefixBlobDoc);

        coreSession.saveDocument(withoutPrefixBlobDoc);
        coreSession.save();

        return withoutPrefixBlobDoc;
    }

    protected DocumentModel createWithPrefixBlobDocument(boolean setMimeType) throws ClientException {
        DocumentModel withoutPrefixBlobDoc = coreSession.createDocumentModel("/", "testFile", "SimpleBlobDocument");
        withoutPrefixBlobDoc.setProperty("dublincore", "title", "TestFile");

        Blob blob = Blobs.createBlob("SOMEDUMMYDATA", null, null, "test.pdf");
        if (setMimeType) {
            blob.setMimeType("application/pdf");
        }
        withoutPrefixBlobDoc.setProperty("simpleblob", "blob", blob);

        withoutPrefixBlobDoc = coreSession.createDocument(withoutPrefixBlobDoc);

        coreSession.saveDocument(withoutPrefixBlobDoc);
        coreSession.save();

        return withoutPrefixBlobDoc;
    }

    protected DocumentModel createFileDocument(boolean setMimeType) throws ClientException {
        DocumentModel fileDoc = coreSession.createDocumentModel("/", "testFile", "File");
        fileDoc.setProperty("dublincore", "title", "TestFile");

        Blob blob = Blobs.createBlob("SOMEDUMMYDATA", null, null, "test.pdf");
        if (setMimeType) {
            blob.setMimeType("application/pdf");
        }
        fileDoc.setProperty("file", "content", blob);

        fileDoc = coreSession.createDocument(fileDoc);

        coreSession.saveDocument(fileDoc);
        coreSession.save();

        return fileDoc;
    }

    protected DocumentModel removeMainBlob(DocumentModel doc) throws ClientException {
        doc.setPropertyValue("file:content", null);
        doc = coreSession.saveDocument(doc);
        coreSession.save();
        return doc;
    }

    protected DocumentModel createWorkspace() throws ClientException {
        DocumentModel doc = coreSession.createDocumentModel("/", "testWorkspace", "Workspace");
        doc.setProperty("dublincore", "title", "TestWorkspace");
        Blob blob = Blobs.createBlob("SOMEDUMMYDATA", null, null, "test.pdf");
        // blob.setMimeType("application/pdf");
        doc.setProperty("file", "content", blob);
        doc = coreSession.createDocument(doc);
        coreSession.saveDocument(doc);
        coreSession.save();
        return doc;
    }

}
