package org.nuxeo.ecm.platform.filemanager.core.listener.tests;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryOSGITestCase;

public class AbstractListenerTest extends RepositoryOSGITestCase {


     @Override
     protected void setUp() throws Exception {
         super.setUp();
         openRepository();
     }


     protected DocumentModel createFileDocument(boolean setMimeType) throws ClientException {

         DocumentModel fileDoc = getCoreSession().createDocumentModel("/", "testFile", "File");
         fileDoc.setProperty("dublincore", "title", "TestFile");

         Blob blob = new StringBlob("SOMEDUMMYDATA");
         blob.setFilename("test.pdf");
         if (setMimeType) {
             blob.setMimeType("application/pdf");
         }
         fileDoc.setProperty("file", "content", blob);

         fileDoc = getCoreSession().createDocument(fileDoc);

         getCoreSession().saveDocument(fileDoc);
         getCoreSession().save();

         return fileDoc;
     }

}
