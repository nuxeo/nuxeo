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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.platform.scanimporter.processor.ScannedFileImporter;
import org.nuxeo.ecm.platform.scanimporter.service.ImporterConfig;
import org.nuxeo.ecm.platform.scanimporter.service.ScannedFileMapperService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.transaction.TransactionHelper;

@Deploy("org.nuxeo.ecm.platform.scanimporter.test:needed-contribution-for-factory-deployment.xml")
@Deploy("org.nuxeo.ecm.platform.scanimporter.test:OSGI-INF/core-type-test-contrib.xml")
public class TestImport extends ImportTestCase {

    private static final Log log = LogFactory.getLog(TestImport.class);

    @Inject
    protected ScannedFileMapperService service;

    // MySQL needs to commit the transaction to see the updated state
    protected void nextTransaction() {
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
    }

    protected ImporterConfig getConfig() {
        return service.getImporterConfig();
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.scanimporter.test:OSGI-INF/importerservice-test-config.xml")
    @Deploy("org.nuxeo.ecm.platform.scanimporter.test:OSGI-INF/importerservice-test-contrib3.xml")
    public void testImport() throws IOException {

        String testPath = deployTestFiles("test3");
        File xmlFile = new File(testPath + "/descriptor.xml");
        assertTrue(xmlFile.exists());

        new ScannedFileImporter().doImport(new File(testPath), getConfig());

        nextTransaction();
        DocumentModelList alldocs = session.query("select * from File order by ecm:path");

        for (DocumentModel doc : alldocs) {
            log.info("imported : " + doc.getPathAsString() + "-" + doc.getType());
        }

        assertEquals(1, alldocs.size());

        DocumentModel doc = alldocs.get(0);

        assertEquals("SFC", doc.getPropertyValue("dc:source"));
        assertEquals("3-77-2", doc.getPropertyValue("dc:title"));
        assertEquals("12345", doc.getPropertyValue("dc:coverage"));

        assertEquals("testFile.txt", ((Blob) doc.getPropertyValue("file:content")).getFilename());
        assertEquals("This is a test.", ((Blob) doc.getPropertyValue("file:content")).getString());

        assertFalse(new File(testPath + "/descriptor.xml").exists());

    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.scanimporter.test:OSGI-INF/importerservice-test-config.xml")
    @Deploy("org.nuxeo.ecm.platform.scanimporter.test:OSGI-INF/importerservice-test-contrib3.xml")
    public void shouldCreateContainerTwiceAfterTwoImportationsAsUpdateDisabled() throws IOException {
        String testPath = deployTestFiles("test3");
        File xmlFile = new File(testPath + "/descriptor.xml");
        assertTrue(xmlFile.exists());

        ScannedFileImporter importer = new ScannedFileImporter();
        ImporterConfig config = getConfig();

        // Import once
        importer.doImport(new File(testPath), config);

        nextTransaction();
        DocumentModelList alldocs = session.query("select * from Folder");
        assertEquals(1, alldocs.size());

        // Import twice
        importer.doImport(new File(testPath), config);

        nextTransaction();
        alldocs = session.query("select * from Folder");
        assertEquals(2, alldocs.size());
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.scanimporter.test:OSGI-INF/importerservice-test-config.xml")
    @Deploy("org.nuxeo.ecm.platform.scanimporter.test:OSGI-INF/importerservice-test-config-update.xml")
    @Deploy("org.nuxeo.ecm.platform.scanimporter.test:OSGI-INF/importerservice-test-contrib3.xml")
    public void shouldCreateContainerOnceAfterTwoImportationsAsUpdateEnabled() throws IOException {
        String testPath = deployTestFiles("test3");
        File xmlFile = new File(testPath + "/descriptor.xml");
        assertTrue(xmlFile.exists());

        ScannedFileImporter importer = new ScannedFileImporter();
        ImporterConfig config = getConfig();

        // Import once
        importer.doImport(new File(testPath), config);

        nextTransaction();
        DocumentModelList alldocs = session.query("select * from Folder");
        assertEquals(1, alldocs.size());

        // Import twice
        importer.doImport(new File(testPath), config);

        nextTransaction();
        alldocs = session.query("select * from Folder");
        assertEquals(1, alldocs.size());
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.scanimporter.test:OSGI-INF/importerservice-test-config.xml")
    @Deploy("org.nuxeo.ecm.platform.scanimporter.test:OSGI-INF/importerservice-test-config-initial-folder.xml")
    @Deploy("org.nuxeo.ecm.platform.scanimporter.test:OSGI-INF/importerservice-test-contrib3.xml")
    public void shouldSkipInitialContainerCreationSkipped() throws IOException {
        String testPath = deployTestFiles("test3");
        File xmlFile = new File(testPath + "/descriptor.xml");
        assertTrue(xmlFile.exists());

        new ScannedFileImporter().doImport(new File(testPath), getConfig());

        nextTransaction();
        DocumentModelList alldocs = session.query("select * from File order by ecm:path");
        assertEquals(1, alldocs.size());
        assertEquals("/testFile.txt", alldocs.get(0).getPathAsString());

    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.scanimporter.test:OSGI-INF/importerservice-test-config.xml")
    @Deploy("org.nuxeo.ecm.platform.scanimporter.test:OSGI-INF/importerservice-test-contrib4.xml")
    public void testDocTypeMappingInImport() throws IOException {

        String testPath = deployTestFiles("test4");
        File xmlFile = new File(testPath + "/descriptor.xml");
        assertTrue(xmlFile.exists());

        new ScannedFileImporter().doImport(new File(testPath), getConfig());

        nextTransaction();
        DocumentModelList alldocs = session.query("select * from Picture order by ecm:path");

        for (DocumentModel doc : alldocs) {
            log.info("imported : " + doc.getPathAsString() + "-" + doc.getType());
        }

        assertEquals(1, alldocs.size());

        DocumentModel doc = alldocs.get(0);
        assertEquals(doc.getType(), "Picture");
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.scanimporter.test:OSGI-INF/importerservice-test-config.xml")
    @Deploy("org.nuxeo.ecm.platform.scanimporter.test:OSGI-INF/importerservice-test-contrib6.xml")
    public void shouldImportWithNoBlobMapping() throws IOException {
        // Exact same test than above but without blob mapping.
        String testPath = deployTestFiles("test4");
        File xmlFile = new File(testPath + "/descriptor.xml");
        assertTrue(xmlFile.exists());

        new ScannedFileImporter().doImport(new File(testPath), getConfig());

        nextTransaction();
        DocumentModelList alldocs = session.query("select * from Picture order by ecm:path");

        for (DocumentModel doc : alldocs) {
            log.info("imported : " + doc.getPathAsString() + "-" + doc.getType());
        }

        assertEquals(1, alldocs.size());

        DocumentModel doc = alldocs.get(0);
        assertEquals(doc.getType(), "Picture");
    }

}
