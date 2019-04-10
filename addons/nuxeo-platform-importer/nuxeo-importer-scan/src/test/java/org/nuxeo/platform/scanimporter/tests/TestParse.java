package org.nuxeo.platform.scanimporter.tests;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.platform.scanimporter.service.ScanFileBlobHolder;
import org.nuxeo.ecm.platform.scanimporter.service.ScannedFileMapperService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestParse extends NXRuntimeTestCase {

    protected List<File> tmpDirectories = new ArrayList<File>();

    protected String deployTestFiles(String name) throws IOException {

        File directory = new File(FileUtils.getResourcePathFromContext("data/" + name));

        String tmpDirectoryPath =System.getProperty("java.io.tmpdir");
        File dst = new File(tmpDirectoryPath);

        FileUtils.copy(directory, dst);
        tmpDirectories.add(dst);

        return dst.getPath() + "/" + name;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.ecm.platform.scanimporter", "OSGI-INF/importerservice-framework.xml");
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        for (File dir : tmpDirectories) {
            if (dir.exists()) {
                FileUtils.deleteTree(dir);
            }
        }
    }


    public void testSimpleParse() throws Exception {

        String testPath = deployTestFiles("test1");
        File xmlFile = new File(testPath + "/descriptor.xml");
        assertTrue(xmlFile.exists());

        deployContrib("org.nuxeo.ecm.platform.scanimporter.test", "OSGI-INF/importerservice-test-contrib1.xml");

        ScannedFileMapperService sfms = Framework.getLocalService(ScannedFileMapperService.class);
        assertNotNull(sfms);

        ScanFileBlobHolder bh = sfms.parseMetaData(xmlFile);

        assertNotNull(bh);
        assertEquals(7, bh.getProperties().size());
        assertEquals("MyTitle", bh.getProperties().get("dc:title"));
        assertEquals("MyDesc", bh.getProperties().get("dc:description"));
        assertEquals(12, bh.getProperties().get("foo:int"));
        assertEquals(1.2, bh.getProperties().get("foo:double"));
        assertEquals("file1", ((Blob)bh.getProperties().get("file:content")).getFilename());

        Calendar cal = new GregorianCalendar();
        cal.setTime(((Date)bh.getProperties().get("foo:date")));
        assertEquals(2005,cal.get(Calendar.YEAR));
        assertEquals(11,cal.get(Calendar.HOUR));

        cal = new GregorianCalendar();
        cal.setTime(((Date)bh.getProperties().get("foo:date2")));
        assertEquals(2005,cal.get(Calendar.YEAR));
        assertEquals(0,cal.get(Calendar.HOUR));

    }

    public void testRSParse() throws Exception {

        String testPath = deployTestFiles("test2");
        File xmlFile = new File(testPath + "/descriptor.xml");
        assertTrue(xmlFile.exists());

        deployContrib("org.nuxeo.ecm.platform.scanimporter.test", "OSGI-INF/importerservice-test-contrib2.xml");

        ScannedFileMapperService sfms = Framework.getLocalService(ScannedFileMapperService.class);
        assertNotNull(sfms);

        ScanFileBlobHolder bh = sfms.parseMetaData(xmlFile);

        assertNotNull(bh);
        assertEquals(2, bh.getProperties().size());
        assertEquals("Eur", bh.getProperties().get("invoice:currency"));
        assertEquals(67.86, bh.getProperties().get("invoice:amount"));

        assertEquals(1,bh.getBlobs().size());
        assertEquals("testFile.txt", bh.getBlobs().get(0).getFilename());

        assertEquals("testFile.txt", bh.getBlob().getFilename());
    }

    public void testSimpleDocTypeMapping() throws Exception {

        String testPath = deployTestFiles("test4");
        File xmlFile = new File(testPath + "/descriptor.xml");
        assertTrue(xmlFile.exists());

        deployContrib("org.nuxeo.ecm.platform.scanimporter.test", "OSGI-INF/importerservice-test-contrib4.xml");

        ScannedFileMapperService sfms = Framework.getLocalService(ScannedFileMapperService.class);
        assertNotNull(sfms);

        assertEquals("Workspace", sfms.getTargetContainerType());

        ScanFileBlobHolder bh = sfms.parseMetaData(xmlFile);

        assertEquals("Picture",bh.getTargetType());

        assertNotNull(bh);
        assertEquals(2, bh.getProperties().size());
        assertEquals("Eur", bh.getProperties().get("invoice:currency"));
        assertEquals(67.86, bh.getProperties().get("invoice:amount"));

        assertEquals(1,bh.getBlobs().size());
        assertEquals("testFile.txt", bh.getBlobs().get(0).getFilename());

        assertEquals("testFile.txt", bh.getBlob().getFilename());
    }

    public void testCustomDocTypeMapping() throws Exception {

        String testPath = deployTestFiles("test4");
        File xmlFile = new File(testPath + "/descriptor.xml");
        assertTrue(xmlFile.exists());

        deployContrib("org.nuxeo.ecm.platform.scanimporter.test", "OSGI-INF/importerservice-test-contrib5.xml");

        ScannedFileMapperService sfms = Framework.getLocalService(ScannedFileMapperService.class);
        assertNotNull(sfms);

        assertEquals("Workspace", sfms.getTargetContainerType());

        ScanFileBlobHolder bh = sfms.parseMetaData(xmlFile);

        assertEquals("BonAPayer_1.0",bh.getTargetType());

        assertNotNull(bh);
        assertEquals(2, bh.getProperties().size());
        assertEquals("Eur", bh.getProperties().get("invoice:currency"));
        assertEquals(67.86, bh.getProperties().get("invoice:amount"));

        assertEquals(1,bh.getBlobs().size());
        assertEquals("testFile.txt", bh.getBlobs().get(0).getFilename());

        assertEquals("testFile.txt", bh.getBlob().getFilename());
    }

}
