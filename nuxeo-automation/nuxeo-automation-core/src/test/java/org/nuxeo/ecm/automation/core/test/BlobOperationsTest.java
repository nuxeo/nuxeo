/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import java.io.File;
import java.io.Serializable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.operations.FetchContextDocument;
import org.nuxeo.ecm.automation.core.operations.RestoreDocumentInput;
import org.nuxeo.ecm.automation.core.operations.SetInputAsVar;
import org.nuxeo.ecm.automation.core.operations.blob.AttachBlob;
import org.nuxeo.ecm.automation.core.operations.blob.BlobToFile;
import org.nuxeo.ecm.automation.core.operations.blob.CreateBlob;
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
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

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

    // ------ Tests comes here --------

    @Test
    public void testSetAndGetAndRemoveBlob() throws Exception {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);

        OperationChain chain = new OperationChain("testChain");
        chain.add(FetchContextDocument.ID);
        chain.add(CreateDocument.ID).set("type", "File").set("name", "file").set(
                "properties", "dc:title=MyDoc");
        chain.add(SetDocumentBlob.ID).set("file",
                new StringBlob("blob content"));
        chain.add(GetDocumentBlob.ID);
        // chain.add(Operations.BLOB_POST).set("url", File.createTempFile("",
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

        File file = File.createTempFile("nx-test-blob-", ".tmp");
        try {
            FileUtils.writeFile(file, "blob content");
            OperationChain chain = new OperationChain("testChain");
            chain.add(FetchContextDocument.ID);
            chain.add(CreateDocument.ID).set("type", "File").set("name", "file").set(
                    "properties", "dc:title=MyDoc");
            chain.add(SetInputAsVar.ID).set("name", "doc");
            chain.add(CreateBlob.ID).set("file", file.toURI().toURL());
            chain.add(AttachBlob.ID).set("document",
                    Scripting.newExpression("doc"));
            chain.add(RestoreDocumentInput.ID).set("name", "doc");
            chain.add(GetDocumentBlob.ID);
            Blob out = (Blob) service.run(ctx, chain);
            assertEquals("blob content", out.getString());
        } finally {
            file.delete();
        }
    }

    @Test
    public void testSetAndGetAndRemoveBlobs() throws Exception {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);

        BlobList blobs = new BlobList();
        blobs.add(new StringBlob("blob1"));
        blobs.add(new StringBlob("blob2"));

        // chain 1 is creating a list of 2 blobs.
        OperationChain chain = new OperationChain("testChain");
        chain.add(FetchContextDocument.ID);
        chain.add(CreateDocument.ID).set("type", "File").set("name", "file").set(
                "properties", "dc:title=MyDoc");
        chain.add(SetDocumentBlob.ID).set("xpath", "files:files").set("file",
                new StringBlob("blob1"));
        chain.add(SetDocumentBlob.ID).set("xpath", "files:files").set("file",
                new StringBlob("blob2"));
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
        blobs.add(new StringBlob("blob1"));
        blobs.add(new StringBlob("blob2"));

        // chain 1 is creating a list of 2 blobs.
        OperationChain chain = new OperationChain("testChain");
        chain.add(FetchContextDocument.ID);
        chain.add(CreateDocument.ID).set("type", "File").set("name", "file").set(
                "properties", "dc:title=MyDoc");
        chain.add(SetDocumentBlob.ID).set("xpath", "files:files").set("file",
                new StringBlob("blob1"));
        chain.add(SetDocumentBlob.ID).set("xpath", "files:files").set("file",
                new StringBlob("blob2"));
        chain.add(GetDocumentBlobs.ID);

        BlobList out = (BlobList) service.run(ctx, chain);
        assertEquals(2, out.size());
        assertEquals("blob1", out.get(0).getString());
        assertEquals("blob2", out.get(1).getString());

        // chain 2 is removing the blobs we constructed earlier.
        chain = new OperationChain("testRemoveChain");
        chain.add(FetchDocument.ID).set("value", new PathRef("/src/file"));
        chain.add(RemoveDocumentBlob.ID).set("xpath", "files:files/file[0]");
        chain.add(GetDocumentBlobs.ID);

        out = (BlobList) service.run(ctx, chain);
        assertEquals(1, out.size());
    }

    @Test
    public void testExportBlobToFile() throws Exception {
        File dir = File.createTempFile("autoamtion-test-", ".tmp");
        dir.delete();
        dir.mkdirs();

        OperationContext ctx = new OperationContext(session);
        Blob blob = new StringBlob("test", "text/plain");
        blob.setFilename("myblob");
        ctx.setInput(blob);

        OperationChain chain = new OperationChain("testChain");
        chain.add(BlobToFile.ID).set("directory", dir.getAbsolutePath()).set(
                "prefix", "test-");
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
        Blob blob = new StringBlob("the blob content");
        blob.setFilename("initial_name.txt");
        blob.setMimeType("text/plain");
        DocumentModel file = session.createDocumentModel(src.getPathAsString(), "blobWithName", "File");
        file.setPropertyValue("dc:title", "The File");
        file.setPropertyValue("file:content", (Serializable)blob);
        file = session.createDocument(file);
        session.save();
        file = session.getDocument(file.getRef());
        blob = (Blob)file.getPropertyValue("file:content");
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
        blob = (Blob)file.getPropertyValue("file:content");
        assertEquals("the blob content", blob.getString());
        assertEquals("modified_name.txt", blob.getFilename());

    }

    // TODO add post and file2pdf tests
}
