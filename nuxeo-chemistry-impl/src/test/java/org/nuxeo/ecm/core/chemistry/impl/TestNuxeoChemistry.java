/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.chemistry.impl;

import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.chemistry.CMISObject;
import org.apache.chemistry.Connection;
import org.apache.chemistry.Folder;
import org.apache.chemistry.Property;
import org.apache.chemistry.Type;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.blob.ByteArrayBlob;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.storage.sql.DatabaseHelper;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.runtime.api.Framework;

public class TestNuxeoChemistry extends SQLRepositoryTestCase {

    public static final String REPOSITORY_NAME = "test";

    public static final String ROOT_TYPE_ID = "Root"; // not in the spec

    protected NuxeoRepository repository;

    protected String folder1id;

    protected String folder2id;

    protected String file4id;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // deployed for fulltext indexing
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.core.convert.api");
        deployBundle("org.nuxeo.ecm.core.convert");
        deployBundle("org.nuxeo.ecm.core.convert.plugins");
        deployBundle("org.nuxeo.ecm.core.storage.sql"); // event listener

        openSession();
        makeRepository();
        repository = new NuxeoRepository(REPOSITORY_NAME);
    }

    @Override
    public void tearDown() throws Exception {
        closeSession();
        super.tearDown();
    }

    protected static Calendar getCalendar(int year, int month, int day,
            int hours, int minutes, int seconds) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Europe/Paris"));
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month - 1); // 0-based
        cal.set(Calendar.DAY_OF_MONTH, day);
        cal.set(Calendar.HOUR_OF_DAY, hours);
        cal.set(Calendar.MINUTE, minutes);
        cal.set(Calendar.SECOND, seconds);
        return cal;
    }

    public void makeRepository() throws IOException, ClientException {

        DocumentModel folder1 = new DocumentModelImpl("/", "testfolder1",
                "Folder");
        folder1.setPropertyValue("dc:title", "testfolder1_Title");
        folder1 = session.createDocument(folder1);
        folder1id = folder1.getId();

        DocumentModel file1 = new DocumentModelImpl("/testfolder1",
                "testfile1", "File");
        file1.setPropertyValue("dc:title", "testfile1_Title");
        file1.setPropertyValue("dc:description", "testfile1_description");
        String content = "Some caf\u00e9 in a restaurant.\nDrink!.\n";
        String filename = "testfile.txt";
        ByteArrayBlob blob1 = new ByteArrayBlob(content.getBytes("UTF-8"),
                "text/plain");
        file1.setPropertyValue("content", blob1);
        file1.setPropertyValue("filename", filename);
        Calendar cal1 = getCalendar(2007, 3, 1, 12, 0, 0);
        file1.setPropertyValue("dc:created", cal1);
        file1.setPropertyValue("dc:coverage", "foo/bar");
        file1.setPropertyValue("dc:subjects", new String[] { "foo", "gee/moo" });
        file1 = session.createDocument(file1);

        DocumentModel file2 = new DocumentModelImpl("/testfolder1",
                "testfile2", "File");
        file2.setPropertyValue("dc:title", "testfile2_Title");
        file2.setPropertyValue("dc:description", "testfile2_DESCRIPTION2");
        Calendar cal2 = getCalendar(2007, 4, 1, 12, 0, 0);
        file2.setPropertyValue("dc:created", cal2);
        file2.setPropertyValue("dc:contributors",
                new String[] { "bob", "pete" });
        file2.setPropertyValue("dc:coverage", "football");
        file2 = session.createDocument(file2);

        DocumentModel file3 = new DocumentModelImpl("/testfolder1",
                "testfile3", "Note");
        file3.setPropertyValue("dc:title", "testfile3_Title");
        file3.setPropertyValue("dc:description",
                "testfile3_desc1 testfile3_desc2,  testfile3_desc3");
        file3.setPropertyValue("dc:contributors",
                new String[] { "bob", "john" });
        file3 = session.createDocument(file3);

        DocumentModel folder2 = new DocumentModelImpl("/", "testfolder2",
                "Folder");
        folder2 = session.createDocument(folder2);
        folder2id = folder2.getId();

        DocumentModel folder3 = new DocumentModelImpl("/testfolder2",
                "testfolder3", "Folder");
        folder3 = session.createDocument(folder3);

        // create file 4
        DocumentModel file4 = new DocumentModelImpl("/testfolder2/testfolder3",
                "testfile4", "File");
        file4.setPropertyValue("dc:title", "testfile4_Title");
        file4.setPropertyValue("dc:description", "testfile4_DESCRIPTION4");
        file4 = session.createDocument(file4);
        file4id = file4.getId();

        session.save();

        Framework.getLocalService(EventService.class).waitForAsyncCompletion();
        DatabaseHelper.DATABASE.sleepForFulltext();
    }

    public void testBasic() {
        assertNotNull(repository);
        Connection conn = repository.getConnection(null);
        assertNotNull(conn);

        Folder root = conn.getRootFolder();
        assertNotNull(root);
        Type rootType = root.getType();
        assertNotNull(rootType);
        assertEquals(ROOT_TYPE_ID, rootType.getId());
        assertEquals(ROOT_TYPE_ID, root.getTypeId());
        assertEquals("CMIS_Root_Folder", root.getName()); // from the spec
        assertEquals(null, root.getParent());
        Map<String, Property> props = root.getProperties();
        assertNotNull(props);
        assertTrue(props.size() > 0);

        List<CMISObject> entries = root.getChildren();
        assertEquals(2, entries.size());
    }

    public void testQuery() {
        Connection conn = repository.getConnection(null);
        Collection<CMISObject> res = conn.query("SELECT * FROM cmis:document",
                false);
        assertNotNull(res);
        assertEquals(4, res.size());
        res = conn.query("SELECT * FROM cmis:folder", false);
        assertEquals(3, res.size());
        res = conn.query(
                "SELECT * FROM cmis:document WHERE dc:title = 'testfile1_Title'",
                false);
        assertEquals(1, res.size());
        // spec says names are case-insensitive
        res = conn.query(
                "SELECT * FROM CMIS:DOCUMENT WHERE DC:TITLE = 'testfile1_Title'",
                false);
        assertEquals(1, res.size());

        // CMIS ANY syntax for multi-valued properties
        res = conn.query(
                "SELECT * FROM cmis:document WHERE 'pete' = ANY dc:contributors",
                false);
        assertEquals(1, res.size());
        res = conn.query(
                "SELECT * FROM cmis:document WHERE 'bob' = ANY dc:contributors",
                false);
        assertEquals(2, res.size());

        // CMIS fulltext
        res = conn.query(
                "SELECT * FROM cmis:document WHERE CONTAINS(,'restaurant')",
                false);
        assertEquals(1, res.size());
        res = conn.query(
                "SELECT * FROM cmis:document WHERE CONTAINS('restaurant')",
                false);
        assertEquals(1, res.size());

        // CMIS IN_TREE / IN_FOLDER
        res = conn.query(
                String.format(
                        "SELECT * FROM cmis:document WHERE IN_FOLDER('%s')",
                        folder1id), false);
        assertEquals(3, res.size());
        res = conn.query(String.format(
                "SELECT * FROM cmis:document WHERE IN_TREE('%s')", folder2id),
                false);
        assertEquals(1, res.size());

        // special CMIS properties
        res = conn.query(String.format(
                "SELECT * FROM cmis:document WHERE cmis:ObjectId = '%s'",
                file4id), false);
        assertEquals(1, res.size());
        res = conn.query(String.format(
                "SELECT * FROM cmis:document WHERE cmis:ParentId = '%s'",
                folder1id), false);
        assertEquals(3, res.size());
        res = conn.query(
                "SELECT * FROM cmis:document WHERE cmis:ObjectTypeId = 'File'",
                false);
        assertEquals(3, res.size());
        res = conn.query(
                "SELECT * FROM cmis:document WHERE cmis:Name = 'testfile4'",
                false);
        assertEquals(1, res.size());
    }

}
