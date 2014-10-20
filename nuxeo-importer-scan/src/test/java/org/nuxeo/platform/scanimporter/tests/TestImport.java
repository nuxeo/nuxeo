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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.platform.scanimporter.processor.ScannedFileImporter;
import org.nuxeo.ecm.platform.scanimporter.service.ImporterConfig;

public class TestImport extends ImportTestCase {

    private static final Log log = LogFactory.getLog(TestImport.class);

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.core");
        deployBundle("org.nuxeo.ecm.core.event");
        deployBundle("org.nuxeo.ecm.core.schema");
        deployContrib("org.nuxeo.ecm.platform.importer.core",
                "OSGI-INF/default-importer-service.xml");
        deployContrib("org.nuxeo.ecm.platform.scanimporter",
                "OSGI-INF/default-scan-config.xml");
        deployContrib("org.nuxeo.ecm.platform.scanimporter.test",
                "needed-contribution-for-factory-deployment.xml");
        deployContrib("org.nuxeo.ecm.platform.scanimporter.test",
                "OSGI-INF/core-type-test-contrib.xml");
        openSession();
        deployContrib("org.nuxeo.ecm.platform.scanimporter",
                "OSGI-INF/importerservice-framework.xml");
    }

    @Override
    @After
    public void tearDown() throws Exception {
       closeSession();
       super.tearDown();
    }

    @Test
    public void testImport() throws Exception {

        String testPath = deployTestFiles("test3");
        File xmlFile = new File(testPath + "/descriptor.xml");
        assertTrue(xmlFile.exists());

        deployContrib("org.nuxeo.ecm.platform.scanimporter.test",
                "OSGI-INF/importerservice-test-contrib3.xml");

        ScannedFileImporter importer = new ScannedFileImporter();

        ImporterConfig config = new ImporterConfig();
        config.setTargetPath("/");
        config.setNbThreads(1);
        config.setBatchSize(10);
        config.setUpdate(false);
        config.setUseXMLMapping(true);

        importer.doImport(new File(testPath), config);

        session.save();

        DocumentModelList alldocs = session.query("select * from File order by ecm:path");

        for (DocumentModel doc : alldocs) {
            log.info("imported : " + doc.getPathAsString() + "-"
                    + doc.getType());
        }

        assertEquals(1, alldocs.size());

        DocumentModel doc = alldocs.get(0);

        assertEquals("SFC", doc.getPropertyValue("dc:source"));
        assertEquals("3-77-2", doc.getPropertyValue("dc:title"));
        assertEquals("12345", doc.getPropertyValue("dc:coverage"));

        assertEquals("testFile.txt",
                ((Blob) doc.getPropertyValue("file:content")).getFilename());
        assertEquals("This is a test.",
                ((Blob) doc.getPropertyValue("file:content")).getString());

        assertFalse(new File(testPath + "/descriptor.xml").exists());

    }

    @Test
    public void shouldCreateContainerTwiceAfterTwoImportationsAsUpdateDisabled()
            throws Exception {
        String testPath = deployTestFiles("test3");
        File xmlFile = new File(testPath + "/descriptor.xml");
        assertTrue(xmlFile.exists());

        deployContrib("org.nuxeo.ecm.platform.scanimporter.test",
                "OSGI-INF/importerservice-test-contrib3.xml");

        ScannedFileImporter importer = new ScannedFileImporter();

        ImporterConfig config = new ImporterConfig();
        config.setTargetPath("/");
        config.setNbThreads(1);
        config.setBatchSize(10);
        config.setUpdate(false);
        config.setUseXMLMapping(true);

        // Import once
        importer.doImport(new File(testPath), config);
        session.save();
        DocumentModelList alldocs = session.query("select * from Folder");
        assertEquals(1, alldocs.size());

        // Import twice
        importer.doImport(new File(testPath), config);
        session.save();
        alldocs = session.query("select * from Folder");
        assertEquals(2, alldocs.size());

        closeSession();

    }

    @Test
    public void shouldCreateContainerOnceAfterTwoImportationsAsUpdateEnabled()
            throws Exception {
        String testPath = deployTestFiles("test3");
        File xmlFile = new File(testPath + "/descriptor.xml");
        assertTrue(xmlFile.exists());

        deployContrib("org.nuxeo.ecm.platform.scanimporter.test",
                "OSGI-INF/importerservice-test-contrib3.xml");

        ScannedFileImporter importer = new ScannedFileImporter();

        ImporterConfig config = new ImporterConfig();
        config.setTargetPath("/");
        config.setNbThreads(1);
        config.setBatchSize(10);
        // Enabled Update new Feature
        config.setUpdate(true);
        config.setUseXMLMapping(true);

        // Import once
        importer.doImport(new File(testPath), config);
        session.save();
        DocumentModelList alldocs = session.query("select * from Folder");
        assertEquals(1, alldocs.size());

        // Import twice
        importer.doImport(new File(testPath), config);
        session.save();
        alldocs = session.query("select * from Folder");
        assertEquals(1, alldocs.size());

        closeSession();

    }

    @Test
    public void shouldSkipInitialContainerCreationSkipped() throws Exception {
        String testPath = deployTestFiles("test3");
        File xmlFile = new File(testPath + "/descriptor.xml");
        assertTrue(xmlFile.exists());

        deployContrib("org.nuxeo.ecm.platform.scanimporter.test",
                "OSGI-INF/importerservice-test-contrib3.xml");

        ScannedFileImporter importer = new ScannedFileImporter();

        ImporterConfig config = new ImporterConfig();
        config.setTargetPath("/");
        config.setNbThreads(1);
        config.setBatchSize(10);
        config.setCreateInitialFolder(false);
        config.setUseXMLMapping(true);

        importer.doImport(new File(testPath), config);

        session.save();
        DocumentModelList alldocs = session.query("select * from File order by ecm:path");
        assertEquals("/testFile.txt", alldocs.get(0).getPathAsString());

    }

    @Test
    public void testDocTypeMappingInImport() throws Exception {

        String testPath = deployTestFiles("test4");
        File xmlFile = new File(testPath + "/descriptor.xml");
        assertTrue(xmlFile.exists());

        deployContrib("org.nuxeo.ecm.platform.scanimporter.test",
                "OSGI-INF/importerservice-test-contrib4.xml");

        ScannedFileImporter importer = new ScannedFileImporter();

        ImporterConfig config = new ImporterConfig();
        config.setTargetPath("/");
        config.setNbThreads(1);
        config.setBatchSize(10);
        config.setUseXMLMapping(true);

        importer.doImport(new File(testPath), config);

        session.save();

        DocumentModelList alldocs = session.query("select * from Picture order by ecm:path");

        for (DocumentModel doc : alldocs) {
            log.info("imported : " + doc.getPathAsString() + "-"
                    + doc.getType());
        }

        assertEquals(1, alldocs.size());

        DocumentModel doc = alldocs.get(0);
        assertEquals(doc.getType(), "Picture");
        closeSession();
    }

    @Test
    public void shouldImportWithNoBlobMapping() throws Exception {
        // Exact same test than above but without blob mapping.
        String testPath = deployTestFiles("test4");
        File xmlFile = new File(testPath + "/descriptor.xml");
        assertTrue(xmlFile.exists());

        deployContrib("org.nuxeo.ecm.platform.scanimporter.test",
                "OSGI-INF/importerservice-test-contrib6.xml");

        ScannedFileImporter importer = new ScannedFileImporter();

        ImporterConfig config = new ImporterConfig();
        config.setTargetPath("/");
        config.setNbThreads(1);
        config.setBatchSize(10);
        config.setUseXMLMapping(true);

        importer.doImport(new File(testPath), config);

        session.save();

        DocumentModelList alldocs = session.query("select * from Picture order by ecm:path");

        for (DocumentModel doc : alldocs) {
            log.info("imported : " + doc.getPathAsString() + "-"
                    + doc.getType());
        }

        assertEquals(1, alldocs.size());

        DocumentModel doc = alldocs.get(0);
        assertEquals(doc.getType(), "Picture");
        closeSession();
    }
}
