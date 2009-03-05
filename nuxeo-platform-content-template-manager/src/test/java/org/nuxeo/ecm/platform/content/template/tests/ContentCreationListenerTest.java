package org.nuxeo.ecm.platform.content.template.tests;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryOSGITestCase;

/**
*   Simple test class for ContentCreationListener
* @ JULIEN THIMONIER < jt@nuxeo.com >
**/
public class ContentCreationListenerTest extends RepositoryOSGITestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.platform.content.template");
        deployBundle("org.nuxeo.ecm.relations");
        openRepository();
    }

    public void testContentCreationListener() throws Exception {
        DocumentModel root = getCoreSession().getRootDocument();
        DocumentModel model = getCoreSession().createDocumentModel(
                root.getPathAsString(), "mondomaine", "Domain");
        DocumentModel doc = getCoreSession().createDocument(model);
        getCoreSession().saveDocument(doc);
        assert (doc != null);

        DocumentModelList modelList = getCoreSession().getChildren(doc.getRef());

        // Check that 3 elements have been created on the new domain
        // (Section,Workspace and Templates)
        // This should be done by ContentCreationListener
        assert (modelList.size() == 3);
    }
}
