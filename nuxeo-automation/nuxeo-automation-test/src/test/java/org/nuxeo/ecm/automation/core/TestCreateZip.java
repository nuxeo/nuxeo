package org.nuxeo.ecm.automation.core;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.operations.blob.CreateZip;
import org.nuxeo.ecm.automation.core.operations.blob.GetDocumentBlob;
import org.nuxeo.ecm.automation.core.operations.document.GetDocumentChildren;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(AutomationFeature.class)
public class TestCreateZip {

    @Inject CoreSession session;

    @Inject AutomationService as;

    @Test
    public void isServiceDeployed() {
        assertNotNull(as);
    }

    @Test public void canGetZip() throws Exception {
        DocumentModel ws1 = session.createDocumentModel("Workspace");
        ws1.setPathInfo("/", "ws1");
        ws1 = session.createDocument(ws1);
        DocumentModel doc2 = session.createDocumentModel("File");
        doc2.setPathInfo("/ws1", "doc2");
        doc2.setPropertyValue("file:content", new StringBlob("content doc2"));
        doc2 = session.createDocument(doc2);
        DocumentModel doc = session.createDocumentModel("File");
        doc.setPathInfo("/ws1", "doc1");
        doc.setPropertyValue("file:content", new StringBlob("content doc1"));
        doc = session.createDocument(doc);
        session.save();
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(ws1);
        OperationChain chain = new OperationChain("ZipWs");
        chain.add(GetDocumentChildren.ID);
        chain.add(GetDocumentBlob.ID);
        chain.add(CreateZip.ID).set("filename", "zip.zip");
        Blob zipBlob = (Blob) as.run(ctx, chain);

        assertNotNull(zipBlob);
        assertTrue("ZIP blob '" + zipBlob.getFilename() + "' is empty", zipBlob.getLength() > 0);
    }

}
