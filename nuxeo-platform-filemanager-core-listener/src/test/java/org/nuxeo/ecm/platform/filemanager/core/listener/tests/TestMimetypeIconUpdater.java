package org.nuxeo.ecm.platform.filemanager.core.listener.tests;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;

public class TestMimetypeIconUpdater extends AbstractListenerTest {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.platform.mimetype.api");
        deployBundle("org.nuxeo.ecm.platform.mimetype.core");
        deployBundle("org.nuxeo.ecm.platform.filemanager.api");
        deployBundle("org.nuxeo.ecm.platform.filemanager.core");
        deployBundle("org.nuxeo.ecm.platform.types.api");
        deployBundle("org.nuxeo.ecm.platform.types.core");
        deployBundle("org.nuxeo.ecm.platform.filemanager.core.listener");
        deployContrib("org.nuxeo.ecm.platform.filemanager.core.listener",
                "OSGI-INF/nxfilemanager-core-listener.xml");
    }

    public void testMimeTypeUpdater() throws Exception {
        DocumentModel file = createFileDocument(false);
        Blob blob = (Blob) file.getProperty("file", "content");
        assertNotNull(blob);
        String mt = blob.getMimeType();
        assertNotNull(mt);
        assertEquals("application/pdf", mt);

        String icon = (String)file.getProperty("common", "icon");
        assertNotNull(icon);
        assertEquals("/icons/pdf.png", icon);
    }
}
