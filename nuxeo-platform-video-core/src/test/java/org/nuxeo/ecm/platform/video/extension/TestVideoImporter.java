package org.nuxeo.ecm.platform.video.extension;

import java.io.File;
import java.util.List;

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
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.core.convert.api");
        deployBundle("org.nuxeo.ecm.core.convert");
        deployBundle("org.nuxeo.ecm.platform.commandline.executor");
        deployBundle("org.nuxeo.ecm.platform.types.api");
        deployBundle("org.nuxeo.ecm.platform.types.core");
        deployBundle("org.nuxeo.ecm.platform.picture.core");
        deployBundle("org.nuxeo.ecm.platform.picture.api");
        deployBundle("org.nuxeo.ecm.platform.picture.convert");
        deployBundle("org.nuxeo.ecm.platform.video.convert");
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
        assertEquals("133.0", docModelResult.getPropertyValue("vid:duration").toString());

    }

    @SuppressWarnings("unchecked")
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

        assertNotNull(docModel.getProperty("file:content"));
        assertEquals("sample.mpg", docModel.getPropertyValue("file:filename"));

        // check that we don't get PropertyExceptions when accessing the video
        // and picture schemas

        // the test video is very short:
        assertEquals(0.0, docModel.getPropertyValue("vid:duration"));
        List<Blob> storyboard = docModel.getProperty("vid:storyboard").getValue(List.class);
        assertNotNull(storyboard);
        assertEquals(2, storyboard.size());
        assertEquals("00000.000-seconds.jpeg", storyboard.get(0).getFilename());
        // is this an artifact of the very short video and ffmpeg or is this a bug?
        assertEquals("00010.000-seconds.jpeg", storyboard.get(1).getFilename());


        // check that the thumbnails where extracted
        assertEquals("Thumbnail", docModel.getPropertyValue("picture:views/0/title"));
        assertEquals(100L, docModel.getPropertyValue("picture:views/0/height"));
        assertEquals(1373L, docModel.getPropertyValue("picture:views/0/content/length"));


        // the original video is also 100 pixels high hence the player preview has the same size
        assertEquals("StaticPlayerView", docModel.getPropertyValue("picture:views/1/title"));
        assertEquals(100L, docModel.getPropertyValue("picture:views/1/height"));
        assertEquals(1373L, docModel.getPropertyValue("picture:views/1/content/length"));

        // TODO: add picture metadata extraction where if
        // they make sense for videos (ie. extract these from the metadata already included in the video
        // and use them to set the appropriate schema properties)
        assertNull("", docModel.getPropertyValue("picture:credit"));

    }

}
