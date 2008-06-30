/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.cache.server;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.cache.CacheServiceException;

/**
 * Unit tests for a single local cache. Tests involve usage of NX core objects
 * like DataModel and DocumentModel.
 * Also structures of DocumentModel like List are tested
 *
 * @author DM
 */
public class TestSimpleServerCache extends TestServerCacheBase {

    private static final Log log = LogFactory.getLog(TestSimpleServerCache.class);

    protected static final String DOC_TYPE_FILE = "File";
    protected static final String DOC_TYPE_FOLDER = "Folder";

    protected class SCHEMA_FILE {
        static final String name = "file";

        static final String attr_filename = "filename";

        static final String attr_content = "content";
    }

    protected class SCHEMA_COMMON {
        static final String name = "common";

        static final String attr_title = "title";

        static final String attr_description = "description";
    }

    private CoreInstance server;

    private CoreSession coreSession;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        server = CoreInstance.getInstance();
        assertNotNull(server);

        Map<String, Serializable> ctx = new HashMap<String, Serializable>();
        ctx.put("username", SecurityConstants.ADMINISTRATOR);
        coreSession = server.open("demo", ctx);
        assertNotNull(coreSession);

        startCacheService();

        log.info("======================================================= Testing...");
        log.info("");
    }

    @Override
    protected void tearDown() throws Exception {
        log.info("");
        log.info("======================================================= End Testing...");
        // close the core session
        server.close(coreSession);

        super.tearDown();
    }

    /**
     * Creates and put onto the cache 2 simple DocumentModel objects.
     * Compare with retrieved references from cache.
     *
     * @throws ClientException
     * @throws CacheServiceException
     */
    public void testLocalCacheWithDummyDocModels() throws ClientException,
            CacheServiceException {

        final DocumentModel root = coreSession.getRootDocument();

        // create a folder
        final DocumentModel dm1 = new DocumentModelImpl(root.getPathAsString(),
                "folder", "Folder");
        coreSession.createDocument(dm1);
        final String dmPath1 = dm1.getPathAsString();

        // create a file
        final DocumentModel dm2 = new DocumentModelImpl(root.getPathAsString(),
                "file", "File");
        coreSession.createDocument(dm2);
        final String dmPath2 = dm2.getPathAsString();

        // set business object onto cache
        log.info("set DocumentModel object onto cache");
        cache.putObject(dmPath1, dm1);
        log.info("compare local DocumentModel with it's reference from cache");
        assertEquals(dm1, cache.getObject(dmPath1));

        cache.putObject(dmPath2, dm2);
        assertEquals(dm2, cache.getObject(dmPath2));
    }

    /**
     * Creates and put onto the cache 2 simple DocumentModel objects.
     * Perform changes onto reference objects.
     * Compares initial objects with retrieved references from cache.
     *
     * @throws ClientException
     * @throws CacheServiceException
     */
    public void testLocalCacheWithSimpleDocModels() throws ClientException,
            CacheServiceException {

        final DocumentModel root = coreSession.getRootDocument();

        // create dm1
        DocumentModel dm1 = new DocumentModelImpl(root.getPathAsString(),
                "a file", DOC_TYPE_FILE);
        dm1 = coreSession.createDocument(dm1);

        // create dm2
        DocumentModel dm2 = new DocumentModelImpl(root.getPathAsString(),
                "other file", DOC_TYPE_FILE);
        dm2 = coreSession.createDocument(dm2);

        final String dmPath1 = dm1.getPathAsString();
        final String dmPath2 = dm2.getPathAsString();

        // set business object onto cache
        cache.putObject(dmPath1, dm1);
        cache.putObject(dmPath2, dm2);

        final String val1 = "second file name";
        final String val2 = "second content";

        dm1.setProperty(SCHEMA_FILE.name, SCHEMA_FILE.attr_filename, val1);
        dm2.setProperty(SCHEMA_FILE.name, SCHEMA_FILE.attr_content, val2);

        // TODO dm1 = client.getDocument(dm1.getRef());

        assertEquals(val1, dm1.getProperty("file", SCHEMA_FILE.attr_filename));
        assertEquals(val2, dm2.getProperty(SCHEMA_FILE.name,
                SCHEMA_FILE.attr_content));

        log.info("compare local DocumentModel with it's reference from cache");
        assertEquals(dm1, cache.getObject(dmPath1));
        assertEquals(dm2, cache.getObject(dmPath2));
    }

    /**
     * Tests local Cache with lists of DocumentModel objects.
     *
     * @throws ClientException
     * @throws SecurityException
     * @throws CacheServiceException
     */
    public void testLocalCacheWithDocModelList() throws ClientException,
            CacheServiceException {
        final DocumentModel root = coreSession.getRootDocument();

        final List<DocumentModel> docList = new ArrayList<DocumentModel>();
        for (int i = 0; i < 10; i++) {
            DocumentModel model = new DocumentModelImpl(root.getPathAsString(),
                    "Test Document " + i, DOC_TYPE_FILE);
            model = coreSession.createDocument(model);
            docList.add(model);
        }

        final String fqn = "test/list";
        cache.putObject(fqn, docList);

        final List<DocumentModel> docListCached = (List<DocumentModel>) cache
                .getObject(fqn);

        int i = 0;
        for (DocumentModel model : docListCached) {
            assertEquals(docList.get(i), model);
            i++;
        }
    }

    /**
     * Creates a hierarchy of DocumentModels and check with their cached copies.
     *
     * @throws ClientException
     * @throws SecurityException
     * @throws CacheServiceException
     *
     */
    public void testLocalCacheWithDocModelHierarchy() throws ClientException,
            CacheServiceException {
        final DocumentModel root = coreSession.getRootDocument();

        // create dm1
        DocumentModel dm1 = new DocumentModelImpl(root.getPathAsString(),
                "a file 2", DOC_TYPE_FOLDER);
        dm1 = coreSession.createDocument(dm1);

        // create dm2 as a child of dm1
        DocumentModel dm2 = new DocumentModelImpl(dm1.getPathAsString(),
                "other file 2", DOC_TYPE_FILE);
        dm2 = coreSession.createDocument(dm2);

        final String dmPath1 = dm1.getRef().reference().toString();
        cache.putObject(dmPath1, dm1);
        final String dmPath2 = dm2.getRef().reference().toString();
        cache.putObject(dmPath2, dm2);

        // get object from the cache
        DocumentModel dm2Cache = (DocumentModel) cache.getObject(dmPath2);
        final String parentKey = dm2Cache.getParentRef().reference().toString();

        DocumentModel dm1Cache = (DocumentModel) cache.getObject(parentKey);
        assertEquals(dm1, dm1Cache);

    }

    public void testListenerObjPut() throws ClientException, CacheServiceException {
        // prepare cache listener
        final ReportingCacheListener cacheListener = new ReportingCacheListener();
        cache.addCacheListener(cacheListener);

        final DocumentModel root = coreSession.getRootDocument();

        // create dm1
        DocumentModel dm1 = new DocumentModelImpl(root.getPathAsString(),
                "File_1", DOC_TYPE_FOLDER);
        dm1 = coreSession.createDocument(dm1);

        final String dmPath1 = dm1.getRef().reference().toString();
        cache.putObject(dmPath1, dm1);

        assertEquals(ReportingCacheListener.EVT_DOCUMENT_UPDATE, cacheListener.getAndConsumeLastEvent());
    }

    public void testListenerObjModify() throws ClientException, CacheServiceException {
        // prepare cache listener
        final ReportingCacheListener cacheListener = new ReportingCacheListener();
        cache.addCacheListener(cacheListener);

        final DocumentModel root = coreSession.getRootDocument();

        // create dm1
        DocumentModel dm1 = new DocumentModelImpl(root.getPathAsString(),
                "a file 2", DOC_TYPE_FOLDER);
        dm1 = coreSession.createDocument(dm1);


        final String dmPath1 = dm1.getRef().reference().toString();
        cache.putObject(dmPath1, dm1);

        assertEquals(ReportingCacheListener.EVT_DOCUMENT_UPDATE, cacheListener.getAndConsumeLastEvent());

        // change object
        dm1.setProperty("common", "title", "title_2");

        assertEquals(ReportingCacheListener.EVT_DOCUMENT_UPDATE, cacheListener.getAndConsumeLastEvent());
    }

    public void testListenerObjDelete() throws ClientException, CacheServiceException {
        // prepare cache listener
        final ReportingCacheListener cacheListener = new ReportingCacheListener();
        cache.addCacheListener(cacheListener);

        final DocumentModel root = coreSession.getRootDocument();

        // create dm1
        DocumentModel dm1 = new DocumentModelImpl(root.getPathAsString(),
                "a file 2", DOC_TYPE_FOLDER);
        dm1 = coreSession.createDocument(dm1);


        final String dmPath1 = dm1.getRef().reference().toString();
        cache.putObject(dmPath1, dm1);

        assertEquals(ReportingCacheListener.EVT_DOCUMENT_UPDATE, cacheListener.getAndConsumeLastEvent());

        // change object
        cache.removeObject(dmPath1);

        assertEquals(ReportingCacheListener.EVT_DOCUMENT_REMOVED, cacheListener.getAndConsumeLastEvent());
    }

    public void testListenerEventSequence1() throws ClientException, CacheServiceException {
        // prepare cache listener
        final ReportingCacheListener cacheListener = new ReportingCacheListener();
        cache.addCacheListener(cacheListener);

        final DocumentModel root = coreSession.getRootDocument();

        // create dm1
        DocumentModel dm1 = new DocumentModelImpl(root.getPathAsString(),
                "a file 2", DOC_TYPE_FOLDER);
        dm1 = coreSession.createDocument(dm1);


        final String dmPath1 = dm1.getRef().reference().toString();
        cache.putObject(dmPath1, dm1);

        List<String> expected = new ArrayList<String>();
        expected.add(ReportingCacheListener.EVT_DOCUMENT_UPDATE);
        assertEqualLists(expected, cacheListener.getReceivedEvents());

        // change object
        cache.removeObject(dmPath1);

        expected.add(ReportingCacheListener.EVT_DOCUMENT_REMOVED);
        assertEqualLists(expected, cacheListener.getReceivedEvents());
    }

    private static void assertEqualLists(List<String> expected, List<String> actual) {
        int actualSize = actual.size();
        int i = 0;
        for (String exp : expected) {
            if (actualSize > i) {
                assertEquals("element #" + i, exp, actual.get(i));
                i++;
            } else {
                fail("expected elements #: " + expected.size() + "< found #: " + i);
            }
        }

        if (actualSize > i) {
            fail("expected elements #: " + expected.size() + "< found #: " + actualSize);
        }
    }
}
