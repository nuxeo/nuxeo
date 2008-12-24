package org.nuxeo.ecm.platform.filemanager.api.blobholder.tests;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.platform.filemanager.api.blobholder.BlobHolder;
import org.nuxeo.ecm.platform.filemanager.api.blobholder.BlobHolderAdapterComponent;
import org.nuxeo.ecm.platform.filemanager.api.blobholder.BlobHolderAdapterService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestAdapterService extends NXRuntimeTestCase {


    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.platform.mimetype.api");
        deployBundle("org.nuxeo.ecm.platform.filemanager.api");
    }


    public void testService() throws Exception {
        BlobHolderAdapterService bhas = Framework.getLocalService(BlobHolderAdapterService.class);
        assertNotNull(bhas);
    }

    public void testContrib() throws Exception {
        assertTrue(BlobHolderAdapterComponent.getFactoryNames().size()==0);
        deployContrib("org.nuxeo.ecm.platform.filemanager.api.tests","OSGI-INF/blob-holder-adapters-test-contrib.xml");
        assertTrue(BlobHolderAdapterComponent.getFactoryNames().size()==1);

        BlobHolderAdapterService bhas = Framework.getLocalService(BlobHolderAdapterService.class);
        assertNotNull(bhas);

        DocumentModel doc = new DocumentModelImpl("Test");
        BlobHolder bh = bhas.getBlobHolderAdapter(doc);

        assertNotNull(bh);

        assertTrue(bh.getFilePath().startsWith("Test"));
        assertTrue(bh.getBlob().getString().equals("Test"));
    }


}
