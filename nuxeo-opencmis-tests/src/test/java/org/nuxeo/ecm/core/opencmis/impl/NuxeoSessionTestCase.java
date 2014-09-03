/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ItemIterable;
import org.apache.chemistry.opencmis.client.api.ObjectId;
import org.apache.chemistry.opencmis.client.api.OperationContext;
import org.apache.chemistry.opencmis.client.api.Policy;
import org.apache.chemistry.opencmis.client.api.Property;
import org.apache.chemistry.opencmis.client.api.QueryResult;
import org.apache.chemistry.opencmis.client.api.Relationship;
import org.apache.chemistry.opencmis.client.api.Rendition;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.Ace;
import org.apache.chemistry.opencmis.commons.data.AllowableActions;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.RenditionData;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.enums.Action;
import org.apache.chemistry.opencmis.commons.enums.RelationshipDirection;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.exceptions.CmisConstraintException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisInvalidArgumentException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.chemistry.opencmis.commons.impl.Base64;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ContentStreamImpl;
import org.apache.commons.httpclient.util.DateUtil;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.opencmis.impl.client.NuxeoSession;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoRepositories;
import org.nuxeo.ecm.core.opencmis.tests.Helper;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;

/**
 * Tests that hit the high-level Session abstraction.
 */
public abstract class NuxeoSessionTestCase extends SQLRepositoryTestCase {

    public static final String BASE_RESOURCE = "jetty-test";

    public static final String NUXEO_ROOT_TYPE = "Root"; // from Nuxeo

    public static final String NUXEO_ROOT_NAME = ""; // NuxeoPropertyDataName;

    public static final String USERNAME = "Administrator";

    public static final String PASSWORD = "test";

    // stream content with non-ASCII characters
    public static final String STREAM_CONTENT = "Caf\u00e9 Diem\none\0two";

    public static final String NOT_NULL = "CONSTRAINT_NOT_NULL";

    protected Session session;

    protected String rootFolderId;

    protected boolean isAtomPub;

    protected boolean isBrowser;

    protected Map<String, String> repoDetails;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        // deployed for fulltext indexing
        deployBundle("org.nuxeo.ecm.core.convert.api");
        deployBundle("org.nuxeo.ecm.core.convert");
        deployBundle("org.nuxeo.ecm.core.convert.plugins");
        // MyDocType
        deployBundle("org.nuxeo.ecm.core.opencmis.tests");
        // MIME Type Icon Updater for renditions
        deployBundle("org.nuxeo.ecm.platform.mimetype.api");
        deployBundle("org.nuxeo.ecm.platform.mimetype.core");
        deployBundle("org.nuxeo.ecm.platform.filemanager.api");
        deployBundle("org.nuxeo.ecm.platform.filemanager.core");
        deployBundle("org.nuxeo.ecm.platform.filemanager.core.listener");
        // Rendition Service
        deployBundle("org.nuxeo.ecm.platform.rendition.api");
        deployBundle("org.nuxeo.ecm.platform.rendition.core");
        deployBundle("org.nuxeo.ecm.automation.core");
        // Audit Service
        deployBundle("org.nuxeo.ecm.core.persistence");
        deployBundle("org.nuxeo.ecm.platform.audit.api");
        deployBundle("org.nuxeo.ecm.platform.audit");
        deployContrib("org.nuxeo.ecm.core.opencmis.tests.tests",
                "OSGI-INF/audit-persistence-config.xml");
        // QueryMaker registration
        deployBundle("org.nuxeo.ecm.core.opencmis.impl");
        // these deployments needed for NuxeoAuthenticationFilter.loginAs
        deployBundle("org.nuxeo.ecm.directory.types.contrib");
        deployBundle("org.nuxeo.ecm.platform.login");
        deployBundle("org.nuxeo.ecm.platform.web.common");

        openSession(); // nuxeo

        setUpCmisSession();

        setUpData();

        RepositoryInfo rid = session.getBinding().getRepositoryService().getRepositoryInfo(
                getRepositoryId(), null);
        assertNotNull(rid);
        rootFolderId = rid.getRootFolderId();
        assertNotNull(rootFolderId);

        isAtomPub = this instanceof TestNuxeoSessionAtomPub;
        isBrowser = this instanceof TestNuxeoSessionBrowser;
    }

    @Override
    @After
    public void tearDown() throws Exception {
        tearDownData();
        tearDownCmisSession();
        closeSession();
        super.tearDown();
    }

    /** Sets up the client, fills "session". */
    public abstract void setUpCmisSession() throws Exception;

    /** Sets up the client, fills "session". */
    protected abstract void setUpCmisSession(String username) throws Exception;

    /** Tears down the client. */
    public abstract void tearDownCmisSession() throws Exception;

    protected void setUpData() throws Exception {
        repoDetails = Helper.makeNuxeoRepository(super.session);
        database.sleepForFulltext();
    }

    protected void tearDownData() {
    }

    protected CoreSession getCoreSession() {
        return super.session;
    }

    protected String getRepositoryId() {
        return super.session.getRepositoryName();
    }

    protected String getRootFolderId() {
        try {
            return super.session.getRootDocument().getId();
        } catch (ClientException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testRoot() {
        Folder root = session.getRootFolder();
        assertNotNull(root);
        assertNotNull(root.getId());
        assertNotNull(root.getType());
        assertEquals(NUXEO_ROOT_TYPE, root.getType().getId());
        assertEquals(rootFolderId, root.getPropertyValue(PropertyIds.OBJECT_ID));
        assertEquals(NUXEO_ROOT_TYPE,
                root.getPropertyValue(PropertyIds.OBJECT_TYPE_ID));
        assertEquals(NUXEO_ROOT_NAME, root.getName());
        List<Property<?>> props = root.getProperties();
        assertNotNull(props);
        assertTrue(props.size() > 0);
        assertEquals("/", root.getPath());
        assertEquals(Collections.singletonList("/"), root.getPaths());
        assertNull(root.getFolderParent());
        assertEquals(Collections.emptyList(), root.getParents());
    }

    @Test
    public void testDefaultProperties() throws Exception {
        Folder root = session.getRootFolder();
        CmisObject child = root.getChildren().iterator().next();
        assertNotNull(child.getProperty("dc:coverage"));
        assertNull(child.getPropertyValue("dc:coverage"));
        Document doc = (Document) session.getObjectByPath("/testfolder1/testfile1");
        List<String> subjects = doc.getPropertyValue("dc:subjects");
        assertEquals(Arrays.asList("foo", "gee/moo"), subjects);
    }

    @Test
    public void testPath() throws Exception {
        Folder folder = (Folder) session.getObjectByPath("/testfolder1");
        assertEquals("/testfolder1", folder.getPath());
        assertEquals(Collections.singletonList("/testfolder1"),
                folder.getPaths());

        Document doc = (Document) session.getObjectByPath("/testfolder1/testfile1");
        assertEquals(Collections.singletonList("/testfolder1/testfile1"),
                doc.getPaths());
    }

    @Test
    public void testParent() throws Exception {
        Folder folder = (Folder) session.getObjectByPath("/testfolder1");
        assertEquals(rootFolderId, folder.getFolderParent().getId());
        List<Folder> parents = folder.getParents();
        assertEquals(1, parents.size());
        assertEquals(rootFolderId, parents.get(0).getId());

        Document doc = (Document) session.getObjectByPath("/testfolder1/testfile1");
        parents = doc.getParents();
        assertEquals(1, parents.size());
        assertEquals(folder.getId(), parents.get(0).getId());
    }

    @Test
    public void testCreateObject() {
        Folder root = session.getRootFolder();
        ContentStream contentStream = null;
        VersioningState versioningState = null;
        List<Policy> policies = null;
        List<Ace> addAces = null;
        List<Ace> removeAces = null;
        OperationContext context = NuxeoSession.DEFAULT_CONTEXT;
        Map<String, Serializable> properties = new HashMap<String, Serializable>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, "Note");
        properties.put(PropertyIds.NAME, "mynote");
        properties.put("note", "bla bla");
        Document doc = root.createDocument(properties, contentStream,
                versioningState, policies, addAces, removeAces, context);
        assertNotNull(doc.getId());
        assertEquals("mynote", doc.getName());
        assertEquals("mynote", doc.getPropertyValue("dc:title"));
        assertEquals("bla bla", doc.getPropertyValue("note"));

        // list children
        ItemIterable<CmisObject> children = root.getChildren();
        assertEquals(3, children.getTotalNumItems());
        CmisObject note = null;
        for (CmisObject child : children) {
            if (child.getName().equals("mynote")) {
                note = child;
            }
        }
        assertNotNull("Missing child", note);
        assertEquals("Note", note.getType().getId());
        assertEquals("bla bla", note.getPropertyValue("note"));
    }

    @Test
    public void testCreateDocumentWithContentStream() throws Exception {
        Folder root = session.getRootFolder();
        ContentStream cs = new ContentStreamImpl("myfile", "text/plain",
                Helper.FILE1_CONTENT);
        OperationContext context = NuxeoSession.DEFAULT_CONTEXT;
        VersioningState versioningState = null;
        List<Policy> policies = null;
        List<Ace> addAces = null;
        List<Ace> removeAces = null;
        Map<String, Serializable> properties = new HashMap<String, Serializable>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, "File");
        properties.put(PropertyIds.NAME, "myfile");
        Document doc = root.createDocument(properties, cs, versioningState,
                policies, addAces, removeAces, context);
        cs = doc.getContentStream();
        assertNotNull(cs);
        assertEquals("text/plain", cs.getMimeType());
        assertEquals("myfile", cs.getFileName());
        if (!(isAtomPub || isBrowser)) {
            assertEquals(Helper.FILE1_CONTENT.length(), cs.getLength());
        }
        assertEquals(Helper.FILE1_CONTENT, Helper.read(cs.getStream(), "UTF-8"));
    }

    @Test
    public void testCreateDocumentThenSetContentStream() throws Exception {
        Folder root = session.getRootFolder();
        OperationContext context = NuxeoSession.DEFAULT_CONTEXT;
        VersioningState versioningState = null;
        List<Policy> policies = null;
        List<Ace> addAces = null;
        List<Ace> removeAces = null;
        Map<String, Serializable> properties = new HashMap<String, Serializable>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, "File");
        properties.put(PropertyIds.NAME, "myfile");
        Document doc = root.createDocument(properties, null, versioningState,
                policies, addAces, removeAces, context);
        ContentStream cs = new ContentStreamImpl("myfile", "text/plain",
                Helper.FILE1_CONTENT);
        doc.setContentStream(cs, true);
        cs = doc.getContentStream();
        assertNotNull(cs);
        assertEquals("text/plain", cs.getMimeType());
        assertEquals("myfile", cs.getFileName());
        if (!(isAtomPub || isBrowser)) {
            assertEquals(Helper.FILE1_CONTENT.length(), cs.getLength());
        }
        assertEquals(Helper.FILE1_CONTENT, Helper.read(cs.getStream(), "UTF-8"));
    }

    @Test
    public void testCreateRelationship() throws Exception {
        String id1 = session.getObjectByPath("/testfolder1/testfile1").getId();
        String id2 = session.getObjectByPath("/testfolder1/testfile2").getId();

        Map<String, Serializable> properties = new HashMap<String, Serializable>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, "Relation");
        properties.put(PropertyIds.NAME, "rel");
        properties.put(PropertyIds.SOURCE_ID, id1);
        properties.put(PropertyIds.TARGET_ID, id2);
        ObjectId relid = session.createRelationship(properties);

        // note: has to be an administrator to get relations
        closeSession();
        NuxeoPrincipal admin = new NuxeoPrincipalImpl("admin", false, true);
        super.session = openSessionAs(admin);
        tearDownCmisSession();
        Thread.sleep(1000); // otherwise sometimes fails to set up again
        // for tests, server-side TrustingLoginProvider makes "admin*" an admin
        setUpCmisSession(admin.getName());

        session.clear(); // clear cache
        ItemIterable<Relationship> rels = session.getRelationships(
                session.createObjectId(id1), false,
                RelationshipDirection.SOURCE, null,
                session.createOperationContext());
        assertEquals(1, rels.getTotalNumItems());
        for (Relationship r : rels) {
            assertEquals(relid.getId(), r.getId());
        }

        Relationship rel = (Relationship) session.getObject(relid);
        assertNotNull(rel);
        assertEquals(id1, rel.getSourceId().getId());
        assertEquals(id2, rel.getTargetId().getId());
    }

    @Test
    public void testUpdate() throws Exception {
        Document doc;

        doc = (Document) session.getObjectByPath("/testfolder1/testfile1");
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("dc:title", "new title");
        map.put("dc:subjects", Arrays.asList("a", "b", "c"));
        doc.updateProperties(map);

        doc = (Document) session.getObjectByPath("/testfolder1/testfile1");
        assertEquals("new title", doc.getPropertyValue("dc:title"));
        assertEquals(Arrays.asList("a", "b", "c"),
                doc.getPropertyValue("dc:subjects"));

        // TODO test transient object API
        map.clear();
        map.put("dc:title", "other title");
        map.put("dc:subjects", Arrays.asList("foo"));
        doc.updateProperties(map);
        doc.refresh(); // reload
        assertEquals("other title", doc.getPropertyValue("dc:title"));
        assertEquals(Arrays.asList("foo"), doc.getPropertyValue("dc:subjects"));
    }

    @Test
    public void testContentStream() throws Exception {
        Document file = (Document) session.getObjectByPath("/testfolder1/testfile1");

        // check Cache Response Headers (eTag and Last-Modified)
        if (isAtomPub || isBrowser) {
            RepositoryInfo ri = session.getRepositoryInfo();
            String uri = ri.getThinClientUri() + ri.getId() + "/";
            uri += isAtomPub ? "content?id=" : "root?objectId=";
            uri += file.getId();
            String eTag = file.getPropertyValue("nuxeo:contentStreamDigest");
            GregorianCalendar lastModifiedCalendar = file.getPropertyValue("dc:modified");
            String lastModified = DateUtil.formatDate(lastModifiedCalendar.getTime());
            String encoding = Base64.encodeBytes(
                    new String(USERNAME + ":" + PASSWORD).getBytes());
            DefaultHttpClient client = new DefaultHttpClient();
            HttpGet request = new HttpGet(uri);
            HttpResponse response = null;
            request.setHeader("Authorization", "Basic " + encoding);
            try {
                request.setHeader("If-None-Match", eTag);
                response = client.execute(request);
                assertEquals(HttpServletResponse.SC_NOT_MODIFIED,
                        response.getStatusLine().getStatusCode());
                request.removeHeaders("If-None-Match");
                request.setHeader("If-Modified-Since", lastModified);
                response = client.execute(request);
                assertEquals(HttpServletResponse.SC_NOT_MODIFIED,
                        response.getStatusLine().getStatusCode());
            } finally {
                client.getConnectionManager().shutdown();
            }
        }

        // get stream
        ContentStream cs = file.getContentStream();
        assertNotNull(cs);
        assertEquals("text/plain", cs.getMimeType());
        assertEquals("testfile.txt", cs.getFileName());
        if (!(isAtomPub || isBrowser)) {
            // TODO fix AtomPub/Browser case where the length is unknown (streaming)
            assertEquals(Helper.FILE1_CONTENT.length(), cs.getLength());
        }
        assertEquals(Helper.FILE1_CONTENT, Helper.read(cs.getStream(), "UTF-8"));

        // set stream
        // TODO convenience constructors for ContentStreamImpl
        byte[] streamBytes = STREAM_CONTENT.getBytes("UTF-8");
        ByteArrayInputStream stream = new ByteArrayInputStream(streamBytes);
        cs = new ContentStreamImpl("foo.txt",
                BigInteger.valueOf(streamBytes.length),
                "text/plain; charset=UTF-8", stream);
        file.setContentStream(cs, true);

        // refetch stream
        file = (Document) session.getObject(file);
        cs = file.getContentStream();
        assertNotNull(cs);
        // AtomPub lowercases charset -> TODO proper mime type comparison
        String mimeType = cs.getMimeType().toLowerCase().replace(" ", "");
        assertEquals("text/plain;charset=utf-8", mimeType);
        // TODO fix AtomPub case where the filename is null
        assertEquals("foo.txt", cs.getFileName());
        if (!(isAtomPub || isBrowser)) {
            // TODO fix AtomPub/Browser case where the length is unknown (streaming)
            assertEquals(streamBytes.length, cs.getLength());
        }
        assertEquals(STREAM_CONTENT, Helper.read(cs.getStream(), "UTF-8"));

        // delete
        file.deleteContentStream();
        file.refresh();
        assertEquals(null, file.getContentStream());
    }

    @Test
    public void testAllowableActions() throws Exception {
        CmisObject ob;
        AllowableActions aa;
        Set<Action> expected;

        ob = session.getObjectByPath("/testfolder1");
        aa = ob.getAllowableActions();
        assertNotNull(aa);
        expected = EnumSet.of( //
                Action.CAN_GET_OBJECT_PARENTS, //
                Action.CAN_GET_PROPERTIES, //
                Action.CAN_GET_DESCENDANTS, //
                Action.CAN_GET_FOLDER_PARENT, //
                Action.CAN_GET_FOLDER_TREE, //
                Action.CAN_GET_CHILDREN, //
                Action.CAN_CREATE_DOCUMENT, //
                Action.CAN_CREATE_FOLDER, //
                Action.CAN_CREATE_RELATIONSHIP, //
                Action.CAN_DELETE_TREE, //
                Action.CAN_GET_RENDITIONS, //
                Action.CAN_UPDATE_PROPERTIES, //
                Action.CAN_MOVE_OBJECT, //
                Action.CAN_DELETE_OBJECT);
        assertEquals(expected, aa.getAllowableActions());

        ob = session.getObjectByPath("/testfolder1/testfile1");
        aa = ob.getAllowableActions();
        assertNotNull(aa);
        expected = EnumSet.of( //
                Action.CAN_GET_OBJECT_PARENTS, //
                Action.CAN_GET_PROPERTIES, //
                Action.CAN_GET_CONTENT_STREAM, //
                Action.CAN_SET_CONTENT_STREAM, //
                Action.CAN_DELETE_CONTENT_STREAM, //
                Action.CAN_UPDATE_PROPERTIES, //
                Action.CAN_MOVE_OBJECT, //
                Action.CAN_DELETE_OBJECT, //
                Action.CAN_ADD_OBJECT_TO_FOLDER, //
                Action.CAN_REMOVE_OBJECT_FROM_FOLDER, //
                Action.CAN_GET_RENDITIONS, //
                Action.CAN_GET_ALL_VERSIONS, //
                Action.CAN_CANCEL_CHECK_OUT, //
                Action.CAN_CHECK_IN);
        assertEquals(expected, aa.getAllowableActions());

        String q = "SELECT cmis:objectId FROM cmis:document WHERE cmis:name = 'testfile1_Title'";
        OperationContext oc = session.createOperationContext();
        oc.setIncludeAllowableActions(true);
        ItemIterable<QueryResult> results = session.query(q, true, oc);
        assertEquals(1, results.getTotalNumItems());
        aa = results.iterator().next().getAllowableActions();
        assertNotNull(aa);
        assertEquals(expected, aa.getAllowableActions());
    }

    public static final Comparator<RenditionData> RENDITION_CMP = new Comparator<RenditionData>() {
        @Override
        public int compare(RenditionData a, RenditionData b) {
            return a.getStreamId().compareTo(b.getStreamId());
        };
    };

    @Test
    public void testRenditions() throws Exception {
        CmisObject ob = session.getObjectByPath("/testfolder1/testfile1");
        List<Rendition> renditions = ob.getRenditions();
        assertTrue(renditions.isEmpty());

        // no renditions by default with object

        session.clear();
        OperationContext oc = session.createOperationContext();
        // oc.setRenditionFilterString("*");
        ob = session.getObject(session.createObjectId(ob.getId()), oc);
        renditions = ob.getRenditions();
        assertTrue(renditions.isEmpty());

        // get renditions with object

        session.clear();
        oc = session.createOperationContext();
        oc.setRenditionFilterString("*");
        ob = session.getObject(session.createObjectId(ob.getId()), oc);
        renditions = ob.getRenditions();
        assertEquals(2, renditions.size());
        Collections.sort(renditions, RENDITION_CMP);
        check(renditions.get(0), true);

        // get renditions with query

        String q = "SELECT cmis:objectId FROM cmis:document WHERE cmis:name = 'testfile1_Title'";
        ItemIterable<QueryResult> results = session.query(q, true, oc);
        assertEquals(1, results.getTotalNumItems());
        renditions = results.iterator().next().getRenditions();
        assertEquals(2, renditions.size());
        Collections.sort(renditions, RENDITION_CMP);
        check(renditions.get(0), false);
        // no rendition stream, Chemistry deficiency (QueryResultImpl
        // constructor call to of.convertRendition with null)
    }

    private static final int TEXT_PNG_ICON_SIZE = 394;

    protected void check(Rendition ren, boolean checkStream) {
        assertEquals("cmis:thumbnail", ren.getKind());
        assertEquals("nuxeo:icon", ren.getStreamId());
        assertEquals("image/png", ren.getMimeType());
        assertEquals("text.png", ren.getTitle());
        assertEquals(TEXT_PNG_ICON_SIZE, ren.getLength());
        if (checkStream) {
            // get rendition stream
            ContentStream cs = ren.getContentStream();
            assertEquals("image/png", cs.getMimeType());
            assertEquals("text.png", cs.getFileName());
            assertEquals(TEXT_PNG_ICON_SIZE, cs.getLength());
        }
    }

    @Test
    public void testDeletedInTrash() throws Exception {
        String file5id = repoDetails.get("file5id");

        try {
            session.getObjectByPath("/testfolder1/testfile5");
            fail("file 5 should be in trash");
        } catch (CmisObjectNotFoundException e) {
            // ok
        }
        try {
            session.getObject(session.createObjectId(file5id));
            fail("file 5 should be in trash");
        } catch (CmisObjectNotFoundException e) {
            // ok
        }

        Folder folder = (Folder) session.getObjectByPath("/testfolder1");
        ItemIterable<CmisObject> children = folder.getChildren();
        assertEquals(3, children.getTotalNumItems());
        for (CmisObject child : children) {
            if (child.getName().equals("title5")) {
                fail("file 5 should be in trash");
            }
        }

        // TODO
        // String query =
        // "SELECT cmis:objectId FROM cmis:document WHERE dc:title = 'title5'";
        // ItemIterable<QueryResult> col = session.query(query, false);
        // assertEquals("file 5 should be in trash", 0, col.getTotalNumItems());

        // cannot delete folder, has children
        try {
            folder.delete(true);
            fail("Should not be able to delete non-empty folder");
        } catch (CmisConstraintException e) {
            // ok
        }

        // test trashed child doesn't block folder delete
        for (CmisObject child : folder.getChildren()) {
            child.delete(true);
        }
        folder.delete(true);
    }

    @Test
    public void testDelete() throws Exception {
        Document doc = (Document) session.getObjectByPath("/testfolder1/testfile1");
        doc.delete(true);

        session.clear();
        try {
            session.getObjectByPath("/testfolder1/testfile1");
            fail("Document should be deleted");
        } catch (CmisObjectNotFoundException e) {
            // ok
        }
    }

    @Test
    public void testDeleteTree() throws Exception {
        Folder folder = (Folder) session.getObjectByPath("/testfolder1");
        List<String> failed = folder.deleteTree(true, null, true);
        assertTrue(failed == null || failed.isEmpty());

        session.clear();
        try {
            session.getObjectByPath("/testfolder1");
            fail("Folder should be deleted");
        } catch (CmisObjectNotFoundException e) {
            // ok
        }
        try {
            session.getObjectByPath("/testfolder1/testfile1");
            fail("Folder should be deleted");
        } catch (CmisObjectNotFoundException e) {
            // ok
        }

        folder = (Folder) session.getObjectByPath("/testfolder2");
        assertNotNull(folder);
    }

    @Test
    public void testCopy() throws Exception {
        if (isAtomPub) {
            // copy not implemented by AtomPub bindings
            return;
        }
        Document doc = (Document) session.getObjectByPath("/testfolder1/testfile1");
        assertEquals("testfile1_Title", doc.getPropertyValue("dc:title"));
        Document copy = doc.copy(session.createObjectId(rootFolderId),
                Collections.singletonMap("dc:title", "new title"), null, null,
                null, null, session.getDefaultContext());
        assertNotSame(doc.getId(), copy.getId());
        assertEquals("new title", copy.getPropertyValue("dc:title"));

        // copy is also available from the folder
        Document copy2 = session.getRootFolder().createDocumentFromSource(doc,
                Collections.singletonMap("dc:title", "other title"), null);
        assertNotSame(copy.getId(), copy2.getId());
        assertNotSame(doc.getId(), copy2.getId());
        assertEquals("other title", copy2.getPropertyValue("dc:title"));
    }

    @Test
    public void testMove() throws Exception {
        Folder folder = (Folder) session.getObjectByPath("/testfolder1");
        Document doc = (Document) session.getObjectByPath("/testfolder2/testfolder3/testfile4");
        String docId = doc.getId();

        // TODO add move(target) convenience method
        doc.move(doc.getParents().get(0), folder);

        assertEquals(docId, doc.getId());
        session.clear();
        try {
            session.getObjectByPath("/testfolder2/testfolder3/testfile4");
            fail("Object should be moved away");
        } catch (CmisObjectNotFoundException e) {
            // ok
        }
        Document doc2 = (Document) session.getObjectByPath("/testfolder1/testfile4");
        assertEquals(docId, doc2.getId());
    }

    @Test
    public void testVersioning() throws Exception {
        CmisObject ob = session.getObjectByPath("/testfolder1/testfile1");
        String id = ob.getId();

        // checked out

        checkValue(PropertyIds.IS_LATEST_VERSION, Boolean.FALSE, ob);
        checkValue(PropertyIds.IS_MAJOR_VERSION, Boolean.FALSE, ob);
        checkValue(PropertyIds.IS_LATEST_MAJOR_VERSION, Boolean.FALSE, ob);
        checkValue(PropertyIds.VERSION_LABEL, null, ob);
        checkValue(PropertyIds.VERSION_SERIES_ID, NOT_NULL, ob);
        checkValue(PropertyIds.IS_VERSION_SERIES_CHECKED_OUT, Boolean.TRUE, ob);
        checkValue(PropertyIds.VERSION_SERIES_CHECKED_OUT_ID, id, ob);
        checkValue(PropertyIds.VERSION_SERIES_CHECKED_OUT_BY, USERNAME, ob);
        checkValue(PropertyIds.CHECKIN_COMMENT, null, ob);
        String series = ob.getPropertyValue(PropertyIds.VERSION_SERIES_ID);

        // check in major -> version 1.0

        ObjectId vid = ((Document) ob).checkIn(true, null, null, "comment");

        CmisObject ver = session.getObject(vid);

        checkValue(PropertyIds.IS_LATEST_VERSION, Boolean.TRUE, ver);
        checkValue(PropertyIds.IS_MAJOR_VERSION, Boolean.TRUE, ver);
        checkValue(PropertyIds.IS_LATEST_MAJOR_VERSION, Boolean.TRUE, ver);
        checkValue(PropertyIds.VERSION_LABEL, "1.0", ver);
        checkValue(PropertyIds.VERSION_SERIES_ID, series, ver);
        checkValue(PropertyIds.IS_VERSION_SERIES_CHECKED_OUT, Boolean.FALSE,
                ver);
        checkValue(PropertyIds.VERSION_SERIES_CHECKED_OUT_ID, null, ver);
        checkValue(PropertyIds.VERSION_SERIES_CHECKED_OUT_BY, null, ver);
        checkValue(PropertyIds.CHECKIN_COMMENT, "comment", ver);

        // look at the checked in document to verify
        // that CMIS views it as a version

        session.clear(); // clear cache
        CmisObject ci = session.getObject(ob);

        checkValue(PropertyIds.IS_LATEST_VERSION, Boolean.TRUE, ci);
        checkValue(PropertyIds.IS_MAJOR_VERSION, Boolean.TRUE, ci);
        checkValue(PropertyIds.IS_LATEST_MAJOR_VERSION, Boolean.TRUE, ci);
        checkValue(PropertyIds.VERSION_LABEL, "1.0", ci);
        checkValue(PropertyIds.VERSION_SERIES_ID, series, ci);
        checkValue(PropertyIds.IS_VERSION_SERIES_CHECKED_OUT, Boolean.FALSE, ci);
        checkValue(PropertyIds.VERSION_SERIES_CHECKED_OUT_ID, null, ci);
        checkValue(PropertyIds.VERSION_SERIES_CHECKED_OUT_BY, null, ci);
        checkValue(PropertyIds.CHECKIN_COMMENT, "comment", ci);

        // check out

        ObjectId coid = ((Document) ci).checkOut();
        session.clear(); // clear cache
        CmisObject co = session.getObject(coid);

        assertEquals(id, coid.getId()); // Nuxeo invariant
        checkValue(PropertyIds.IS_LATEST_VERSION, Boolean.FALSE, co);
        checkValue(PropertyIds.IS_MAJOR_VERSION, Boolean.FALSE, co);
        checkValue(PropertyIds.IS_LATEST_MAJOR_VERSION, Boolean.FALSE, co);
        checkValue(PropertyIds.VERSION_LABEL, null, co);
        checkValue(PropertyIds.VERSION_SERIES_ID, series, co);
        checkValue(PropertyIds.IS_VERSION_SERIES_CHECKED_OUT, Boolean.TRUE, co);
        checkValue(PropertyIds.VERSION_SERIES_CHECKED_OUT_ID, coid.getId(), co);
        checkValue(PropertyIds.VERSION_SERIES_CHECKED_OUT_BY, USERNAME, co);
        checkValue(PropertyIds.CHECKIN_COMMENT, null, co);
    }

    @Test
    public void testVersionBasedLocking() throws Exception {
        CmisObject ob = session.getObjectByPath("/testfolder1/testfile1");

        // implicitly checked out after create - unlocked
        assertFalse(isDocumentLocked(ob));

        ((Document) ob).checkIn(true, null, null, "comment");

        // checked in - unlocked
        assertFalse(isDocumentLocked(ob));

        CmisObject ci = session.getObject(ob);
        ObjectId coid = ((Document) ci).checkOut();
        session.clear(); // clear cache
        CmisObject co = session.getObject(coid);

        // explicitly checked out - locked
        assertTrue(isDocumentLocked(co));

        ((Document) co).cancelCheckOut();
        session.clear(); // clear cache
        CmisObject cco = session.getObject(ob);

        // cancelled check out - unlocked
        assertFalse(isDocumentLocked(cco));

        // cannot check out a locked document
        lockDocument(cco);
        try {
            ((Document) cco).checkOut();
            fail("Cannot check out a locked document");
        } catch (CmisInvalidArgumentException e) {
            // ok
        }
    }

    @Test
    public void testDeleteObjectOrCancelCheckOut() throws Exception {
        // test cancelCheckOut
        CmisObject ob = session.getObjectByPath("/testfolder1/testfile1");

        ((Document) ob).checkIn(true, null, null, "comment");
        ((Document) ob).checkOut();

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("dc:title", "new title");
        map.put("dc:subjects", Arrays.asList("a", "b", "c"));
        ob.updateProperties(map);

        ((Document) ob).cancelCheckOut();

        session.clear();
        ob = session.getObjectByPath("/testfolder1/testfile1");
        assertFalse("new title".equals(ob.getPropertyValue("dc:title")));

        //test deleteObject
        ob = session.getObjectByPath("/testfolder1/testfile2");

        map = new HashMap<String, Object>();
        map.put("dc:title", "new title");
        map.put("dc:subjects", Arrays.asList("a", "b", "c"));
        ob.updateProperties(map);

        ((Document) ob).cancelCheckOut();

        session.clear();
        try {
            ob = session.getObjectByPath("/testfolder1/testfile2");
            fail("Document should be deleted");
        } catch (CmisObjectNotFoundException e) {
           // ok
        }

    }

    @Test
    public void testCheckInWithChanges() throws Exception {
        CmisObject ob = session.getObjectByPath("/testfolder1/testfile1");

        // check in with data
        Map<String, Serializable> props = new HashMap<String, Serializable>();
        props.put("dc:title", "newtitle");
        byte[] bytes = "foo-bar".getBytes("UTF-8");
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        ContentStream cs = session.getObjectFactory().createContentStream(
                "test.pdf", bytes.length, "application/pdf", in);

        ObjectId vid = ((Document) ob).checkIn(true, props, cs, "comment");

        CmisObject ver = session.getObject(vid);
        checkValue(PropertyIds.IS_LATEST_VERSION, Boolean.TRUE, ver);
        checkValue(PropertyIds.VERSION_LABEL, "1.0", ver);
        checkValue(PropertyIds.CHECKIN_COMMENT, "comment", ver);

        // check changes applied
        checkValue("dc:title", "newtitle", ver);
        ContentStream cs2 = ((Document) ver).getContentStream();
        assertEquals("application/pdf", cs2.getMimeType());
        if (!(isAtomPub || isBrowser)) {
            assertEquals(bytes.length, cs2.getLength());
            assertEquals("test.pdf", cs2.getFileName());
        }
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        IOUtils.copy(cs2.getStream(), os);
        assertEquals("foo-bar", os.toString("UTF-8"));
    }

    @Test
    public void testUserWorkspace() throws ClientException {
        String wsPath = Helper.createUserWorkspace(getCoreSession(),
                (isAtomPub || isBrowser) ? USERNAME : "Administrator");
        Folder ws = (Folder) session.getObjectByPath(wsPath);
        assertNotNull(ws);
    }

    protected void checkValue(String prop, Object expected, CmisObject ob) {
        Object value = ob.getPropertyValue(prop);
        if (expected == NOT_NULL) {
            assertNotNull(value);
        } else {
            assertEquals(expected, value);
        }
    }

    private boolean isDocumentLocked(CmisObject ob) throws ClientException {
        return getCoreSession().getDocument(new IdRef(ob.getId())).isLocked();
    }

    private Lock lockDocument(CmisObject ob) throws ClientException {
        return getCoreSession().getDocument(new IdRef(ob.getId())).setLock();
    }

}
