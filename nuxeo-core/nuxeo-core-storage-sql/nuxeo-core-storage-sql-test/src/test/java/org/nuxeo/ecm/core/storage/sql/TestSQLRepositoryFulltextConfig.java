/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Thierry Martins
 */
package org.nuxeo.ecm.core.storage.sql;

import java.util.Calendar;
import java.util.TimeZone;

import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.blob.ByteArrayBlob;

public class TestSQLRepositoryFulltextConfig extends SQLRepositoryTestCase {

    @Override
    public void setUp() throws Exception {
        initialOpenSessions = CoreInstance.getInstance().getNumberOfSessions();
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.schema");
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.core");
        deployBundle("org.nuxeo.ecm.core.event");
        deployBundle("org.nuxeo.ecm.core.storage.sql");
        database.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        closeSession();
        super.tearDown();
    }

    protected Calendar getCalendar(int year, int month, int day, int hours,
            int minutes, int seconds) {
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
        DocumentModel folder1 = new DocumentModelImpl("/", "testfolder1",
                "Folder");
        folder1.setPropertyValue("dc:title", "testfolder1_Title");
        folder1.setPropertyValue("dc:description",
                "first test folder description");
        folder1 = session.createDocument(folder1);

        DocumentModel file1 = new DocumentModelImpl("/testfolder1",
                "testfile1", "File");
        file1.setPropertyValue("dc:title", "testfile1_Title");
        file1.setPropertyValue("dc:description", "test file description");
        String content = "Some caf\u00e9 in a restaurant.\nDrink!.\n";
        String filename = "testfile.txt";
        ByteArrayBlob blob1 = new ByteArrayBlob(content.getBytes("UTF-8"),
                "text/plain");
        file1.setPropertyValue("content", blob1);
        file1.setPropertyValue("filename", filename);
        Calendar cal1 = getCalendar(2007, 3, 1, 12, 0, 0);
        file1.setPropertyValue("dc:created", cal1);
        file1.setPropertyValue("dc:coverage", "football");
        file1.setPropertyValue("dc:subjects", new String[] { "foo", "gee/moo" });
        file1.setPropertyValue("uid", "uid123");
        file1 = session.createDocument(file1);

        DocumentModel file2 = new DocumentModelImpl("/testfolder1",
                "testfile2", "File");
        file2.setPropertyValue("dc:title", "testfile2_Title");
        file2.setPropertyValue("dc:description", "test file description");
        Calendar cal2 = getCalendar(2007, 4, 1, 12, 0, 0);
        file2.setPropertyValue("dc:created", cal2);
        file2.setPropertyValue("dc:contributors",
                new String[] { "bob", "pete" });
        file2.setPropertyValue("dc:coverage", "foo/bar");
        file2 = session.createDocument(file2);

        DocumentModel file3 = new DocumentModelImpl("/testfolder1",
                "testfile3", "Note");
        file3.setPropertyValue("dc:title", "testfile3_Title");
        file3.setPropertyValue("dc:description", "test note description");
        file3.setPropertyValue("dc:contributors",
                new String[] { "bob", "john" });
        file3 = session.createDocument(file3);

        DocumentModel folder2 = new DocumentModelImpl("/", "testfolder2",
                "Folder");
        folder2.setPropertyValue("dc:description",
                "second test folder description");
        folder2 = session.createDocument(folder2);

        DocumentModel folder3 = new DocumentModelImpl("/testfolder2",
                "testfolder3", "Folder");
        folder3 = session.createDocument(folder3);

        // create file 4
        DocumentModel file4 = new DocumentModelImpl("/testfolder2/testfolder3",
                "testfile4", "File");
        // title without space or _ for Oracle fulltext searchability
        // (testFulltextProxy)
        file4.setPropertyValue("dc:title", "testfile4Title");
        file4.setPropertyValue("dc:description", "test file description");
        file4 = session.createDocument(file4);

        session.save();
    }

    public void testFulltextOnlyNoteFile() throws Exception {
        if (!(database instanceof DatabaseH2)) {
            return;
        }
        // deploy contrib where only Note and File documents are fulltext
        // indexed
        deployContrib("org.nuxeo.ecm.core.storage.sql.test",
                "OSGI-INF/test-repo-fulltext-note-file-only-contrib.xml");

        deployContrib("org.nuxeo.ecm.core.storage.sql.test.tests",
                "OSGI-INF/testquery-core-types-contrib.xml");
        deployContrib("org.nuxeo.ecm.core.storage.sql.test.tests",
                "OSGI-INF/test-repo-core-types-contrib-2.xml");
        openSession();
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

    public void testFulltextNoteFileExcluded() throws Exception {
        if (!(database instanceof DatabaseH2)) {
            return;
        }
        // deploy contrib where only Note and File are not fulltext indexed
        deployContrib("org.nuxeo.ecm.core.storage.sql.test",
                "OSGI-INF/test-repo-fulltext-note-file-excluded-contrib.xml");

        deployContrib("org.nuxeo.ecm.core.storage.sql.test.tests",
                "OSGI-INF/testquery-core-types-contrib.xml");
        deployContrib("org.nuxeo.ecm.core.storage.sql.test.tests",
                "OSGI-INF/test-repo-core-types-contrib-2.xml");
        openSession();
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

    public void testFulltextMixedConfig() throws Exception {
        if (!(database instanceof DatabaseH2)) {
            return;
        }
        // deploy contrib where fulltext configuration is mixed
        // include types should have the priority
        deployContrib("org.nuxeo.ecm.core.storage.sql.test",
                "OSGI-INF/test-repo-fulltext-mixed-contrib.xml");

        deployContrib("org.nuxeo.ecm.core.storage.sql.test.tests",
                "OSGI-INF/testquery-core-types-contrib.xml");
        deployContrib("org.nuxeo.ecm.core.storage.sql.test.tests",
                "OSGI-INF/test-repo-core-types-contrib-2.xml");
        openSession();
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

}
