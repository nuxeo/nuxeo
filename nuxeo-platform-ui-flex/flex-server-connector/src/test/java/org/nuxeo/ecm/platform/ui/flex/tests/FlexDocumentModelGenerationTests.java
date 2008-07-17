package org.nuxeo.ecm.platform.ui.flex.tests;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryOSGITestCase;
import org.nuxeo.ecm.flex.javadto.FlexDocumentModel;
import org.nuxeo.ecm.platform.ui.flex.mapping.DocumentModelTranslator;

public class FlexDocumentModelGenerationTests extends RepositoryOSGITestCase {

    private DocumentModel doc;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.platform.content.template");
        deployBundle("org.nuxeo.ecm.platform.types.api");

        openRepository();

    }

    private void createDocuments() throws Exception {
        DocumentModel wsRoot = coreSession.getDocument(new PathRef(
                "default-domain/workspaces"));

        DocumentModel ws = coreSession.createDocumentModel(
                wsRoot.getPathAsString(), "ws1", "Workspace");
        ws.setProperty("dublincore", "title", "test WS");
        ws = coreSession.createDocument(ws);

        doc = coreSession.createDocumentModel(ws.getPathAsString(), "file",
                "File");
        doc.setProperty("dublincore", "title", "MyDoc");
        doc.setProperty("dublincore", "coverage", "MyDocCoverage");
        doc = coreSession.createDocument(doc);
    }


    public void testGenFlexDocumentModel() throws Exception
    {
        createDocuments();

        FlexDocumentModel fdm = DocumentModelTranslator.toFlexType(doc);

        String title = (String) fdm.getProperty("dublincore", "title");
        assertEquals("MyDoc", title);

        String coverage = (String) fdm.getProperty("dublincore", "coverage");
        assertEquals("MyDocCoverage", coverage);

    }

    public void testGenFlexDocumentModelFromPrefetch() throws Exception
    {
        createDocuments();

        FlexDocumentModel fdm = DocumentModelTranslator.toFlexTypeFromPrefetch(doc);

        String title = (String) fdm.getProperty("dublincore", "title");
        assertEquals("MyDoc", title);

        String coverage = (String) fdm.getProperty("dublincore", "coverage");
        assertNull(coverage);

    }

}
