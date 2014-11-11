package org.nuxeo.opensocial.gadgets;

import static org.junit.Assert.*;

import java.net.URL;

import javax.ws.rs.core.EntityTag;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.platform.dublincore")
public class DocumentToEtagTest {

    @Inject
    CoreSession session;

    private DocumentModel doc2;
    private DocumentModel doc1;

    @Inject
    public DocumentToEtagTest(CoreSession session) throws ClientException {
        doc1 = createDocForFile("blob1", "testBlob1.txt",session);
        doc2 = createDocForFile("blob2", "testBlob2.txt",session);
    }

    @Test
    public void sameDocProducesSameEtag() throws Exception {

        EntityTag tag1 = GadgetDocument.getEntityTagForDocument(doc1);
        DocumentModel diffDoc = session.getDocument(new PathRef("/blob1"));
        EntityTag tag2 = GadgetDocument.getEntityTagForDocument(diffDoc);

        assertEquals(tag1.getValue(), tag2.getValue());
    }

    @Test
    public void differentDocProducesDifferentEtag() throws Exception {

        EntityTag tag1 = GadgetDocument.getEntityTagForDocument(doc1);
        EntityTag tag2 = GadgetDocument.getEntityTagForDocument(doc2);
        assertFalse(tag2.getValue()
                .equals(tag1.getValue()));

    }

    @Test
    public void changingDocChangesEtag() throws Exception {
        EntityTag tag1 = GadgetDocument.getEntityTagForDocument(doc1);

        doc1.setPropertyValue("dc:title", "new Title");
        session.saveDocument(doc1);
        session.save();
        doc1 = session.getDocument(new PathRef("/blob1"));


        EntityTag tag2 = GadgetDocument.getEntityTagForDocument(doc1);
        assertFalse(tag2.getValue()
                .equals(tag1.getValue()));

    }

    private DocumentModel createDocForFile(String name, String path, CoreSession session)
            throws ClientException {
        URL resource = getResource(path);

        DocumentModel doc = session.createDocumentModel("/", name, "File");
        // Updating known attributes (title, filename, content)
        doc.setProperty("dublincore", "title", path);
        doc.setProperty("file", "filename", path);
        doc.setProperty("file", "content",
                StreamingBlob.createFromURL(resource));

        // writing the new document to the repository
        doc = session.createDocument(doc);
        session.saveDocument(doc);
        session.save();
        return doc;

    }

    private static URL getResource(String resource) {
        return Thread.currentThread()
                .getContextClassLoader()
                .getResource(resource);
    }

}
