package org.nuxeo.ecm.platform.publisher.test;

import junit.framework.TestCase;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.remoting.marshaling.DefaultPublishedDocumentMarshaler;
import org.nuxeo.ecm.platform.publisher.remoting.marshaling.basic.BasicPublishedDocument;
import org.nuxeo.ecm.platform.publisher.remoting.marshaling.interfaces.PublishedDocumentMarshaler;
import org.nuxeo.ecm.platform.publisher.remoting.marshaling.interfaces.PublishingMarshalingException;

/**
 * 
 * Test simple marshaling
 * 
 * @author tiry
 * 
 */
public class TestMarshaling extends TestCase {

    public void testPublishedDocMarshaling()
            throws PublishingMarshalingException {
        PublishedDocument pubDoc = new BasicPublishedDocument(new IdRef("1"),
                "demo", "local", "1.0", "path", "parentPath", true);

        PublishedDocumentMarshaler marshaler = new DefaultPublishedDocumentMarshaler();

        String data = marshaler.marshalPublishedDocument(pubDoc);

        assertNotNull(data);
        System.out.print(data);

        PublishedDocument pubDoc2 = marshaler.unMarshalPublishedDocument(data);
        assertNotNull(pubDoc2);

        assertEquals(pubDoc.getSourceDocumentRef(),
                pubDoc2.getSourceDocumentRef());
        assertEquals(pubDoc.getSourceRepositoryName(),
                pubDoc2.getSourceRepositoryName());
        assertEquals(pubDoc.getSourceServer(), pubDoc2.getSourceServer());
        assertEquals(pubDoc.getSourceVersionLabel(),
                pubDoc2.getSourceVersionLabel());
        assertEquals(pubDoc.getPath(), pubDoc2.getPath());
        assertEquals(pubDoc.getParentPath(), pubDoc2.getParentPath());
        assertEquals(pubDoc.isPending(), pubDoc2.isPending());
    }

}
