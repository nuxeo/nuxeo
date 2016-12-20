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
 *     Dragos Mihalache
 *     Florent Guillaume
 *     Benoit Delbosc
 *     Benjamin Jalon
 */
package org.nuxeo.ecm.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.ExceptionUtils;
import org.nuxeo.ecm.core.api.AbstractSession;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.Filter;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.ScrollResult;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.FacetFilter;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.query.QueryFilter;
import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.StorageConfiguration;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.LogCaptureFeature;
import org.nuxeo.runtime.test.runner.RuntimeHarness;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, LogCaptureFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.core.convert", //
        "org.nuxeo.ecm.core.convert.plugins", //
})
@LocalDeploy({ "org.nuxeo.ecm.core.test.tests:OSGI-INF/testquery-core-types-contrib.xml",
        "org.nuxeo.ecm.core.test.tests:OSGI-INF/test-repo-core-types-contrib-2.xml" })
public class TestSQLRepositoryQuery {

    @Inject
    protected RuntimeHarness runtimeHarness;

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected CoreSession session;

    @Inject
    LogCaptureFeature.Result logCaptureResult;

    protected boolean proxies;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    protected boolean isDBS() {
        return coreFeature.getStorageConfiguration().isDBS();
    }

    protected boolean isDBSMongoDB() {
        return coreFeature.getStorageConfiguration().isDBSMongoDB();
    }

    protected boolean isDBSMarkLogic() {
        return coreFeature.getStorageConfiguration().isDBSMarkLogic();
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

    /**
     * Query of NOT (something) matches docs where (something) did not match because the field was null.
     */
    public boolean notMatchesNull() {
        return isDBSMongoDB() || isDBSMarkLogic();
    }

    public boolean supportsDistinct() {
        return !isDBS();
    }

    public boolean supportsTags() {
        return !isDBS();
    }

    public boolean supportsScroll() {
        StorageConfiguration conf = coreFeature.getStorageConfiguration();
        // DBS mem and marklogic are not yet supported
        return (conf.isDBSMongoDB() || conf.isVCS());
    }

    // ---------------------------------------

    protected Calendar getCalendar(int year, int month, int day, int hours, int minutes, int seconds) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month - 1); // 0-based
        cal.set(Calendar.DAY_OF_MONTH, day);
        cal.set(Calendar.HOUR_OF_DAY, hours);
        cal.set(Calendar.MINUTE, minutes);
        cal.set(Calendar.SECOND, seconds);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    /**
     * Creates the following structure of documents:
     *
     * VCS:
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
     *
     * DBS:
     * <pre>
     *  root (UUID_0)
     *  |- testfolder1 (UUID_1)
     *  |  |- testfile1 (UUID_2)
     *  |  |- testfile2 (UUID_3)
     *  |  \- testfile3 (UUID_4) (Note)
     *  \- tesfolder2 (UUID_5)
     *     \- testfolder3 (UUID_6)
     *        \- testfile4 (UUID_7)
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

    /**
     * Publishes testfile4 to testfolder1:
     * <p>
     * VCS: version (UUID_12, content UUID_13), proxy (UUID_14)
     * <p>
     * DBS: version (UUID_8), proxy (UUID_9)
     */
    protected DocumentModel publishDoc() throws Exception {
        DocumentModel doc = session.getDocument(new PathRef("/testfolder2/testfolder3/testfile4"));
        DocumentModel sec = session.getDocument(new PathRef("/testfolder1"));
        DocumentModel proxy = session.publishDocument(doc, sec);
        session.save();
        DocumentModelList proxies = session.getProxies(doc.getRef(), sec.getRef());
        assertEquals(1, proxies.size());
        return proxy;
    }

    // from TestAPI
    @Test
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

        dml = session.query("SELECT * FROM File WHERE NOT dc:title != 'testfile1_Title'");
        assertEquals(1, dml.size());

        dml = session.query("SELECT * FROM File WHERE NOT dc:title = 'testfile1_Title'");
        assertEquals(2, dml.size());

        dml = session.query("SELECT * FROM Document WHERE dc:title = 'testfile1_Title'");
        assertEquals(1, dml.size());

        dml = session.query("SELECT * FROM File WHERE dc:title = 'testfile1_Title' OR dc:title = 'testfile2_Title'");
        assertEquals(2, dml.size());

        dml = session.query("SELECT * FROM File WHERE dc:title = 'testfolder1_Title'");
        assertEquals(0, dml.size());

        dml = session.query("SELECT * FROM File WHERE content/name = 'testfile.txt'");
        assertEquals(1, dml.size());

        dml = session.query("SELECT * FROM Note WHERE dc:title = 'testfile3_Title'");
        assertEquals(1, dml.size());

        // two uses of the same schema
        dml = session.query("SELECT * FROM Note WHERE dc:title = 'testfile3_Title' OR dc:description = 'hmmm'");
        assertEquals(1, dml.size());

        // property in a schema with no prefix
        dml = session.query("SELECT * FROM Document WHERE uid = 'uid123'");
        assertEquals(1, dml.size());
        // compat syntax for old search service:
        dml = session.query("SELECT * FROM Document WHERE uid:uid = 'uid123'");
        assertEquals(1, dml.size());

        // this needs an actual LEFT OUTER JOIN
        dml = session.query("SELECT * FROM Document WHERE content/name = 'testfile.txt' OR dc:title = 'testfile3_Title'");
        assertEquals(2, dml.size());

        dml = session.query("SELECT * FROM Document WHERE content/name = 'testfile.txt' OR dc:contributors = 'bob'");
        assertEquals(3, dml.size());

        // early detection of conflicting types for VCS
        dml = session.query("SELECT * FROM Document WHERE ecm:primaryType = 'foo'");
        assertEquals(0, dml.size());

        // query complex type
        dml = session.query("SELECT * FROM File WHERE content/length > 0");
        assertEquals(1, dml.size());
        dml = session.query("SELECT * FROM File WHERE content/name = 'testfile.txt'");
        assertEquals(1, dml.size());
        // with prefix (even though schema has no prefix)
        dml = session.query("SELECT * FROM File WHERE file:content/length > 0");
        assertEquals(1, dml.size());
        dml = session.query("SELECT * FROM File WHERE file:content/name = 'testfile.txt'");
        assertEquals(1, dml.size());
    }

    @Test
    public void testQueryBasic2() throws Exception {
        // ?
        assumeTrue(!coreFeature.getStorageConfiguration().isVCSDerby());

        createDocs();
        DocumentModelList dml;

        dml = session.query("SELECT * FROM Document WHERE dc:title ILIKE 'test%'");
        assertEquals(5, dml.size());

        dml = session.query("SELECT * FROM Document WHERE dc:title ILIKE 'Test%'");
        assertEquals(5, dml.size());

        dml = session.query("SELECT * FROM Document WHERE dc:title NOT ILIKE 'foo%'");
        assertEquals(notMatchesNull() ? 7 : 5, dml.size());

        dml = session.query("SELECT * FROM Document WHERE dc:title NOT ILIKE 'Foo%'");
        assertEquals(notMatchesNull() ? 7 : 5, dml.size());

        dml = session.query("SELECT * FROM Document WHERE dc:subjects ILIKE '%oo'");
        assertEquals(1, dml.size());

        dml = session.query("SELECT * FROM Document WHERE dc:subjects NOT ILIKE '%oo'");
        assertEquals(6, dml.size());
    }

    @Test
    public void testQueryWithType() throws Exception {
        DocumentModelList dml;
        createDocs();

        dml = session.query("SELECT * FROM File", "NXQL", null, 0, 0, false);
        assertEquals(3, dml.size());
        for (DocumentModel dm : dml) {
            assertEquals("File", dm.getType());
        }

        try {
            session.query("SELECT * FROM File", "NOSUCHQUERYTYPE", null, 0, 0, false);
            fail("Unknown query type should be rejected");
        } catch (NuxeoException e) {
            String m = e.getMessage();
            assertTrue(m, m.contains("No QueryMaker accepts"));
        }
    }

    @Test
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

    @Test
    public void testQueryMultipleNew() throws Exception {
        DocumentModelList dml;
        createDocs();

        dml = session.query("SELECT * FROM File WHERE dc:contributors/* = 'pete'");
        assertEquals(1, dml.size());

        dml = session.query("SELECT * FROM File WHERE dc:contributors/* = 'bob'");
        assertEquals(1, dml.size());

        dml = session.query("SELECT * FROM Document WHERE dc:contributors/* = 'bob'");
        assertEquals(2, dml.size());

        dml = session.query("SELECT * FROM Document WHERE dc:contributors/* IN ('bob', 'pete')");
        assertEquals(2, dml.size());

        dml = session.query("SELECT * FROM Document WHERE dc:contributors/* IN ('bob', 'john')");
        assertEquals(2, dml.size());

        dml = session.query("SELECT * FROM Document WHERE dc:contributors/* NOT IN ('bob', 'pete')");
        assertEquals(notMatchesNull() ? 5 : 1, dml.size());

        dml = session.query("SELECT * FROM Document WHERE dc:contributors/* NOT IN ('bob', 'john')");
        assertEquals(notMatchesNull() ? 5 : 1, dml.size());

        dml = session.query("SELECT * FROM Document WHERE dc:contributors/* LIKE 'pe%'");
        assertEquals(1, dml.size());

        dml = session.query("SELECT * FROM Document WHERE dc:contributors/* LIKE 'bo%'");
        assertEquals(2, dml.size());

        dml = session.query("SELECT * FROM Document WHERE dc:contributors/* LIKE '%o%'");
        assertEquals(2, dml.size());

        dml = session.query("SELECT * FROM Document WHERE dc:contributors/* NOT LIKE '%o%'");
        assertEquals(notMatchesNull() ? 5 : 1, dml.size());

        dml = session.query("SELECT * FROM Document WHERE dc:subjects/* LIKE '%oo%'");
        assertEquals(1, dml.size());

        dml = session.query("SELECT * FROM File WHERE dc:subjects/* NOT LIKE '%oo%'");
        assertEquals(notMatchesNull() ? 2 : 0, dml.size());
    }

    @Test
    public void testQueryNegativeMultiple() throws Exception {
        DocumentModelList dml;
        createDocs();
        dml = session.query("SELECT * FROM Document WHERE dc:contributors <> 'pete'");
        assertEquals(6, dml.size());

        dml = session.query("SELECT * FROM Document WHERE dc:contributors <> 'blah'");
        assertEquals(7, dml.size());

        dml = session.query("SELECT * FROM File WHERE dc:contributors <> 'blah' AND ecm:isProxy = 0");
        assertEquals(3, dml.size());

        dml = session.query(
                "SELECT * FROM Document WHERE ecm:mixinType = 'Versionable' AND ecm:mixinType <> 'Downloadable'");
        assertEquals(1, dml.size()); // 1 note
    }

    @Test
    public void testQueryAfterEdit() throws Exception {
        DocumentModel root = session.getRootDocument();

        String fname1 = "file1";
        DocumentModel childFile1 = new DocumentModelImpl(root.getPathAsString(), fname1, "File");

        DocumentModel[] childDocs = new DocumentModel[1];
        childDocs[0] = childFile1;

        DocumentModel[] returnedChildDocs = session.createDocument(childDocs);
        assertEquals(1, returnedChildDocs.length);

        childFile1 = returnedChildDocs[0];

        // add a blob
        String s = "<html><head/><body>La la la!</body></html>";
        Blob blob = Blobs.createBlob(s, "text/html");
        blob.setFilename("f1");
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

    @Test
    public void testOrderBy() throws Exception {
        String sql;
        DocumentModelList dml;
        createDocs();

        sql = "SELECT * FROM Document WHERE dc:title LIKE 'testfile%' ORDER BY dc:description";
        dml = session.query(sql);
        assertEquals(4, dml.size());
        assertEquals("testfile1_description", dml.get(0).getPropertyValue("dc:description"));

        // without proxies as well
        sql = "SELECT * FROM Document WHERE dc:title LIKE 'testfile%' AND ecm:isProxy = 0 ORDER BY dc:description";
        dml = session.query(sql);
        assertEquals(4, dml.size());
        assertEquals("testfile1_description", dml.get(0).getPropertyValue("dc:description"));

        // desc
        sql = "SELECT * FROM Document WHERE dc:title LIKE 'testfile%' ORDER BY dc:description DESC";
        dml = session.query(sql);
        assertEquals(4, dml.size());
        assertEquals("testfile4_DESCRIPTION4", dml.get(0).getPropertyValue("dc:description"));
    }

    @Test
    public void testOrderBySeveralColumns() throws Exception {
        String sql;
        DocumentModelList dml;
        createDocs();

        // avoid null dc:coverage, null sort first/last is db-dependent
        sql = "SELECT * FROM File " + " WHERE dc:title in ('testfile1_Title', 'testfile2_Title')"
                + " ORDER BY dc:title, dc:coverage";
        dml = session.query(sql);
        assertEquals(2, dml.size());
        assertEquals("testfile1", dml.get(0).getName());
        assertEquals("testfile2", dml.get(1).getName());

        // swap columns
        sql = "SELECT * FROM File " + " WHERE dc:title in ('testfile1_Title', 'testfile2_Title')"
                + " ORDER BY dc:coverage, dc:title";
        dml = session.query(sql);
        assertEquals(2, dml.size());
        assertEquals("testfile2", dml.get(0).getName());
        assertEquals("testfile1", dml.get(1).getName());
    }

    @Test
    public void testOrderBySameColumns() throws Exception {
        // SQL Server cannot ORDER BY foo, foo
        assumeTrue("SQL Server cannot ORDER BY foo, foo", !coreFeature.getStorageConfiguration().isVCSSQLServer());

        String sql;
        DocumentModelList dml;
        createDocs();

        sql = "SELECT * FROM File " + " WHERE dc:title in ('testfile1_Title', 'testfile2_Title')"
                + " ORDER BY dc:title, dc:title";
        dml = session.query(sql);
        assertEquals(2, dml.size());
        assertEquals("testfile1", dml.get(0).getName());
        assertEquals("testfile2", dml.get(1).getName());

        sql = "SELECT * FROM File " + " WHERE dc:title in ('testfile1_Title', 'testfile2_Title')"
                + " ORDER BY dc:title DESC, dc:title";
        dml = session.query(sql);
        assertEquals(2, dml.size());
        assertEquals("testfile2", dml.get(0).getName());
        assertEquals("testfile1", dml.get(1).getName());
    }

    @Test
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
        assertEquals("/testfolder2/testfolder3/testfile4", dml.get(6).getPathAsString());

        sql = "SELECT * FROM Document ORDER BY ecm:path DESC";
        dml = session.query(sql);
        assertEquals(7, dml.size());
        assertEquals("/testfolder2/testfolder3/testfile4", dml.get(0).getPathAsString());
        assertEquals("/testfolder1", dml.get(6).getPathAsString());

        // then with batching

        sql = "SELECT * FROM Document ORDER BY ecm:path";
        dml = session.query(sql, null, 2, 3, false);
        assertEquals(2, dml.size());
        assertEquals("/testfolder1/testfile3", dml.get(0).getPathAsString());
        assertEquals("/testfolder2", dml.get(1).getPathAsString());
    }

    @Test
    public void testOrderByPos() throws Exception {
        DocumentModelList dml;

        DocumentModel ofolder = new DocumentModelImpl("/", "ofolder", "OrderedFolder");
        ofolder = session.createDocument(ofolder);
        DocumentModel file1 = new DocumentModelImpl("/ofolder", "testfile1", "File");
        file1 = session.createDocument(file1);
        DocumentModel file2 = new DocumentModelImpl("/ofolder", "testfile2", "File");
        file2 = session.createDocument(file2);
        DocumentModel file3 = new DocumentModelImpl("/ofolder", "testfile3", "File");
        file3 = session.createDocument(file3);
        session.save();

        String sql = String.format("SELECT * FROM Document WHERE ecm:parentId = '%s' ORDER BY ecm:pos",
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

    @Test
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

    @Test
    public void testQueryLimits() throws Exception {
        DocumentModelList dml;
        createDocs();

        String sql = "SELECT * FROM Document ORDER BY ecm:name";

        dml = session.query(sql);
        assertEquals(7, dml.size());
        assertEquals(7, dml.totalSize());

        // countUpTo = 0 -> no total count, dml set the total size to the list
        // size
        // equivalent to totalCount=false
        dml = session.query(sql, null, 0, 0, 0);
        assertEquals(7, dml.size());
        assertEquals(7, dml.totalSize());

        dml = session.query(sql, null, 2, 2, 0);
        assertEquals(2, dml.size());
        assertEquals(2, dml.totalSize());

        dml = session.query(sql, null, 10, 10, 0);
        assertEquals(0, dml.size());
        assertEquals(0, dml.totalSize());

        // countUpTo = -1 -> ask for exact total size, regardless of
        // offset/limit
        // equivalent to totalCount=true
        dml = session.query(sql, null, 0, 0, -1);
        assertEquals(7, dml.size());
        assertEquals(7, dml.totalSize());

        dml = session.query(sql, null, 2, 2, -1);
        assertEquals(2, dml.size());
        assertEquals(7, dml.totalSize());

        dml = session.query(sql, null, 2, 10, -1);
        assertEquals(0, dml.size());
        assertEquals(7, dml.totalSize());

        dml = session.query(sql, null, 20, 0, -1);
        assertEquals(7, dml.size());
        assertEquals(7, dml.totalSize());

        // countUpTo = n
        // equivalent to totalCount=true if there are less than n results
        dml = session.query(sql, null, 0, 0, 10);
        assertEquals(7, dml.size());
        assertEquals(7, dml.totalSize());

        dml = session.query(sql, null, 0, 0, 7);
        assertEquals(7, dml.size());
        assertEquals(7, dml.totalSize());

        // truncate result to 6
        dml = session.query(sql, null, 0, 0, 6);
        assertTrue(dml.totalSize() < 0);
        // watch out, the size of the list can be countUpTo + 1
        assertEquals(7, dml.size());

        // use limit to have an exact size
        dml = session.query(sql, null, 6, 0, 6);
        assertTrue(dml.totalSize() < 0);
        assertEquals(6, dml.size());

        // use limit to have an exact size
        dml = session.query(sql, null, 3, 0, 3);
        assertTrue(dml.totalSize() < 0);
        assertEquals(3, dml.size());

        // limit/offset overrides the countUpTo
        dml = session.query(sql, null, 5, 0, 2);
        assertTrue(dml.totalSize() < 0);
        assertEquals(5, dml.size());

        dml = session.query(sql, null, 3, 4, 2);
        assertTrue(dml.totalSize() < 0);
        assertEquals(3, dml.size());

        // Test limitation override when using totalCount=true
        dml = session.query(sql, null, 5, 0, true);
        assertEquals(5, dml.size());
        assertEquals(7, dml.totalSize());
        Properties properties = Framework.getProperties();
        properties.setProperty(AbstractSession.LIMIT_RESULTS_PROPERTY, "true");
        properties.setProperty(AbstractSession.MAX_RESULTS_PROPERTY, "5");
        // need to open a new session to refresh properties
        try (CoreSession admSession = CoreInstance.openCoreSession(session.getRepositoryName(), "Administrator")) {
            dml = admSession.query(sql, null, 5, 0, true);
            assertEquals(5, dml.size());
            assertTrue(dml.totalSize() < 0);
        } finally {
            properties.remove(AbstractSession.LIMIT_RESULTS_PROPERTY);
            properties.remove(AbstractSession.MAX_RESULTS_PROPERTY);
        }
    }

    @Test
    public void testQueryConstantsLeft() throws Exception {
        assumeTrue("DBS MongoDB cannot query const = const", !isDBSMongoDB());
        assumeTrue("DBS MarkLogic cannot query const = const", !isDBSMarkLogic());

        String sql;
        DocumentModelList dml;
        createDocs();

        sql = "SELECT * FROM Document WHERE 1 = 0";
        dml = session.query(sql);
        assertEquals(0, dml.totalSize());

        sql = "SELECT * FROM Document WHERE 0 = 0";
        dml = session.query(sql);
        assertEquals(7, dml.totalSize());
    }

    // from TestSQLWithPath
    @Test
    public void testEcmPathEqual() throws Exception {
        String sql;
        DocumentModelList dml;
        createDocs();

        sql = "SELECT * FROM document WHERE ecm:path = '/testfolder1'";
        dml = session.query(sql);
        assertEquals(1, dml.size());

        // trailing slash accepted
        sql = "SELECT * FROM document WHERE ecm:path = '/testfolder1/'";
        dml = session.query(sql);
        assertEquals(1, dml.size());

        sql = "SELECT * FROM document WHERE ecm:path <> '/testfolder1'";
        dml = session.query(sql);
        assertEquals(6, dml.size());
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
    public void testStartsWithCopy() throws Exception {
        String sql;
        DocumentModelList dml;
        createDocs();

        // copy folder2 into folder1
        session.copy(new PathRef("/testfolder2"), new PathRef("/testfolder1"), null);
        // session.save() not needed, implicit in DBS to follow VCS behavior

        sql = "SELECT * FROM document WHERE ecm:path STARTSWITH '/testfolder1'";
        dml = session.query(sql);
        assertEquals(6, dml.size());

        sql = "SELECT * FROM document WHERE ecm:path STARTSWITH '/testfolder1/testfolder2'";
        dml = session.query(sql);
        assertEquals(2, dml.size());

        sql = "SELECT * FROM document WHERE ecm:path STARTSWITH '/testfolder2'";
        dml = session.query(sql);
        assertEquals(2, dml.size());
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

        dml = session.query(
                String.format("SELECT * FROM document WHERE dc:title='testfile1_Title' AND ecm:ancestorId = '%s'",
                        session.getRootDocument().getId()));
        assertEquals(1, dml.size());

        dml = session.query(
                String.format("SELECT * FROM document WHERE dc:title LIKE 'testfile%%' AND ecm:ancestorId = '%s'",
                        session.getRootDocument().getId()));
        assertEquals(4, dml.size());
    }

    @Test
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

        // System properties are also supported to have a coherent behavior
        // even if it's useless: (primaryType='Folder' OR primaryType LIKE
        // 'Folder/%')
        sql = "SELECT * FROM Document WHERE ecm:primaryType STARTSWITH 'Folder'";
        assertTrue(session.query(sql).size() > 0);
    }

    @Test
    public void testReindexEditedDocument() throws Exception {
        String sql = "SELECT * FROM document WHERE dc:title LIKE 'testfile1_Ti%'";
        DocumentModelList dml;
        createDocs();

        dml = session.query(sql);
        assertEquals(1, dml.size());

        DocumentModel file1 = session.getDocument(new PathRef("/testfolder1/testfile1"));

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

    @Test
    public void testTimestamp() throws Exception {
        String sql;
        DocumentModelList dml;
        createDocs();

        sql = "SELECT * FROM Document WHERE dc:created >= TIMESTAMP '2007-03-15 00:00:00'";
        dml = session.query(sql);
        assertEquals(1, dml.size());

        sql = "SELECT * FROM Document WHERE dc:created < TIMESTAMP '2037-01-01 01:02:03'";
        dml = session.query(sql);
        assertEquals(2, dml.size());
    }

    // old-style date comparisons (actually using timestamps)
    @Test
    public void testDateOld() throws Exception {
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

        sql = "SELECT * FROM Document WHERE dc:created >= DATE '2007-02-15' AND dc:created <= DATE '2007-03-15'";
        dml = session.query(sql);
        assertEquals(1, dml.size());

        dml = session.query("SELECT * FROM Document WHERE dc:created BETWEEN DATE '2007-01-01' AND DATE '2008-01-01'");
        assertEquals(2, dml.size());

        dml = session.query("SELECT * FROM Document WHERE dc:created BETWEEN DATE '2007-03-15' AND DATE '2008-01-01'");
        assertEquals(1, dml.size());

        if (!coreFeature.getStorageConfiguration().isVCSDerby()) {
            // Derby 10.5.3.0 has bugs with LEFT JOIN and NOT BETWEEN
            // http://issues.apache.org/jira/browse/DERBY-4388

            // Documents without creation date don't match any DATE query
            // 2 documents with creation date

            dml = session.query(
                    "SELECT * FROM Document WHERE dc:created NOT BETWEEN DATE '2007-01-01' AND DATE '2008-01-01'");
            assertEquals(0, dml.size()); // 2 Documents match the BETWEEN query

            dml = session.query(
                    "SELECT * FROM Document WHERE dc:created NOT BETWEEN DATE '2007-03-15' AND DATE '2008-01-01'");
            assertEquals(1, dml.size()); // 1 Document matches the BETWEEN query

            dml = session.query(
                    "SELECT * FROM Document WHERE dc:created NOT BETWEEN DATE '2009-03-15' AND DATE '2009-01-01'");
            assertEquals(2, dml.size()); // 0 Document matches the BETWEEN query
        }
    }

    // new-style date comparisons (casting to native DATE type)
    @Test
    public void testDateNew() throws Exception {
        assumeFalse("MongoDB does not support NXQL DATE casts", isDBSMongoDB());
        assumeFalse("MarkLogic does not support NXQL DATE casts", isDBSMarkLogic());

        String sql;
        DocumentModelList dml;
        createDocs();

        // create file 5 (type File2)
        DocumentModel file5 = new DocumentModelImpl("/", "testfile5", "File2");
        file5.setPropertyValue("dc:title", "testfile5Title");
        Calendar cal = getCalendar(2012, 3, 1, 1, 2, 3);
        file5.setPropertyValue("tst2:dates", new Serializable[] { cal });
        file5 = session.createDocument(file5);
        session.save();

        // same as above but with cast
        sql = "SELECT * FROM File WHERE DATE(dc:created) >= DATE '2007-01-01'";
        dml = session.query(sql);
        assertEquals(2, dml.size());

        sql = "SELECT * FROM File WHERE DATE(dc:created) >= DATE '2007-03-15'";
        dml = session.query(sql);
        assertEquals(1, dml.size());

        sql = "SELECT * FROM File WHERE DATE(dc:created) >= DATE '2007-05-01'";
        dml = session.query(sql);
        assertEquals(0, dml.size());

        // equality testing
        sql = "SELECT * FROM File WHERE DATE(dc:created) = DATE '2007-03-01'";
        dml = session.query(sql);
        assertEquals(1, dml.size());

        // switched order
        sql = "SELECT * FROM File WHERE DATE '2007-01-01' <= DATE(dc:created)";
        dml = session.query(sql);
        assertEquals(2, dml.size());

        // list with subquery
        sql = "SELECT * FROM File WHERE DATE(tst2:dates) = DATE '2012-03-01'";
        dml = session.query(sql);
        assertEquals(1, dml.size());

        // list with join
        sql = "SELECT * FROM File WHERE DATE(tst2:dates/*) = DATE '2012-03-01'";
        dml = session.query(sql);
        assertEquals(1, dml.size());

        // less-than on just date, not timestamp at 00:00:00
        sql = "SELECT * FROM File WHERE DATE(dc:created) <= DATE '2007-03-01'";
        dml = session.query(sql);
        assertEquals(1, dml.size());

        // TODO check bounds for meaningful test
        sql = "SELECT * FROM File WHERE DATE(dc:created) NOT BETWEEN DATE '2007-03-15' AND DATE '2008-01-01'";
        dml = session.query(sql);
        assertEquals(1, dml.size()); // 1 Document matches the BETWEEN query
    }

    @Test
    public void testDateBad() throws Exception {
        String sql;
        createDocs();

        try {
            sql = "SELECT * FROM File WHERE DATE(dc:title) = DATE '2012-01-01'";
            session.query(sql);
            fail("Should fail due to invalid cast");
        } catch (QueryParseException e) {
            String m = e.getMessage();
            assertTrue(m, m.contains("Cannot cast to DATE"));
        }

        try {
            sql = "SELECT * FROM File WHERE DATE(dc:created) = TIMESTAMP '2012-01-01 00:00:00'";
            session.query(sql);
            fail("Should fail due to invalid cast");
        } catch (QueryParseException e) {
            String m = e.getMessage();
            assertTrue(m, m.contains("DATE() cast must be used with DATE literal, not TIMESTAMP"));
        }

        try {
            sql = "SELECT * FROM File WHERE DATE(dc:created) BETWEEN TIMESTAMP '2012-01-01 00:00:00' AND DATE '2012-02-02'";
            session.query(sql);
            fail("Should fail due to invalid cast");
        } catch (QueryParseException e) {
            String m = e.getMessage();
            assertTrue(m, m.contains("DATE() cast must be used with DATE literal, not TIMESTAMP"));
        }

    }

    // other tests
    @Test
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

        DocumentModel doc = new DocumentModelImpl("/testfolder1", "mydoc", "MyDocType");
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

    @Test
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
        acl.add(new ACE("Administrator", "Everything", true));
        acl.add(ACE.BLOCK);
        acp.addACL(acl);
        folder1.setACP(acp, true);
        session.save();

        try (CoreSession bobSession = CoreInstance.openCoreSession(session.getRepositoryName(), "bob")) {
            DocumentModelList dml = bobSession.query("SELECT * FROM Document");
            assertEquals(3, dml.size());
        }
    }

    // same with queryAndFetch
    @Test
    public void testQueryWithSecurity2() throws Exception {
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
        acl.add(new ACE("Administrator", "Everything", true));
        acl.add(ACE.BLOCK);
        acp.addACL(acl);
        folder1.setACP(acp, true);
        session.save();

        try (CoreSession bobSession = CoreInstance.openCoreSession(session.getRepositoryName(), "bob")) {
            IterableQueryResult res = bobSession.queryAndFetch("SELECT * FROM Document", "NXQL");
            assertEquals(3, res.size());
            res.close();
        }
    }

    @Test
    public void testWithoutSecurityManager() throws Exception {
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
    }

    @Test
    // NoFileSecurityPolicy
    @LocalDeploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/security-policy-contrib.xml")
    public void testSecurityManagerBasic() throws Exception {
        doTestSecurityManager();
    }

    @Test
    // NoFile2SecurityPolicy
    @LocalDeploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/security-policy2-contrib.xml")
    public void testSecurityManagerWithTransformer() throws Exception {
        doTestSecurityManager();
    }

    public void doTestSecurityManager() throws Exception {
        createDocs();
        DocumentModelList dml;

        // needs a user who is not really an administrator
        // otherwise security policies are bypassed
        try (CoreSession admSession = CoreInstance.openCoreSession(session.getRepositoryName(), "Administrator")) {
            dml = admSession.query("SELECT * FROM Document");
            assertEquals(4, dml.size());
            assertEquals(4, dml.totalSize());
            dml = admSession.query("SELECT * FROM Document", null, 2, 0, true);
            assertEquals(2, dml.size());
            assertEquals(4, dml.totalSize());
            dml = admSession.query("SELECT * FROM Document", null, 2, 2, true);
            assertEquals(2, dml.size());
            assertEquals(4, dml.totalSize());
            dml = admSession.query("SELECT * FROM Document", null, 2, 3, true);
            assertEquals(1, dml.size());
            assertEquals(4, dml.totalSize());

            // add an ACL as well
            DocumentModel root = admSession.getRootDocument();
            ACP acp = new ACPImpl();
            ACL acl = new ACLImpl();
            acl.add(new ACE("Administrator", "Everything", true));
            acl.add(new ACE("bob", "Browse", true));
            acp.addACL(acl);
            root.setACP(acp, true);
            DocumentModel folder1 = admSession.getDocument(new PathRef("/testfolder2/testfolder3"));
            acp = new ACPImpl();
            acl = new ACLImpl();
            acl.add(new ACE("Administrator", "Everything", true));
            acl.add(ACE.BLOCK);
            acp.addACL(acl);
            folder1.setACP(acp, true);
            admSession.save();
        }

        try (CoreSession bobSession = CoreInstance.openCoreSession(session.getRepositoryName(), "bob")) {
            dml = bobSession.query("SELECT * FROM Document");
            assertEquals(3, dml.size());
            assertEquals(3, dml.totalSize());
            dml = bobSession.query("SELECT * FROM Document", null, 2, 0, true);
            assertEquals(2, dml.size());
            assertEquals(3, dml.totalSize());
            dml = bobSession.query("SELECT * FROM Document", null, 2, 1, true);
            assertEquals(2, dml.size());
            assertEquals(3, dml.totalSize());
            dml = bobSession.query("SELECT * FROM Document", null, 2, 2, true);
            assertEquals(1, dml.size());
            assertEquals(3, dml.totalSize());
        }
    }

    private static void assertIdSet(DocumentModelList dml, String... ids) {
        Collection<String> expected = new HashSet<String>(Arrays.asList(ids));
        Collection<String> actual = new HashSet<String>();
        for (DocumentModel d : dml) {
            actual.add(d.getId());
        }
        assertEquals(expected, actual);
    }

    @Test
    public void testQueryACL() throws Exception {
        createDocs();
        DocumentModel folder1 = session.getDocument(new PathRef("/testfolder1"));
        ACP acp = new ACPImpl();
        ACL acl = new ACLImpl();
        acl.add(new ACE("Administrator", "Everything", true));
        acl.add(new ACE("bob", "Browse", true));
        acl.add(new ACE("steve", "Read", true));
        Date now = new Date();
        Calendar begin = new GregorianCalendar();
        begin.setTimeInMillis(now.toInstant().minus(5, ChronoUnit.DAYS).toEpochMilli());
        Calendar end = new GregorianCalendar();
        end.setTimeInMillis(now.toInstant().plus(5, ChronoUnit.DAYS).toEpochMilli());
        acl.add(ACE.builder("leela", "Write").creator("Administrator").begin(begin).end(end).build());

        acl.add(ACE.BLOCK);
        acp.addACL(acl);
        folder1.setACP(acp, true);
        session.save();

        String queryBase = "SELECT * FROM Document WHERE ecm:isProxy = 0 AND ";

        // simple query
        checkQueryACL(1, queryBase + "ecm:acl/*/principal = 'bob'");

        // documents with both bob and steve
        checkQueryACL(1, queryBase + "ecm:acl/*/principal = 'bob' AND ecm:acl/*/principal = 'steve'");

        // bob cannot be steve, no match
        checkQueryACL(0, queryBase + "ecm:acl/*1/principal = 'bob' AND ecm:acl/*1/principal = 'steve'");

        // bob with Browse
        checkQueryACL(1, queryBase + "ecm:acl/*1/principal = 'bob' AND ecm:acl/*1/permission = 'Browse'");

        // bob with Browse granted
        checkQueryACL(1, queryBase
                + "ecm:acl/*1/principal = 'bob' AND ecm:acl/*1/permission = 'Browse' AND ecm:acl/*1/grant = 1");

        // bob with Browse denied, no match
        checkQueryACL(0, queryBase
                + "ecm:acl/*1/principal = 'bob' AND ecm:acl/*1/permission = 'Browse' AND ecm:acl/*1/grant = 0");

        // bob with Read, no match
        checkQueryACL(0, queryBase + "ecm:acl/*1/principal = 'bob' AND ecm:acl/*1/permission = 'Read'");

        // a bob and a Read
        checkQueryACL(1, queryBase + "ecm:acl/*/principal = 'bob' AND ecm:acl/*/permission = 'Read'");

        // creator is Administrator
        checkQueryACL(1, queryBase + "ecm:acl/*/creator = 'Administrator'");

        // document for leela with a begin date after 2007-01-01
        checkQueryACL(1, queryBase
                + "ecm:acl/*1/principal = 'leela' AND ecm:acl/*1/begin >= DATE '2007-01-01' AND ecm:acl/*1/end <= DATE '2020-01-01'");

        // document for leela with an end date after 2020-01-01, no match
        checkQueryACL(0, queryBase + "ecm:acl/*1/principal = 'leela' AND ecm:acl/*1/end >= DATE '2020-01-01'");

        // document with valid begin date but not end date, no match
        checkQueryACL(0, queryBase + "ecm:acl/*1/begin >= DATE '2007-01-01' AND ecm:acl/*1/end >= DATE '2020-01-01'");

        // document with effective acl
        checkQueryACL(1, queryBase + "ecm:acl/*1/status = 1");

        if (!notMatchesNull()) {
            // document with pending or archived acl, no match
            checkQueryACL(0, queryBase + "ecm:acl/*1/status <> 1");
        }

        // block
        checkQueryACL(1, queryBase
                + "ecm:acl/*1/principal = 'Everyone' AND ecm:acl/*1/permission = 'Everything' AND ecm:acl/*1/grant = 0");

        if (!isDBS()) {
            // explicit array index
            checkQueryACL(1, queryBase + "ecm:acl/1/principal = 'bob'");
        }
    }

    protected void checkQueryACL(int expected, String query) {
        DocumentModelList dml = session.query(query);
        assertEquals(expected, dml.size());

        IterableQueryResult res = session.queryAndFetch(query, "NXQL");
        long size = res.size();
        res.close();
        assertEquals(expected, size);
    }

    @Test
    public void testQueryACLReturnedValue() throws Exception {
        createDocs();
        DocumentModel folder1 = session.getDocument(new PathRef("/testfolder1"));
        ACP acp = new ACPImpl();
        ACL acl = new ACLImpl();
        acl.add(new ACE("Administrator", "Everything", true));
        acl.add(new ACE("bob", "Browse", true));
        acl.add(new ACE("steve", "Read", true));
        Date now = new Date();
        Calendar begin = new GregorianCalendar();
        begin.setTimeInMillis(now.toInstant().minus(5, ChronoUnit.DAYS).toEpochMilli());
        // avoid DB rounding for timestamp on seconds / milliseconds
        begin.set(Calendar.SECOND, 0);
        begin.set(Calendar.MILLISECOND, 0);
        Calendar end = new GregorianCalendar();
        end.setTimeInMillis(now.toInstant().plus(5, ChronoUnit.DAYS).toEpochMilli());
        end.set(Calendar.SECOND, 0);
        end.set(Calendar.MILLISECOND, 0);
        acl.add(ACE.builder("leela", "Write").creator("Administrator").begin(begin).end(end).build());
        acl.add(ACE.BLOCK);
        acp.addACL(acl);
        folder1.setACP(acp, true);
        session.save();

        // update the begin and end dates from the one being stored in the DB to correctly match them after
        ACP updatedACP = session.getACP(folder1.getRef());
        ACL updatedACL = updatedACP.getACL(ACL.LOCAL_ACL);
        for (ACE ace : updatedACL) {
            if ("leela".equals(ace.getUsername())) {
                begin = ace.getBegin();
                end = ace.getEnd();
                break;
            }
        }

        IterableQueryResult res;
        // simple query
        res = session.queryAndFetch(
                "SELECT ecm:uuid, ecm:acl/*1/name, ecm:acl/*1/principal, ecm:acl/*1/permission FROM Document WHERE ecm:isProxy = 0 AND "
                        + "ecm:acl/*1/permission in ('Read', 'Browse') AND ecm:acl/*1/grant = 1",
                "NXQL");
        assertEquals(2, res.size());
        Set<String> set = new HashSet<>();
        for (Map<String, Serializable> map : res) {
            set.add(map.get("ecm:acl/*1/name") + ":" + map.get("ecm:acl/*1/principal") + ":"
                    + map.get("ecm:acl/*1/permission"));
        }
        res.close();
        assertEquals(new HashSet<>(Arrays.asList("local:bob:Browse", "local:steve:Read")), set);

        // read full ACL
        // ecm:pos in VCS-specific so not checked
        res = session.queryAndFetch(
                "SELECT ecm:uuid, ecm:acl/*1/name, ecm:acl/*1/principal, ecm:acl/*1/permission, ecm:acl/*1/grant"
                        + ", ecm:acl/*1/creator, ecm:acl/*1/begin, ecm:acl/*1/end FROM Document"
                        + " WHERE ecm:isProxy = 0 AND " + "ecm:acl/*/principal = 'bob'",
                "NXQL");
        assertEquals(5, res.size());
        set = new HashSet<>();
        for (Map<String, Serializable> map : res) {
            String ace = map.get("ecm:acl/*1/name") + ":" + map.get("ecm:acl/*1/principal") + ":"
                    + map.get("ecm:acl/*1/permission") + ":" + map.get("ecm:acl/*1/grant") + ":"
                    + map.get("ecm:acl/*1/creator");
            Calendar cal = (Calendar) map.get("ecm:acl/*1/begin");
            ace += ":" + (cal != null ? cal.getTimeInMillis() : null);
            cal = (Calendar) map.get("ecm:acl/*1/end");
            ace += ":" + (cal != null ? cal.getTimeInMillis() : null);
            set.add(ace);
        }
        res.close();
        assertEquals(
                new HashSet<>(
                        Arrays.asList("local:Administrator:Everything:true:null:null:null",
                                "local:bob:Browse:true:null:null:null", "local:steve:Read:true:null:null:null",
                                "local:leela:Write:true:Administrator:" + begin.getTimeInMillis() + ":"
                                        + end.getTimeInMillis(),
                                "local:Everyone:Everything:false:null:null:null")),
                set);
    }

    @Test
    public void testQueryWithProxies() throws Exception {
        createDocs();
        DocumentModel proxy = publishDoc();

        DocumentModel doc = session.getDocument(new PathRef("/testfolder2/testfolder3/testfile4"));
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
        dml = session.query("SELECT * FROM Document WHERE dc:title = 'testfile4Title'");
        assertIdSet(dml, docId, proxyId, versionId);

        dml = session.query("SELECT * FROM Document WHERE ecm:isProxy = 1");
        assertIdSet(dml, proxyId);
        dml = session.query("SELECT * FROM Document WHERE ecm:isProxy <> 0");
        assertIdSet(dml, proxyId);

        dml = session.query("SELECT * FROM Document WHERE ecm:isProxy = 0");
        assertEquals(8, dml.size()); // 7 folder/docs, 1 version
        dml = session.query("SELECT * FROM Document WHERE ecm:isProxy <> 1");
        assertEquals(8, dml.size());

        dml = session.query("SELECT * FROM Document WHERE ecm:isCheckedInVersion = 1");
        assertIdSet(dml, versionId);

        dml = session.query("SELECT * FROM Document WHERE ecm:isCheckedInVersion = 0");
        assertEquals(8, dml.size()); // 7 folder/docs, 1 proxy

        dml = session.query("SELECT * FROM Document WHERE ecm:isProxy = 0 AND ecm:isCheckedInVersion = 0");
        assertEquals(7, dml.size()); // 7 folder/docs

        // filter out proxies explicitely, keeps live and version
        dml = session.query("SELECT * FROM Document WHERE dc:title = 'testfile4Title' AND ecm:isProxy = 0");
        assertIdSet(dml, docId, versionId);

        // only keep proxies
        dml = session.query("SELECT * FROM Document WHERE dc:title = 'testfile4Title' AND ecm:isProxy = 1");
        assertIdSet(dml, proxyId);

        // only keep versions
        dml = session.query("SELECT * FROM Document WHERE dc:title = 'testfile4Title' AND ecm:isCheckedInVersion = 1");
        assertIdSet(dml, versionId);

        // "deep" isProxy
        dml = session.query("SELECT * FROM Document WHERE (dc:title = 'blah' OR ecm:isProxy = 1)");
        assertIdSet(dml, proxyId);
        dml = session.query(
                "SELECT * FROM Document WHERE ecm:isProxy = 0 AND (dc:title = 'testfile1_Title' OR ecm:isProxy = 1)");
        assertEquals(1, dml.size());

        // proxy query with order by
        dml = session.query("SELECT * FROM Document WHERE dc:title = 'testfile4Title' ORDER BY dc:title");
        assertIdSet(dml, docId, proxyId, versionId);
        dml = session.query("SELECT * FROM File WHERE dc:title = 'testfile4Title' ORDER BY dc:description");
        assertIdSet(dml, docId, proxyId, versionId);
    }

    @Test
    public void testQueryPaging() throws Exception {
        createDocs();
        DocumentModelList whole = session.query("SELECT * FROM Document ORDER BY dc:modified, ecm:uuid");
        assertTrue(whole.size() >= 2);
        DocumentModelList firstPage = session.query("SELECT * from Document ORDER BY dc:modified, ecm:uuid", null, 1, 0,
                false);
        assertEquals(1, firstPage.size());
        assertEquals(whole.get(0).getId(), firstPage.get(0).getId());
        DocumentModelList secondPage = session.query("SELECT * from Document ORDER BY dc:modified, ecm:uuid", null, 1,
                1, false);
        assertEquals(1, secondPage.size());
        assertEquals(whole.get(1).getId(), secondPage.get(0).getId());
    }

    @Test
    public void testQueryPrimaryTypeOptimization() throws Exception {
        // check these queries in the logs

        // Folder
        session.query("SELECT * FROM Document WHERE ecm:primaryType = 'Folder'");
        // empty
        session.query("SELECT * FROM Document WHERE ecm:primaryType = 'Folder'" + " AND ecm:primaryType = 'File'");
        // empty
        session.query("SELECT * FROM Folder WHERE ecm:primaryType = 'Note'");
        // Folder
        session.query("SELECT * FROM Document WHERE ecm:primaryType IN ('Folder', 'Note')"
                + " AND ecm:primaryType = 'Folder'");

        // just folderish
        session.query("SELECT * FROM Document WHERE ecm:mixinType = 'Folderish'");
        // no folderish
        session.query("SELECT * FROM Document WHERE ecm:mixinType <> 'Folderish'");
        // just hidden
        session.query("SELECT * FROM Document WHERE ecm:mixinType = 'HiddenInNavigation'");
        // no hidden
        session.query("SELECT * FROM Document WHERE ecm:mixinType <> 'HiddenInNavigation'");

        // empty
        session.query("SELECT * FROM Note WHERE ecm:mixinType = 'Folderish'");
    }

    @Test
    public void testQueryMixinTypeNotPerDocument() throws Exception {
        createDocs();
        DocumentModel file1 = session.getDocument(new PathRef("/testfolder1/testfile1"));
        file1.addFacet("NotPerDocFacet");
        file1.addFacet("NotPerDocFacet2");
        file1 = session.saveDocument(file1);
        session.save();

        // doc has facet but not found by search
        DocumentModelList dml;
        if (!isDBS()) {
            // VCS compat in repository config
            dml = session.query("SELECT * FROM Document WHERE ecm:mixinType = 'NotPerDocFacet'");
            assertEquals(0, dml.size());
        }
        // same thing with type service
        dml = session.query("SELECT * FROM Document WHERE ecm:mixinType = 'NotPerDocFacet2'");
        assertEquals(0, dml.size());
    }

    @Test
    public void testQuerySpecialFields() throws Exception {
        // ecm:isProxy and ecm:isCheckedInVersion are already tested in
        // testQueryWithProxies

        // ecm:path already tested in testStartsWith

        createDocs();
        DocumentModel proxy = publishDoc();
        DocumentModel version = session.getDocument(new IdRef(proxy.getSourceId()));

        DocumentModelList dml;
        DocumentModel folder1 = session.getDocument(new PathRef("/testfolder1"));
        DocumentModel file1 = session.getDocument(new PathRef("/testfolder1/testfile1"));
        DocumentModel file2 = session.getDocument(new PathRef("/testfolder1/testfile2"));
        DocumentModel file3 = session.getDocument(new PathRef("/testfolder1/testfile3"));
        DocumentModel file4 = session.getDocument(new PathRef("/testfolder2/testfolder3/testfile4"));
        file1.setLock();
        session.save();

        /*
         * ecm:uuid
         */
        dml = session.query(String.format("SELECT * FROM Document WHERE ecm:uuid = '%s'", file1.getId()));
        assertIdSet(dml, file1.getId());
        dml = session.query(String.format("SELECT * FROM Document WHERE ecm:uuid = '%s'", proxy.getId()));
        assertIdSet(dml, proxy.getId());

        /*
         * ecm:name
         */
        dml = session.query(String.format("SELECT * FROM Document WHERE ecm:name = '%s'", file1.getName()));
        assertIdSet(dml, file1.getId());
        // Disabled, version and proxies names don't need to be identical
        // dml = session.query(String.format(
        // "SELECT * FROM Document WHERE ecm:name = '%s'", file4.getName()));
        // assertIdSet(dml, file4.getId(), proxy.getId(), version.getId());

        /*
         * ecm:parentId
         */
        dml = session.query(String.format("SELECT * FROM Document WHERE ecm:parentId = '%s'", folder1.getId()));
        assertIdSet(dml, file1.getId(), file2.getId(), file3.getId(), proxy.getId());

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
        // same with facet
        FacetFilter filter;
        filter = new FacetFilter(FacetNames.FOLDERISH, true);
        dml = session.query("SELECT * FROM Document ", filter);
        assertEquals(3, dml.size());
        filter = new FacetFilter(FacetNames.FOLDERISH, false);
        dml = session.query("SELECT * FROM Document ", filter);
        assertEquals(6, dml.size());
        filter = new FacetFilter(FacetNames.DOWNLOADABLE, true);
        dml = session.query("SELECT * FROM Document ", filter);
        assertEquals(5, dml.size()); // 3 files, 1 proxy, 1 version
        filter = new FacetFilter(FacetNames.DOWNLOADABLE, false);
        dml = session.query("SELECT * FROM Document ", filter);
        assertEquals(4, dml.size());
        filter = new FacetFilter(FacetNames.VERSIONABLE, true);
        dml = session.query("SELECT * FROM Document ", filter);
        assertEquals(6, dml.size()); // 1 note, 3 files, 1 proxy, 1 version
        filter = new FacetFilter(FacetNames.VERSIONABLE, false);
        dml = session.query("SELECT * FROM Document ", filter);
        assertEquals(3, dml.size());

        /*
         * ecm:currentLifeCycleState
         */
        dml = session.query("SELECT * FROM Document WHERE ecm:currentLifeCycleState = 'project'");
        // 3 folders, 1 note, 3 files, 1 proxy, 1 version
        assertEquals(9, dml.size());

        // to observe locks, which have been set from another transaction
        // we may need to actually commit and re-open a transaction (MySQL)
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        /*
         * ecm:lockOwner
         */
        // don't use a '' here for Oracle, for which '' IS NULL
        dml = session.query("SELECT * FROM Document WHERE ecm:lockOwner <> '_'");
        if (notMatchesNull()) {
            // check that we find the doc in the list
            assertTrue(dml.stream().map(doc -> doc.getId()).collect(Collectors.toSet()).contains(file1.getId()));
        } else {
            assertIdSet(dml, file1.getId());
        }
        dml = session.query("SELECT * FROM Document ORDER BY ecm:lockOwner");
        assertEquals(9, dml.size());

        /*
         * ecm:lockCreated
         */
        dml = session.query("SELECT * FROM Document ORDER BY ecm:lockCreated");
        assertEquals(9, dml.size());
    }

    @Test
    public void testQuerySpecialFieldsVersioning() throws Exception {
        createDocs();
        DocumentModel doc = session.getDocument(new PathRef("/testfolder2/testfolder3/testfile4"));
        DocumentModel proxy = publishDoc(); // testfile4 to testfolder1
        DocumentModel version = session.getDocument(new IdRef(proxy.getSourceId()));
        DocumentModel file1 = session.getDocument(new PathRef("/testfolder1/testfile1"));
        DocumentRef v1 = session.checkIn(file1.getRef(), VersioningOption.MAJOR, "comment1");
        session.checkOut(file1.getRef());
        DocumentRef v2 = session.checkIn(file1.getRef(), VersioningOption.MAJOR, "comment2");
        session.save();

        DocumentModelList dml;

        /*
         * ecm:isCheckedIn
         */
        dml = session.query("SELECT * FROM Document WHERE ecm:isCheckedIn = 1");
        assertIdSet(dml, doc.getId(), file1.getId());
        dml = session.query("SELECT * FROM Document WHERE ecm:isCheckedIn = 0");
        assertEquals(9, dml.size());
        dml = session.query("SELECT * FROM Document WHERE ecm:isCheckedIn = 0 AND ecm:isProxy = 0");
        assertEquals(8, dml.size());
        // checkout and make sure we find it in correct state
        session.checkOut(file1.getRef());
        session.save();
        dml = session.query("SELECT * FROM Document WHERE ecm:isCheckedIn = 1");
        assertIdSet(dml, doc.getId());
        dml = session.query("SELECT * FROM Document WHERE ecm:isCheckedIn = 0");
        assertEquals(10, dml.size());

        /*
         * ecm:isVersion / ecm:isCheckedInVersion
         */
        dml = session.query("SELECT * FROM Document WHERE ecm:isVersion = 1");
        assertIdSet(dml, version.getId(), v1.toString(), v2.toString());
        dml = session.query("SELECT * FROM Document WHERE ecm:isVersion = 0");
        assertEquals(8, dml.size()); // 7 folder/docs, 1 proxy
        // old spelling ecm:isCheckedInVersion
        dml = session.query("SELECT * FROM Document WHERE ecm:isCheckedInVersion = 1");
        assertIdSet(dml, version.getId(), v1.toString(), v2.toString());
        dml = session.query("SELECT * FROM Document WHERE ecm:isCheckedInVersion = 0");
        assertEquals(8, dml.size()); // 7 folder/docs, 1 proxy

        /*
         * ecm:isLatestVersion
         */
        dml = session.query("SELECT * FROM Document WHERE ecm:isLatestVersion = 1");
        assertIdSet(dml, version.getId(), v2.toString(), proxy.getId());
        dml = session.query("SELECT * FROM Document WHERE ecm:isLatestVersion = 1 AND ecm:isProxy = 0");
        assertIdSet(dml, version.getId(), v2.toString());
        dml = session.query("SELECT * FROM Document WHERE ecm:isLatestVersion = 0");
        assertEquals(8, dml.size()); // 7 folder/docs, 1 proxy
        dml = session.query("SELECT * FROM Document WHERE ecm:isLatestVersion = 0 AND ecm:isProxy = 0");
        assertEquals(8, dml.size()); // 7 folder/docs, 1 proxy

        /*
         * ecm:isLatestMajorVersion
         */
        dml = session.query("SELECT * FROM Document WHERE ecm:isLatestMajorVersion = 1");
        assertIdSet(dml, v2.toString());
        dml = session.query("SELECT * FROM Document WHERE ecm:isLatestMajorVersion = 0");
        assertEquals(10, dml.size());

        /*
         * ecm:versionLabel
         */
        dml = session.query("SELECT * FROM Document WHERE ecm:versionLabel = '0.1'");
        // we can check the version label on a proxy
        assertIdSet(dml, version.getId(), proxy.getId());
        dml = session.query("SELECT * FROM Document WHERE ecm:versionLabel = '0.1' AND ecm:isProxy = 0");
        assertIdSet(dml, version.getId());

        /*
         * ecm:versionDescription
         */
        dml = session.query("SELECT * FROM Document WHERE ecm:versionDescription = 'comment1'");
        assertIdSet(dml, v1.toString());
        dml = session.query("SELECT * FROM Document WHERE ecm:versionDescription = 'comment2'");
        assertIdSet(dml, v2.toString());

        /*
         * ecm:versionCreated
         */
        dml = session.query("SELECT * FROM Document WHERE ecm:versionCreated IS NOT NULL");
        assertIdSet(dml, version.getId(), v1.toString(), v2.toString(), proxy.getId());
        dml = session.query("SELECT * FROM Document WHERE ecm:versionCreated IS NOT NULL and ecm:isProxy = 0");
        assertIdSet(dml, version.getId(), v1.toString(), v2.toString());

        /*
         * ecm:versionVersionableId
         */
        dml = session.query("SELECT * FROM Document WHERE ecm:versionVersionableId = '" + doc.getId() + "'");
        assertIdSet(dml, version.getId(), proxy.getId());
        dml = session.query(
                "SELECT * FROM Document WHERE ecm:versionVersionableId = '" + doc.getId() + "' AND ecm:isProxy = 0");
        assertIdSet(dml, version.getId());

        /*
         * ecm:proxyTargetId
         */
        dml = session.query("SELECT * FROM Document WHERE ecm:proxyTargetId = '" + version.getId() + "'");
        assertIdSet(dml, proxy.getId());

        /*
         * ecm:proxyVersionableId
         */
        dml = session.query("SELECT * FROM Document WHERE ecm:proxyVersionableId = '" + doc.getId() + "'");
        assertIdSet(dml, proxy.getId());
    }

    @Test
    public void testEmptyLifecycle() throws Exception {
        DocumentModelList dml;
        createDocs();
        String sql = "SELECT * FROM Document WHERE ecm:currentLifeCycleState <> 'deleted'";

        dml = session.query(sql);
        assertEquals(7, dml.size());

        // create a doc with no lifecycle associated
        DocumentModel doc = new DocumentModelImpl("/testfolder1", "mydoc", "MyDocType");
        doc = session.createDocument(doc);
        session.save();
        assertEquals("undefined", doc.getCurrentLifeCycleState());
        dml = session.query(sql);
        assertEquals(8, dml.size());
    }

    @Test
    public void testOrderByAndDistinct() throws Exception {
        createDocs();

        String query;
        DocumentModelList dml;

        DocumentModel file1 = session.getDocument(new PathRef("/testfolder1/testfile1"));
        file1.setProperty("dublincore", "title", "hello world 1");

        session.saveDocument(file1);
        session.save();

        waitForFulltextIndexing();

        query = "SELECT * FROM File Where dc:title = 'hello world 1' ORDER BY ecm:currentLifeCycleState";

        dml = session.query(query);

        assertIdSet(dml, file1.getId());
        query = "SELECT * FROM File Where dc:title = 'hello world 1' ORDER BY ecm:versionLabel";

        dml = session.query(query);
        assertIdSet(dml, file1.getId());
    }

    @Test
    public void testQueryIterable() throws Exception {
        createDocs();

        IterableQueryResult res = session.queryAndFetch("SELECT * FROM File", "NXQL");
        List<Map<String, Serializable>> l = new LinkedList<Map<String, Serializable>>();
        for (Map<String, Serializable> x : res) {
            l.add(x);
        }
        assertEquals(3, l.size());
        res.close();

        // cursor behavior
        res = session.queryAndFetch("SELECT * FROM File", "NXQL");
        Iterator<Map<String, Serializable>> it = res.iterator();
        assertEquals(0, res.pos());
        it.next();
        assertEquals(1, res.pos());
        assertEquals(3, res.size());
        assertEquals(1, res.pos());
        assertTrue(it.hasNext());
        assertEquals(1, res.pos());
        it.next();
        assertEquals(2, res.pos());
        assertTrue(it.hasNext());
        assertEquals(2, res.pos());
        it.next();
        assertEquals(3, res.pos());
        assertFalse(it.hasNext());
        assertEquals(3, res.pos());

        res.skipTo(1);
        assertEquals(3, res.size());
        assertTrue(it.hasNext());
        assertEquals(1, res.pos());
        it.next();
        assertEquals(2, res.pos());
        res.close();

        // checking size when at end
        res = session.queryAndFetch("SELECT * FROM File", "NXQL");
        it = res.iterator();
        it.next();
        it.next();
        it.next();
        assertFalse(it.hasNext());
        assertEquals(3, res.size());
        res.close();

        // size when query returns nothing
        res = session.queryAndFetch("SELECT * FROM File WHERE dc:title = 'zzz'", "NXQL");
        it = res.iterator();
        assertFalse(it.hasNext());
        assertEquals(0, res.size());
        res.close();
    }

    @Test
    // NoFile2SecurityPolicy
    @LocalDeploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/security-policy2-contrib.xml")
    public void testQueryIterableWithTransformer() throws Exception {
        createDocs();
        IterableQueryResult res;

        res = session.queryAndFetch("SELECT * FROM Document", "NXQL");
        assertEquals(4, res.size()); // instead of 7 without security policy
        res.close();
    }

    @Test
    public void testQueryComplexTypeFiles() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "myfile", "File");
        List<Object> files = new LinkedList<>();
        Map<String, Object> f = new HashMap<>();
        f.put("file", Blobs.createBlob("b1", "text/plain", "UTF-8", "f1"));
        files.add(f);
        doc.setProperty("files", "files", files);
        doc = session.createDocument(doc);
        session.save();

        DocumentModelList dml = session.query("SELECT * FROM File");
        assertEquals(1, dml.size());
        // Case insensitive databases may fail with:
        // ERROR Unknown document type: file
        // due to its case-insensitivity in = and IN tests...
        // and returning an empty query, cf SQLQueryResult.getDocumentModels
    }

    @Test
    public void testSelectColumns() throws Exception {
        String query;
        IterableQueryResult res;
        Iterator<Map<String, Serializable>> it;
        Map<String, Serializable> map;

        createDocs();

        // check proper tables are joined even if not in FROM
        query = "SELECT ecm:uuid, dc:title FROM File";
        res = session.queryAndFetch(query, "NXQL");
        assertEquals(3, res.size());
        map = res.iterator().next();
        assertTrue(map.containsKey("dc:title"));
        assertTrue(map.containsKey(NXQL.ECM_UUID));
        res.close();

        // check with no proxies (no subselect)
        query = "SELECT ecm:uuid, dc:title FROM File where ecm:isProxy = 0";
        res = session.queryAndFetch(query, "NXQL");
        assertEquals(3, res.size());
        res.close();

        // check content
        query = "SELECT ecm:uuid, dc:title FROM File ORDER BY dc:title";
        res = session.queryAndFetch(query, "NXQL");
        assertEquals(3, res.size());
        it = res.iterator();
        map = it.next();
        assertEquals("testfile1_Title", map.get("dc:title"));
        map = it.next();
        assertEquals("testfile2_Title", map.get("dc:title"));
        map = it.next();
        assertEquals("testfile4Title", map.get("dc:title"));
        res.close();

        // check content with no proxies (simpler query with no UNION ALL)
        query = "SELECT ecm:uuid, dc:title FROM File WHERE ecm:isProxy = 0 ORDER BY dc:title";
        res = session.queryAndFetch(query, "NXQL");
        assertEquals(3, res.size());
        it = res.iterator();
        map = it.next();
        assertEquals("testfile1_Title", map.get("dc:title"));
        map = it.next();
        assertEquals("testfile2_Title", map.get("dc:title"));
        map = it.next();
        assertEquals("testfile4Title", map.get("dc:title"));
        res.close();
    }

    @Test
    public void testSelectColumnsSameName() throws Exception {
        String query;
        IterableQueryResult res;
        Map<String, Serializable> map;

        // two fields with same key
        DocumentModel file = new DocumentModelImpl("/", "testfile", "File2");
        file.setPropertyValue("dc:title", "title1");
        file.setPropertyValue("tst2:title", "title2");
        file = session.createDocument(file);
        session.save();

        query = "SELECT tst2:title, dc:title FROM File WHERE dc:title = 'title1' AND ecm:isProxy = 0";
        res = session.queryAndFetch(query, "NXQL");
        assertEquals(1, res.size());
        map = res.iterator().next();
        assertEquals("title1", map.get("dc:title"));
        assertEquals("title2", map.get("tst2:title"));
        res.close();

        // now with proxies, which needs a subselect and re-selects columns
        query = "SELECT tst2:title, dc:title FROM File WHERE dc:title = 'title1' ORDER BY ecm:uuid";
        res = session.queryAndFetch(query, "NXQL");
        assertEquals(1, res.size());
        map = res.iterator().next();
        assertEquals("title1", map.get("dc:title"));
        assertEquals("title2", map.get("tst2:title"));
        res.close();

        // same without ORDER BY
        query = "SELECT tst2:title, dc:title FROM File WHERE dc:title = 'title1'";
        res = session.queryAndFetch(query, "NXQL");
        assertEquals(1, res.size());
        map = res.iterator().next();
        assertEquals("title1", map.get("dc:title"));
        assertEquals("title2", map.get("tst2:title"));
        res.close();
    }

    @Test
    public void testQueryDistinctId() throws Exception {
        DocumentModelList dml;
        createDocs();

        dml = session.query("SELECT DISTINCT ecm:uuid FROM File");
        assertEquals(3, dml.size());
    }

    @Test
    public void testQueryAndFetchDistinctId() throws Exception {
        makeComplexDoc();

        String query = "SELECT DISTINCT ecm:uuid FROM TestDoc WHERE tst:friends/*/firstname = 'John'";
        IterableQueryResult res = session.queryAndFetch(query, "NXQL");
        assertEquals(1, res.size());
        res.close();
    }

    @Test
    public void testSelectColumnsDistinct() throws Exception {
        assumeTrue("DBS does not support DISTINCT in queries", supportsDistinct());

        String query;
        IterableQueryResult res;

        createDocs();

        query = "SELECT DISTINCT dc:title FROM File";
        res = session.queryAndFetch(query, "NXQL");
        assertEquals(3, res.size());
        res.close();

        // some parents are identical
        query = "SELECT DISTINCT ecm:parentId FROM File";
        res = session.queryAndFetch(query, "NXQL");
        assertEquals(2, res.size());
        res.close();

        // without column aliasing
        query = "SELECT DISTINCT ecm:parentId FROM File WHERE ecm:isProxy = 0";
        res = session.queryAndFetch(query, "NXQL");
        assertEquals(2, res.size());
        res.close();
    }

    // ----- timestamp tests -----

    protected Date setupDocTest() throws Exception {
        Date currentDate = new Date();
        DocumentModel testDocument = new DocumentModelImpl("/", "testfolder1", "Folder");
        testDocument.setPropertyValue("dc:title", "test");
        testDocument.setPropertyValue("dc:modified", currentDate);
        testDocument = session.createDocument(testDocument);
        session.save();
        return ((Calendar) testDocument.getPropertyValue("dc:modified")).getTime();
    }

    protected static Date addSecond(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.SECOND, 1);
        return cal.getTime();
    }

    protected static String formatTimestamp(Date date) {
        return new SimpleDateFormat("'TIMESTAMP' ''yyyy-MM-dd HH:mm:ss.SSS''").format(date);
    }

    @Test
    public void testEqualsTimeWithMilliseconds() throws Exception {
        Date currentDate = setupDocTest();
        String testQuery = String.format(
                "SELECT * FROM Folder WHERE dc:title = 'test' AND dc:modified = %s" + " AND ecm:isProxy = 0",
                formatTimestamp(currentDate));
        DocumentModelList docs = session.query(testQuery);
        assertEquals(1, docs.size());
    }

    @Test
    public void testLTTimeWithMilliseconds() throws Exception {
        Date currentDate = setupDocTest();
        // add a second to be sure that the document is found
        currentDate = addSecond(currentDate);
        String testQuery = String.format(
                "SELECT * FROM Folder WHERE dc:title = 'test' AND dc:modified < %s" + " AND ecm:isProxy = 0",
                formatTimestamp(currentDate));
        DocumentModelList docs = session.query(testQuery);
        assertEquals(1, docs.size());
    }

    @Test
    public void testQueryIsNull() throws Exception {
        DocumentModelList dml;
        createDocs();

        dml = session.query("SELECT * FROM File WHERE dc:title IS NOT NULL");
        assertEquals(3, dml.size());
        dml = session.query("SELECT * FROM File WHERE dc:title IS NULL");
        assertEquals(0, dml.size());

        DocumentModel file1 = session.getDocument(new PathRef("/testfolder1/testfile1"));
        file1.setPropertyValue("dc:title", null);
        session.saveDocument(file1);
        session.save();

        dml = session.query("SELECT * FROM File WHERE dc:title IS NOT NULL");
        assertEquals(2, dml.size());
        dml = session.query("SELECT * FROM File WHERE dc:title IS NULL");
        assertEquals(1, dml.size());

        // we didn't write the uid schema for all files
        dml = session.query("SELECT * FROM File WHERE uid IS NOT NULL");
        assertEquals(1, dml.size());
        dml = session.query("SELECT * FROM File WHERE uid IS NULL");
        assertEquals(2, dml.size());
    }

    @Test
    public void testMultilineQuery() throws Exception {
        DocumentModelList dml;
        createDocs();

        String query = "SELECT * \n            FROM File \n      WHERE dc:title IS NOT NULL \n       ORDER BY ecm:path";
        dml = session.query(query);
        assertEquals(3, dml.size());

        query = "SELECT * \r\n        FROM File \r\n      WHERE dc:title IS NULL \r\n       ORDER BY ecm:path DESC";
        dml = session.query(query);
        assertEquals(0, dml.size());
    }

    protected static final String TAG_DOCUMENT_TYPE = "Tag";

    protected static final String TAG_LABEL_FIELD = "tag:label";

    protected static final String TAGGING_DOCUMENT_TYPE = "Tagging";

    protected static final String TAGGING_SOURCE_FIELD = "relation:source";

    protected static final String TAGGING_TARGET_FIELD = "relation:target";

    // file1: tag1, tag2
    // file2: tag1
    protected void createTags() throws Exception {
        DocumentModel file1 = session.getDocument(new PathRef("/testfolder1/testfile1"));
        DocumentModel file2 = session.getDocument(new PathRef("/testfolder1/testfile2"));

        String label1 = "tag1";
        DocumentModel tag1 = session.createDocumentModel(null, label1, TAG_DOCUMENT_TYPE);
        // tag.setPropertyValue("dc:created", date);
        tag1.setPropertyValue(TAG_LABEL_FIELD, label1);
        tag1 = session.createDocument(tag1);

        String label2 = "tag2";
        DocumentModel tag2 = session.createDocumentModel(null, label2, TAG_DOCUMENT_TYPE);
        // tag.setPropertyValue("dc:created", date);
        tag2.setPropertyValue(TAG_LABEL_FIELD, label2);
        tag2 = session.createDocument(tag2);

        DocumentModel tagging1to1 = session.createDocumentModel(null, label1, TAGGING_DOCUMENT_TYPE);
        // tagging.setPropertyValue("dc:created", date);
        // if (username != null) {
        // tagging.setPropertyValue("dc:creator", username);
        // }
        tagging1to1.setPropertyValue(TAGGING_SOURCE_FIELD, file1.getId());
        tagging1to1.setPropertyValue(TAGGING_TARGET_FIELD, tag1.getId());
        tagging1to1 = session.createDocument(tagging1to1);

        DocumentModel tagging1to2 = session.createDocumentModel(null, label2, TAGGING_DOCUMENT_TYPE);
        tagging1to2.setPropertyValue(TAGGING_SOURCE_FIELD, file1.getId());
        tagging1to2.setPropertyValue(TAGGING_TARGET_FIELD, tag2.getId());
        tagging1to2 = session.createDocument(tagging1to2);

        DocumentModel tagging2to1 = session.createDocumentModel(null, label1, TAGGING_DOCUMENT_TYPE);
        tagging2to1.setPropertyValue(TAGGING_SOURCE_FIELD, file2.getId());
        tagging2to1.setPropertyValue(TAGGING_TARGET_FIELD, tag1.getId());
        tagging2to1 = session.createDocument(tagging2to1);

        // create a relation that isn't a Tagging
        DocumentModel rel = session.createDocumentModel(null, label1, "Relation");
        rel.setPropertyValue(TAGGING_SOURCE_FIELD, file1.getId());
        rel.setPropertyValue(TAGGING_TARGET_FIELD, tag1.getId());
        rel = session.createDocument(rel);

        session.save();
        waitForFulltextIndexing();
    }

    protected String nxql(String nxql) {
        if (proxies) {
            return nxql;
        } else if (nxql.contains(" WHERE ")) {
            return nxql.replace(" WHERE ", " WHERE ecm:isProxy = 0 AND ");
        } else {
            return nxql + " WHERE ecm:isProxy = 0";
        }
    }

    @Test
    public void testTagsWithProxies() throws Exception {
        proxies = true;
        testTags();
    }

    @Test
    public void testTagsWithoutProxies() throws Exception {
        proxies = false;
        testTags();
    }

    protected void testTags() throws Exception {
        assumeTrue("DBS does not support tags", supportsTags());

        String nxql;
        DocumentModelList dml;
        IterableQueryResult res;

        createDocs();
        createTags();
        DocumentModel file1 = session.getDocument(new PathRef("/testfolder1/testfile1"));

        nxql = nxql("SELECT * FROM File WHERE ecm:tag = 'tag0'");
        assertEquals(0, session.query(nxql).size());

        nxql = nxql("SELECT * FROM File WHERE ecm:tag = 'tag1'");
        assertEquals(2, session.query(nxql).size());

        nxql = nxql("SELECT * FROM File WHERE ecm:tag = 'tag2'");
        dml = session.query(nxql);
        assertEquals(1, dml.size());
        assertEquals(file1.getId(), dml.get(0).getId());

        nxql = nxql("SELECT * FROM File WHERE ecm:tag IN ('tag1', 'tag2')");
        assertEquals(2, session.query(nxql).size());

        // unqualified name refers to the same tag
        nxql = nxql("SELECT * FROM File WHERE ecm:tag = 'tag1' AND ecm:tag = 'tag2'");
        assertEquals(0, session.query(nxql).size());

        // unqualified name refers to the same tag
        nxql = nxql("SELECT * FROM File WHERE ecm:tag = 'tag1' OR ecm:tag = 'tag2'");
        assertEquals(2, session.query(nxql).size());

        // any tag instance
        nxql = nxql("SELECT * FROM File WHERE ecm:tag/* = 'tag1'");
        assertEquals(2, session.query(nxql).size());

        // any tag instance
        nxql = nxql("SELECT * FROM File WHERE ecm:tag/* = 'tag1' AND ecm:tag/* = 'tag2'");
        dml = session.query(nxql);
        assertEquals(1, dml.size());
        assertEquals(file1.getId(), dml.get(0).getId());

        // any tag instance
        nxql = nxql("SELECT * FROM File WHERE ecm:tag/* = 'tag1' OR ecm:tag/* = 'tag2'");
        dml = session.query(nxql);
        assertEquals(2, dml.size());

        // numbered tag instance
        nxql = nxql("SELECT * FROM File WHERE ecm:tag/*1 = 'tag1'");
        assertEquals(2, session.query(nxql).size());

        // numbered tag instance are the same tag
        nxql = nxql("SELECT * FROM File WHERE ecm:tag/*1 = 'tag1' AND ecm:tag/*1 = 'tag2'");
        assertEquals(0, session.query(nxql).size());

        // different numbered tags
        nxql = nxql("SELECT * FROM File WHERE ecm:tag/*1 = 'tag1' AND ecm:tag/*2 = 'tag2'");
        assertEquals(1, session.query(nxql).size());

        // needs DISTINCT
        nxql = nxql("SELECT * FROM File WHERE ecm:tag IN ('tag1', 'tag2')");
        assertEquals(2, session.query(nxql).size());

        // needs DISTINCT
        nxql = nxql("SELECT * FROM File WHERE ecm:tag IN ('tag1', 'tag2') AND dc:title = 'testfile1_Title'");
        dml = session.query(nxql);
        assertEquals(1, dml.size());
        assertEquals(file1.getId(), dml.get(0).getId());

        // ----- queryAndFetch -----

        nxql = nxql("SELECT ecm:tag FROM File");
        res = session.queryAndFetch(nxql, NXQL.NXQL);
        // file1: tag1, tag2; file2: tag1
        assertIterableQueryResult(res, 3, "ecm:tag", "tag1", "tag2");
        res.close();

        nxql = nxql("SELECT ecm:tag FROM File WHERE ecm:tag LIKE '%1'");
        res = session.queryAndFetch(nxql, NXQL.NXQL);
        assertIterableQueryResult(res, 2, "ecm:tag", "tag1");
        res.close();

        // explicit DISTINCT
        nxql = nxql("SELECT DISTINCT ecm:tag FROM File");
        res = session.queryAndFetch(nxql, NXQL.NXQL);
        assertIterableQueryResult(res, 2, "ecm:tag", "tag1", "tag2");
        res.close();

        nxql = nxql("SELECT ecm:tag FROM File WHERE dc:title = 'testfile1_Title'");
        res = session.queryAndFetch(nxql, NXQL.NXQL);
        assertIterableQueryResult(res, 2, "ecm:tag", "tag1", "tag2");
        res.close();

        // unqualified name refers to the same tag
        nxql = nxql("SELECT ecm:tag FROM File WHERE ecm:tag = 'tag1'");
        res = session.queryAndFetch(nxql, NXQL.NXQL);
        assertIterableQueryResult(res, 2, "ecm:tag", "tag1");
        res.close();

        // unqualified name refers to the same tag
        nxql = nxql("SELECT ecm:tag FROM File WHERE ecm:tag = 'tag2'");
        res = session.queryAndFetch(nxql, NXQL.NXQL);
        assertIterableQueryResult(res, 1, "ecm:tag", "tag2");
        res.close();

        // numbered tag
        nxql = nxql("SELECT ecm:tag/*1 FROM File WHERE ecm:tag/*1 = 'tag1'");
        res = session.queryAndFetch(nxql, NXQL.NXQL);
        assertIterableQueryResult(res, 2, "ecm:tag/*1", "tag1");
        res.close();
    }

    protected static void assertIterableQueryResult(IterableQueryResult actual, int size, String prop,
            String... expected) {
        assertEquals(size, actual.size());
        Collection<String> set = new HashSet<String>();
        for (Map<String, Serializable> map : actual) {
            set.add((String) map.get(prop));
        }
        assertEquals(new HashSet<String>(Arrays.asList(expected)), set);
    }

    /**
     * Make sure that even when we use a sequence, the id is a String, for compat with the rest of the framework.
     */
    @Test
    public void testIdType() throws Exception {
        createDocs();
        IterableQueryResult res = session.queryAndFetch("SELECT ecm:uuid, ecm:parentId FROM File", NXQL.NXQL);
        assertEquals(3, res.size());
        for (Map<String, Serializable> map : res) {
            Serializable id = map.get(NXQL.ECM_UUID);
            assertTrue(id.getClass().getName(), id instanceof String);
            Serializable parentId = map.get(NXQL.ECM_PARENTID);
            assertTrue(parentId.getClass().getName(), parentId instanceof String);
        }
        res.close();
    }

    protected DocumentModel makeComplexDoc() {
        DocumentModel doc = session.createDocumentModel("/", "doc", "TestDoc");

        // tst:title = 'hello world'
        doc.setPropertyValue("tst:title", "hello world");

        // tst:subjects = ['foo', 'bar', 'moo']
        // tst:subjects/item[0] = 'foo'
        // tst:subjects/0 = 'foo'
        doc.setPropertyValue("tst:subjects", new String[] { "foo", "bar", "moo" });

        Map<String, Object> owner = new HashMap<>();
        // tst:owner/firstname = 'Bruce'
        owner.put("firstname", "Bruce");
        // tst:owner/lastname = 'Willis'
        owner.put("lastname", "Willis");
        doc.setPropertyValue("tst:owner", (Serializable) owner);

        Map<String, Object> first = new HashMap<>();
        // tst:couple/first/firstname = 'Steve'
        first.put("firstname", "Steve");
        // tst:couple/first/lastname = 'Jobs'
        first.put("lastname", "Jobs");
        Map<String, Object> second = new HashMap<>();
        // tst:couple/second/firstname = 'Steve'
        second.put("firstname", "Steve");
        // tst:couple/second/lastname = 'McQueen'
        second.put("lastname", "McQueen");
        Map<String, Object> couple = new HashMap<>();
        couple.put("first", first);
        couple.put("second", second);
        doc.setPropertyValue("tst:couple", (Serializable) couple);

        Map<String, Object> friend0 = new HashMap<>();
        // tst:friends/item[0]/firstname = 'John'
        // tst:friends/0/firstname = 'John'
        friend0.put("firstname", "John");
        // tst:friends/0/lastname = 'Lennon'
        friend0.put("lastname", "Lennon");
        Map<String, Object> friend1 = new HashMap<>();
        // tst:friends/1/firstname = 'John'
        friend1.put("firstname", "John");
        // tst:friends/1/lastname = 'Smith'
        friend1.put("lastname", "Smith");
        List<Map<String, Object>> friends = Arrays.asList(friend0, friend1);
        doc.setPropertyValue("tst:friends", (Serializable) friends);

        // this one doesn't have a schema prefix
        Map<String, Object> animal = new HashMap<>();
        // animal/race = 'dog'
        animal.put("race", "dog");
        // animal/name = 'Scooby'
        animal.put("name", "Scooby");
        doc.setPropertyValue("animal", (Serializable) animal);

        doc = session.createDocument(doc);
        session.save();
        return doc;
    }

    protected List<String> getIds(DocumentModelList list) {
        List<String> ids = new ArrayList<>(list.size());
        for (DocumentModel doc : list) {
            ids.add(doc.getId());
        }
        return ids;
    }

    protected static String FROM_WHERE = " FROM TestDoc WHERE ecm:isProxy = 0 AND ";

    protected static String SELECT_WHERE = "SELECT *" + FROM_WHERE;

    protected static String SELECT_TITLE_WHERE = "SELECT tst:title" + FROM_WHERE;

    @Test
    public void testQueryComplexWhere() throws Exception {
        DocumentModel doc = makeComplexDoc();
        String docId = doc.getId();

        String clause;
        DocumentModelList res;
        IterableQueryResult it;

        // hierarchy h
        // JOIN hierarchy h2 ON h2.parentid = h.id
        // LEFT JOIN person p ON p.id = h2.id
        // WHERE h2.name = 'tst:owner'
        // AND p.firstname = 'Bruce'
        clause = "tst:owner/firstname = 'Bruce'";
        res = session.query(SELECT_WHERE + clause);
        assertEquals(Arrays.asList(docId), getIds(res));
        it = session.queryAndFetch("SELECT tst:title, tst:owner/lastname" + FROM_WHERE + clause, "NXQL");
        assertEquals(1, it.size());
        assertEquals("Willis", it.iterator().next().get("tst:owner/lastname"));
        it.close();

        // check other operators

        clause = "tst:owner/firstname LIKE 'B%'";
        res = session.query(SELECT_WHERE + clause);
        assertEquals(Arrays.asList(docId), getIds(res));

        clause = "tst:owner/firstname IS NOT NULL";
        res = session.query(SELECT_WHERE + clause);
        assertEquals(Arrays.asList(docId), getIds(res));

        clause = "tst:owner/firstname IN ('Bruce', 'Bilbo')";
        res = session.query(SELECT_WHERE + clause);
        assertEquals(Arrays.asList(docId), getIds(res));

        // hierarchy h
        // JOIN hierarchy h2 ON h2.parentid = h.id
        // JOIN hierarchy h3 ON h3.parentid = h2.id
        // LEFT JOIN person p ON p.id = h3.id
        // WHERE h2.name = 'tst:couple'
        // AND h3.name = 'first'
        // AND p.firstname = 'Steve'
        clause = "tst:couple/first/firstname = 'Steve'";
        res = session.query(SELECT_WHERE + clause);
        assertEquals(Arrays.asList(docId), getIds(res));
        it = session.queryAndFetch("SELECT tst:title, tst:couple/first/lastname" + FROM_WHERE + clause, "NXQL");
        assertEquals(1, it.size());
        assertEquals("Jobs", it.iterator().next().get("tst:couple/first/lastname"));
        it.close();

        // hierarchy h
        // JOIN hierarchy h2 ON h2.parentid = h.id
        // LEFT JOIN person p ON p.id = h2.id
        // WHERE h2.name = 'tst:friends' AND h2.pos = 0
        // AND p.firstname = 'John'
        clause = "tst:friends/0/firstname = 'John'";
        res = session.query(SELECT_WHERE + clause);
        assertEquals(Arrays.asList(docId), getIds(res));
        it = session.queryAndFetch("SELECT tst:title, tst:friends/0/lastname" + FROM_WHERE + clause, "NXQL");
        assertEquals(1, it.size());
        assertEquals("Lennon", it.iterator().next().get("tst:friends/0/lastname"));
        it.close();

        // alternate xpath syntax
        clause = "tst:friends/item[0]/firstname = 'John'";
        res = session.query(SELECT_WHERE + clause);
        assertEquals(Arrays.asList(docId), getIds(res));

        // hierarchy h
        // JOIN hierarchy h2 ON h2.parentid = h.id
        // LEFT JOIN person p ON p.id = h2.id
        // WHERE h2.name = 'tst:friends'
        // AND p.firstname = 'John'
        clause = "tst:friends/*/firstname = 'John'";
        res = session.query(SELECT_WHERE + clause);
        assertEquals(Arrays.asList(docId), getIds(res));
        it = session.queryAndFetch(SELECT_TITLE_WHERE + clause, "NXQL");
        // MongoDB query projecting on a non-wildcard values doesn't repeat matches
        // as this would entail re-evaluating the projection from the full state
        // just to get duplicated identical rows
        assertEquals(isDBSMongoDB() ? 1 : 2, it.size()); // two uncorrelated stars
        it.close();

        // alternate xpath syntax
        clause = "tst:friends/item[*]/firstname = 'John'";
        res = session.query(SELECT_WHERE + clause);
        assertEquals(Arrays.asList(docId), getIds(res));

        // hierarchy h
        // JOIN hierarchy h2 ON h2.parentid = h.id
        // LEFT JOIN person p ON p.id = h2.id
        // WHERE h2.name = 'tst:friends'
        // AND p.firstname = 'John'
        // AND p.lastname = 'Smith'
        clause = "tst:friends/*1/firstname = 'John'" + " AND tst:friends/*1/lastname = 'Smith'";
        res = session.query(SELECT_WHERE + clause);
        assertEquals(Arrays.asList(docId), getIds(res));
        it = session.queryAndFetch("SELECT tst:title, tst:friends/*1/lastname" + FROM_WHERE + clause, "NXQL");
        assertEquals(1, it.size()); // correlated stars
        assertEquals("Smith", it.iterator().next().get("tst:friends/*1/lastname"));
        it.close();

        // alternate xpath syntax
        clause = "tst:friends/item[*1]/firstname = 'John'" + " AND tst:friends/item[*1]/lastname = 'Smith'";
        res = session.query(SELECT_WHERE + clause);
        assertEquals(Arrays.asList(docId), getIds(res));
    }

    @Test
    public void testQueryComplexPrefix() throws Exception {
        DocumentModel doc = makeComplexDoc();
        String docId = doc.getId();

        String clause;
        DocumentModelList res;

        // schema with a prefix
        clause = "tst:owner/firstname = 'Bruce'";
        res = session.query(SELECT_WHERE + clause);
        assertEquals(Arrays.asList(docId), getIds(res));

        // use of prefix is mandatory if defined
        try {
            clause = "owner/firstname = 'Bruce'";
            session.query(SELECT_WHERE + clause);
            fail("Should fail on missing prefix");
        } catch (QueryParseException e) {
            assertEquals("Failed to execute query: "
                    + "SELECT * FROM TestDoc WHERE ecm:isProxy = 0 AND owner/firstname = 'Bruce'" + ", "
                    + "No such property: owner/firstname", e.getMessage());
        }

        // schema without a prefix
        clause = "animal/race = 'dog'";
        res = session.query(SELECT_WHERE + clause);
        assertEquals(Arrays.asList(docId), getIds(res));

        // allow use with schema-name-as-prefix
        clause = "testschema3:animal/race = 'dog'";
        res = session.query(SELECT_WHERE + clause);
        assertEquals(Arrays.asList(docId), getIds(res));
    }

    @Test
    public void testQueryComplexReturned() throws Exception {
        DocumentModel doc = makeComplexDoc();
        String docId = doc.getId();

        String clause;
        DocumentModelList res;
        IterableQueryResult it;
        Set<String> set;

        // SELECT p.lastname
        // FROM hierarchy h
        // JOIN hierarchy h2 ON h2.parentid = h.id
        // LEFT JOIN person p ON p.id = h2.id
        // WHERE h2.name = 'tst:friends'
        clause = "tst:title = 'hello world'";
        res = session.query(SELECT_WHERE + clause);
        assertEquals(Arrays.asList(docId), getIds(res));
        it = session.queryAndFetch("SELECT tst:friends/*/lastname" + FROM_WHERE + clause, "NXQL");
        assertEquals(2, it.size());
        set = new HashSet<>();
        for (Map<String, Serializable> map : it) {
            set.add((String) map.get("tst:friends/*/lastname"));
        }
        assertEquals(new HashSet<>(Arrays.asList("Lennon", "Smith")), set);
        it.close();

        // SELECT p.firstname, p.lastname
        // FROM hierarchy h
        // JOIN hierarchy h2 ON h2.parentid = h.id
        // LEFT JOIN person p ON p.id = h2.id
        // WHERE h2.name = 'tst:friends'
        clause = "tst:title = 'hello world'";
        res = session.query(SELECT_WHERE + clause);
        assertEquals(Arrays.asList(docId), getIds(res));
        it = session.queryAndFetch("SELECT tst:friends/*1/firstname, tst:friends/*1/lastname" + FROM_WHERE + clause,
                "NXQL");
        assertEquals(2, it.size());
        Set<String> fn = new HashSet<>();
        Set<String> ln = new HashSet<>();
        for (Map<String, Serializable> map : it) {
            fn.add((String) map.get("tst:friends/*1/firstname"));
            ln.add((String) map.get("tst:friends/*1/lastname"));
        }
        assertEquals(Collections.singleton("John"), fn);
        assertEquals(new HashSet<>(Arrays.asList("Lennon", "Smith")), ln);
        it.close();

    }

    @Test
    public void testQueryComplexListElement() throws Exception {
        DocumentModel doc = makeComplexDoc();
        String docId = doc.getId();

        String clause;
        DocumentModelList res;
        IterableQueryResult it;
        Set<String> set;

        // hierarchy h
        // JOIN tst_subjects s ON h.id = s.id // not LEFT JOIN
        // WHERE s.pos = 0
        // AND s.item = 'foo'
        clause = "tst:subjects/0 = 'foo'";
        res = session.query(SELECT_WHERE + clause);
        assertEquals(Arrays.asList(docId), getIds(res));
        clause = "tst:subjects/0 = 'bar'";
        res = session.query(SELECT_WHERE + clause);
        assertEquals(0, res.size());

        // SELECT s.item
        // FROM hierarchy h
        // JOIN tst_subjects s ON h.id = s.id // not LEFT JOIN
        // WHERE s.pos = 0
        // AND s.item = 'bar'
        clause = "tst:subjects/0 = 'foo'";
        it = session.queryAndFetch("SELECT tst:subjects/0" + FROM_WHERE + clause, "NXQL");
        assertEquals(1, it.size());
        assertEquals("foo", it.iterator().next().get("tst:subjects/0"));
        it.close();

        // SELECT s1.item
        // FROM hierarchy h
        // JOIN tst_subjects s0 ON h.id = s0.id // not LEFT JOIN
        // JOIN tst_subjects s1 ON h.id = s1.id // not LEFT JOIN
        // WHERE s0.pos = 0 AND s1.pos = 1
        // AND s0.item LIKE 'foo%'
        clause = "tst:subjects/0 LIKE 'foo%'";
        res = session.query(SELECT_WHERE + clause);
        assertEquals(Arrays.asList(docId), getIds(res));
        it = session.queryAndFetch("SELECT tst:subjects/1" + FROM_WHERE + clause, "NXQL");
        assertEquals(1, it.size());
        assertEquals("bar", it.iterator().next().get("tst:subjects/1"));
        it.close();

        // SELECT s.item
        // FROM hierarchy h
        // LEFT JOIN tst_subjects s ON h.id = s.id
        // WHERE s.item LIKE '%oo'
        clause = "tst:subjects/*1 LIKE '%oo'";
        res = session.query(SELECT_WHERE + clause);
        assertEquals(Arrays.asList(docId), getIds(res));
        it = session.queryAndFetch("SELECT tst:subjects/*1" + FROM_WHERE + clause, "NXQL");
        assertEquals(2, it.size());
        set = new HashSet<>();
        for (Map<String, Serializable> map : it) {
            set.add((String) map.get("tst:subjects/*1"));
        }
        assertEquals(new HashSet<>(Arrays.asList("foo", "moo")), set);
        it.close();

        clause = "tst:subjects/* LIKE '%oo'";
        res = session.query(SELECT_WHERE + clause);
        assertEquals(Arrays.asList(docId), getIds(res));
        it = session.queryAndFetch("SELECT tst:subjects/*" + FROM_WHERE + clause, "NXQL");
        // two uncorrelated stars, resulting in a cross join
        assertEquals(6, it.size());
        set = new HashSet<>();
        for (Map<String, Serializable> map : it) {
            set.add((String) map.get("tst:subjects/*"));
        }
        assertEquals(new HashSet<>(Arrays.asList("foo", "moo", "bar")), set);
        it.close();

        // WHAT
        clause = "tst:title = 'hello world'";
        it = session.queryAndFetch("SELECT tst:subjects/*" + FROM_WHERE + clause, "NXQL");
        assertEquals(3, it.size());
        set = new HashSet<>();
        for (Map<String, Serializable> map : it) {
            set.add((String) map.get("tst:subjects/*"));
        }
        assertEquals(new HashSet<>(Arrays.asList("foo", "bar", "moo")), set);
        it.close();
    }

    @Test
    public void testQueryComplexOrderBy() throws Exception {
        DocumentModel doc = makeComplexDoc();
        String docId = doc.getId();

        String clause;
        DocumentModelList res;
        IterableQueryResult it;

        clause = "tst:title LIKE '%' ORDER BY tst:owner/firstname";
        res = session.query(SELECT_WHERE + clause);
        assertEquals(Arrays.asList(docId), getIds(res));

        clause = "tst:owner/firstname = 'Bruce' ORDER BY tst:title";
        res = session.query(SELECT_WHERE + clause);
        assertEquals(Arrays.asList(docId), getIds(res));

        clause = "tst:owner/firstname = 'Bruce' ORDER BY tst:owner/firstname";
        res = session.query(SELECT_WHERE + clause);
        assertEquals(Arrays.asList(docId), getIds(res));

        // this produces a DISTINCT and adds tst:title to the select list
        clause = "tst:subjects/* = 'foo' ORDER BY tst:title";
        res = session.query(SELECT_WHERE + clause);
        assertEquals(Arrays.asList(docId), getIds(res));

        clause = "tst:friends/*/firstname = 'John' ORDER BY tst:title";
        res = session.query(SELECT_WHERE + clause);
        assertEquals(Arrays.asList(docId), getIds(res));

        // no wildcard index so no DISTINCT needed
        clause = "tst:title LIKE '%' ORDER BY tst:friends/0/lastname";
        res = session.query(SELECT_WHERE + clause);
        assertEquals(Arrays.asList(docId), getIds(res));
        clause = "tst:title LIKE '%' ORDER BY tst:subjects/0";
        res = session.query(SELECT_WHERE + clause);
        assertEquals(Arrays.asList(docId), getIds(res));

        // SELECT * statement cannot ORDER BY array or complex list element
        clause = "tst:subjects/*1 = 'foo' ORDER BY tst:subjects/*1";
        try {
            session.query(SELECT_WHERE + clause);
            if (!isDBS()) {
                // ORDER BY tst:subjects works on MongoDB
                fail();
            }
        } catch (QueryParseException e) {
            String expected = "Failed to execute query: "
                    + "SELECT * FROM TestDoc WHERE ecm:isProxy = 0 AND tst:subjects/*1 = 'foo' ORDER BY tst:subjects/*1"
                    + ", " + "For SELECT * the ORDER BY columns cannot use wildcard indexes";
            assertEquals(expected, e.getMessage());
        }

        clause = "tst:title = 'hello world' ORDER BY tst:subjects/*1";
        it = session.queryAndFetch("SELECT tst:title" + FROM_WHERE + clause, "NXQL");
        // MongoDB/MarkLogic query projecting on a non-wildcard values doesn't repeat matches
        // as this would entail re-evaluating the projection from the full state
        // just to get duplicated identical rows
        assertEquals(isDBSMongoDB() || isDBSMarkLogic() ? 1 : 3, it.size());
        it.close();
    }

    @Test
    public void testQueryComplexTwoWildcards() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "doc", "File2");
        Map<String, Serializable> map1 = new HashMap<>();
        map1.put("name", "bob");
        map1.put("subscribers", new String[] { "sub1", "sub2" });
        Map<String, Serializable> map2 = new HashMap<>();
        map2.put("name", "pete");
        map2.put("subscribers", new String[] { "sub1" });
        doc.setPropertyValue("tst2:notifs", (Serializable) Arrays.asList(map1, map2));
        doc = session.createDocument(doc);
        session.save();

        String docId = doc.getId();

        String query = "SELECT * FROM File2 WHERE ecm:isProxy = 0 AND tst2:notifs/*/subscribers/* = 'sub1'";
        DocumentModelList res = session.query(query);
        assertEquals(Arrays.asList(docId), getIds(res));
    }

    @Test
    public void testQueryComplexBoolean() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "doc", "File2");
        Map<String, Serializable> map1 = new HashMap<>();
        map1.put("name", "bob");
        map1.put("enabled", Long.valueOf(1));
        map1.put("subscribers", new String[] { "sub1", "sub2" });
        doc.setPropertyValue("tst2:notifs", (Serializable) Collections.singletonList(map1));
        doc = session.createDocument(doc);
        session.save();

        String docId = doc.getId();

        String query = "SELECT * FROM File2 WHERE ecm:isProxy = 0 AND tst2:notifs/*/enabled = 1";
        DocumentModelList res = session.query(query);
        assertEquals(Arrays.asList(docId), getIds(res));
    }

    @Test
    public void testQueryDistinct() throws Exception {
        assumeTrue("DBS does not support DISTINCT in queries", supportsDistinct());

        makeComplexDoc();

        String clause;
        IterableQueryResult it;
        List<String> list;

        // same with DISTINCT, cannot work
        clause = "tst:title = 'hello world' ORDER BY tst:subjects/*1";
        try {
            session.queryAndFetch("SELECT DISTINCT tst:title" + FROM_WHERE + clause, "NXQL");
            fail();
        } catch (QueryParseException e) {
            String expected = "Failed to execute query: "
                    + "NXQL: SELECT DISTINCT tst:title FROM TestDoc WHERE ecm:isProxy = 0 AND tst:title = 'hello world' ORDER BY tst:subjects/*1"
                    + ", "
                    + "For SELECT DISTINCT the ORDER BY columns must be in the SELECT list, missing: [tst:subjects/*1]";
            assertEquals(expected, e.getMessage());
        }

        // ok if ORDER BY column added to SELECT columns
        it = session.queryAndFetch("SELECT DISTINCT tst:title, tst:subjects/*1" + FROM_WHERE + clause, "NXQL",
                QueryFilter.EMPTY);
        assertEquals(3, it.size());
        it.close();

        clause = "tst:title = 'hello world' ORDER BY tst:subjects/*1";
        it = session.queryAndFetch("SELECT tst:subjects/*1" + FROM_WHERE + clause, "NXQL");
        assertEquals(3, it.size());
        list = new LinkedList<>();
        for (Map<String, Serializable> map : it) {
            list.add((String) map.get("tst:subjects/*1"));
        }
        assertEquals(Arrays.asList("bar", "foo", "moo"), list);
        it.close();

        clause = "tst:title = 'hello world' ORDER BY tst:subjects/*1";
        it = session.queryAndFetch("SELECT DISTINCT tst:subjects/*1" + FROM_WHERE + clause, "NXQL");
        assertEquals(3, it.size());
        it.close();
    }

    @Test
    public void testQueryComplexOrderByProxies() throws Exception {
        DocumentModel doc = makeComplexDoc();
        String docId = doc.getId();

        String clause;
        DocumentModelList res;

        clause = "tst:friends/*/firstname = 'John' ORDER BY tst:title";
        res = session.query("SELECT * FROM TestDoc WHERE " + clause);
        assertEquals(Arrays.asList(docId), getIds(res));
    }

    @Test
    public void testQueryComplexOr() throws Exception {
        // doc1 tst:title = 'hello world'
        DocumentModel doc1 = session.createDocumentModel("/", "doc1", "TestDoc");
        doc1.setPropertyValue("tst:title", "hello world");
        doc1 = session.createDocument(doc1);

        // doc2 tst:owner/firstname = 'Bruce'
        DocumentModel doc2 = session.createDocumentModel("/", "doc2", "TestDoc");
        doc2.setPropertyValue("tst:owner", (Serializable) Collections.singletonMap("firstname", "Bruce"));
        doc2 = session.createDocument(doc2);

        // doc3 tst:friends/0/firstname = 'John'
        DocumentModel doc3 = session.createDocumentModel("/", "doc3", "TestDoc");
        doc3.setPropertyValue("tst:friends",
                (Serializable) Arrays.asList(Collections.singletonMap("firstname", "John")));
        doc3 = session.createDocument(doc3);

        // doc4 tst:subjects/0 = 'foo'
        DocumentModel doc4 = session.createDocumentModel("/", "doc4", "TestDoc");
        doc4.setPropertyValue("tst:subjects", new String[] { "foo" });
        doc4 = session.createDocument(doc4);

        session.save();

        String s1 = "SELECT * FROM TestDoc WHERE ecm:isProxy = 0 AND (";
        String s2 = ")";
        String o = " OR ";
        String c1 = "tst:title = 'hello world'";
        String c2 = "tst:owner/firstname = 'Bruce'";
        String c3 = "tst:friends/0/firstname = 'John'";
        String c4 = "tst:subjects/0 = 'foo'";
        DocumentModelList res;

        res = session.query(s1 + c1 + s2);
        assertEquals(Arrays.asList(doc1.getId()), getIds(res));

        res = session.query(s1 + c2 + s2);
        assertEquals(Arrays.asList(doc2.getId()), getIds(res));

        res = session.query(s1 + c3 + s2);
        assertEquals(Arrays.asList(doc3.getId()), getIds(res));

        res = session.query(s1 + c4 + s2);
        assertEquals(Arrays.asList(doc4.getId()), getIds(res));

        res = session.query(s1 + c1 + o + c2 + s2);
        assertEquals(2, res.size());

        res = session.query(s1 + c1 + o + c3 + s2);
        assertEquals(2, res.size());

        res = session.query(s1 + c1 + o + c4 + s2);
        assertEquals(2, res.size());

        res = session.query(s1 + c2 + o + c3 + s2);
        assertEquals(2, res.size());

        res = session.query(s1 + c2 + o + c4 + s2);
        assertEquals(2, res.size());

        res = session.query(s1 + c3 + o + c4 + s2);
        assertEquals(2, res.size());

        res = session.query(s1 + c1 + o + c2 + o + c3 + s2);
        assertEquals(3, res.size());

        res = session.query(s1 + c1 + o + c2 + o + c4 + s2);
        assertEquals(3, res.size());

        res = session.query(s1 + c1 + o + c3 + o + c4 + s2);
        assertEquals(3, res.size());

        res = session.query(s1 + c2 + o + c3 + o + c4 + s2);
        assertEquals(3, res.size());

        res = session.query(s1 + c1 + o + c2 + o + c3 + o + c4 + s2);
        assertEquals(4, res.size());
    }

    @Test
    public void testQueryLikeWildcard() throws Exception {
        DocumentModelList dml;
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        doc.setPropertyValue("dc:title", "foo");
        doc.setPropertyValue("dc:description", "fo%");
        doc.setPropertyValue("dc:rights", "fo_");
        doc.setPropertyValue("dc:source", "fo\\");
        doc = session.createDocument(doc);
        session.save();

        // regular % wildcard
        dml = session.query("SELECT * FROM File WHERE dc:title LIKE 'f%'");
        assertEquals(1, dml.size());
        // regular _ wildcard
        dml = session.query("SELECT * FROM File WHERE dc:title LIKE 'fo_'");
        assertEquals(1, dml.size());

        // escaped % wildcard
        dml = session.query("SELECT * FROM File WHERE dc:title LIKE 'fo\\%'");
        assertEquals(0, dml.size());
        dml = session.query("SELECT * FROM File WHERE dc:description LIKE 'fo\\%'");
        assertEquals(1, dml.size());

        // escaped _ wildcard
        dml = session.query("SELECT * FROM File WHERE dc:title LIKE 'fo\\_'");
        assertEquals(0, dml.size());
        dml = session.query("SELECT * FROM File WHERE dc:rights LIKE 'fo\\_'");
        assertEquals(1, dml.size());

        // explicit \
        // because \ is already an escape char in NXQL strings, we have to double-double it
        // doubled for NXQL string escaping, and doubled for LIKE escaping
        dml = session.query("SELECT * FROM File WHERE dc:title LIKE 'fo\\\\\\\\'");
        assertEquals(0, dml.size());
        dml = session.query("SELECT * FROM File WHERE dc:source LIKE 'fo\\\\\\\\'");
        assertEquals(1, dml.size());
    }

    @Test
    public void testQueryIdNotFromUuid() throws Exception {
        DocumentModel doc1 = new DocumentModelImpl("/", "doc1", "File");
        doc1 = session.createDocument(doc1);

        DocumentModel doc2 = new DocumentModelImpl("/", "doc2", "File");
        doc2.setPropertyValue("dc:source", doc1.getId());
        doc2 = session.createDocument(doc2);
        session.save();

        DocumentModelList dml = session.query("SELECT dc:source FROM File WHERE ecm:name = 'doc2'");
        assertEquals(1, dml.size());
        DocumentModel doc = dml.get(0);
        assertEquals(doc1.getId(), doc.getId());
    }

    @Test
    public void testQueryIdListNotFromUuid() throws Exception {
        DocumentModel doc1 = new DocumentModelImpl("/", "doc1", "File");
        doc1 = session.createDocument(doc1);
        DocumentModel doc2 = new DocumentModelImpl("/", "doc2", "File");
        doc2 = session.createDocument(doc2);
        DocumentModel doc3 = new DocumentModelImpl("/", "doc3", "File");
        doc3 = session.createDocument(doc3);

        // test both orders
        for (Pair<DocumentModel, DocumentModel> pair : Arrays.asList(Pair.of(doc1, doc2), Pair.of(doc2, doc1))) {
            DocumentModel doca = pair.getLeft();
            DocumentModel docb = pair.getRight();
            String[] prop = new String[] { "not-a-valid-id", doca.getId(), doca.getId(), docb.getId() };
            List<String> expected = Arrays.asList(prop[1], prop[2], prop[3]);
            doc3.setPropertyValue("dc:subjects", prop);
            doc3 = session.saveDocument(doc3);
            session.save();

            DocumentModelList dml;
            List<String> actual;
            String query = "SELECT dc:subjects/* FROM File WHERE ecm:name = 'doc3'";

            // expect a specific order (NXP-19484)
            // test with proxies
            dml = session.query(query);
            assertEquals(3, dml.size());
            actual = Arrays.asList(dml.get(0).getId(), dml.get(1).getId(), dml.get(2).getId());
            assertEquals(expected, actual);

            // same without proxies
            dml = session.query(query + " AND ecm:isProxy = 0");
            assertEquals(3, dml.size());
            actual = Arrays.asList(dml.get(0).getId(), dml.get(1).getId(), dml.get(2).getId());
            assertEquals(expected, actual);
        }
    }

    @Test
    public void testScrollApi() throws Exception {
        final int nbDocs = 127;
        final int batchSize = 13;
        DocumentModel doc;
        for (int i=0; i<nbDocs; i++) {
            doc = new DocumentModelImpl("/", "doc1", "File");
            session.createDocument(doc);
        }
        session.save();
        DocumentModelList dml;
        dml = session.query("SELECT * FROM Document");
        assertEquals(nbDocs, dml.size());

        ScrollResult ret = session.scroll("SELECT * FROM Document", batchSize, 10);
        int total = 0;
        while (ret.hasResults()) {
            List<String> ids = ret.getResultIds();
            ids.stream().forEach(id -> assertFalse(id.isEmpty()));
            total += ids.size();
            ret = session.scroll(ret.getScrollId());
        }
        assertEquals(nbDocs, total);

        // the scroll id is now closed
        exception.expect(NuxeoException.class);
        exception.expectMessage("Unknown or timed out scrollId");
        ret = session.scroll(ret.getScrollId());
        assertFalse(ret.hasResults());
    }

    @Test
    @LogCaptureFeature.FilterOn(logLevel = "WARN")
    public void testScrollApiEmtpy() throws Exception {
        // do a scroll that return nothing
        ScrollResult ret = session.scroll("SELECT * FROM File", 10, 1);
        assertFalse(ret.hasResults());
        // wait for the scroll timeout
        Thread.sleep(1100);
        // A new scroll call will warn about timed out scroll
        ret = session.scroll("SELECT * FROM File", 10, 1);
        assertFalse(ret.hasResults());
        // we expect to have no warn because empty scroll should not maintain any cursor
        List<LoggingEvent> events = logCaptureResult.getCaughtEvents();
        assertTrue(events.isEmpty());
    }

    @Test
    public void testScrollApiRequiresAdminRights() throws Exception {
        ScrollResult ret = session.scroll("SELECT * FROM Document", 3, 1);
        assertFalse(ret.hasResults());

        try (CoreSession bobSession = CoreInstance.openCoreSession(session.getRepositoryName(), "bob")) {
            exception.expect(NuxeoException.class);
            exception.expectMessage("Only Administrators can scroll");
            // raise an illegal access
            ret = bobSession.scroll("SELECT * FROM Document", 3, 1);
            assertFalse(ret.hasResults());
        }
    }

    @Test
    public void testScrollApiRequiresAdminRightsBis() throws Exception {
        assumeTrue("Backend must support true scrolling", supportsScroll());

        DocumentModel doc1 = new DocumentModelImpl("/", "doc1", "File");
        session.createDocument(doc1);
        DocumentModel doc2 = new DocumentModelImpl("/", "doc2", "File");
        session.createDocument(doc2);
        session.save();

        ScrollResult ret = session.scroll("SELECT * FROM Document", 1, 10);
        assertTrue(ret.hasResults());
        assertEquals(1, ret.getResultIds().size());

        try (CoreSession bobSession = CoreInstance.openCoreSession(session.getRepositoryName(), "bob")) {
            exception.expect(NuxeoException.class);
            exception.expectMessage("Only Administrators can scroll");
            // raise an illegal access
            ret = bobSession.scroll(ret.getScrollId());
            assertFalse(ret.hasResults());
        }
    }

    @Test
    public void testScrollTimeout() throws Exception {
        assumeTrue("Backend must support true scrolling", supportsScroll());

        DocumentModel doc1 = new DocumentModelImpl("/", "doc1", "File");
        session.createDocument(doc1);
        DocumentModel doc2 = new DocumentModelImpl("/", "doc2", "File");
        session.createDocument(doc2);
        session.save();

        ScrollResult ret = session.scroll("SELECT * FROM Document", 1, 1);
        assertTrue(ret.hasResults());
        assertEquals(1, ret.getResultIds().size());
        // wait for scroll timeout
        Thread.sleep(1100);

        exception.expect(NuxeoException.class);
        exception.expectMessage("Timed out scrollId");
        ret = session.scroll(ret.getScrollId());
        assertFalse(ret.hasResults());
    }

    @Test
    public void testScrollBadUsageInvalidScrollId() throws Exception {
        exception.expect(NuxeoException.class);
        exception.expectMessage("Unknown or timed out scrollId");
        ScrollResult ret = session.scroll("foo");
        assertFalse(ret.hasResults());
    }

    @Test
    public void testScrollBadUsage() throws Exception {
        assumeTrue("Backend must support true scrolling", supportsScroll());

        DocumentModel doc1 = new DocumentModelImpl("/", "doc1", "File");
        session.createDocument(doc1);
        DocumentModel doc2 = new DocumentModelImpl("/", "doc2", "File");
        session.createDocument(doc2);
        session.save();

        ScrollResult ret1 = session.scroll("SELECT * FROM Document", 1, 1);
        ScrollResult ret2 = session.scroll("SELECT * FROM Document", 1, 1);
        ScrollResult ret3 = session.scroll("SELECT * FROM Document", 1, 1);
        assertTrue(ret1.hasResults());
        assertEquals(1, ret1.getResultIds().size());

        Thread.sleep(1100);
        // normal timeout on ret1
        try {
            session.scroll(ret1.getScrollId());
        } catch (NuxeoException e) {
            assertEquals("Timed out scrollId", e.getMessage());
        }
        // This new call will clean leaked scroll
        ScrollResult ret4 = session.scroll("SELECT * FROM Document", 1, 1);
        assertTrue(ret4.hasResults());

        // ret2 is now unknown because it has been cleaned
        exception.expect(NuxeoException.class);
        exception.expectMessage("Unknown or timed out scrollId");
        session.scroll(ret2.getScrollId());
    }

    @Test
    public void testScrollApiConcurrency() throws Exception {
        final int nbDocs = 127;
        final int batchSize = 13;
        final int nbThread = nbDocs/batchSize + 1;
        // System.out.println("nbDocs: " + nbDocs + ", batch: " + batchSize + ", thread: " + nbThread);
        DocumentModel doc;
        for (int i=0; i<nbDocs; i++) {
            doc = new DocumentModelImpl("/", "doc1", "File");
            session.createDocument(doc);
        }
        session.save();

        DocumentModelList dml;
        dml = session.query("SELECT * FROM Document");
        assertEquals(nbDocs, dml.size());

        ScrollResult ret = session.scroll("SELECT * FROM Document", batchSize, 10);
        List<String> ids = ret.getResultIds();
        int total = ids.size();
        String scrollId = ret.getScrollId();
        // System.out.println("first call: " + total);
        List<CompletableFuture<Integer>> futures = new ArrayList<>(nbThread);
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(nbThread);
        final CountDownLatch latch = new CountDownLatch(nbThread);
        for (int n=0; n < nbThread; n++) {
            CompletableFuture completableFuture = CompletableFuture.supplyAsync(() -> {
                TransactionHelper.startTransaction();
                try {
                    // make sure all threads ask to scroll at the same time
                    latch.countDown();
                    try {
                        latch.await();
                    } catch (InterruptedException e) {
                        ExceptionUtils.checkInterrupt(e);
                    }
                    int nb = session.scroll(scrollId).getResultIds().size();
                    // System.out.println(Thread.currentThread().getName() + ": return: " + nb);
                    return nb;
                } finally {
                    TransactionHelper.commitOrRollbackTransaction();
                }
            }, executor);
            futures.add(completableFuture);
        }
        for (int n=0; n < nbThread; n++) {
            int count = futures.get(n).get();
            total += count;
        }
        assertEquals(nbDocs, total);
    }

    @Test
    public void testScrollCleaningConcurrency() throws Exception {
        final int NB_TRHEADS = 15;
        final int NB_SCROLLS = 100;

        assumeTrue("Backend must support true scrolling", supportsScroll());

        DocumentModel doc = new DocumentModelImpl("/", "doc1", "File");
        session.createDocument(doc);
        doc = new DocumentModelImpl("/", "doc2", "File");
        session.createDocument(doc);
        session.save();
        ScrollResult ret;
        for (int i = 0; i < NB_SCROLLS; i++) {
            session.scroll("SELECT * FROM Document", 1, 1).getScrollId();
        }
        // wait for timeout
        Thread.sleep(1100);

        List<CompletableFuture<Integer>> futures = new ArrayList<>(NB_TRHEADS);
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(NB_TRHEADS);
        final CountDownLatch latch = new CountDownLatch(NB_TRHEADS);
        for (int n=0; n < NB_TRHEADS; n++) {

            CompletableFuture completableFuture = CompletableFuture.supplyAsync(() -> {
                TransactionHelper.startTransaction();
                try {
                    // make sure all threads ask to scroll at the same time
                    latch.countDown();
                    try {
                        latch.await();
                    } catch (InterruptedException e) {
                        ExceptionUtils.checkInterrupt(e);
                    }
                    session.scroll("SELECT * FROM Document", 1, 1).getResultIds().size();
                    return 1;
                } finally {
                    TransactionHelper.commitOrRollbackTransaction();
                }
            }, executor);
            futures.add(completableFuture);
        }
        int total = 0;
        for (int n=0; n < NB_TRHEADS; n++) {
            int count = futures.get(n).get();
            total += count;
        }
        assertEquals(NB_TRHEADS, total);
    }

}

