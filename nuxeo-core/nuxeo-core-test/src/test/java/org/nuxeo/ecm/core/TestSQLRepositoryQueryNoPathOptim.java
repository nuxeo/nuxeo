/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Benoit Delbosc
 */
package org.nuxeo.ecm.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assume.assumeTrue;

import java.io.Serializable;
import java.util.Calendar;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor;
import org.nuxeo.ecm.core.storage.sql.coremodel.SQLRepositoryService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.reload.ReloadService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeHarness;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Test path search without pathOptimizations
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.runtime.reload")
public class TestSQLRepositoryQueryNoPathOptim {

    @Inject
    protected RuntimeHarness runtimeHarness;

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected EventService eventService;

    @Inject
    protected CoreSession session;

    @Inject
    protected SQLRepositoryService sqlRepositoryService;

    @Inject
    protected ReloadService reloadService;

    @Before
    public void setUp() throws Exception {
        // cannot be done through @LocalDeploy, because the framework variables
        // about repository configuration aren't ready yet
        runtimeHarness.deployContrib("org.nuxeo.ecm.core.test.tests",
                "OSGI-INF/test-repo-no-pathoptimizations-contrib.xml");
        // assume after deploy so that tearDown can undeploy
        assumeTrue(coreFeature.getStorageConfiguration().isVCS());
        newRepository(); // fully reread repo
        RepositoryDescriptor desc = sqlRepositoryService.getRepositoryDescriptor(session.getRepositoryName());
        assertFalse("Path optim should be disabled", desc.getPathOptimizationsEnabled());
    }

    @After
    public void tearDown() throws Exception {
        runtimeHarness.undeployContrib("org.nuxeo.ecm.core.test.tests",
                "OSGI-INF/test-repo-no-pathoptimizations-contrib.xml");
    }

    protected void newRepository() {
        waitForAsyncCompletion();
        coreFeature.releaseCoreSession();
        // reload repo with new config
        reloadService.reloadRepository();
        session = coreFeature.createCoreSession();
    }

    protected void waitForAsyncCompletion() {
        nextTransaction();
        eventService.waitForAsyncCompletion();
    }

    protected void nextTransaction() {
        if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
    }

    protected Calendar getCalendar(int year, int month, int day, int hours, int minutes, int seconds) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month - 1); // 0-based
        cal.set(Calendar.DAY_OF_MONTH, day);
        cal.set(Calendar.HOUR_OF_DAY, hours);
        cal.set(Calendar.MINUTE, minutes);
        cal.set(Calendar.SECOND, seconds);
        return cal;
    }

    /**
     * Creates the following structure of documents:
     *
     * <pre>
     *  root (UUID_1)
     *  |- testfolder1 (UUID_2)
     *  |  |- testfile1 (UUID_3) (content UUID_4)
     *  |  |- testfile2 (UUID_5) (content UUID_6)
     *  |  \- testfile3 (UUID_7) (Note)
     *  \- tesfolder2 (UUID_8)
     *     \- testfolder3 (UUID_9)
     *        \- testfile4 (UUID_10) (content UUID_11)
     * </pre>
     */
    protected void createDocs() throws Exception {
        DocumentModel folder1 = new DocumentModelImpl("/", "testfolder1", "Folder");
        folder1.setPropertyValue("dc:title", "testfolder1_Title");
        folder1 = session.createDocument(folder1);

        DocumentModel file1 = new DocumentModelImpl("/testfolder1", "testfile1", "File");
        file1.setPropertyValue("dc:title", "testfile1_Title");
        file1.setPropertyValue("dc:description", "testfile1_description");
        String content = "Some caf\u00e9 in a restaurant.\nDrink!.\n";
        String filename = "testfile.txt";
        Blob blob1 = Blobs.createBlob(content);
        blob1.setFilename(filename);
        file1.setPropertyValue("content", (Serializable) blob1);
        Calendar cal1 = getCalendar(2007, 3, 1, 12, 0, 0);
        file1.setPropertyValue("dc:created", cal1);
        file1.setPropertyValue("dc:coverage", "football");
        file1.setPropertyValue("dc:subjects", new String[] { "foo", "gee/moo" });
        file1.setPropertyValue("uid", "uid123");
        file1 = session.createDocument(file1);

        DocumentModel file2 = new DocumentModelImpl("/testfolder1", "testfile2", "File");
        file2.setPropertyValue("dc:title", "testfile2_Title");
        file2.setPropertyValue("dc:description", "testfile2_DESCRIPTION2");
        Calendar cal2 = getCalendar(2007, 4, 1, 12, 0, 0);
        file2.setPropertyValue("dc:created", cal2);
        file2.setPropertyValue("dc:contributors", new String[] { "bob", "pete" });
        file2.setPropertyValue("dc:coverage", "foo/bar");
        file2 = session.createDocument(file2);

        DocumentModel file3 = new DocumentModelImpl("/testfolder1", "testfile3", "Note");
        file3.setPropertyValue("dc:title", "testfile3_Title");
        file3.setPropertyValue("dc:description", "testfile3_desc1 testfile3_desc2,  testfile3_desc3");
        file3.setPropertyValue("dc:contributors", new String[] { "bob", "john" });
        file3 = session.createDocument(file3);

        DocumentModel folder2 = new DocumentModelImpl("/", "testfolder2", "Folder");
        folder2 = session.createDocument(folder2);

        DocumentModel folder3 = new DocumentModelImpl("/testfolder2", "testfolder3", "Folder");
        folder3 = session.createDocument(folder3);

        // create file 4
        DocumentModel file4 = new DocumentModelImpl("/testfolder2/testfolder3", "testfile4", "File");
        // title without space or _ for Oracle fulltext searchability
        // (testFulltextProxy)
        file4.setPropertyValue("dc:title", "testfile4Title");
        file4.setPropertyValue("dc:description", "testfile4_DESCRIPTION4");
        file4 = session.createDocument(file4);

        session.save();
    }

    @Test
    public void testStartsWith() throws Exception {
        String sql;
        DocumentModelList dml;
        createDocs();

        sql = "SELECT * FROM document WHERE ecm:path STARTSWITH '/'";
        dml = session.query(sql);
        assertEquals(7, dml.size());

        sql = "SELECT * FROM document WHERE ecm:path STARTSWITH '/nothere/'";
        dml = session.query(sql);
        assertEquals(0, dml.size());

        sql = "SELECT * FROM document WHERE ecm:path STARTSWITH '/testfolder1/'";
        dml = session.query(sql);
        assertEquals(3, dml.size());

        sql = "SELECT * FROM document WHERE ecm:path STARTSWITH '/testfolder2/'";
        dml = session.query(sql);
        assertEquals(2, dml.size());

        sql = "SELECT * FROM document WHERE dc:title='testfile1_Title' AND ecm:path STARTSWITH '/'";
        dml = session.query(sql);
        assertEquals(1, dml.size());

        sql = "SELECT * FROM document WHERE dc:title LIKE 'testfile%' AND ecm:path STARTSWITH '/'";
        dml = session.query(sql);
        assertEquals(4, dml.size());

    }

    @Test
    public void testStartsWithMove() throws Exception {
        String sql;
        DocumentModelList dml;
        createDocs();

        // move folder2 into folder1
        session.move(new PathRef("/testfolder2/"), new PathRef("/testfolder1/"), null);
        session.save();

        sql = "SELECT * FROM document WHERE ecm:path STARTSWITH '/testfolder1/'";
        dml = session.query(sql);
        assertEquals(6, dml.size());

        sql = "SELECT * FROM document WHERE ecm:path STARTSWITH '/testfolder1/testfolder2/'";
        dml = session.query(sql);
        assertEquals(2, dml.size());

        sql = "SELECT * FROM document WHERE ecm:path STARTSWITH '/testfolder2/'";
        dml = session.query(sql);
        assertEquals(0, dml.size());
    }

    @Test
    public void testRemoveChildren() throws Exception {
        String sql;
        DocumentModelList dml;
        createDocs();
        DocumentModel doc = session.getDocument(new PathRef("/testfolder2"));
        DocumentModelList children = session.getChildren(doc.getRef());
        assertEquals(1, children.totalSize());

        // removeChildren use the Dialect getInTreeSql to invalidate VCS cache
        session.removeChildren(new PathRef("/testfolder2/"));

        children = session.getChildren(doc.getRef());
        assertEquals(0, children.totalSize());

        sql = "SELECT * FROM document WHERE ecm:path STARTSWITH '/testfolder2/'";
        dml = session.query(sql);
        assertEquals(0, dml.size());
    }

    @Test
    public void testAncestorId() throws Exception {
        DocumentModelList dml;
        createDocs();

        String query = "SELECT * FROM Document WHERE ecm:ancestorId = '%s'";
        dml = session.query(String.format(query, session.getRootDocument().getId()));
        assertEquals(7, dml.size());

        dml = session.query(String.format(query, "nosuchid"));
        assertEquals(0, dml.size());

        dml = session.query(String.format(query, session.getDocument(new PathRef("/testfolder1")).getId()));
        assertEquals(3, dml.size());

        dml = session.query(String.format(query, session.getDocument(new PathRef("/testfolder2")).getId()));
        assertEquals(2, dml.size());

        // negative query
        dml = session.query(String.format("SELECT * FROM Document WHERE ecm:ancestorId <> '%s'",
                session.getDocument(new PathRef("/testfolder1")).getId()));
        assertEquals(4, dml.size());

        dml = session.query(String.format(
                "SELECT * FROM document WHERE dc:title='testfile1_Title' AND ecm:ancestorId = '%s'",
                session.getRootDocument().getId()));
        assertEquals(1, dml.size());

        dml = session.query(String.format(
                "SELECT * FROM document WHERE dc:title LIKE 'testfile%%' AND ecm:ancestorId = '%s'",
                session.getRootDocument().getId()));
        assertEquals(4, dml.size());
    }

}
