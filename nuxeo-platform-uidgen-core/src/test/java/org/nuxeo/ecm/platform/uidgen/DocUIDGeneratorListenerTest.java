package org.nuxeo.ecm.platform.uidgen;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryOSGITestCase;

/**
 * Simple test Case for DocUIDGeneratorListener
 * 
 * @author Julien Thimonier <jt@nuxeo.com>
 */
public class DocUIDGeneratorListenerTest extends RepositoryOSGITestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        openRepository();
        deployBundle("org.nuxeo.ecm.platform.uidgen.core.tests");
        // deployContrib("org.nuxeo.ecm.platform.uidgen.core",
        // "../test-classes/nxuidgenerator-bundle-contrib.xml");
    }

    protected DocumentModel createFileDocument() throws ClientException {

        DocumentModel fileDoc = getCoreSession().createDocumentModel("/",
                "testFile", "Note");

        fileDoc.setProperty("dublincore", "title", "TestFile");
        fileDoc.setProperty("dublincore", "description", "RAS");

        fileDoc = getCoreSession().createDocument(fileDoc);

        getCoreSession().saveDocument(fileDoc);
        getCoreSession().save();

        return fileDoc;
    }

    public void testListener() throws ClientException {
        DocumentModel doc = createFileDocument();
        assertNotNull(doc.getProperty("uid", "uid"));
    }

}
