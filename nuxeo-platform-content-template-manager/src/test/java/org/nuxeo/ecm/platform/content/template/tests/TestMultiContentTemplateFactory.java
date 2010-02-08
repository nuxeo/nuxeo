package org.nuxeo.ecm.platform.content.template.tests;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;

public class TestMultiContentTemplateFactory extends ContentTemplateFactoryTestCase {

    @Override
    protected void initRepo() throws Exception {
        super.initRepo();
        deployContrib("org.nuxeo.ecm.platform.content.template.tests", "test-multi-content-template-contrib.xml");
    }
    
    public void createTestWorkspace() throws ClientException {
        DocumentModel root = session.getRootDocument();
        service.executeFactoryForType(root);
        DocumentModel firstDomain = session.getChildren(root.getRef()).get(0);
        DocumentModel wsRoot = session.getChildren(firstDomain.getRef(), "WorkspaceRoot").get(0);
        DocumentModel testWS = session.createDocumentModel(wsRoot.getPathAsString(), "TestWS", "Workspace");
        testWS.setProperty("dublincore", "title", "MyTestWorkspace");
        testWS.setProperty("dublincore", "source", "test");
        testWS = session.createDocument(testWS);
        session.save();
    }
    
    public void testWorkspaceHasNote() throws ClientException {
        createTestWorkspace();
    }
}
