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

import org.apache.tools.ant.filters.StringInputStream;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.ui.web.tag.fn.LiveEditConstants;

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

    }

}
