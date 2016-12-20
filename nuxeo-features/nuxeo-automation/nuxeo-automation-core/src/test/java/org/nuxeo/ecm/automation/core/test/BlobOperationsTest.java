/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.operations.FetchContextDocument;
import org.nuxeo.ecm.automation.core.operations.RestoreDocumentInput;
import org.nuxeo.ecm.automation.core.operations.SetInputAsVar;
import org.nuxeo.ecm.automation.core.operations.blob.AttachBlob;
import org.nuxeo.ecm.automation.core.operations.blob.BlobToFile;
import org.nuxeo.ecm.automation.core.operations.blob.ConcatenatePDFs;
import org.nuxeo.ecm.automation.core.operations.blob.CreateBlob;
import org.nuxeo.ecm.automation.core.operations.blob.GetAllDocumentBlobs;
import org.nuxeo.ecm.automation.core.operations.blob.GetDocumentBlob;
import org.nuxeo.ecm.automation.core.operations.blob.GetDocumentBlobs;
import org.nuxeo.ecm.automation.core.operations.blob.SetBlobFileName;
import org.nuxeo.ecm.automation.core.operations.document.CreateDocument;
import org.nuxeo.ecm.automation.core.operations.document.FetchDocument;
import org.nuxeo.ecm.automation.core.operations.document.RemoveDocumentBlob;
import org.nuxeo.ecm.automation.core.operations.document.SetDocumentBlob;
import org.nuxeo.ecm.automation.core.scripting.Scripting;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.automation.core")
@LocalDeploy("org.nuxeo.ecm.automation.core:test-operations.xml")
// @RepositoryConfig(cleanup=Granularity.METHOD)
public class BlobOperationsTest {

    protected DocumentModel src;

    protected DocumentModel dst;

    @Inject
    AutomationService service;

    @Inject
    CoreSession session;

    @Before
    public void initRepo() throws Exception {
        session.removeChildren(session.getRootDocument().getRef());
        session.save();

        src = session.createDocumentModel("/", "src", "Workspace");
        src.setPropertyValue("dc:title", "Source");
        src = session.createDocument(src);
        session.save();
        src = session.getDocument(src.getRef());

        dst = session.createDocumentModel("/", "dst", "Workspace");
        dst.setPropertyValue("dc:title", "Destination");
        dst = session.createDocument(dst);
        session.save();
        dst = session.getDocument(dst.getRef());
    }

    @After
    public void clearRepo() throws Exception {
        Framework.getLocalService(EventService.class).waitForAsyncCompletion();
    }

    // ------ Tests comes here --------

    @Test
    public void testSetAndGetAndRemoveBlob() throws Exception {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);

        OperationChain chain = new OperationChain("testChain");
        chain.add(FetchContextDocument.ID);
        chain.add(CreateDocument.ID).set("type", "File").set("name", "file").set("properties", "dc:title=MyDoc");
        chain.add(SetDocumentBlob.ID).set("file", Blobs.createBlob("blob content"));
        chain.add(GetDocumentBlob.ID);
        // chain.add(Operations.BLOB_POST).set("url", Framework.createTempFile("",
        // suffix));

        Blob out = (Blob) service.run(ctx, chain);
        assertEquals("blob content", out.getString());

        // chain 2 is removing the blob created earlier
        chain = new OperationChain("testRemoveChain");
        chain.add(FetchDocument.ID).set("value", new PathRef("/src/file"));
        chain.add(RemoveDocumentBlob.ID);
        chain.add(GetDocumentBlob.ID);

        out = (Blob) service.run(ctx, chain);
        assertNotNull(out);
    }

    @Test
    public void testCreateAndAttachBlob() throws Exception {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);

        File file = Framework.createTempFile("nx-test-blob-", ".tmp");
        try {
            CreateBlob.skipProtocolCheck = true;
            FileUtils.writeFile(file, "blob content");
            OperationChain chain = new OperationChain("testChain");
            chain.add(FetchContextDocument.ID);
            chain.add(CreateDocument.ID).set("type", "File").set("name", "file").set("properties", "dc:title=MyDoc");
            chain.add(SetInputAsVar.ID).set("name", "doc");
            chain.add(CreateBlob.ID).set("file", file.toURI().toURL());
            chain.add(AttachBlob.ID).set("document", Scripting.newExpression("doc"));
            chain.add(RestoreDocumentInput.ID).set("name", "doc");
            chain.add(GetDocumentBlob.ID);
            Blob out = (Blob) service.run(ctx, chain);
            assertEquals("blob content", out.getString());
        } finally {
            CreateBlob.skipProtocolCheck = false;
            file.delete();
        }
    }

    @Test
    public void testSetAndGetAndRemoveBlobs() throws Exception {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);

        BlobList blobs = new BlobList();
        blobs.add(Blobs.createBlob("blob1"));
        blobs.add(Blobs.createBlob("blob2"));

        // chain 1 is creating a list of 2 blobs.
        OperationChain chain = new OperationChain("testChain");
        chain.add(FetchContextDocument.ID);
        chain.add(CreateDocument.ID).set("type", "File").set("name", "file").set("properties", "dc:title=MyDoc");
        chain.add(SetDocumentBlob.ID).set("xpath", "files:files").set("file", Blobs.createBlob("blob1"));
        chain.add(SetDocumentBlob.ID).set("xpath", "files:files").set("file", Blobs.createBlob("blob2"));
        chain.add(GetDocumentBlobs.ID);

        BlobList out = (BlobList) service.run(ctx, chain);
        assertEquals(2, out.size());
        assertEquals("blob1", out.get(0).getString());
        assertEquals("blob2", out.get(1).getString());

        // chain 2 is removing the blobs we constructed earlier.
        chain = new OperationChain("testRemoveChain");
        chain.add(FetchDocument.ID).set("value", new PathRef("/src/file"));
        chain.add(RemoveDocumentBlob.ID).set("xpath", "files:files");
        chain.add(GetDocumentBlobs.ID);

        out = (BlobList) service.run(ctx, chain);
        assertEquals(0, out.size());
    }

    @Test
    public void testRemoveBlobFromList() throws Exception {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);

        BlobList blobs = new BlobList();
        blobs.add(Blobs.createBlob("blob1"));
        blobs.add(Blobs.createBlob("blob2"));

        // chain 1 is creating a list of 2 blobs.
        OperationChain chain = new OperationChain("testChain");
        chain.add(FetchContextDocument.ID);
        chain.add(CreateDocument.ID).set("type", "File").set("name", "file").set("properties", "dc:title=MyDoc");
        chain.add(SetDocumentBlob.ID).set("xpath", "files:files").set("file", Blobs.createBlob("blob1"));
        chain.add(SetDocumentBlob.ID).set("xpath", "files:files").set("file", Blobs.createBlob("blob2"));
        chain.add(SetDocumentBlob.ID).set("xpath", "files:files").set("file", Blobs.createBlob("blob3"));
        chain.add(GetDocumentBlobs.ID);

        BlobList out = (BlobList) service.run(ctx, chain);
        assertEquals(3, out.size());
        assertEquals("blob1", out.get(0).getString());
        assertEquals("blob2", out.get(1).getString());
        assertEquals("blob3", out.get(2).getString());

        // chain 2 is removing blob2.
        chain = new OperationChain("testRemoveChain");
        chain.add(FetchDocument.ID).set("value", new PathRef("/src/file"));
        chain.add(RemoveDocumentBlob.ID).set("xpath", "files:files/file[1]");
        chain.add(GetDocumentBlobs.ID);

        out = (BlobList) service.run(ctx, chain);
        assertEquals(2, out.size());
        assertEquals("blob1", out.get(0).getString());
        assertEquals("blob3", out.get(1).getString());
    }

    @Test
    public void testExportBlobToFile() throws Exception {
        File dir = File.createTempFile("automation-test-", ".tmp",
                org.apache.commons.io.FileUtils.getTempDirectory().getParentFile().getParentFile());
        dir.delete();
        dir.mkdirs();

        OperationContext ctx = new OperationContext(session);
        Blob blob = Blobs.createBlob("test");
        blob.setFilename("myblob");
        ctx.setInput(blob);

        OperationChain chain = new OperationChain("testChain");
        chain.add(BlobToFile.ID).set("directory", dir.getAbsolutePath()).set("prefix", "test-");
        Blob out = (Blob) service.run(ctx, chain);
        assertSame(blob, out);

        File file = new File(dir, "test-" + blob.getFilename());
        assertEquals(blob.getString(), FileUtils.readFile(file));

        file.delete();

        // test again but without prefix
        chain = new OperationChain("testChain");
        chain.add(BlobToFile.ID).set("directory", dir.getAbsolutePath());
        out = (Blob) service.run(ctx, chain);
        assertSame(blob, out);

        file = new File(dir, blob.getFilename());
        assertEquals(blob.getString(), FileUtils.readFile(file));
        file.delete();

        dir.delete();
    }

    @Test
    public void testFilenameModification() throws Exception {
        // create a file
        Blob blob = Blobs.createBlob("the blob content");
        blob.setFilename("initial_name.txt");
        DocumentModel file = session.createDocumentModel(src.getPathAsString(), "blobWithName", "File");
        file.setPropertyValue("dc:title", "The File");
        file.setPropertyValue("file:content", (Serializable) blob);
        file = session.createDocument(file);
        session.save();
        file = session.getDocument(file.getRef());
        blob = (Blob) file.getPropertyValue("file:content");
        assertEquals("the blob content", blob.getString());
        assertEquals("initial_name.txt", blob.getFilename());

        // execute the set blob name operation
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(file);
        OperationChain chain = new OperationChain("testChain");
        chain.add(FetchContextDocument.ID);
        chain.add(SetBlobFileName.ID).set("name", "modified_name.txt");
        service.run(ctx, chain);

        file = session.getDocument(file.getRef());
        blob = (Blob) file.getPropertyValue("file:content");
        assertEquals("the blob content", blob.getString());
        assertEquals("modified_name.txt", blob.getFilename());

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetAllDocumentBlobsOperation() throws Exception {
        // Create a file
        Blob mainFile = Blobs.createBlob("the blob content");
        // Create files list
        Map<String, Serializable> file = new HashMap<>();
        ArrayList<Map<String, Serializable>> files = new ArrayList<>();
        // Attach one file to the list
        File tmpFile = Framework.createTempFile("test", ".txt");
        FileUtils.writeFile(tmpFile, "Content");
        Blob blob = Blobs.createBlob(tmpFile);
        blob.setFilename("initial_name.txt");
        Framework.trackFile(tmpFile, blob);
        file.put("file", (Serializable) blob);
        files.add(file);
        // Create document
        DocumentModel docFile = session.createDocumentModel(src.getPathAsString(), "blobWithName", "File");
        // Attach files to document
        docFile.setPropertyValue("dc:title", "The File");
        docFile.setPropertyValue("file:content", (Serializable) mainFile);
        docFile.setPropertyValue("files:files", files);
        docFile = session.createDocument(docFile);
        session.save();
        // execute operation chain containing GetAllDocumentBlobs one
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(docFile);
        OperationChain chain = new OperationChain("testBlobsChain");
        chain.add(FetchContextDocument.ID);
        chain.add(GetAllDocumentBlobs.ID);
        files = (ArrayList<Map<String, Serializable>>) service.run(ctx, chain);
        assertEquals(files.size(), 2);
    }

    @Test
    public void testPDFMerge() throws Exception {
        // Fetch two files
        File pdfMerge1 = FileUtils.getResourceFileFromContext("pdfMerge1.pdf");
        File pdfMerge2 = FileUtils.getResourceFileFromContext("pdfMerge2.pdf");
        Blob pdf1 = Blobs.createBlob(pdfMerge1);
        Blob pdf2 = Blobs.createBlob(pdfMerge2);
        pdf1.setMimeType("application/pdf");
        pdf2.setMimeType("application/pdf");
        // Add them to list
        BlobList blobs = new BlobList();
        blobs.add(pdf1);
        blobs.add(pdf2);
        // Execute pdf merge operation
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(blobs);
        // Inject a blob into context
        ctx.put("blobToAppend", pdf1);
        Map<String, Object> params = new HashMap<>();
        params.put("filename", "pdfresult");
        // Put as parameter the context variable to get the context blob
        params.put("blob_to_append", "blobToAppend");
        Blob blob = (Blob) service.run(ctx, ConcatenatePDFs.ID, params);
        assertNotNull(blob);
        assertEquals("pdfresult", blob.getFilename());

        // Test failures

        // Test check on mimetype failure
        blobs.clear();
        pdf1.setMimeType("application/html");
        blobs.add(pdf1);
        blobs.add(pdf2);
        params.clear();
        params.put("filename", "pdfresult");
        ctx = new OperationContext(session);
        ctx.setInput(blobs);
        try {
            service.run(ctx, ConcatenatePDFs.ID, params);
            // Should fails before
            fail();
        } catch (OperationException e) {
            assertEquals("Blob pdfMerge1.pdf is not a PDF.", e.getCause().getMessage());
        }

        // Test check on context blob failure
        pdf1.setMimeType("application/pdf");
        blobs.clear();
        blobs.add(pdf1);
        blobs.add(pdf2);
        params.clear();
        params.put("filename", "pdfresult");
        params.put("blob_to_append", "blobToAppend");
        ctx = new OperationContext(session);
        ctx.setInput(blobs);
        // Inject a file into context for failing
        ctx.put("blobToAppend", pdfMerge1);
        try {
            service.run(ctx, ConcatenatePDFs.ID, params);
            // Should fails before
            fail();
        } catch (OperationException e) {
            assertNotNull("The blob to append from variable context: 'blobToAppend' is not a blob.",
                    e.getCause().getMessage());
        }
    }
    // TODO add post and file2pdf tests
}
