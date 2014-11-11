/*
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Dragos Mihalache
 *     Florent Guillaume
 *     Benoit Delbosc
 */
package org.nuxeo.ecm.core.storage.mongodb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.blob.ByteArrayBlob;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;

/**
 * @author Dragos Mihalache
 * @author Florent Guillaume
 * @author Benjamin Jalon
 */
public class TestMongoDBRepositoryFulltextQuery extends
        MongoDBRepositoryTestCase {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.convert.api");
        deployBundle("org.nuxeo.ecm.core.convert");
        deployBundle("org.nuxeo.ecm.core.convert.plugins");
        deployContrib("org.nuxeo.ecm.core.storage.mongodb.tests",
                "OSGI-INF/test-repo-types-query.xml");
        deployContrib("org.nuxeo.ecm.core.storage.mongodb.tests",
                "OSGI-INF/test-repo-types.xml");
        deployContrib("org.nuxeo.ecm.core.storage.mongodb.tests",
                "OSGI-INF/test-repo-types-2.xml");
        openSession();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        session.save();
        waitForAsyncCompletion();
        closeSession();
        super.tearDown();
    }

    protected static void assertIdSet(DocumentModelList dml, String... ids) {
        Collection<String> expected = new HashSet<String>(Arrays.asList(ids));
        Collection<String> actual = new HashSet<String>();
        for (DocumentModel d : dml) {
            actual.add(d.getId());
        }
        assertEquals(expected, actual);
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
                "text/plain", "UTF-8", filename, null);
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
        file2.setPropertyValue("dc:description", "testfile2_DESCRIPTION2");
        Calendar cal2 = getCalendar(2007, 4, 1, 12, 0, 0);
        file2.setPropertyValue("dc:created", cal2);
        file2.setPropertyValue("dc:contributors",
                new String[] { "bob", "pete" });
        file2.setPropertyValue("dc:coverage", "foo/bar");
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
        // title without space or _ for Oracle fulltext searchability
        // (testFulltextProxy)
        file4.setPropertyValue("dc:title", "testfile4Title");
        file4.setPropertyValue("dc:description", "testfile4_DESCRIPTION4");
        file4 = session.createDocument(file4);

        session.save();
    }

    /**
     * Publishes testfile4 to testfolder1:
     * <p>
     * version (UUID_12, content UUID_13)
     * <p>
     * proxy (UUID_14)
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

    @Test
    public void testFulltext() throws Exception {
        createDocs();
        waitForFulltextIndexing();
        String query, nquery;
        DocumentModelList dml;
        DocumentModel file1 = session.getDocument(new PathRef(
                "/testfolder1/testfile1"));
        DocumentModel file2 = session.getDocument(new PathRef(
                "/testfolder1/testfile2"));
        DocumentModel file3 = session.getDocument(new PathRef(
                "/testfolder1/testfile3"));
        DocumentModel file4 = session.getDocument(new PathRef(
                "/testfolder2/testfolder3/testfile4"));

        // query
        query = "SELECT * FROM File WHERE ecm:fulltext = 'world'";
        dml = session.query(query);
        assertEquals(0, dml.size());

        // negative query
        // NOT fulltext not possible on MongoDB

        file1.setProperty("dublincore", "title", "hello world");
        session.saveDocument(file1);
        session.save();
        waitForFulltextIndexing();

        // query
        dml = session.query(query);
        assertIdSet(dml, file1.getId());

        file2.setProperty("dublincore", "description", "the world is my oyster");
        session.saveDocument(file2);
        session.save();
        waitForFulltextIndexing();

        // query
        dml = session.query(query);
        assertIdSet(dml, file1.getId(), file2.getId());

        file3.setProperty("dublincore", "title", "brave new world");
        session.saveDocument(file3);
        session.save();
        waitForFulltextIndexing();

        // query
        dml = session.query(query);
        assertIdSet(dml, file1.getId(), file2.getId()); // file3 is a Note

        query = "SELECT * FROM Note WHERE ecm:fulltext = 'world'";
        dml = session.query(query);
        assertIdSet(dml, file3.getId());

        query = "SELECT * FROM Document WHERE ecm:fulltext = 'world' "
                + "AND dc:contributors = 'pete'";
        waitForFulltextIndexing();
        dml = session.query(query);
        assertIdSet(dml, file2.getId());

        // multi-valued field
        query = "SELECT * FROM Document WHERE ecm:fulltext = 'bzzt'";
        waitForFulltextIndexing();
        dml = session.query(query);
        assertEquals(0, dml.size());
        file1.setProperty("dublincore", "subjects", new String[] { "bzzt" });
        session.saveDocument(file1);
        session.save();
        waitForFulltextIndexing();
        query = "SELECT * FROM Document WHERE ecm:fulltext = 'bzzt'";
        dml = session.query(query);
        assertIdSet(dml, file1.getId());
    }

    @Test
    public void testFulltext2() throws Exception {
        createDocs();
        waitForFulltextIndexing();
        String query;

        query = "SELECT * FROM File WHERE ecm:fulltext = 'restaurant'";
        assertEquals(1, session.query(query).size());

        // NOT fulltext not possible on MongoDB

        // multiple fulltext not possible on MongoDB

        // other query generation cases

        // no union and implicit score sort
        query = "SELECT * FROM File WHERE ecm:fulltext = 'restaurant' AND ecm:isProxy = 0";
        assertEquals(1, session.query(query).size());

        // order by so no implicit score sort
        query = "SELECT * FROM File WHERE ecm:fulltext = 'restaurant' ORDER BY dc:title";
        assertEquals(1, session.query(query).size());

        // order by and no union so no implicit score sort
        query = "SELECT * FROM File WHERE ecm:fulltext = 'restaurant' AND ecm:isProxy = 0 ORDER BY dc:title";
        assertEquals(1, session.query(query).size());

        // no union but distinct so no implicit score sort
        query = "SELECT DISTINCT * FROM File WHERE ecm:fulltext = 'restaurant' AND ecm:isProxy = 0";
        assertEquals(1, session.query(query).size());
    }

    /*
     * This used to crash SQL Server 2008 R2 (NXP-6143). It works on SQL Server
     * 2005.
     */
    @Test
    public void testFulltextCrashingSQLServer2008() throws Exception {
        createDocs();
        waitForFulltextIndexing();

        String query = "SELECT * FROM File WHERE ecm:fulltext = 'restaurant' AND dc:title = 'testfile1_Title'";
        assertEquals(1, session.query(query).size());
    }

    // no prefix for MongoDB
    @Ignore
    @Test
    public void testFulltextPrefix() throws Exception {
        createDocs();
        DocumentModel file1 = session.getDocument(new PathRef(
                "/testfolder1/testfile1"));
        file1.setPropertyValue("dc:title", "hello world citizens");
        session.saveDocument(file1);
        session.save();
        waitForFulltextIndexing();
        String query;

        query = "SELECT * FROM File WHERE ecm:fulltext = 'wor*'";
        assertEquals(1, session.query(query).size());

        query = "SELECT * FROM File WHERE ecm:fulltext = 'wor%'";
        assertEquals(1, session.query(query).size());

        // prefix in phrase search
        query = "SELECT * FROM File WHERE ecm:fulltext = '\"hello wor*\"'";
        assertEquals(1, session.query(query).size());
    }

    // no MongoDB
    @Ignore
    @Test
    public void testFulltextSpuriousCharacters() throws Exception {
        createDocs();
        waitForFulltextIndexing();

        String query = "SELECT * FROM File WHERE ecm:fulltext = 'restaurant :'";
        assertEquals(1, session.query(query).size());
    }

    @Test
    public void testFulltextMixin() throws Exception {
        createDocs();
        DocumentModel file1 = session.getDocument(new PathRef(
                "/testfolder1/testfile1"));
        file1.addFacet("Aged");
        file1.setPropertyValue("age:age", "barbar");
        session.saveDocument(file1);
        session.save();
        waitForFulltextIndexing();

        String query = "SELECT * FROM File WHERE ecm:fulltext = 'barbar'";
        assertEquals(1, session.query(query).size());
    }

    @Test
    public void testFulltextProxy() throws Exception {
        createDocs();
        waitForFulltextIndexing();

        String query;
        DocumentModelList dml;

        DocumentModel doc = session.getDocument(new PathRef(
                "/testfolder2/testfolder3/testfile4"));
        String docId = doc.getId();

        query = "SELECT * FROM Document WHERE ecm:fulltext = 'testfile4Title'";
        dml = session.query(query);
        assertIdSet(dml, docId);

        // publish doc
        DocumentModel proxy = publishDoc();
        String proxyId = proxy.getId();
        String versionId = proxy.getSourceId();
        waitForFulltextIndexing();

        // query must return also proxies and versions
        dml = session.query(query);
        assertIdSet(dml, docId, proxyId, versionId);

        // remove proxy
        session.removeDocument(proxy.getRef());
        session.save();
        waitForAsyncCompletion();
        session.save(); // process invalidations

        // leaves live doc and version
        dml = session.query(query);
        assertIdSet(dml, docId, versionId);

        // remove live doc
        session.removeDocument(doc.getRef());
        session.save();

        // wait for async version removal
        waitForAsyncCompletion();

        // version gone as well
        dml = session.query(query);
        // XXX TODO MongoDB orphanVersionRemoverListener had an error
        // assertTrue(dml.isEmpty());
    }

    @Test
    public void testFulltextExpressionSyntax() throws Exception {
        createDocs();
        waitForFulltextIndexing();
        String query;
        DocumentModelList dml;
        DocumentModel file1 = session.getDocument(new PathRef(
                "/testfolder1/testfile1"));

        file1.setProperty("dublincore", "title", "the world is my oyster");
        session.saveDocument(file1);
        session.save();
        waitForFulltextIndexing();

        query = "SELECT * FROM File WHERE ecm:fulltext = 'pete'";
        dml = session.query(query);
        assertEquals(1, dml.size());

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

        // OR

        query = "SELECT * FROM File WHERE ecm:fulltext = 'pete OR world'";
        dml = session.query(query);
        assertEquals(2, dml.size());

        query = "SELECT * FROM File WHERE ecm:fulltext = 'pete OR world smurf'";
        dml = session.query(query);
        // assertEquals(1, dml.size()); // MongoDB different semantics

        query = "SELECT * FROM File WHERE ecm:fulltext = 'pete OR world -smurf'";
        dml = session.query(query);
        assertEquals(2, dml.size());

        query = "SELECT * FROM File WHERE ecm:fulltext = '-smurf world OR pete'";
        dml = session.query(query);
        assertEquals(2, dml.size());

        query = "SELECT * FROM File WHERE ecm:fulltext = 'pete OR world oyster'";
        dml = session.query(query);
        assertEquals(2, dml.size());

        query = "SELECT * FROM File WHERE ecm:fulltext = 'pete OR world -oyster'";
        dml = session.query(query);
        assertEquals(1, dml.size());

        query = "SELECT * FROM File WHERE ecm:fulltext = '-oyster world OR pete'";
        dml = session.query(query);
        assertEquals(1, dml.size());
    }

    // don't use small words, they are eliminated by some fulltext engines
    @Test
    public void testFulltextExpressionPhrase() throws Exception {
        String query;

        DocumentModel file1 = new DocumentModelImpl("/", "testfile1", "File");
        file1.setPropertyValue("dc:title",
                "bobby can learn international commerce easily");
        file1 = session.createDocument(file1);
        // other files with data to avoid words being present in
        // too high a percentage of the indexes
        for (int i = 0; i < 10; i++) {
            DocumentModel f = new DocumentModelImpl("/", "otherfile" + i,
                    "File");
            f.setPropertyValue("dc:title", "some other text never matched");
            f.setPropertyValue("dc:description", "desc" + i);
            f = session.createDocument(f);
        }
        session.save();
        waitForFulltextIndexing();

        query = "SELECT * FROM File WHERE ecm:fulltext = '\"international commerce\"'";
        assertEquals(1, session.query(query).size());

        query = "SELECT * FROM File WHERE ecm:fulltext = '\"learn commerce\"'";
        assertEquals(0, session.query(query).size());

        query = "SELECT * FROM File WHERE ecm:fulltext = 'bobby \"can learn\"'";
        assertEquals(1, session.query(query).size());

        // negative phrase search
        query = "SELECT * FROM File WHERE ecm:fulltext = 'bobby -\"commerce easily\"'";
        assertEquals(0, session.query(query).size());

        query = "SELECT * FROM File WHERE ecm:fulltext = 'bobby -\"hello world\"'";
        assertEquals(1, session.query(query).size());

        query = "SELECT * FROM File WHERE ecm:fulltext = 'bobby -\"hello world\" commerce'";
        assertEquals(1, session.query(query).size());

        query = "SELECT * FROM File WHERE ecm:fulltext = 'bobby -\"hello world\" \"commerce easily\"'";
        assertEquals(1, session.query(query).size());

        query = "SELECT * FROM File WHERE ecm:fulltext = 'bobby \"commerce easily\" -\"hello world\"'";
        assertEquals(1, session.query(query).size());
    }

    @Test
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
        waitForFulltextIndexing();

        // check main fulltext index
        query = "SELECT * FROM Document WHERE ecm:fulltext = 'world'";
        dml = session.query(query);
        assertIdSet(dml, file1.getId(), file2.getId(), file3.getId());
        // check secondary fulltext index, just for title field
        // no secondary indexes on MongoDB

        // field-based fulltext
        // index exists
        query = "SELECT * FROM Document WHERE ecm:fulltext.dc:title = 'brave'";
        dml = session.query(query);
        assertIdSet(dml, file3.getId());
        // no index exists
        query = "SELECT * FROM Document WHERE ecm:fulltext.dc:description = 'oyster'";
        dml = session.query(query);
        assertIdSet(dml, file2.getId());
        query = "SELECT * FROM Document WHERE ecm:fulltext.dc:description = 'world OYSTER'";
        dml = session.query(query);
        assertIdSet(dml, file2.getId());
    }

    @Test
    public void testFulltextBlob() throws Exception {
        createDocs();
        waitForFulltextIndexing();

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
        waitForFulltextIndexing();
    }

    @Test
    public void testFulltextCopy() throws Exception {
        createDocs();
        String query;
        DocumentModelList dml;
        DocumentModel folder1 = session.getDocument(new PathRef("/testfolder1"));
        DocumentModel file1 = session.getDocument(new PathRef(
                "/testfolder1/testfile1"));
        file1.setProperty("dublincore", "title", "hello world");
        session.saveDocument(file1);
        session.save();
        waitForFulltextIndexing();

        query = "SELECT * FROM File WHERE ecm:fulltext = 'world'";

        dml = session.query(query);
        assertIdSet(dml, file1.getId());

        // copy
        DocumentModel copy = session.copy(file1.getRef(), folder1.getRef(),
                "file1Copy");
        // the save is needed to update the read acls
        session.save();
        waitForFulltextIndexing();

        dml = session.query(query);
        assertIdSet(dml, file1.getId(), copy.getId());
    }

    @Test
    public void testFulltextComplexType() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "complex-doc",
                "ComplexDoc");
        doc = session.createDocument(doc);
        DocumentRef docRef = doc.getRef();
        session.save();
        waitForAsyncCompletion();

        // test setting and reading a map with an empty list
        closeSession();
        openSession();
        doc = session.getDocument(docRef);
        Map<String, Object> attachedFile = new HashMap<String, Object>();
        List<Map<String, Object>> vignettes = new ArrayList<Map<String, Object>>();
        attachedFile.put("name", "somename");
        attachedFile.put("vignettes", vignettes);
        doc.setPropertyValue("cmpf:attachedFile", (Serializable) attachedFile);
        session.saveDocument(doc);
        session.save();

        closeSession();
        waitForFulltextIndexing();
        openSession();

        // test fulltext indexing of complex property at level one
        DocumentModelList results = session.query(
                "SELECT * FROM Document WHERE ecm:fulltext = 'somename'", 1);
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("complex-doc", results.get(0).getTitle());

        // test setting and reading a list of maps without a complex type in the
        // maps
        Map<String, Object> vignette = new HashMap<String, Object>();
        vignette.put(
                "content",
                StreamingBlob.createFromString("textblob content", "text/plain"));
        vignette.put("label", "vignettelabel");
        vignettes.add(vignette);
        doc.setPropertyValue("cmpf:attachedFile", (Serializable) attachedFile);
        session.saveDocument(doc);
        session.save();

        closeSession();
        waitForFulltextIndexing();
        openSession();

        // test fulltext indexing of complex property at level 3
        results = session.query("SELECT * FROM Document"
                + " WHERE ecm:fulltext = 'vignettelabel'", 2);
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("complex-doc", results.get(0).getTitle());

        // test fulltext indexing of complex property at level 3 in blob
        results = session.query("SELECT * FROM Document"
                + " WHERE ecm:fulltext = 'textblob content'", 2);
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("complex-doc", results.get(0).getTitle());

        // test deleting the list of vignette and ensure that the fulltext index
        // has been properly updated (regression test for NXP-6315)
        doc.setPropertyValue("cmpf:attachedFile/vignettes",
                new ArrayList<Map<String, Object>>());
        session.saveDocument(doc);
        session.save();

        closeSession();
        waitForFulltextIndexing();
        openSession();

        results = session.query("SELECT * FROM Document"
                + " WHERE ecm:fulltext = 'vignettelabel'", 2);
        assertNotNull(results);
        assertEquals(0, results.size());

        results = session.query("SELECT * FROM Document"
                + " WHERE ecm:fulltext = 'textblob content'", 2);
        assertNotNull(results);
        assertEquals(0, results.size());
    }

    @Test
    public void testFulltextSecurity() throws Exception {
        createDocs();
        closeSession();
        session = openSessionAs("bob");
        session.query("SELECT * FROM Document WHERE ecm:isProxy = 0 AND ecm:fulltext = 'world'");
        // this failed with ORA-00918 on Oracle (NXP-5410)
        session.query("SELECT * FROM Document WHERE ecm:fulltext = 'world'");
        // we don't care about the answer, just that the query executes
    }

    @Test
    public void testFulltextFacet() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "foo", "File");
        doc.addFacet("Aged");
        doc.setPropertyValue("age:age", "barbar");
        doc = session.createDocument(doc);
        session.save();
        waitForFulltextIndexing();

        DocumentModelList list = session.query("SELECT * FROM File WHERE ecm:fulltext = 'barbar'");
        assertEquals(1, list.size());
    }

    // XXX TODO on MongoDB
    @Ignore
    @Test
    public void testGetBinaryFulltext() throws Exception {
        createDocs();
        waitForFulltextIndexing();
        DocumentModelList list = session.query("SELECT * FROM File WHERE ecm:fulltext = 'Drink'");
        assertTrue(!list.isEmpty());
        Map<String, String> map = session.getBinaryFulltext(list.get(0).getRef());
        assertTrue(map.containsKey("binarytext"));
        assertTrue(map.get("binarytext").contains("drink"));
    }

}
