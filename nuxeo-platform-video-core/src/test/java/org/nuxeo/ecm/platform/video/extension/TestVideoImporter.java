package org.nuxeo.ecm.platform.video.extension;

import java.io.File;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.runtime.api.Framework;

/*
 * Tests that the VideoImporter class works by importing a sample video
 */
public class TestVideoImporter extends SQLRepositoryTestCase {

    protected static final String VIDEO_TYPE = "Video";

    protected FileManager fileManagerService;

    protected DocumentModel root;

    private File getTestFile() {
        return new File(
                FileUtils.getResourcePathFromContext("test-data/sample.mpg"));
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        deployBundle("org.nuxeo.ecm.platform.types.api");
        deployBundle("org.nuxeo.ecm.platform.types.core");
        deployBundle("org.nuxeo.ecm.platform.picture.core");
        deployBundle("org.nuxeo.ecm.platform.video.core");

        // use these to get the fileManagerService
        deployBundle("org.nuxeo.ecm.platform.filemanager.api");
        deployBundle("org.nuxeo.ecm.platform.filemanager.core");

        openSession();

        root = session.getRootDocument();
        fileManagerService = Framework.getService(FileManager.class);
    }

    @Override
    public void tearDown() throws Exception {
        fileManagerService = null;
        root = null;
    }

    public void testVideoType() throws ClientException {

        DocumentType videoType = session.getDocumentType(VIDEO_TYPE);
        assertNotNull("Does our type exist?", videoType);
        
        //TODO: check get/set properties on common properties of videos
        
        // Create a new DocumentModel of our type in memory
        DocumentModel docModel = session.createDocumentModel("/", "doc", VIDEO_TYPE);
        assertNotNull(docModel);
      
        assertNull(docModel.getPropertyValue("common:icon"));
        assertNull(docModel.getPropertyValue("dc:title"));
        assertNull(docModel.getPropertyValue("picture:credit"));
        assertNull(docModel.getPropertyValue("uid:uid"));
        assertNull(docModel.getPropertyValue("vid:duration"));
        
        docModel.setPropertyValue("common:icon", "/icons/video.png");
        docModel.setPropertyValue("dc:title", "testTitle");
        docModel.setPropertyValue("picture:credit", "testUser");
        docModel.setPropertyValue("uid:uid", "testUid");
        docModel.setPropertyValue("vid:duration", 133);
        
        DocumentModel docModelResult = session.createDocument(docModel);
        assertNotNull(docModelResult);
        
        assertEquals("/icons/video.png", docModelResult.getPropertyValue("common:icon"));
        assertEquals("testTitle", docModelResult.getPropertyValue("dc:title"));
        assertEquals("testUser", docModelResult.getPropertyValue("picture:credit"));
        assertEquals("testUid", docModelResult.getPropertyValue("uid:uid"));
        assertEquals("133", docModelResult.getPropertyValue("vid:duration").toString());

    }

    public void testImportVideo() throws Exception {

        File testFile = getTestFile();
        Blob blob = StreamingBlob.createFromFile(testFile, "video/mpg");
        String rootPath = root.getPathAsString();
        assertNotNull(blob);
        assertNotNull(rootPath);
        assertNotNull(session);
        assertNotNull(fileManagerService);

        DocumentModel docModel = fileManagerService.createDocumentFromBlob(
                session, blob, rootPath, true, "test-data/sample.mpg");

        assertNotNull(docModel);
        DocumentRef ref = docModel.getRef();
        session.save();

        closeSession();
        openSession();

        docModel = session.getDocument(ref);
        assertEquals("Video", docModel.getType());
        assertEquals("sample", docModel.getTitle());

        // check that we don't get PropertyExceptions when accessing the video
        // and picture schemas
        
        // TODO: add duration detection
        assertNull(docModel.getPropertyValue("vid:duration"));

        // TODO: add thumbnail generation and picture metadata extraction where
        // they make sense for videos (ie. extract these from the metadata already included in the video
        // and use them to set the appropriate schema properties)
        assertNull(docModel.getPropertyValue("picture:credit"));

        tearDown();

    }

}
