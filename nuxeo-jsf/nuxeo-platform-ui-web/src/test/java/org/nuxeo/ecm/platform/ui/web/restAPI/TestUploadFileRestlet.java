/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.platform.ui.web.restAPI;

import java.util.List;

import org.apache.tools.ant.filters.StringInputStream;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.ui.web.tag.fn.LiveEditConstants;
import org.nuxeo.runtime.AbstractRuntimeService;

/**
 * Unit testing the save operation of upload file restlet
 *
 * @author Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 *
 */
public class TestUploadFileRestlet extends SQLRepositoryTestCase implements
        LiveEditConstants {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.platform.versioning");
        deployBundle("org.nuxeo.ecm.platform.ui");
        openSession();
    }

    /**
     * Unit test of the upload file restlet.
     */
    @SuppressWarnings("serial")
    public void testUploadRestletSave() throws Exception {
        // create a empty File document
        DocumentModel doc = session.createDocumentModel("/", "myFile", "File");
        doc = session.createDocument(doc);
        session.save();
        doc = session.getDocument(doc.getRef());
        assertNull(
                "At the begining the file document shouldn't have any content",
                doc.getProperty("file", "content"));

        // saving the current version to be compared later
        Long major = (Long) doc.getPropertyValue("uid:major_version");
        Long minor = (Long) doc.getPropertyValue("uid:minor_version");

        // call the save of uploadfile restlet
        UploadFileRestlet restlet = new UploadFileRestlet() {
            @Override
            protected CoreSession getDocumentManager() {
                return session;
            }
        };
        String expectedFileName = "myfile.txt";
        String blobPropertyName = DEFAULT_SCHEMA + ":" + DEFAULT_BLOB_FIELD;
        String filenamePropertyName = DEFAULT_SCHEMA + ":"
                + DEFAULT_FILENAME_FIELD;
        restlet.saveFileToDocument(expectedFileName, doc, blobPropertyName,
                filenamePropertyName, new StringInputStream(
                        "Content of the file."));

        // check that the upload has been done correctly
        doc = session.getDocument(doc.getRef());
        assertEquals("The file name is", expectedFileName, doc.getProperty(
                DEFAULT_SCHEMA, DEFAULT_FILENAME_FIELD));
        assertEquals(
                "The content of the file is",
                "Content of the file.",
                ((Blob) doc.getProperty(DEFAULT_SCHEMA, DEFAULT_BLOB_FIELD)).getString());

        // checking that no version has been created
        List<DocumentModel> versions = session.getVersions(doc.getRef());
        assertTrue("Should have at least one version snapshotted",
                versions == null || versions.size() <= 0);
        doc = session.getDocument(doc.getRef());
        assertEquals("The major version shouldn't have been incremented",
                major, doc.getPropertyValue("uid:major_version"));
        assertEquals("The minor version shoudln't have been incremented",
                minor, doc.getPropertyValue("uid:minor_version"));

    }

    /**
     * Unit testing autoversioning of the upload file restlet: minor increment
     */
    @SuppressWarnings("serial")
    public void testUploadRestletSaveWithAutoIncr() throws Exception {
        // mock property setting
        ((AbstractRuntimeService) runtime).setProperty("lived.autoversioning",
                "minor");

        // create a empty File document
        DocumentModel doc = session.createDocumentModel("/", "myFile", "File");
        doc = session.createDocument(doc);
        session.save();
        doc = session.getDocument(doc.getRef());
        assertNull(
                "At the begining the file document shouldn't have any content",
                doc.getProperty("file", "content"));

        // saving the current version to be compared later
        Long major = (Long) doc.getPropertyValue("uid:major_version");
        Long minor = (Long) doc.getPropertyValue("uid:minor_version");

        // call the save of uploadfile restlet
        UploadFileRestlet restlet = new UploadFileRestlet() {
            @Override
            protected CoreSession getDocumentManager() {
                return session;
            }
        };
        String expectedFileName = "myfile.txt";
        String blobPropertyName = DEFAULT_SCHEMA + ":" + DEFAULT_BLOB_FIELD;
        String filenamePropertyName = DEFAULT_SCHEMA + ":"
                + DEFAULT_FILENAME_FIELD;
        restlet.saveFileToDocument(expectedFileName, doc, blobPropertyName,
                filenamePropertyName, new StringInputStream(
                        "Content of the file."));

        // check that the upload has been done correctly
        doc = session.getDocument(doc.getRef());
        assertEquals("The file name is", expectedFileName, doc.getProperty(
                DEFAULT_SCHEMA, DEFAULT_FILENAME_FIELD));
        assertEquals(
                "The content of the file is",
                "Content of the file.",
                ((Blob) doc.getProperty(DEFAULT_SCHEMA, DEFAULT_BLOB_FIELD)).getString());

        // checking that version has been created and incremented
        List<DocumentModel> versions = session.getVersions(doc.getRef());
        assertTrue("Should have at least one version snapshotted",
                versions != null && versions.size() == 1);
        doc = session.getDocument(doc.getRef());
        assertEquals("The major version shouldn't have been incremented",
                major, doc.getPropertyValue("uid:major_version"));
        assertEquals("The minor version should have been incremented",
                new Long(minor + 1), doc.getPropertyValue("uid:minor_version"));

    }

    /**
     * Unit testing autoversioning of the upload file restlet: minor increment
     * according to the document life cycle state: if the option minor increment
     * is in the list of rules AND if theses rules doesn't contain the no
     * increment options (from the extension point
     * org.nuxeo.ecm.platform.versioning.service.VersioningService-rules)
     *
     */
    @SuppressWarnings("serial")
    public void testUploadRestletSaveWithAutoIncrAndLifeCycle()
            throws Exception {
        // mock property setting
        ((AbstractRuntimeService) runtime).setProperty("lived.autoversioning",
                "minor_on_rules");

        // create a empty File document
        DocumentModel doc = session.createDocumentModel("/", "myFile", "File");
        doc = session.createDocument(doc);
        session.save();
        doc = session.getDocument(doc.getRef());
        assertNull(
                "At the begining the file document shouldn't have any content",
                doc.getProperty("file", "content"));

        // saving the current version to be compared later
        Long major = (Long) doc.getPropertyValue("uid:major_version");
        Long minor = (Long) doc.getPropertyValue("uid:minor_version");

        // call the save of uploadfile restlet
        UploadFileRestlet restlet = new UploadFileRestlet() {
            @Override
            protected CoreSession getDocumentManager() {
                return session;
            }
        };
        String expectedFileName = "myfile.txt";
        String blobPropertyName = DEFAULT_SCHEMA + ":" + DEFAULT_BLOB_FIELD;
        String filenamePropertyName = DEFAULT_SCHEMA + ":"
                + DEFAULT_FILENAME_FIELD;
        restlet.saveFileToDocument(expectedFileName, doc, blobPropertyName,
                filenamePropertyName, new StringInputStream(
                        "Content of the file."));

        // check that the upload has been done correctly
        doc = session.getDocument(doc.getRef());
        assertEquals("The file name is", expectedFileName, doc.getProperty(
                DEFAULT_SCHEMA, DEFAULT_FILENAME_FIELD));
        assertEquals(
                "The content of the file is",
                "Content of the file.",
                ((Blob) doc.getProperty(DEFAULT_SCHEMA, DEFAULT_BLOB_FIELD)).getString());

        // checking that version has been created and incremented
        List<DocumentModel> versions = session.getVersions(doc.getRef());
        assertTrue("Should have one version snapshotted", versions != null
                && versions.size() == 0);
        doc = session.getDocument(doc.getRef());
        assertEquals("The major version shouldn't have been incremented",
                major, doc.getPropertyValue("uid:major_version"));
        assertEquals("The minor version shouldn't have been incremented",
                minor, doc.getPropertyValue("uid:minor_version"));

        // Testing now with another life cycle state: valid
        session.followTransition(doc.getRef(), "approve");
        session.save();
        doc = session.getDocument(doc.getRef());
        assertEquals("The document should be in approved state", "approved",
                doc.getCurrentLifeCycleState());

        restlet.saveFileToDocument(expectedFileName, doc, blobPropertyName,
                filenamePropertyName, new StringInputStream(
                        "Another Content of the file"));
        // shouldn't have change as there is no rules version incr for the
        // approved state, same checks as before
        versions = session.getVersions(doc.getRef());
        assertTrue(
                "Should have one (more) version snapshotted as it is in approved state.",
                versions != null && versions.size() == 1);
        doc = session.getDocument(doc.getRef());
        assertEquals(
                "The major version shouldn't have been incremented as the document is in approved state",
                major, doc.getPropertyValue("uid:major_version"));
        assertEquals(
                "The minor version should have been incremented as the document is in approved state",
                new Long(minor + 1), doc.getPropertyValue("uid:minor_version"));

        // The document should be back to project,
        // it should not increment as the rule noincrement doesn't exist
        doc = session.getDocument(doc.getRef());
        assertEquals("The document should be in project state", "project",
                doc.getCurrentLifeCycleState());

        restlet.saveFileToDocument(expectedFileName, doc, blobPropertyName,
                filenamePropertyName, new StringInputStream(
                        "Another Content of the file again"));
        // shouldn't have change as there is no rules version incr for the
        // approved state, same checks as before
        versions = session.getVersions(doc.getRef());
        assertTrue(
                "Should still have one version snapshotted as the document is in project state",
                versions != null && versions.size() == 1);
        doc = session.getDocument(doc.getRef());
        assertEquals("The major version shouldn't have been incremented",
                major, doc.getPropertyValue("uid:major_version"));
        assertEquals(
                "The minor version shouldn't have been incremented as the document is in project state",
                new Long(minor + 1), doc.getPropertyValue("uid:minor_version"));

    }

}
