package org.nuxeo.ecm.core.api.blobholder;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestBlobHolderAdapterService extends NXRuntimeTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.api");
    }

    public void testService() throws Exception {
        BlobHolderAdapterService bhas = Framework.getLocalService(BlobHolderAdapterService.class);
        assertNotNull(bhas);
    }

    public void testContrib() throws Exception {
        assertSame(0, BlobHolderAdapterComponent.getFactoryNames().size());
        deployContrib("org.nuxeo.ecm.core.facade.tests","blob-holder-adapters-test-contrib.xml");
        assertSame(1, BlobHolderAdapterComponent.getFactoryNames().size());

        BlobHolderAdapterService bhas = Framework.getLocalService(BlobHolderAdapterService.class);
        assertNotNull(bhas);

        DocumentModel doc = new DocumentModelImpl("Test");
        BlobHolder bh = bhas.getBlobHolderAdapter(doc);

        assertNotNull(bh);

        assertTrue(bh.getFilePath().startsWith("Test"));
        assertEquals("Test", bh.getBlob().getString());
    }

}
