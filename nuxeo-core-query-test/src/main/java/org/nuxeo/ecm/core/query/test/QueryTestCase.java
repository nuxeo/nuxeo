/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Dragos Mihalache
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.query.test;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TimeZone;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.Filter;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.FacetFilter;
import org.nuxeo.ecm.core.api.impl.blob.ByteArrayBlob;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author Dragos Mihalache
 * @author Florent Guillaume
 * @author Benjamin Jalon
 */
public abstract class QueryTestCase extends NXRuntimeTestCase {

    public static final String REPOSITORY_NAME = "test";

    public CoreSession session;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.schema");
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.core");
        deployContrib("org.nuxeo.ecm.core.query.test",
                "OSGI-INF/core-types-contrib.xml");
        deployRepository();
        openSession();
    }

    public abstract void deployRepository() throws Exception;

    public abstract void undeployRepository() throws Exception;

    @Override
    public void tearDown() throws Exception {
        try {
            closeSession();
        } catch (Exception e) {
            // ignore
        }
        super.tearDown();
        // undeploy repository last
        try {
            undeployRepository();
        } catch (Exception e) {
            // ignore
        }
    }

    public void openSession() throws ClientException {
        session = openSessionAs(SecurityConstants.ADMINISTRATOR);
        assertNotNull(session);
    }

    protected CoreSession openSessionAs(String username) throws ClientException {
        Map<String, Serializable> context = new HashMap<String, Serializable>();
        context.put("username", username);
        return CoreInstance.getInstance().open(REPOSITORY_NAME, context);
    }

    public void closeSession() {
        closeSession(session);
    }

    protected void closeSession(CoreSession session) {
        CoreInstance.getInstance().close(session);
    }

    // ---------------------------------------

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

    /**
     * Creates the following structure of documents:
     *
     * <pre>
     *  root (UUID_1)
     *  |- testfolder1 (UUID_2)
     *  |  |- testfile1 (UUID_3) (content UUID_4)
     *  |  |- testfile2 (UUID_5) (content UUID_6)
     *  |  \- testfile3 (Note) (UUID_7) (trans UUID_8 stylesheet UUID_9)
     *  \- tesfolder2 (UUID_10)
     *     \- testfolder3 (UUID_11)
     *        \- testfile4 (UUID_12) (content UUID_13)
     * </pre>
     */
    protected void createDocs() throws Exception {
        DocumentModel folder1 = new DocumentModelImpl("/", "testfolder1",
                "Folder");
        folder1.setPropertyValue("dc:title", "testfolder1_Title");
        folder1 = session.createDocument(folder1);

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
        file1.setPropertyValue("uid", "uid123");
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

        DocumentModel folder3 = new DocumentModelImpl("/testfolder2",
                "testfolder3", "Folder");
        folder3 = session.createDocument(folder3);

        // create file 4
        DocumentModel file4 = new DocumentModelImpl("/testfolder2/testfolder3",
                "testfile4", "File");
        file4.setPropertyValue("dc:title", "testfile4_Title");
        file4.setPropertyValue("dc:description", "testfile4_DESCRIPTION4");
        file4 = session.createDocument(file4);

        session.save();
    }

    /**
     * Publishes testfile4 to testfolder1:
     * <p>
     * version (UUID_14, content UUID_15)
     * <p>
     * proxy (UUID_16)
     */
    protected DocumentModel publishDoc() throws Exception {
        DocumentModel doc = session.getDocument(new PathRef(
                "/testfolder2/testfolder3/testfile4"));
        DocumentModel sec = session.getDocument(new PathRef("/testfolder1"));
        DocumentModel proxy = session.publishDocument(doc, sec);
        session.save();
        DocumentModelList proxies = session.getProxies(doc.getRef(),
                sec.getRef());
        assertEquals(1, proxies.size());
        return proxy;
    }

    // from TestAPI

    public void testQueryBasic() throws Exception {
        DocumentModelList dml;
        createDocs();

        dml = session.query("SELECT * FROM File");
        assertEquals(3, dml.size());
        for (DocumentModel dm : dml) {
            assertEquals("File", dm.getType());
        }

        dml = session.query("SELECT * FROM Note");
        assertEquals(1, dml.size());
        for (DocumentModel dm : dml) {
            assertEquals("Note", dm.getType());
        }

        dml = session.query("SELECT * FROM Folder");
        assertEquals(3, dml.size());
        for (DocumentModel dm : dml) {
            assertEquals("Folder", dm.getType());
        }

        dml = session.query("SELECT * FROM Document");
        assertEquals(7, dml.size());

        dml = session.query("SELECT * FROM File");
        assertEquals(3, dml.size());

        dml = session.query("SELECT * FROM File WHERE dc:title = 'testfile1_Title'");
        assertEquals(1, dml.size());

        dml = session.query("SELECT * FROM File WHERE NOT dc:title = 'testfile1_Title'");
        assertEquals(2, dml.size());

        dml = session.query("SELECT * FROM Document WHERE dc:title = 'testfile1_Title'");
        assertEquals(1, dml.size());

        dml = session.query("SELECT * FROM File WHERE dc:title = 'testfile1_Title' OR dc:title = 'testfile2_Title'");
        assertEquals(2, dml.size());

        dml = session.query("SELECT * FROM File WHERE dc:title = 'testfolder1_Title'");
        assertEquals(0, dml.size());

        dml = session.query("SELECT * FROM File WHERE filename = 'testfile.txt'");
        assertEquals(1, dml.size());

        dml = session.query("SELECT * FROM Note WHERE dc:title = 'testfile3_Title'");
        assertEquals(1, dml.size());

        // property in a schema with no prefix
        dml = session.query("SELECT * FROM Document WHERE uid = 'uid123'");
        assertEquals(1, dml.size());
        // compat syntax for old search service:
        dml = session.query("SELECT * FROM Document WHERE uid:uid = 'uid123'");
        assertEquals(1, dml.size());

        // this needs an actual LEFT OUTER JOIN
        dml = session.query("SELECT * FROM Document WHERE filename = 'testfile.txt' OR dc:title = 'testfile3_Title'");
        assertEquals(2, dml.size());

        dml = session.query("SELECT * FROM Document WHERE filename = 'testfile.txt' OR dc:contributors = 'bob'");
        assertEquals(3, dml.size());

        dml = session.query("SELECT * FROM Document WHERE dc:created BETWEEN DATE '2007-01-01' AND DATE '2008-01-01'");
        assertEquals(2, dml.size());

        dml = session.query("SELECT * FROM Document WHERE dc:created BETWEEN DATE '2007-03-15' AND DATE '2008-01-01'");
        assertEquals(1, dml.size());

        // early detection of conflicting types for VCS
        dml = session.query("SELECT * FROM Document WHERE ecm:primaryType = 'foo'");
        assertEquals(0, dml.size());
    }

    public void testQueryMultiple() throws Exception {
        DocumentModelList dml;
        createDocs();

        dml = session.query("SELECT * FROM File WHERE dc:contributors = 'pete'");
        assertEquals(1, dml.size());

        dml = session.query("SELECT * FROM File WHERE dc:contributors = 'bob'");
        assertEquals(1, dml.size());

        dml = session.query("SELECT * FROM Document WHERE dc:contributors = 'bob'");
        assertEquals(2, dml.size());

        dml = session.query("SELECT * FROM Document WHERE dc:contributors IN ('bob', 'pete')");
        assertEquals(2, dml.size());

        dml = session.query("SELECT * FROM Document WHERE dc:contributors IN ('bob', 'john')");
        assertEquals(2, dml.size());

        dml = session.query("SELECT * FROM Document WHERE dc:contributors NOT IN ('bob', 'pete')");
        assertEquals(5, dml.size());

        dml = session.query("SELECT * FROM Document WHERE dc:contributors NOT IN ('bob', 'john')");
        assertEquals(5, dml.size());

        dml = session.query("SELECT * FROM Document WHERE dc:contributors LIKE 'pe%'");
        assertEquals(1, dml.size());

        dml = session.query("SELECT * FROM Document WHERE dc:contributors LIKE 'bo%'");
        assertEquals(2, dml.size());

        dml = session.query("SELECT * FROM Document WHERE dc:contributors LIKE '%o%'");
        assertEquals(2, dml.size());

        dml = session.query("SELECT * FROM Document WHERE dc:subjects LIKE '%oo%'");
        assertEquals(1, dml.size());

        dml = session.query("SELECT * FROM File WHERE dc:subjects NOT LIKE '%oo%'");
        assertEquals(2, dml.size());
    }

    // this is disabled for JCR
    public void testQueryNegativeMultiple() throws Exception {
        DocumentModelList dml;
        createDocs();
        dml = session.query("SELECT * FROM Document WHERE dc:contributors <> 'pete'");
        assertEquals(6, dml.size());

        dml = session.query("SELECT * FROM Document WHERE dc:contributors <> 'blah'");
        assertEquals(7, dml.size());

        dml = session.query("SELECT * FROM File WHERE dc:contributors <> 'blah' AND ecm:isProxy = 0");
        assertEquals(3, dml.size());

        dml = session.query("SELECT * FROM Document WHERE ecm:mixinType = 'Versionable' AND ecm:mixinType <> 'Downloadable'");
        assertEquals(1, dml.size()); // 1 note
    }

    public void testQueryAfterEdit() throws ClientException, IOException {
        DocumentModel root = session.getRootDocument();

        String fname1 = "file1";
        DocumentModel childFile1 = new DocumentModelImpl(
                root.getPathAsString(), fname1, "File");

        DocumentModel[] childDocs = new DocumentModel[1];
        childDocs[0] = childFile1;

        DocumentModel[] returnedChildDocs = session.createDocument(childDocs);
        assertEquals(1, returnedChildDocs.length);

        childFile1 = returnedChildDocs[0];
        childFile1.setProperty("file", "filename", "f1");

        // add a blob
        String s = "<html><head/><body>La la la!</body></html>";
        Blob blob = new ByteArrayBlob(s.getBytes("UTF-8"), "text/html");
        childFile1.setProperty("file", "content", blob);

        session.saveDocument(childFile1);
        session.save();

        DocumentModelList list;
        DocumentModel docModel;

        list = session.query("SELECT * FROM Document");
        assertEquals(1, list.size());
        docModel = list.get(0);

        // read the properties
        docModel.getProperty("dublincore", "title");

        // XXX: FIXME: OG the following throws a class cast exception since the
        // get property returns an HashMap instance instead of a LazyBlob when
        // the tests are all run together:

        Blob blob2 = (Blob) docModel.getProperty("file", "content");
        assertEquals(s.length(), blob2.getLength()); // only ascii chars
        assertEquals("text/html", blob2.getMimeType());

        // edit the title without touching the blob
        docModel.setProperty("dublincore", "title", "edited title");
        docModel.setProperty("dublincore", "description", "edited description");
        session.saveDocument(docModel);
        session.save();

        list = session.query("SELECT * FROM Document");
        assertEquals(1, list.size());
        docModel = list.get(0);

        session.removeDocument(docModel.getRef());
    }

    // from TestQuery in nuxeo-core-jcr-connector-test

    public void testOrderBy() throws Exception {
        String sql;
        DocumentModelList dml;
        createDocs();

        sql = "SELECT * FROM Document WHERE dc:title LIKE 'testfile%' ORDER BY dc:description";
        dml = session.query(sql);
        assertEquals(4, dml.size());
        assertEquals("testfile1_description", dml.get(0).getPropertyValue(
                "dc:description"));

        sql = "SELECT * FROM Document WHERE dc:title LIKE 'testfile%' ORDER BY dc:description DESC";
        dml = session.query(sql);
        assertEquals(4, dml.size());
        assertEquals("testfile4_DESCRIPTION4", dml.get(0).getPropertyValue(
                "dc:description"));
    }

    public void testOrderByPath() throws Exception {
        String sql;
        DocumentModelList dml;
        createDocs();

        sql = "SELECT * FROM Document ORDER BY ecm:path";
        dml = session.query(sql);
        assertEquals(7, dml.size());
        assertEquals("/testfolder1", dml.get(0).getPathAsString());
        assertEquals("/testfolder1/testfile1", dml.get(1).getPathAsString());
        assertEquals("/testfolder1/testfile2", dml.get(2).getPathAsString());
        assertEquals("/testfolder1/testfile3", dml.get(3).getPathAsString());
        assertEquals("/testfolder2", dml.get(4).getPathAsString());
        assertEquals("/testfolder2/testfolder3", dml.get(5).getPathAsString());
        assertEquals("/testfolder2/testfolder3/testfile4",
                dml.get(6).getPathAsString());

        sql = "SELECT * FROM Document ORDER BY ecm:path DESC";
        dml = session.query(sql);
        assertEquals(7, dml.size());
        assertEquals("/testfolder2/testfolder3/testfile4",
                dml.get(0).getPathAsString());
        assertEquals("/testfolder1", dml.get(6).getPathAsString());

        // then with batching

        sql = "SELECT * FROM Document ORDER BY ecm:path";
        dml = session.query(sql, null, 2, 3, false);
        assertEquals(2, dml.size());
        assertEquals("/testfolder1/testfile3", dml.get(0).getPathAsString());
        assertEquals("/testfolder2", dml.get(1).getPathAsString());
    }

    // this is disabled for JCR
    public void testOrderByPos() throws Exception {
        DocumentModelList dml;

        DocumentModel ofolder = new DocumentModelImpl("/", "ofolder",
                "OrderedFolder");
        ofolder = session.createDocument(ofolder);
        DocumentModel file1 = new DocumentModelImpl("/ofolder", "testfile1",
                "File");
        file1 = session.createDocument(file1);
        DocumentModel file2 = new DocumentModelImpl("/ofolder", "testfile2",
                "File");
        file2 = session.createDocument(file2);
        DocumentModel file3 = new DocumentModelImpl("/ofolder", "testfile3",
                "File");
        file3 = session.createDocument(file3);
        session.save();

        String sql = String.format(
                "SELECT * FROM Document WHERE ecm:parentId = '%s' ORDER BY ecm:pos",
                ofolder.getId());
        String sqldesc = sql + " DESC";

        dml = session.query(sql);
        assertEquals(3, dml.size());
        assertEquals(file1.getId(), dml.get(0).getId());
        assertEquals(file2.getId(), dml.get(1).getId());
        assertEquals(file3.getId(), dml.get(2).getId());

        dml = session.query(sqldesc);
        assertEquals(file3.getId(), dml.get(0).getId());
        assertEquals(file2.getId(), dml.get(1).getId());
        assertEquals(file1.getId(), dml.get(2).getId());

        session.orderBefore(ofolder.getRef(), "testfile3", "testfile2");
        session.save();

        dml = session.query(sql);
        assertEquals(file1.getId(), dml.get(0).getId());
        assertEquals(file3.getId(), dml.get(1).getId());
        assertEquals(file2.getId(), dml.get(2).getId());

        dml = session.query(sqldesc);
        assertEquals(file2.getId(), dml.get(0).getId());
        assertEquals(file3.getId(), dml.get(1).getId());
        assertEquals(file1.getId(), dml.get(2).getId());

        // test ecm:pos as a field
        sql = "SELECT * FROM Document WHERE ecm:pos = 1";
        dml = session.query(sql);
        assertEquals(1, dml.size());
        assertEquals(file3.getId(), dml.iterator().next().getId());
    }

    public void testBatching() throws Exception {
        doBatching(true);
    }

    public void doBatching(boolean checkNames) throws Exception {
        DocumentModelList dml;
        createDocs();

        String sql = "SELECT * FROM Document ORDER BY ecm:name";

        dml = session.query(sql);
        assertEquals(7, dml.size());
        assertEquals(7, dml.totalSize());
        if (checkNames) {
            assertEquals("testfile1", dml.get(0).getName());
            assertEquals("testfile2", dml.get(1).getName());
            assertEquals("testfile3", dml.get(2).getName());
            assertEquals("testfile4", dml.get(3).getName());
            assertEquals("testfolder1", dml.get(4).getName());
            assertEquals("testfolder2", dml.get(5).getName());
            assertEquals("testfolder3", dml.get(6).getName());
        }

        dml = session.query(sql, null, 99, 0, true);
        assertEquals(7, dml.size());
        assertEquals(7, dml.totalSize());
        if (checkNames) {
            assertEquals("testfile1", dml.get(0).getName());
            assertEquals("testfolder3", dml.get(6).getName());
        }

        dml = session.query(sql, null, 7, 0, true);
        assertEquals(7, dml.size());
        assertEquals(7, dml.totalSize());
        if (checkNames) {
            assertEquals("testfile1", dml.get(0).getName());
            assertEquals("testfolder3", dml.get(6).getName());
        }

        dml = session.query(sql, null, 6, 0, true);
        assertEquals(6, dml.size());
        assertEquals(7, dml.totalSize());
        if (checkNames) {
            assertEquals("testfile1", dml.get(0).getName());
            assertEquals("testfolder2", dml.get(5).getName());
        }

        dml = session.query(sql, null, 6, 1, true);
        assertEquals(6, dml.size());
        assertEquals(7, dml.totalSize());
        if (checkNames) {
            assertEquals("testfile2", dml.get(0).getName());
            assertEquals("testfolder3", dml.get(5).getName());
        }

        dml = session.query(sql, null, 99, 3, true);
        assertEquals(4, dml.size());
        assertEquals(7, dml.totalSize());
        if (checkNames) {
            assertEquals("testfile4", dml.get(0).getName());
            assertEquals("testfolder3", dml.get(3).getName());
        }

        dml = session.query(sql, null, 99, 50, true);
        assertEquals(0, dml.size());
        assertEquals(7, dml.totalSize());
    }

    // from TestSQLWithPath
    public void testEcmPathEqual() throws Exception {
        String sql;
        DocumentModelList dml;
        createDocs();

        sql = "SELECT * FROM document WHERE ecm:path='/testfolder1/'";
        dml = session.query(sql);
        assertEquals(1, dml.size());
    }

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

        sql = "SELECT * FROM document WHERE dc:title='testfile1_Title' AND ecm:path STARTSWITH '/'";
        dml = session.query(sql);
        assertEquals(1, dml.size());

        sql = "SELECT * FROM document WHERE dc:title LIKE 'testfile%' AND ecm:path STARTSWITH '/'";
        dml = session.query(sql);
        assertEquals(4, dml.size());
    }

    public void testStartsWithNonPath() throws Exception {
        String sql;
        createDocs();

        sql = "SELECT * FROM Document WHERE dc:coverage STARTSWITH 'foo'";
        assertEquals(1, session.query(sql).size());

        sql = "SELECT * FROM Document WHERE dc:coverage STARTSWITH 'foo/bar'";
        assertEquals(1, session.query(sql).size());

        sql = "SELECT * FROM Document WHERE dc:coverage STARTSWITH 'foo/bar/baz'";
        assertEquals(0, session.query(sql).size());

        sql = "SELECT * FROM Document WHERE dc:subjects STARTSWITH 'foo'";
        assertEquals(1, session.query(sql).size());

        sql = "SELECT * FROM Document WHERE dc:subjects STARTSWITH 'gee'";
        assertEquals(1, session.query(sql).size());

        sql = "SELECT * FROM Document WHERE dc:subjects STARTSWITH 'gee/moo'";
        assertEquals(1, session.query(sql).size());

        sql = "SELECT * FROM Document WHERE dc:subjects STARTSWITH 'gee/moo/blah'";
        assertEquals(0, session.query(sql).size());
    }

    public void testReindexEditedDocument() throws Exception {
        String sql = "SELECT * FROM document WHERE dc:title LIKE 'testfile1_Ti%'";
        DocumentModelList dml;
        createDocs();

        dml = session.query(sql);
        assertEquals(1, dml.size());

        DocumentModel file1 = session.getDocument(new PathRef(
                "/testfolder1/testfile1"));

        // edit file1
        file1.setPropertyValue("dc:description", "testfile1_description");
        file1.setPropertyValue("content", null);
        session.saveDocument(file1);
        session.save();

        // rerunning the same query
        dml = session.query(sql);
        assertEquals(1, dml.size());

        // edit the title
        file1.setPropertyValue("dc:title", "testfile1_ModifiedTitle");
        session.saveDocument(file1);
        session.save();

        // rerun the same query
        dml = session.query(sql);
        assertEquals(0, dml.size());

        // editithe title
        file1.setPropertyValue("dc:description", "Yet another description");
        session.saveDocument(file1);
        session.save();

        // adjust the query to the new title
        sql = "SELECT * FROM document WHERE dc:title LIKE 'testfile1_Mo%'";
        dml = session.query(sql);
        assertEquals(1, dml.size());
    }

    // from TestSQLWithDate

    public void testDate() throws Exception {
        String sql;
        DocumentModelList dml;
        createDocs();

        sql = "SELECT * FROM document WHERE dc:created >= DATE '2007-01-01'";
        dml = session.query(sql);
        assertEquals(2, dml.size());

        sql = "SELECT * FROM document WHERE dc:created >= DATE '2007-03-15'";
        dml = session.query(sql);
        assertEquals(1, dml.size());

        sql = "SELECT * FROM document WHERE dc:created >= DATE '2007-05-01'";
        dml = session.query(sql);
        assertEquals(0, dml.size());

        sql = "SELECT * FROM document WHERE dc:created >= TIMESTAMP '2007-03-15 00:00:00'";
        dml = session.query(sql);
        assertEquals(1, dml.size());

        sql = "SELECT * FROM document WHERE dc:created >= DATE '2007-02-15' AND dc:created <= DATE '2007-03-15'";
        dml = session.query(sql);
        assertEquals(1, dml.size());
    }

    // other tests

    public void testBoolean() throws Exception {
        String sql;
        DocumentModelList dml;
        createDocs();

        sql = "SELECT * FROM document WHERE my:boolean = 1";
        dml = session.query(sql);
        assertEquals(0, dml.size());
        sql = "SELECT * FROM document WHERE my:boolean = 0";
        dml = session.query(sql);
        assertEquals(0, dml.size());

        DocumentModel doc = new DocumentModelImpl("/testfolder1", "mydoc",
                "MyDocType");
        doc.setPropertyValue("my:boolean", Boolean.TRUE);
        doc = session.createDocument(doc);
        session.save();

        sql = "SELECT * FROM document WHERE my:boolean = 1";
        dml = session.query(sql);
        assertEquals(1, dml.size());
        sql = "SELECT * FROM document WHERE my:boolean = 0";
        dml = session.query(sql);
        assertEquals(0, dml.size());

        doc.setPropertyValue("my:boolean", Boolean.FALSE);
        session.saveDocument(doc);
        session.save();

        sql = "SELECT * FROM document WHERE my:boolean = 1";
        dml = session.query(sql);
        assertEquals(0, dml.size());
        sql = "SELECT * FROM document WHERE my:boolean = 0";
        dml = session.query(sql);
        assertEquals(1, dml.size());
    }

    public void testQueryWithSecurity() throws Exception {
        createDocs();
        DocumentModel root = session.getRootDocument();
        ACP acp = new ACPImpl();
        ACL acl = new ACLImpl();
        acl.add(new ACE("Administrator", "Everything", true));
        acl.add(new ACE("bob", "Browse", true));
        acp.addACL(acl);
        root.setACP(acp, true);
        DocumentModel folder1 = session.getDocument(new PathRef("/testfolder1"));
        acp = new ACPImpl();
        acl = new ACLImpl();
        acl.add(new ACE("bob", "Browse", false));
        acp.addACL(acl);
        folder1.setACP(acp, true);
        session.save();
        closeSession();
        session = openSessionAs("bob");

        DocumentModelList dml = session.query("SELECT * FROM Document");
        assertEquals(3, dml.size());
    }

    public void testSecurityManagerBasic() throws Exception {
        doTestSecurityManager("OSGI-INF/security-policy-contrib.xml");
    }

    public void testSecurityManagerWithTransformer() throws Exception {
        doTestSecurityManager("OSGI-INF/security-policy2-contrib.xml");
    }

    public void doTestSecurityManager(String contrib) throws Exception {
        createDocs();
        DocumentModelList dml;

        dml = session.query("SELECT * FROM Document");
        assertEquals(7, dml.size());
        assertEquals(7, dml.totalSize());
        dml = session.query("SELECT * FROM Document", null, 2, 0, true);
        assertEquals(2, dml.size());
        assertEquals(7, dml.totalSize());
        dml = session.query("SELECT * FROM Document", null, 2, 5, true);
        assertEquals(2, dml.size());
        assertEquals(7, dml.totalSize());
        dml = session.query("SELECT * FROM Document", null, 2, 6, true);
        assertEquals(1, dml.size());
        assertEquals(7, dml.totalSize());

        // now add a security policy hiding docs of type File
        deployContrib("org.nuxeo.ecm.core.query.test", contrib);

        dml = session.query("SELECT * FROM Document");
        assertEquals(4, dml.size());
        assertEquals(4, dml.totalSize());
        dml = session.query("SELECT * FROM Document", null, 2, 0, true);
        assertEquals(2, dml.size());
        assertEquals(4, dml.totalSize());
        dml = session.query("SELECT * FROM Document", null, 2, 2, true);
        assertEquals(2, dml.size());
        assertEquals(4, dml.totalSize());
        dml = session.query("SELECT * FROM Document", null, 2, 3, true);
        assertEquals(1, dml.size());
        assertEquals(4, dml.totalSize());

        // add an ACL as well
        DocumentModel root = session.getRootDocument();
        ACP acp = new ACPImpl();
        ACL acl = new ACLImpl();
        acl.add(new ACE("Administrator", "Everything", true));
        acl.add(new ACE("bob", "Browse", true));
        acp.addACL(acl);
        root.setACP(acp, true);
        DocumentModel folder1 = session.getDocument(new PathRef(
                "/testfolder2/testfolder3"));
        acp = new ACPImpl();
        acl = new ACLImpl();
        acl.add(new ACE("bob", "Browse", false));
        acp.addACL(acl);
        folder1.setACP(acp, true);
        session.save();
        closeSession();
        session = openSessionAs("bob");

        dml = session.query("SELECT * FROM Document");
        assertEquals(3, dml.size());
        assertEquals(3, dml.totalSize());
        dml = session.query("SELECT * FROM Document", null, 2, 0, true);
        assertEquals(2, dml.size());
        assertEquals(3, dml.totalSize());
        dml = session.query("SELECT * FROM Document", null, 2, 1, true);
        assertEquals(2, dml.size());
        assertEquals(3, dml.totalSize());
        dml = session.query("SELECT * FROM Document", null, 2, 2, true);
        assertEquals(1, dml.size());
        assertEquals(3, dml.totalSize());
    }

    private void assertIdSet(DocumentModelList dml, String... ids) {
        Collection<String> expected = new HashSet<String>(Arrays.asList(ids));
        Collection<String> actual = new HashSet<String>();
        for (DocumentModel d : dml) {
            actual.add(d.getId());
        }
        assertEquals(expected, actual);
    }

    public void testQueryWithProxies() throws Exception {
        createDocs();
        DocumentModel proxy = publishDoc();

        DocumentModel doc = session.getDocument(new PathRef(
                "/testfolder2/testfolder3/testfile4"));
        String docId = doc.getId();
        String proxyId = proxy.getId();
        String versionId = proxy.getSourceId();
        assertNotSame(docId, proxyId);
        assertNotNull(versionId);
        assertNotSame(docId, versionId);
        assertNotSame(proxyId, versionId);

        DocumentModelList dml;
        Filter filter;

        // queries must return proxies *and versions*
        dml = session.query("SELECT * FROM Document WHERE dc:title = 'testfile4_Title'");
        assertIdSet(dml, docId, proxyId, versionId);

        // facet filter: immutable
        filter = new FacetFilter(FacetNames.IMMUTABLE, true);
        dml = session.query(
                "SELECT * FROM Document WHERE dc:title = 'testfile4_Title'",
                filter, 99);
        assertIdSet(dml, proxyId, versionId);

        // facet filter: not immutable
        filter = new FacetFilter(FacetNames.IMMUTABLE, false);
        dml = session.query(
                "SELECT * FROM Document WHERE dc:title = 'testfile4_Title'",
                filter, 99);
        assertIdSet(dml, docId);

        dml = session.query("SELECT * FROM Document WHERE ecm:isProxy = 1");
        assertIdSet(dml, proxyId);

        dml = session.query("SELECT * FROM Document WHERE ecm:isProxy = 0");
        assertEquals(8, dml.size()); // 7 folder/docs, 1 version

        dml = session.query("SELECT * FROM Document WHERE ecm:isCheckedInVersion = 1");
        assertIdSet(dml, versionId);

        dml = session.query("SELECT * FROM Document WHERE ecm:isCheckedInVersion = 0");
        assertEquals(8, dml.size()); // 7 folder/docs, 1 proxy

        dml = session.query("SELECT * FROM Document WHERE ecm:isProxy = 0 AND ecm:isCheckedInVersion = 0");
        assertEquals(7, dml.size()); // 7 folder/docs

        // filter out proxies explicitely, keeps live and version
        dml = session.query("SELECT * FROM Document WHERE dc:title = 'testfile4_Title' AND ecm:isProxy = 0");
        assertIdSet(dml, docId, versionId);

        // only keep proxies
        dml = session.query("SELECT * FROM Document WHERE dc:title = 'testfile4_Title' AND ecm:isProxy = 1");
        assertIdSet(dml, proxyId);

        // only keep versions
        dml = session.query("SELECT * FROM Document WHERE dc:title = 'testfile4_Title' AND ecm:isCheckedInVersion = 1");
        assertIdSet(dml, versionId);

        // only keep immutable (proxy + version)
        dml = session.query("SELECT * FROM Document WHERE ecm:mixinType = 'Immutable'");
        assertIdSet(dml, proxyId, versionId);

        dml = session.query("SELECT * FROM Document WHERE ecm:mixinType = 'Immutable' AND ecm:isProxy = 0");
        assertIdSet(dml, versionId);

        dml = session.query("SELECT * FROM Document WHERE ecm:mixinType = 'Immutable' AND ecm:isProxy = 1");
        assertIdSet(dml, proxyId);

        dml = session.query("SELECT * FROM Document WHERE ecm:mixinType = 'Immutable' AND ecm:isCheckedInVersion = 0");
        assertIdSet(dml, proxyId);

        dml = session.query("SELECT * FROM Document WHERE ecm:mixinType = 'Immutable' AND ecm:isCheckedInVersion = 1");
        assertIdSet(dml, versionId);

        // conflict between where and filter
        filter = new FacetFilter(FacetNames.IMMUTABLE, false);
        dml = session.query(
                "SELECT * FROM Document WHERE ecm:mixinType = 'Immutable'",
                filter, 99);
        assertEquals(0, dml.size()); // contradictory clauses

        // "deep" isProxy
        dml = session.query("SELECT * FROM Document WHERE (dc:title = 'blah' OR ecm:isProxy = 1)");
        assertIdSet(dml, proxyId);
        dml = session.query("SELECT * FROM Document WHERE ecm:isProxy = 0 AND (dc:title = 'testfile1_Title' OR ecm:isProxy = 1)");
        assertEquals(1, dml.size());

        // proxy query with order by
        dml = session.query("SELECT * FROM Document WHERE dc:title = 'testfile4_Title' ORDER BY dc:title");
        assertIdSet(dml, docId, proxyId, versionId);
        dml = session.query("SELECT * FROM File WHERE dc:title = 'testfile4_Title' ORDER BY dc:description");
        assertIdSet(dml, docId, proxyId, versionId);
    }

    // this is disabled for JCR
    public void testQueryWithProxiesNegativeMultiple() throws Exception {
        createDocs();
        publishDoc();
        DocumentModelList dml;
        Filter filter;

        dml = session.query("SELECT * FROM Document WHERE ecm:mixinType <> 'Immutable' AND ecm:isProxy = 0");
        assertEquals(7, dml.size()); // 7 folder/docs

        dml = session.query("SELECT * FROM Document WHERE ecm:mixinType <> 'Immutable' AND ecm:isProxy = 1");
        assertEquals(0, dml.size()); // contradictory clauses

        dml = session.query("SELECT * FROM Document WHERE ecm:mixinType <> 'Immutable' AND ecm:isCheckedInVersion = 0");
        assertEquals(7, dml.size()); // 7 folder/docs

        dml = session.query("SELECT * FROM Document WHERE ecm:mixinType <> 'Immutable' AND ecm:isCheckedInVersion = 1");
        assertEquals(0, dml.size()); // contradictory clauses

        // conflict between where and filter
        filter = new FacetFilter(FacetNames.IMMUTABLE, true);
        dml = session.query(
                "SELECT * FROM Document WHERE ecm:mixinType <> 'Immutable'",
                filter, 99);
        assertEquals(0, dml.size()); // contradictory clauses
    }

    public void testQuerySpecialFields() throws Exception {
        // ecm:isProxy and ecm:isCheckedInVersion are already tested in
        // testQueryWithProxies

        // ecm:path already tested in testStartsWith

        createDocs();
        DocumentModel proxy = publishDoc();
        DocumentModel version = session.getDocument(new IdRef(
                proxy.getSourceId()));

        DocumentModelList dml;
        DocumentModel folder1 = session.getDocument(new PathRef("/testfolder1"));
        DocumentModel file1 = session.getDocument(new PathRef(
                "/testfolder1/testfile1"));
        DocumentModel file2 = session.getDocument(new PathRef(
                "/testfolder1/testfile2"));
        DocumentModel file3 = session.getDocument(new PathRef(
                "/testfolder1/testfile3"));
        DocumentModel file4 = session.getDocument(new PathRef(
                "/testfolder2/testfolder3/testfile4"));

        /*
         * ecm:uuid
         */
        dml = session.query(String.format(
                "SELECT * FROM Document WHERE ecm:uuid = '%s'", file1.getId()));
        assertIdSet(dml, file1.getId());
        dml = session.query(String.format(
                "SELECT * FROM Document WHERE ecm:uuid = '%s'", proxy.getId()));
        assertIdSet(dml, proxy.getId());

        /*
         * ecm:name
         */
        dml = session.query(String.format(
                "SELECT * FROM Document WHERE ecm:name = '%s'", file1.getName()));
        assertIdSet(dml, file1.getId());
        // Disabled, version and proxies names don't need to be identical
        // dml = session.query(String.format(
        // "SELECT * FROM Document WHERE ecm:name = '%s'", file4.getName()));
        // assertIdSet(dml, file4.getId(), proxy.getId(), version.getId());

        /*
         * ecm:parentId
         */
        dml = session.query(String.format(
                "SELECT * FROM Document WHERE ecm:parentId = '%s'",
                folder1.getId()));
        assertIdSet(dml, file1.getId(), file2.getId(), file3.getId(),
                proxy.getId());

        /*
         * ecm:primaryType
         */
        dml = session.query("SELECT * FROM Document WHERE ecm:primaryType = 'Folder'");
        assertEquals(3, dml.size());
        dml = session.query("SELECT * FROM Document WHERE ecm:primaryType <> 'Folder'");
        assertEquals(6, dml.size()); // 3 files, 1 note, 1 proxy, 1 version
        dml = session.query("SELECT * FROM Document WHERE ecm:primaryType = 'Note'");
        assertEquals(1, dml.size());
        dml = session.query("SELECT * FROM Document WHERE ecm:primaryType = 'File'");
        assertEquals(5, dml.size()); // 3 files, 1 proxy, 1 version
        dml = session.query("SELECT * FROM Document WHERE ecm:primaryType IN ('Folder', 'Note')");
        assertEquals(4, dml.size());
        dml = session.query("SELECT * FROM Document WHERE ecm:primaryType NOT IN ('Folder', 'Note')");
        assertEquals(5, dml.size()); // 3 files, 1 proxy, 1 version

        /*
         * ecm:mixinType
         */

        dml = session.query("SELECT * FROM Document WHERE ecm:mixinType = 'Folderish'");
        assertEquals(3, dml.size());
        dml = session.query("SELECT * FROM Document WHERE ecm:mixinType = 'Downloadable'");
        assertEquals(5, dml.size()); // 3 files, 1 proxy, 1 version
        dml = session.query("SELECT * FROM Document WHERE ecm:mixinType = 'Versionable'");
        assertEquals(6, dml.size()); // 1 note, 3 files, 1 proxy, 1 version
        dml = session.query("SELECT * FROM Document WHERE ecm:mixinType IN ('Folderish', 'Downloadable')");
        assertEquals(8, dml.size()); // 3 folders, 3 files, 1 proxy, 1 version
        dml = session.query("SELECT * FROM Document WHERE ecm:mixinType NOT IN ('Folderish', 'Downloadable')");
        assertEquals(1, dml.size()); // 1 note

        /*
         * ecm:currentLifeCycleState
         */
        dml = session.query("SELECT * FROM Document WHERE ecm:currentLifeCycleState = 'project'");
        // 3 folders, 1 note, 3 files, 1 proxy, 1 version
        assertEquals(9, dml.size());

        /*
         * ecm:versionLabel
         */
        dml = session.query("SELECT * FROM Document WHERE ecm:versionLabel = '1'");
        assertIdSet(dml, version.getId());

        // ecm:fulltext tested below
    }

    public void testEmptyLifecycle() throws Exception {
        DocumentModelList dml;
        createDocs();
        String sql = "SELECT * FROM Document WHERE ecm:currentLifeCycleState <> 'deleted'";

        dml = session.query(sql);
        assertEquals(7, dml.size());

        // create a doc with no lifecycle associated
        DocumentModel doc = new DocumentModelImpl("/testfolder1", "mydoc",
                "MyDocType");
        doc = session.createDocument(doc);
        session.save();
        assertEquals("undefined", doc.getCurrentLifeCycleState());
        dml = session.query(sql);
        assertEquals(8, dml.size());
    }

    /**
     * Wait a bit to give time to the asynchronous fulltext extractor.
     * <p>
     * Subclassed for MS SQL Server which is itself asynchronous when indexing
     * fulltext.
     */
    protected void sleepForFulltext() throws Exception {
        Framework.getLocalService(EventService.class).waitForAsyncCompletion();
    }

    public void testFulltext() throws Exception {
        createDocs();
        sleepForFulltext();
        String query;
        DocumentModelList dml;
        DocumentModel file1 = session.getDocument(new PathRef(
                "/testfolder1/testfile1"));
        DocumentModel file2 = session.getDocument(new PathRef(
                "/testfolder1/testfile2"));
        DocumentModel file3 = session.getDocument(new PathRef(
                "/testfolder1/testfile3"));

        query = "SELECT * FROM File WHERE ecm:fulltext = 'world'";

        dml = session.query(query);
        assertEquals(0, dml.size());

        file1.setProperty("dublincore", "title", "hello world");
        session.saveDocument(file1);
        session.save();
        sleepForFulltext();

        dml = session.query(query);
        assertIdSet(dml, file1.getId());

        file2.setProperty("dublincore", "description", "the world is my oyster");
        session.saveDocument(file2);
        session.save();
        sleepForFulltext();

        dml = session.query(query);
        assertIdSet(dml, file1.getId(), file2.getId());

        file3.setProperty("dublincore", "title", "brave new world");
        session.saveDocument(file3);
        session.save();
        sleepForFulltext();

        dml = session.query(query);
        assertIdSet(dml, file1.getId(), file2.getId()); // file3 is a Note

        query = "SELECT * FROM Note WHERE ecm:fulltext = 'world'";
        dml = session.query(query);
        assertIdSet(dml, file3.getId());

        query = "SELECT * FROM Document WHERE ecm:fulltext = 'world' "
                + "AND dc:contributors = 'pete'";
        sleepForFulltext();
        dml = session.query(query);
        assertIdSet(dml, file2.getId());

        // multi-valued field
        query = "SELECT * FROM Document WHERE ecm:fulltext = 'bzzt'";
        sleepForFulltext();
        dml = session.query(query);
        assertEquals(0, dml.size());
        file1.setProperty("dublincore", "subjects", new String[] { "bzzt" });
        session.saveDocument(file1);
        session.save();
        sleepForFulltext();
        query = "SELECT * FROM Document WHERE ecm:fulltext = 'bzzt'";
        dml = session.query(query);
        assertIdSet(dml, file1.getId());
    }

    public void testFulltextExpressionSyntax() throws Exception {
        createDocs();
        sleepForFulltext();
        String query;
        DocumentModelList dml;
        DocumentModel file1 = session.getDocument(new PathRef(
                "/testfolder1/testfile1"));

        file1.setProperty("dublincore", "title", "the world is my oyster");
        session.saveDocument(file1);
        session.save();
        sleepForFulltext();

        query = "SELECT * FROM File WHERE ecm:fulltext = 'world'";
        dml = session.query(query);
        assertEquals(1, dml.size());

        query = "SELECT * FROM File WHERE ecm:fulltext = '+world'";
        dml = session.query(query);
        assertEquals(1, dml.size());

        query = "SELECT * FROM File WHERE ecm:fulltext = 'oyster'";
        dml = session.query(query);
        assertEquals(1, dml.size());

        query = "SELECT * FROM File WHERE ecm:fulltext = 'kangaroo'"; // absent
        dml = session.query(query);
        assertEquals(0, dml.size());

        // implicit AND

        query = "SELECT * FROM File WHERE ecm:fulltext = 'world oyster'";
        dml = session.query(query);
        assertEquals(1, dml.size());

        query = "SELECT * FROM File WHERE ecm:fulltext = 'world +oyster'";
        dml = session.query(query);
        assertEquals(1, dml.size());

        query = "SELECT * FROM File WHERE ecm:fulltext = 'world kangaroo'";
        dml = session.query(query);
        assertEquals(0, dml.size());

        query = "SELECT * FROM File WHERE ecm:fulltext = 'kangaroo oyster'";
        dml = session.query(query);
        assertEquals(0, dml.size());

        // NOT

        query = "SELECT * FROM File WHERE ecm:fulltext = 'world -oyster'";
        dml = session.query(query);
        assertEquals(0, dml.size());

        query = "SELECT * FROM File WHERE ecm:fulltext = '-world oyster'";
        dml = session.query(query);
        assertEquals(0, dml.size());

        query = "SELECT * FROM File WHERE ecm:fulltext = 'world -kangaroo'";
        dml = session.query(query);
        assertEquals(1, dml.size());

        query = "SELECT * FROM File WHERE ecm:fulltext = 'world -kangaroo -smurf'";
        dml = session.query(query);
        assertEquals(1, dml.size());

        query = "SELECT * FROM File WHERE ecm:fulltext = '-world kangaroo'";
        dml = session.query(query);
        assertEquals(0, dml.size());

        query = "SELECT * FROM File WHERE ecm:fulltext = 'kangaroo -oyster'";
        dml = session.query(query);
        assertEquals(0, dml.size());

        query = "SELECT * FROM File WHERE ecm:fulltext = '-kangaroo oyster'";
        dml = session.query(query);
        assertEquals(1, dml.size());
    }

    public void testFulltextSecondary() throws Exception {
        createDocs();
        String query;
        DocumentModelList dml;
        DocumentModel file1 = session.getDocument(new PathRef(
                "/testfolder1/testfile1"));
        DocumentModel file2 = session.getDocument(new PathRef(
                "/testfolder1/testfile2"));
        DocumentModel file3 = session.getDocument(new PathRef(
                "/testfolder1/testfile3"));

        file1.setProperty("dublincore", "title", "hello world");
        session.saveDocument(file1);
        file2.setProperty("dublincore", "description", "the world is my oyster");
        session.saveDocument(file2);
        file3.setProperty("dublincore", "title", "brave new world");
        session.saveDocument(file3);
        session.save();
        sleepForFulltext();

        // check main fulltext index
        query = "SELECT * FROM Document WHERE ecm:fulltext = 'world'";
        dml = session.query(query);
        assertIdSet(dml, file1.getId(), file2.getId(), file3.getId());
        // check secondary fulltext index, just for title field
        query = "SELECT * FROM Document WHERE ecm:fulltext_title = 'world'";
        dml = session.query(query);
        assertIdSet(dml, file1.getId(), file3.getId()); // file2 has it in descr

        // field-based fulltext
        // index exists
        query = "SELECT * FROM Document WHERE ecm:fulltext.dc:title = 'hello'";
        dml = session.query(query);
        assertIdSet(dml, file1.getId());
        // no index exists
        query = "SELECT * FROM Document WHERE ecm:fulltext.dc:description = 'oyster'";
        dml = session.query(query);
        assertIdSet(dml, file2.getId());
        query = "SELECT * FROM Document WHERE ecm:fulltext.dc:description = 'world OYSTER'";
        dml = session.query(query);
        assertIdSet(dml, file2.getId());
    }

    public void testFulltextBlob() throws Exception {
        createDocs();
        sleepForFulltext();
        String query;
        DocumentModelList dml;
        DocumentModel file1 = session.getDocument(new PathRef(
                "/testfolder1/testfile1"));
        query = "SELECT * FROM File WHERE ecm:isProxy = 0 AND ecm:fulltext = 'restaurant'";
        dml = session.query(query);
        assertIdSet(dml, file1.getId());
        query = "SELECT * FROM File WHERE ecm:isProxy = 1 AND ecm:fulltext = 'restaurant'";
        dml = session.query(query);
        assertEquals(0, dml.size());
        query = "SELECT * FROM File WHERE ecm:fulltext = 'restaurant'";
        dml = session.query(query);
        assertIdSet(dml, file1.getId());
        // check text extraction with '\0' in it
        String content = "Text with a \0 in it";
        ByteArrayBlob blob1 = new ByteArrayBlob(content.getBytes("UTF-8"),
                "text/plain");
        file1.setPropertyValue("content", blob1);
        session.saveDocument(file1);
        session.save();
        sleepForFulltext();
    }

    public void testFullTextCopy() throws Exception {
        createDocs();
        String query;
        DocumentModelList dml;
        DocumentModel folder1 = session.getDocument(new PathRef("/testfolder1"));
        DocumentModel file1 = session.getDocument(new PathRef(
                "/testfolder1/testfile1"));
        file1.setProperty("dublincore", "title", "hello world");
        session.saveDocument(file1);
        session.save();
        sleepForFulltext();

        query = "SELECT * FROM File WHERE ecm:fulltext = 'world'";

        dml = session.query(query);
        assertIdSet(dml, file1.getId());

        // copy
        DocumentModel copy = session.copy(file1.getRef(), folder1.getRef(),
                "file1Copy");
        // the save is needed to update the read acls
        session.save();
        sleepForFulltext();

        dml = session.query(query);
        assertIdSet(dml, file1.getId(), copy.getId());
    }

    public void testOrderByAndDistinct() throws Exception {
        createDocs();

        String query;
        DocumentModelList dml;

        DocumentModel file1 = session.getDocument(new PathRef(
                "/testfolder1/testfile1"));
        file1.setProperty("dublincore", "title", "hello world 1");

        session.saveDocument(file1);
        session.save();

        sleepForFulltext();

        query = "SELECT * FROM File Where dc:title = 'hello world 1' ORDER BY ecm:currentLifeCycleState";

        dml = session.query(query);

        assertIdSet(dml, file1.getId());
        query = "SELECT * FROM File Where dc:title = 'hello world 1' ORDER BY ecm:versionLabel";

        dml = session.query(query);
        assertIdSet(dml, file1.getId());
    }

}
