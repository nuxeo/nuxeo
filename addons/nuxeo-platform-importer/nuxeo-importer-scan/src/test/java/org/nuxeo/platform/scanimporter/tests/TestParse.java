/*
 * Copyright (c) 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.platform.scanimporter.service.ScanFileBlobHolder;
import org.nuxeo.ecm.platform.scanimporter.service.ScannedFileMapperService;
import org.nuxeo.runtime.api.Framework;

public class TestParse extends ImportTestCase {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.ecm.platform.scanimporter",
                "OSGI-INF/importerservice-framework.xml");
    }

    @Test
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

    @Test
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

    @Test
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

    @Test
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
