/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.operations.blob.AttachBlob;
import org.nuxeo.ecm.automation.core.operations.document.LockDocument;
import org.nuxeo.ecm.automation.core.operations.document.SetDocumentProperty;
import org.nuxeo.ecm.automation.core.scripting.Scripting;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRefList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.impl.DocumentRefListImpl;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.automation.core")
// For version label info
@Deploy("org.nuxeo.ecm.automation.core:test-operations.xml")
@RepositoryConfig(cleanup = Granularity.METHOD)
public class IterableOperationsTest {

    protected DocumentModel src;

    protected DocumentModel dst;

    @Inject
    AutomationService service;

    @Inject
    CoreSession session;

    @Before
    public void initRepo() throws Exception {
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

        Framework.getService(EventService.class).waitForAsyncCompletion();
    }

    protected DocumentModel createFolder(String name, String title) throws Exception {
        DocumentModel doc = session.createDocumentModel("/src", name, "Folder");
        doc.setPropertyValue("dc:title", title);
        doc = session.createDocument(doc);
        session.save();
        return session.getDocument(doc.getRef());
    }

    protected DocumentModel createFile(DocumentModel parent, String name, String title) throws Exception {
        DocumentModel doc = session.createDocumentModel(parent.getPathAsString(), name, "File");
        doc.setPropertyValue("dc:title", title);
        doc = session.createDocument(doc);
        session.save();
        return session.getDocument(doc.getRef());
    }

    // ------ Tests comes here --------

    /**
     * Test if iterable operation methods work
     */
    @Test
    public void testChain1() throws Exception {
        OperationContext ctx = new OperationContext(session);
        DocumentModelListImpl docs = new DocumentModelListImpl();
        docs.add(src);
        docs.add(dst);
        ctx.setInput(docs);

        OperationChain chain = new OperationChain("testChain");
        chain.add(SetDocumentProperty.ID).set("xpath", "dc:description").set("value", "mydesc");

        DocumentModelList out = (DocumentModelList) service.run(ctx, chain);
        assertEquals(2, out.size());
        assertEquals("mydesc", out.get(0).getPropertyValue("dc:description"));
        assertEquals("mydesc", out.get(1).getPropertyValue("dc:description"));
    }

    /**
     * The same as before but use doc ref as input
     *
     * @throws Exception
     */
    @Test
    public void testChain2() throws Exception {
        OperationContext ctx = new OperationContext(session);
        DocumentRefList docs = new DocumentRefListImpl();
        docs.add(src.getRef());
        docs.add(dst.getRef());
        ctx.setInput(docs);

        OperationChain chain = new OperationChain("testChain");
        chain.add(SetDocumentProperty.ID).set("xpath", "dc:description").set("value", "mydesc");

        DocumentModelList out = (DocumentModelList) service.run(ctx, chain);
        assertEquals(2, out.size());
        assertEquals("mydesc", out.get(0).getPropertyValue("dc:description"));
        assertEquals("mydesc", out.get(1).getPropertyValue("dc:description"));
    }

    /**
     * lock documents passed as a list of docrefs
     *
     * @throws Exception
     */
    @Test
    public void testChain3() throws Exception {
        DocumentModel root = createFolder("test3", "test 3");
        DocumentModel f1 = createFile(root, "file1", "File 1");
        DocumentModel f2 = createFile(root, "file2", "File 2");
        Framework.getService(EventService.class).waitForAsyncCompletion();

        OperationContext ctx = new OperationContext(session);
        DocumentRefList docs = new DocumentRefListImpl();
        docs.add(f1.getRef());
        docs.add(f2.getRef());
        ctx.setInput(docs);

        OperationChain chain = new OperationChain("testChain");
        chain.add(LockDocument.ID);

        DocumentModelList out = (DocumentModelList) service.run(ctx, chain);

        assertEquals(2, out.size());
        assertEquals(SecurityConstants.ADMINISTRATOR, out.get(0).getLockInfo().getOwner());
        assertEquals(SecurityConstants.ADMINISTRATOR, out.get(1).getLockInfo().getOwner());
    }

    /**
     * test that input context variable is pointing to the current iterated object and not to the list object.
     *
     * @throws Exception
     */
    @Test
    public void testChain4() throws Exception {
        DocumentModel root = createFolder("test4", "Parent Folder");
        DocumentModel f1 = createFile(root, "file1", "File 1");
        DocumentModel f2 = createFile(root, "file2", "File 2");
        Framework.getService(EventService.class).waitForAsyncCompletion();

        OperationContext ctx = new OperationContext(session);
        DocumentModelList docs = new DocumentModelListImpl();
        docs.add(f1);
        docs.add(f2);
        ctx.setInput(docs);

        OperationChain chain = new OperationChain("testChain");
        chain.add(SetDocumentProperty.ID).set("xpath", "dc:description").set("value",
                Scripting.newExpression("Document.getParent()['dc:title']"));

        DocumentModelList out = (DocumentModelList) service.run(ctx, chain);

        assertEquals(2, out.size());
        assertEquals("Parent Folder", out.get(0).getPropertyValue("dc:description"));
        assertEquals("Parent Folder", out.get(0).getPropertyValue("dc:description"));
    }

    @Test
    public void testChain5() throws Exception {
        DocumentModel f = createFile(src, "file5", "the file5");
        Framework.getService(EventService.class).waitForAsyncCompletion();

        BlobList blobs = new BlobList();
        Blob b1 = Blobs.createBlob("the content 1");
        Blob b2 = Blobs.createBlob("the content 2");
        blobs.add(b1);
        blobs.add(b2);
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(blobs);

        OperationChain chain = new OperationChain("testChain");
        chain.add(AttachBlob.ID).set("document", f.getRef());
        BlobList out = (BlobList) service.run(ctx, chain);

        assertEquals(2, out.size());
        // only the last blob is set since it overwrite the previous blob
        assertEquals("the content 2", ((Blob) f.getPropertyValue("file:content")).getString());

        // same but use the xpath for the files schemas to append both blobs.
        chain = new OperationChain("testChain");
        chain.add(AttachBlob.ID).set("document", f.getRef()).set("xpath", "files:files");
        out = (BlobList) service.run(ctx, chain);

        assertEquals(2, out.size());
        // both blobs are set in files:files
        Object o = f.getPropertyValue("files:files/file[0]/file");
        assertNotNull(o);
        Blob r1 = (Blob) f.getPropertyValue("files:files/file[0]/file");
        Blob r2 = (Blob) f.getPropertyValue("files:files/file[1]/file");
        assertEquals("the content 1", r1.getString());
        assertEquals("the content 2", r2.getString());
    }

}
