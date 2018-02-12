/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Thierry Delprat
 */
package org.nuxeo.platform.scanimporter.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.inject.Inject;

import org.junit.Test;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.platform.scanimporter.service.ScanFileBlobHolder;
import org.nuxeo.ecm.platform.scanimporter.service.ScannedFileMapperService;
import org.nuxeo.runtime.test.runner.Deploy;

public class TestParse extends ImportTestCase {

    @Inject
    protected ScannedFileMapperService sfms;

    @Test
    @Deploy("org.nuxeo.ecm.platform.scanimporter.test:OSGI-INF/importerservice-test-contrib1.xml")
    public void testSimpleParse() throws Exception {

        String testPath = deployTestFiles("test1");
        File xmlFile = new File(testPath + "/descriptor.xml");
        assertTrue(xmlFile.exists());

        ScanFileBlobHolder bh = sfms.parseMetaData(xmlFile);

        assertNotNull(bh);
        assertEquals(9, bh.getProperties().size());
        assertEquals("MyTitle", bh.getProperties().get("dc:title"));
        assertEquals("MyDesc", bh.getProperties().get("dc:description"));
        assertEquals(12, bh.getProperties().get("foo:int"));
        assertEquals(1.2, bh.getProperties().get("foo:double"));
        assertEquals("file1", ((Blob) bh.getProperties().get("file:content")).getFilename());

        assertEquals(true, bh.getProperties().get("foo:bool1"));
        assertEquals(false, bh.getProperties().get("foo:bool2"));

        Calendar cal = new GregorianCalendar();
        cal.setTime(((Date) bh.getProperties().get("foo:date")));
        assertEquals(2005, cal.get(Calendar.YEAR));
        assertEquals(11, cal.get(Calendar.HOUR));

        cal = new GregorianCalendar();
        cal.setTime(((Date) bh.getProperties().get("foo:date2")));
        assertEquals(2005, cal.get(Calendar.YEAR));
        assertEquals(0, cal.get(Calendar.HOUR));

    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.scanimporter.test:OSGI-INF/importerservice-test-contrib2.xml")
    public void testRSParse() throws Exception {

        String testPath = deployTestFiles("test2");
        File xmlFile = new File(testPath + "/descriptor.xml");
        assertTrue(xmlFile.exists());

        ScanFileBlobHolder bh = sfms.parseMetaData(xmlFile);

        assertNotNull(bh);
        assertEquals(2, bh.getProperties().size());
        assertEquals("Eur", bh.getProperties().get("invoice:currency"));
        assertEquals(67.86, bh.getProperties().get("invoice:amount"));

        assertEquals(1, bh.getBlobs().size());
        assertEquals("testFile.txt", bh.getBlobs().get(0).getFilename());

        assertEquals("testFile.txt", bh.getBlob().getFilename());
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.scanimporter.test:OSGI-INF/importerservice-test-contrib4.xml")
    public void testSimpleDocTypeMapping() throws Exception {

        String testPath = deployTestFiles("test4");
        File xmlFile = new File(testPath + "/descriptor.xml");
        assertTrue(xmlFile.exists());

        assertEquals("Workspace", sfms.getTargetContainerType());

        ScanFileBlobHolder bh = sfms.parseMetaData(xmlFile);

        assertEquals("Picture", bh.getTargetType());

        assertNotNull(bh);
        assertEquals(2, bh.getProperties().size());
        assertEquals("Eur", bh.getProperties().get("invoice:currency"));
        assertEquals(67.86, bh.getProperties().get("invoice:amount"));

        assertEquals(1, bh.getBlobs().size());
        assertEquals("testFile.txt", bh.getBlobs().get(0).getFilename());

        assertEquals("testFile.txt", bh.getBlob().getFilename());
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.scanimporter.test:OSGI-INF/importerservice-test-contrib5.xml")
    public void testCustomDocTypeMapping() throws Exception {

        String testPath = deployTestFiles("test4");
        File xmlFile = new File(testPath + "/descriptor.xml");
        assertTrue(xmlFile.exists());

        assertEquals("Workspace", sfms.getTargetContainerType());

        ScanFileBlobHolder bh = sfms.parseMetaData(xmlFile);

        assertEquals("BonAPayer_1.0", bh.getTargetType());

        assertNotNull(bh);
        assertEquals(2, bh.getProperties().size());
        assertEquals("Eur", bh.getProperties().get("invoice:currency"));
        assertEquals(67.86, bh.getProperties().get("invoice:amount"));

        assertEquals(1, bh.getBlobs().size());
        assertEquals("testFile.txt", bh.getBlobs().get(0).getFilename());

        assertEquals("testFile.txt", bh.getBlob().getFilename());
    }

}
