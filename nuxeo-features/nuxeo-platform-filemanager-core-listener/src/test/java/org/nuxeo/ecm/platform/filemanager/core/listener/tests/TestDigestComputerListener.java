package org.nuxeo.ecm.platform.filemanager.core.listener.tests;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;

public class TestDigestComputerListener extends AbstractListenerTest {


     @Override
     protected void setUp() throws Exception {
         super.setUp();
         deployBundle("org.nuxeo.ecm.platform.filemanager.api");
         deployBundle("org.nuxeo.ecm.platform.filemanager.core");
         deployBundle("org.nuxeo.ecm.platform.filemanager.core.listener");
         deployContrib("org.nuxeo.ecm.platform.filemanager.core.listener", "OSGI-INF/nxfilemanager-core-listener.xml");
     }




     public void testDigest() throws Exception {
         DocumentModel file = createFileDocument(true);
         Blob blob = (Blob) file.getProperty("file", "content");
         assertNotNull(blob);
         String digest = blob.getDigest();
         assertNotNull(digest);
         assertFalse("".equals(digest));
         System.out.println("digest = " + digest);
     }
}
