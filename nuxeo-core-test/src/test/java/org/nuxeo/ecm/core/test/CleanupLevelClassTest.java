package org.nuxeo.ecm.core.test;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;

import com.google.inject.Inject;

@RunWith(NuxeoCoreRunner.class)
public class CleanupLevelClassTest {
    @Inject CoreSession session;

    @Test
    public void firstTestToCreateADoc() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "default-domain", "Domain");
        doc.setProperty("dublincore", "title", "Default domain");
        doc = session.createDocument(doc);
        session.saveDocument(doc);
        session.save();
        assertTrue(session.exists(new PathRef("/default-domain")));
    }

    @Test
    public void docStillExists() throws Exception {
        assertTrue(session.exists(new PathRef("/default-domain")));
    }
}
