/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Martins
 */
package org.nuxeo.ecm.core;

import static org.junit.Assert.assertEquals;

import java.io.Serializable;
import java.util.Calendar;
import java.util.TimeZone;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.TestSQLRepositoryFulltextConfig.IgnoreNonVCS;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.ConditionalIgnoreRule;
import org.nuxeo.runtime.test.runner.ConditionalIgnoreRule.Condition;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.runtime.reload")
@ConditionalIgnoreRule.Ignore(condition = IgnoreNonVCS.class)
public class TestSQLRepositoryFulltextConfig {

    public static class IgnoreNonVCS implements Condition {

        @Inject
        protected CoreFeature coreFeature;

        @Override
        public boolean shouldIgnore() {
            return !coreFeature.getStorageConfiguration().isVCS();
        }

    }

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected EventService eventService;

    @Inject
    protected CoreSession session;

    protected void waitForAsyncCompletion() {
        nextTransaction();
        eventService.waitForAsyncCompletion();
    }

    protected void waitForFulltextIndexing() {
        nextTransaction();
        coreFeature.getStorageConfiguration().waitForFulltextIndexing();
    }

    protected void nextTransaction() {
        if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
    }

    protected Calendar getCalendar(int year, int month, int day, int hours, int minutes, int seconds) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Europe/Paris"));
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month - 1); // 0-based
        cal.set(Calendar.DAY_OF_MONTH, day);
        cal.set(Calendar.HOUR_OF_DAY, hours);
        cal.set(Calendar.MINUTE, minutes);
        cal.set(Calendar.SECOND, seconds);
        return cal;
    }

    protected void createDocs() throws Exception {
        DocumentModel folder1 = session.createDocumentModel("/", "testfolder1", "Folder");
        folder1.setPropertyValue("dc:title", "testfolder1_Title");
        folder1.setPropertyValue("dc:description", "first test folder description");
        folder1 = session.createDocument(folder1);

        DocumentModel file1 = session.createDocumentModel("/testfolder1", "testfile1", "File");
        file1.setPropertyValue("dc:title", "testfile1_Title");
        file1.setPropertyValue("dc:description", "test file description");
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

        DocumentModel file2 = session.createDocumentModel("/testfolder1", "testfile2", "File");
        file2.setPropertyValue("dc:title", "testfile2_Title");
        file2.setPropertyValue("dc:description", "test file description");
        Calendar cal2 = getCalendar(2007, 4, 1, 12, 0, 0);
        file2.setPropertyValue("dc:created", cal2);
        file2.setPropertyValue("dc:contributors", new String[] { "bob", "pete" });
        file2.setPropertyValue("dc:coverage", "foo/bar");
        file2 = session.createDocument(file2);

        DocumentModel file3 = session.createDocumentModel("/testfolder1", "testfile3", "Note");
        file3.setPropertyValue("dc:title", "testfile3_Title");
        file3.setPropertyValue("dc:description", "test note description");
        file3.setPropertyValue("dc:contributors", new String[] { "bob", "john" });
        file3 = session.createDocument(file3);

        DocumentModel folder2 = session.createDocumentModel("/", "testfolder2", "Folder");
        folder2.setPropertyValue("dc:description", "second test folder description");
        folder2 = session.createDocument(folder2);

        DocumentModel folder3 = session.createDocumentModel("/testfolder2", "testfolder3", "Folder");
        folder3 = session.createDocument(folder3);

        // create file 4
        DocumentModel file4 = session.createDocumentModel("/testfolder2/testfolder3", "testfile4", "File");
        // title without space or _ for Oracle fulltext searchability
        // (testFulltextProxy)
        file4.setPropertyValue("dc:title", "testfile4Title");
        file4.setPropertyValue("dc:description", "test file description");
        file4 = session.createDocument(file4);

        session.save();
        waitForFulltextIndexing();
    }

    @Test
    // deploy contrib where only Note and File documents are fulltext indexed
    @Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-repo-fulltext-note-file-only-contrib.xml")
    @Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/testquery-core-types-contrib.xml")
    @Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-repo-core-types-contrib-2.xml")
    public void testFulltextOnlyNoteFile() throws Exception {
        DocumentModelList dml;
        createDocs();

        // query test for all types
        String query = "SELECT * FROM Document WHERE ecm:fulltext = 'test'";
        dml = session.query(query);
        assertEquals(4, dml.size());

        // query for Note only
        query = "SELECT * FROM Note WHERE ecm:fulltext = 'test'";
        dml = session.query(query);
        assertEquals(1, dml.size());

        // query for File only
        query = "SELECT * FROM File WHERE ecm:fulltext = 'test'";
        dml = session.query(query);
        assertEquals(3, dml.size());

        // query for Folder only
        query = "SELECT * FROM Folder WHERE ecm:fulltext = 'test'";
        dml = session.query(query);
        assertEquals(0, dml.size());
    }

    @Test
    // deploy contrib where only Note and File are not fulltext indexed

    @Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-repo-fulltext-note-file-excluded-contrib.xml")
    @Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/testquery-core-types-contrib.xml")
    @Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-repo-core-types-contrib-2.xml")
    public void testFulltextNoteFileExcluded() throws Exception {
        DocumentModelList dml;
        createDocs();

        // query test for all types
        String query = "SELECT * FROM Document WHERE ecm:fulltext = 'test'";
        dml = session.query(query);
        assertEquals(2, dml.size());

        // query for Note only
        query = "SELECT * FROM Note WHERE ecm:fulltext = 'test'";
        dml = session.query(query);
        assertEquals(0, dml.size());

        // query for File only
        query = "SELECT * FROM File WHERE ecm:fulltext = 'test'";
        dml = session.query(query);
        assertEquals(0, dml.size());

        // query for Folder only
        query = "SELECT * FROM Folder WHERE ecm:fulltext = 'first'";
        dml = session.query(query);
        assertEquals(1, dml.size());

    }

    @Test
    // deploy contrib where fulltext configuration is mixed include types should have the priority
    @Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-repo-fulltext-mixed-contrib.xml")
    @Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/testquery-core-types-contrib.xml")
    @Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-repo-core-types-contrib-2.xml")
    public void testFulltextMixedConfig() throws Exception {
        DocumentModelList dml;
        createDocs();

        // query test for all types
        String query = "SELECT * FROM Document WHERE ecm:fulltext = 'test'";
        dml = session.query(query);
        assertEquals(3, dml.size());

        // query for Note only
        query = "SELECT * FROM Note WHERE ecm:fulltext = 'test'";
        dml = session.query(query);
        assertEquals(0, dml.size());

        // query for File only
        query = "SELECT * FROM File WHERE ecm:fulltext = 'test'";
        dml = session.query(query);
        assertEquals(3, dml.size());

        // query for Folder only
        query = "SELECT * FROM Folder WHERE ecm:fulltext = 'first'";
        dml = session.query(query);
        assertEquals(0, dml.size());

    }

    @Test
    // deploy contrib where only Note and File are not fulltext indexed
    @Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-repo-core-types-note-not-indexable-contrib.xml")
    public void testNotFulltextIndexableFacet() throws Exception {
        DocumentModelList dml;
        createDocs();

        // query test for all types
        String query = "SELECT * FROM Document WHERE ecm:fulltext = 'test'";
        dml = session.query(query);
        assertEquals(5, dml.size());

        // query for Note only
        query = "SELECT * FROM Note WHERE ecm:fulltext = 'test'";
        dml = session.query(query);
        assertEquals(0, dml.size());

        // query for File only
        query = "SELECT * FROM File WHERE ecm:fulltext = 'test'";
        dml = session.query(query);
        assertEquals(3, dml.size());
    }

}
