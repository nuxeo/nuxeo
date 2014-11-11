package org.nuxeo.ecm.automation.core.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.automation.server.jaxrs.batch.BatchManager;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({ "org.nuxeo.ecm.automation.core", "org.nuxeo.ecm.automation.features",
        "org.nuxeo.ecm.automation.server", "org.nuxeo.ecm.platform.query.api" })
public class DnDOperationsTest {

    @Inject
    AutomationService service;

    @Inject
    CoreSession session;

    @Inject
    BatchManager batchManager;

    @Test
    public void testCreate() throws Exception {

        Blob source = new StringBlob("YoMan");
        source.setFilename("Test.txt");
        source.setMimeType("text/plain");

        BlobList blobs = new BlobList(source);

        DocumentModel file = session.createDocumentModel("/", "file", "File");
        file.setPropertyValue("dc:title", "MyFile");
        file = session.createDocument(file);

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(blobs);

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("currentDocument", file.getId());

        ctx.putAll(params);

        // run first operation to update main blob
        service.run(ctx, "VersionAndAttachFile");

        file = session.getDocument(file.getRef());
        assertEquals("0.1+", file.getVersionLabel());
        BlobHolder bh = file.getAdapter(BlobHolder.class);

        assertNotNull(bh);
        assertNotNull(bh.getBlob());
        assertEquals("YoMan", bh.getBlob().getString());

        // prepare blobs attachements on the Files

        Blob source2 = new StringBlob("YoMan2");
        source2.setFilename("Test2.txt");
        source2.setMimeType("text/plain");

        Blob source3 = new StringBlob("YoMan3");
        source3.setFilename("Test3.txt");
        source3.setMimeType("text/plain");

        blobs = new BlobList();
        blobs.add(source2);
        blobs.add(source3);

        ctx = new OperationContext(session);
        ctx.setInput(blobs);

        params = new HashMap<String, Object>();
        params.put("currentDocument", file.getId());
        ctx.putAll(params);

        // run second operation to add blobs
        service.run(ctx, "VersionAndAttachFiles");

        file = session.getDocument(file.getRef());
        assertEquals("0.2+", file.getVersionLabel());
        bh = file.getAdapter(BlobHolder.class);

        assertNotNull(bh);
        assertNotNull(bh.getBlob());
        assertEquals("YoMan", bh.getBlob().getString());
        assertEquals("YoMan2", bh.getBlobs().get(0).getString());
        assertEquals("YoMan3", bh.getBlobs().get(1).getString());

    }
}
