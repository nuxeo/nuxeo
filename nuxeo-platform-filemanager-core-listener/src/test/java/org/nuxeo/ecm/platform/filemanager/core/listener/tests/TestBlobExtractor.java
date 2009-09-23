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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.SchemaManagerImpl;
import org.nuxeo.ecm.core.schema.TypeService;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.filemanager.core.listener.BlobExtractor;
import org.nuxeo.runtime.api.Framework;

public class TestBlobExtractor extends SQLRepositoryTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.ecm.platform.filemanager.core.listener.test",
                "OSGI-INF/core-type-contrib.xml");
        openSession();
        typeMgr = getTypeManager();
    }

    @Override
    public void tearDown() throws Exception {
        closeSession();
        super.tearDown();
    }

    public void testCaching() throws Exception {

        BlobExtractor bec = new BlobExtractor();

        Map<String, List<String>> paths = bec.getBlobFieldPathForDocumentType("NoBlobDocument");
        assertEquals(0, paths.size());

        paths = bec.getBlobFieldPathForDocumentType("SimpleBlobDocument");
        assertEquals(1, paths.size());
        assertEquals("simpleblob", paths.keySet().toArray()[0]);
        assertEquals(1, paths.get("simpleblob").size());
        assertEquals("/blob", paths.get("simpleblob").get(0));

        paths = bec.getBlobFieldPathForDocumentType("WithoutPrefixDocument");
        assertEquals(1, paths.size());
        assertEquals("wihtoutpref", paths.keySet().toArray()[0]);
        assertEquals(1, paths.get("wihtoutpref").size());
        assertEquals("/blob", paths.get("wihtoutpref").get(0));

        paths = bec.getBlobFieldPathForDocumentType("BlobInListDocument");
        assertEquals(1, paths.size());
        assertEquals("blobinlist", paths.keySet().toArray()[0]);
        assertEquals(1, paths.get("blobinlist").size());
        assertEquals("/files/*/file", paths.get("blobinlist").get(0));

    }

    public void testGetBlobsFromDocumentModelNoBlob() throws Exception {
        deployBundle("org.nuxeo.ecm.platform.filemanager.core.listener.test");
        BlobExtractor bec = new BlobExtractor();

        DocumentModel noBlob = session.createDocumentModel("/", "testNoBlob",
                "NoBlobDocument");
        noBlob.setProperty("dublincore", "title", "NoBlobDocument");
        noBlob = session.createDocument(noBlob);
        session.saveDocument(noBlob);

        session.save();

        List<Property> blobProperties = bec.getBlobsProperties(noBlob);
        assertEquals(0, blobProperties.size());

    }

    public void testGetBlobsFromDocumentModelSimpleBlob() throws Exception {
        deployBundle("org.nuxeo.ecm.platform.filemanager.core.listener.test");
        BlobExtractor bec = new BlobExtractor();

        DocumentModel simpleBlob = session.createDocumentModel("/",
                "testSimpleBlob", "SimpleBlobDocument");
        simpleBlob.setProperty("dublincore", "title", "SimpleBlobDocument");
        simpleBlob.setProperty("simpleblob", "blob", createTestBlob(false,
                "test.pdf"));

        simpleBlob = session.createDocument(simpleBlob);
        session.saveDocument(simpleBlob);
        session.save();

        // END INITIALIZATION

        List<Property> blobs = bec.getBlobsProperties(simpleBlob);
        assertEquals(1, blobs.size());
        Blob blob = (Blob) blobs.get(0).getValue();
        assertEquals("test.pdf", blob.getFilename());
    }

    public void testGetBlobsFromDocumentModelSimpleBlobWithoutPrefix()
            throws Exception {
        deployBundle("org.nuxeo.ecm.platform.filemanager.core.listener.test");
        BlobExtractor bec = new BlobExtractor();

        DocumentModel simpleBlob = session.createDocumentModel("/",
                "testSimpleBlob", "WithoutPrefixDocument");
        simpleBlob.setProperty("dublincore", "title", "WithoutPrefixDocument");
        simpleBlob.setProperty("wihtoutpref", "blob", createTestBlob(false,
                "test.pdf"));

        simpleBlob = session.createDocument(simpleBlob);
        session.saveDocument(simpleBlob);
        session.save();

        // END INITIALIZATION

        List<Property> blobs = bec.getBlobsProperties(simpleBlob);
        assertEquals(1, blobs.size());
        Blob blob = (Blob) blobs.get(0).getValue();
        assertEquals("test.pdf", blob.getFilename());
    }

    @SuppressWarnings("unchecked")
    public void testGetBlobsFromBlobInListDocument() throws Exception {
        deployBundle("org.nuxeo.ecm.platform.filemanager.core.listener.test");
        BlobExtractor bec = new BlobExtractor();

        DocumentModel blobInListEmpty = session.createDocumentModel("/",
                "testBlobInListDocumentEmpty", "BlobInListDocument");

        DocumentModel blobInListWithBlobs = session.createDocumentModel("/",
                "testBlobInListDocument1", "BlobInListDocument");
        blobInListWithBlobs.setProperty("dublincore", "title",
                "BlobInListDocument");
        Collection files = (Collection) blobInListWithBlobs.getProperty(
                "blobinlist", "files");

        HashMap<String, Object> blob1Map = new HashMap<String, Object>(2);
        blob1Map.put("file", createTestBlob(false, "test1.pdf"));
        blob1Map.put("filename", "test1.pdf");

        HashMap<String, Object> blob2Map = new HashMap<String, Object>(2);
        blob2Map.put("file", createTestBlob(false, "test2.pdf"));
        blob2Map.put("filename", "test2.pdf");

        files.add(blob1Map);
        files.add(blob2Map);
        blobInListWithBlobs.setProperty("blobinlist", "files", files);

        blobInListWithBlobs = session.createDocument(blobInListWithBlobs);
        session.saveDocument(blobInListWithBlobs);
        session.save();

        // END INITIALIZATION

        List<Property> blobs = bec.getBlobsProperties(blobInListEmpty);
        assertEquals(0, blobs.size());

        blobs = bec.getBlobsProperties(blobInListWithBlobs);
        assertEquals(2, blobs.size());
        Blob blob = (Blob) blobs.get(0).getValue();
        assertEquals("test1.pdf", blob.getFilename());
        blob = (Blob) blobs.get(1).getValue();
        assertEquals("test2.pdf", blob.getFilename());

    }

    protected Blob createTestBlob(boolean setMimeType, String filename) {
        Blob blob = new StringBlob("SOMEDUMMYDATA");
        blob.setFilename(filename);
        if (setMimeType) {
            blob.setMimeType("application/pdf");
        }
        return blob;

    }

    protected SchemaManager typeMgr;

    public static SchemaManagerImpl getTypeManager() {
        return (SchemaManagerImpl) getTypeService().getTypeManager();
    }

    public static TypeService getTypeService() {
        return (TypeService) Framework.getRuntime().getComponent(
                TypeService.NAME);
    }

}
