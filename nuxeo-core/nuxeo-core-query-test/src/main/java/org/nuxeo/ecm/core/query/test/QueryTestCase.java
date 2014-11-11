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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TimeZone;
import java.util.Collection;

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
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author Dragos Mihalache
 * @author Florent Guillaume
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
        try {
            undeployRepository();
        } catch (Exception e) {
            // ignore
        }
        super.tearDown();
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

    public void closeSession() throws ClientException {
        closeSession(session);
    }

    protected void closeSession(CoreSession session) throws ClientException {
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
        String content = "This is a file.\nCaf\u00e9.";
        String filename = "testfile.txt";
        ByteArrayBlob blob1 = new ByteArrayBlob(content.getBytes("UTF-8"),
                "text/plain");
        file1.setPropertyValue("content", blob1);
        file1.setPropertyValue("filename", filename);
        Calendar cal1 = getCalendar(2007, 3, 1, 12, 0, 0);
        file1.setPropertyValue("dc:created", cal1);
        file1 = session.createDocument(file1);

        DocumentModel file2 = new DocumentModelImpl("/testfolder1",
                "testfile2", "File");
        file2.setPropertyValue("dc:title", "testfile2_Title");
        file2.setPropertyValue("dc:description", "testfile2_DESCRIPTION2");
        Calendar cal2 = getCalendar(2007, 4, 1, 12, 0, 0);
        file2.setPropertyValue("dc:created", cal2);
        file2.setPropertyValue("dc:contributors",
                new String[] { "bob", "pete" });
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

        dml = session.query("SELECT * FROM File WHERE dc:title = 'testfile1_Title'");
        assertEquals(1, dml.size());

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

        // this needs an actual LEFT OUTER JOIN
        dml = session.query("SELECT * FROM Document WHERE filename = 'testfile.txt' OR dc:title = 'testfile3_Title'");
        assertEquals(2, dml.size());

        dml = session.query("SELECT * FROM Document WHERE filename = 'testfile.txt' OR dc:contributors = 'bob'");
        assertEquals(3, dml.size());
    }

    // this is disabled for JCR
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
        dml = session.query("SELECT * FROM Document WHERE dc:contributors <> 'pete'");
        assertEquals(6, dml.size());
        dml = session.query("SELECT * FROM Document WHERE dc:contributors <> 'blah'");
        assertEquals(7, dml.size());
        dml = session.query("SELECT * FROM File WHERE dc:contributors <> 'blah' AND ecm:isProxy = 0");
        assertEquals(3, dml.size());
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

    public void TODOtestQueryResultsTypes() throws Exception {
        // assertEquals("testQueryResultsTypes", doc.getPropertyValue("title"));
        // assertEquals(Boolean.TRUE, doc.getPropertyValue("my:boolean"));
        // assertEquals(3.14, doc.getPropertyValue("my:double"));
        // assertEquals(1234567890, doc.getPropertyValue("my:long"));
    }

    // from TestSQLWithPath

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

    public void TODOtestSQLFulltextAndSubpath() throws Exception {
        createDocs();
        String sql = "SELECT * FROM document WHERE content LIKE '% Nuxeo%' AND ecm:path STARTSWITH '/'";
        DocumentModelList dml = session.query(sql);
        assertEquals(1, dml.size());
    }

    // from TestSQLWithDate

    public void testSQLWithDate() throws Exception {
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

    private void assertIdSet(DocumentModelList dml, String... ids) {
        assertEquals(ids.length, dml.size());
        Collection<String> expected = new HashSet<String>(Arrays.asList(ids));
        Collection<String> actual = new HashSet<String>();
        for (DocumentModel d : dml) {
            actual.add(d.getId());
        }
        assertEquals(expected, actual);
    }

    // this is disabled for JCR
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
        filter = new FacetFilter("Immutable", true);
        dml = session.query(
                "SELECT * FROM Document WHERE dc:title = 'testfile4_Title'",
                filter, 99);
        assertIdSet(dml, proxyId, versionId);

        // facet filter: not immutable
        filter = new FacetFilter("Immutable", false);
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
        filter = new FacetFilter("Immutable", false);
        dml = session.query(
                "SELECT * FROM Document WHERE ecm:mixinType = 'Immutable'",
                filter, 99);
        assertEquals(0, dml.size()); // contradictory clauses

        dml = session.query("SELECT * FROM Document WHERE ecm:mixinType <> 'Immutable' AND ecm:isProxy = 0");
        assertEquals(7, dml.size()); // 7 folder/docs

        dml = session.query("SELECT * FROM Document WHERE ecm:mixinType <> 'Immutable' AND ecm:isProxy = 1");
        assertEquals(0, dml.size()); // contradictory clauses

        dml = session.query("SELECT * FROM Document WHERE ecm:mixinType <> 'Immutable' AND ecm:isCheckedInVersion = 0");
        assertEquals(7, dml.size()); // 7 folder/docs

        dml = session.query("SELECT * FROM Document WHERE ecm:mixinType <> 'Immutable' AND ecm:isCheckedInVersion = 1");
        assertEquals(0, dml.size()); // contradictory clauses

        // conflict between where and filter
        filter = new FacetFilter("Immutable", true);
        dml = session.query(
                "SELECT * FROM Document WHERE ecm:mixinType <> 'Immutable'",
                filter, 99);
        assertEquals(0, dml.size()); // contradictory clauses

        // "deep" isProxy
        dml = session.query("SELECT * FROM Document WHERE (dc:title = 'blah' OR ecm:isProxy = 1)");
        assertIdSet(dml, proxyId);
        dml = session.query("SELECT * FROM Document WHERE ecm:isProxy = 0 AND (dc:title = 'testfile1_Title' OR ecm:isProxy = 1)");
        assertEquals(1, dml.size());
    }

    // this is disabled for JCR
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
        dml = session.query(String.format(
                "SELECT * FROM Document WHERE ecm:name = '%s'", file4.getName()));
        assertIdSet(dml, file4.getId(), proxy.getId(), version.getId());

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
        dml = session.query("SELECT * FROM Document WHERE ecm:mixinType = 'Folderish' AND ecm:mixinType <> 'blah'");
        assertEquals(3, dml.size());
        dml = session.query("SELECT * FROM Document WHERE ecm:mixinType = 'Downloadable'");
        assertEquals(5, dml.size()); // 3 files, 1 proxy, 1 version
        dml = session.query("SELECT * FROM Document WHERE ecm:mixinType = 'Versionable'");
        assertEquals(6, dml.size()); // 1 note, 3 files, 1 proxy, 1 version
        dml = session.query("SELECT * FROM Document WHERE ecm:mixinType = 'Versionable' AND ecm:mixinType <> 'Downloadable'");
        assertEquals(1, dml.size()); // 1 note
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

        // ecm:fulltext

    }
}
