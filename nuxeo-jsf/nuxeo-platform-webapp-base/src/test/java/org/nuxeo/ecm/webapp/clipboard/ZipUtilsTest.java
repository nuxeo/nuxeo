/*
 * (C) Copyright 2006-2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.webapp.clipboard;

import java.io.File;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipFile;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.runtime.api.Framework;

public class ZipUtilsTest extends SQLRepositoryTestCase {

    List<DocumentModel> documents;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        openSession();
        documents = new LinkedList<DocumentModel>();
        DocumentModel parent = new DocumentModelImpl("/", "parent", "Folder");
        parent = session.createDocument(parent);
        parent.setPropertyValue("dc:title", "parent");
        parent = session.saveDocument(parent);
        DocumentModel file = new DocumentModelImpl("/parent", "éèà", "File");
        file = session.createDocument(file);
        Serializable strBlob = new StringBlob("ééà");
        file.setPropertyValue("file:content", strBlob);
        file.setPropertyValue("file:filename", "éèà");
        file.setPropertyValue("dc:title", "éèà");
        file = session.saveDocument(file);
        documents.add(parent);
    }

    public void testZippingSummary() throws Exception {
        DocumentListZipExporter zipExporter = new DocumentListZipExporter();
        File file = zipExporter.exportWorklistAsZip(documents, session, true);
        assertNotNull(file);
        ZipFile zipFile = new ZipFile(file);
        assertNotNull(zipFile.getEntry("parent/éèà"));
        Framework.getProperties().setProperty(
                DocumentListZipExporter.ZIP_ENTRY_ENCODING_PROPERTY,
                DocumentListZipExporter.ZIP_ENTRY_ENCODING_OPTIONS.ascii.name());
        file = zipExporter.exportWorklistAsZip(documents, session, true);
        assertNotNull(file);
        zipFile = new ZipFile(file);
        assertNotNull(zipFile.getEntry("parent/eea"));
    }
}
