/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.impl;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static org.apache.chemistry.opencmis.commons.BasicPermissions.ALL;
import static org.apache.chemistry.opencmis.commons.BasicPermissions.READ;
import static org.apache.chemistry.opencmis.commons.BasicPermissions.WRITE;
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
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
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
import org.apache.chemistry.opencmis.client.api.SessionFactory;
import org.apache.chemistry.opencmis.client.bindings.CmisBindingFactory;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.data.Ace;
import org.apache.chemistry.opencmis.commons.data.Acl;
import org.apache.chemistry.opencmis.commons.data.AllowableActions;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.Principal;
import org.apache.chemistry.opencmis.commons.data.RenditionData;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.enums.Action;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.commons.enums.DateTimeFormat;
import org.apache.chemistry.opencmis.commons.enums.RelationshipDirection;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.exceptions.CmisConstraintException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisInvalidArgumentException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.chemistry.opencmis.commons.impl.Base64;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.AccessControlEntryImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.AccessControlPrincipalDataImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ContentStreamImpl;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.RecoverableClientException;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.opencmis.impl.client.NuxeoSession;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoContentStream;
import org.nuxeo.ecm.core.opencmis.tests.Helper;
import org.nuxeo.ecm.core.opencmis.tests.StatusLoggingDefaultHttpInvoker;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.HotDeployer;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Test the high-level session using a local connection.
 */
@RunWith(FeaturesRunner.class)
@Features(CmisFeature.class)
//required for JsonFactoryManager service used indirectly in #testComplexProperties by NuxeoPropertyData.convertComplexPropertyToCMIS
@Deploy("org.nuxeo.ecm.webengine.core")
@Deploy("org.nuxeo.ecm.core.opencmis.tests.tests:OSGI-INF/types-contrib.xml")
@Deploy("org.nuxeo.ecm.core.opencmis.tests.tests:OSGI-INF/throw-exception-listener.xml")
@RepositoryConfig(cleanup = Granularity.METHOD)
public class CmisSuiteSession {

    private static final Log log = LogFactory.getLog(CmisSuiteSession.class);

    public static final String NUXEO_ROOT_TYPE = "Root"; // from Nuxeo

    public static final String NUXEO_ROOT_NAME = ""; // NuxeoPropertyDataName;

    public static final String USERNAME = "Administrator";

    public static final String PASSWORD = "test";

    // stream content with non-ASCII characters
    public static final String STREAM_CONTENT = "Caf\u00e9 Diem\none\0two";

    public static final String NOT_NULL = "CONSTRAINT_NOT_NULL";

    @Inject
    protected HotDeployer deployer;

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected CmisFeatureSession cmisFeatureSession;

    @Inject
    protected CoreSession coreSession;

    @Inject
    protected EventService eventService;

    @Inject
    protected Session session;

    protected String rootFolderId;

    protected boolean isHttp;

    protected boolean isAtomPub;

    protected boolean isBrowser;

    protected Map<String, String> repoDetails;

    @Before
    public void setUp() throws Exception {
        setUpData();
        session.clear(); // clear cache

        RepositoryInfo rid = session.getBinding().getRepositoryService().getRepositoryInfo(
                coreSession.getRepositoryName(), null);
        assertNotNull(rid);
        rootFolderId = rid.getRootFolderId();
        assertNotNull(rootFolderId);

        isHttp = cmisFeatureSession.isHttp;
        isAtomPub = cmisFeatureSession.isAtomPub;
        isBrowser = cmisFeatureSession.isBrowser;
    }

    protected void setUpData() throws Exception {
        repoDetails = Helper.makeNuxeoRepository(coreSession);
        txFeature.nextTransaction();
        coreFeature.getStorageConfiguration().sleepForFulltext();
    }

    @Inject
    TransactionalFeature txFeature;

    protected void waitForAsyncCompletion() {
        txFeature.nextTransaction();
    }

    @Test
    public void testRoot() {
        Folder root = session.getRootFolder();
        assertNotNull(root);
        assertNotNull(root.getId());
        assertNotNull(root.getType());
        assertEquals(NUXEO_ROOT_TYPE, root.getType().getId());
        assertEquals(rootFolderId, root.getPropertyValue(PropertyIds.OBJECT_ID));
        assertEquals(NUXEO_ROOT_TYPE, root.getPropertyValue(PropertyIds.OBJECT_TYPE_ID));
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
        assertEquals(Collections.singletonList("/testfolder1"), folder.getPaths());

        Document doc = (Document) session.getObjectByPath("/testfolder1/testfile1");
        assertEquals(Collections.singletonList("/testfolder1/testfile1"), doc.getPaths());
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
        Map<String, Serializable> properties = new HashMap<>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, "Note");
        properties.put(PropertyIds.NAME, "mynote");
        properties.put("note", "bla bla");
        Document doc = root.createDocument(properties, contentStream, versioningState, policies, addAces, removeAces,
                context);
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
        ContentStream cs = new ContentStreamImpl("myfile", "text/plain", Helper.FILE1_CONTENT);
        OperationContext context = NuxeoSession.DEFAULT_CONTEXT;
        VersioningState versioningState = null;
        List<Policy> policies = null;
        List<Ace> addAces = null;
        List<Ace> removeAces = null;
        Map<String, Serializable> properties = new HashMap<>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, "File");
        properties.put(PropertyIds.NAME, "myfile");
        Document doc = root.createDocument(properties, cs, versioningState, policies, addAces, removeAces, context);
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
        Map<String, Serializable> properties = new HashMap<>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, "File");
        properties.put(PropertyIds.NAME, "myfile");
        Document doc = root.createDocument(properties, null, versioningState, policies, addAces, removeAces, context);
        ContentStream cs = new ContentStreamImpl("myfile", "text/plain", Helper.FILE1_CONTENT);
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
    public void testCreateDocumentWithoutName() throws Exception {
        Map<String, Serializable> properties = new HashMap<>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, "File");
        try {
            session.getRootFolder().createDocument(properties, null, null, null, null, null,
                    NuxeoSession.DEFAULT_CONTEXT);
            fail("Creation without cmis:name should fail");
        } catch (CmisConstraintException e) {
            // ok
        }
    }

    @Test
    public void testCreateRelationship() throws Exception {
        if (!(isAtomPub || isBrowser)) {
            // createRelationship admin user only empowered for AtomPub &
            // Browser tests
            return;
        }

        String id1 = session.getObjectByPath("/testfolder1/testfile1").getId();
        String id2 = session.getObjectByPath("/testfolder1/testfile2").getId();

        Map<String, Serializable> properties = new HashMap<>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, "Relation");
        properties.put(PropertyIds.NAME, "rel");
        properties.put(PropertyIds.SOURCE_ID, id1);
        properties.put(PropertyIds.TARGET_ID, id2);
        ObjectId relid = session.createRelationship(properties);

        ItemIterable<Relationship> rels = session.getRelationships(session.createObjectId(id1), false,
                RelationshipDirection.SOURCE, null, session.createOperationContext());
        assertEquals(1, rels.getTotalNumItems());
        for (Relationship r : rels) {
            assertEquals(relid.getId(), r.getId());
        }

        Relationship rel = (Relationship) session.getObject(relid);
        assertNotNull(rel);
        assertEquals(id1, rel.getSourceId().getId());
        assertEquals(id2, rel.getTargetId().getId());
    }

    // HiddenRelation, like the standard DefaultRelation, is marked HiddenInNavigation
    @Test
    public void testCreateHiddenRelation() throws Exception {
        if (!(isAtomPub || isBrowser)) {
            // createRelationship admin user only empowered for AtomPub &
            // Browser tests
            return;
        }

        String id1 = session.getObjectByPath("/testfolder1/testfile1").getId();
        String id2 = session.getObjectByPath("/testfolder1/testfile2").getId();

        Map<String, Serializable> properties = new HashMap<>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, "HiddenRelation");
        properties.put(PropertyIds.NAME, "rel");
        properties.put(PropertyIds.SOURCE_ID, id1);
        properties.put(PropertyIds.TARGET_ID, id2);
        ObjectId relid = session.createRelationship(properties);

        ItemIterable<Relationship> rels = session.getRelationships(session.createObjectId(id1), false,
                RelationshipDirection.SOURCE, null, session.createOperationContext());
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
        Map<String, Object> map = new HashMap<>();
        map.put("dc:title", "new title");
        map.put("dc:subjects", Arrays.asList("a", "b", "c"));
        doc.updateProperties(map);

        doc = (Document) session.getObjectByPath("/testfolder1/testfile1");
        assertEquals("new title", doc.getPropertyValue("dc:title"));
        assertEquals(Arrays.asList("a", "b", "c"), doc.getPropertyValue("dc:subjects"));

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
    public void testUpdateDescription() throws Exception {
        Document doc;
        doc = (Document) session.getObjectByPath("/testfolder1/testfile1");

        doc.updateProperties(Collections.singletonMap("cmis:description", "desc1"));

        doc = (Document) session.getObjectByPath("/testfolder1/testfile1");
        assertEquals("desc1", doc.getPropertyValue("cmis:description"));
        assertEquals("desc1", doc.getPropertyValue("dc:description"));

        doc.updateProperties(Collections.singletonMap("dc:description", "desc2"));

        doc = (Document) session.getObjectByPath("/testfolder1/testfile1");
        assertEquals("desc2", doc.getPropertyValue("cmis:description"));
        assertEquals("desc2", doc.getPropertyValue("dc:description"));
    }

    @Test
    public void testPropertyFromSecondaryType() throws Exception {
        DocumentModel doc = coreSession.getDocument(new PathRef("/testfolder1/testfile1"));
        doc.addFacet("CustomFacetWithMySchema2");
        doc.setPropertyValue("my2:string", "foo");
        coreSession.saveDocument(doc);
        coreSession.save();
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        Document file = (Document) session.getObjectByPath("/testfolder1/testfile1");

        Property<?> p = file.getProperty("cmis:secondaryObjectTypeIds");
        assertNotNull(p);
        @SuppressWarnings("unchecked")
        List<String> stl = (List<String>) p.getValues();
        assertNotNull(stl);
        assertTrue(stl.contains("facet:CustomFacetWithMySchema2"));
        assertEquals("foo", file.getPropertyValue("my2:string"));

        // change secondary prop
        file.updateProperties(Collections.singletonMap("my2:string", "bar"), true); // refresh
        // check updated
        assertEquals("bar", file.getPropertyValue("my2:string"));
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
            String lastModified = RFC_1123_DATE_TIME.format(lastModifiedCalendar.toZonedDateTime());
            String encoding = Base64.encodeBytes(new String(USERNAME + ":" + PASSWORD).getBytes());
            DefaultHttpClient client = new DefaultHttpClient();
            HttpGet request = new HttpGet(uri);
            HttpResponse response = null;
            request.setHeader("Authorization", "Basic " + encoding);
            try {
                request.setHeader("If-None-Match", eTag);
                response = client.execute(request);
                assertEquals(HttpServletResponse.SC_NOT_MODIFIED, response.getStatusLine().getStatusCode());
                request.removeHeaders("If-None-Match");
                request.setHeader("If-Modified-Since", lastModified);
                response = client.execute(request);
                String debug = "lastModified=" + lastModifiedCalendar.getTimeInMillis() + " If-Modified-Since="
                        + lastModified + " NuxeoContentStream last=" + NuxeoContentStream.LAST_MODIFIED;
                // TODO NXP-16198 there are still timezone issues here
                // @Ignore
                // assertEquals(debug, HttpServletResponse.SC_NOT_MODIFIED, response.getStatusLine().getStatusCode());
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
            // TODO fix AtomPub/Browser case where the length is unknown
            // (streaming)
            assertEquals(Helper.FILE1_CONTENT.length(), cs.getLength());
        }
        assertEquals(Helper.FILE1_CONTENT, Helper.read(cs.getStream(), "UTF-8"));

        // set stream
        // TODO convenience constructors for ContentStreamImpl
        byte[] streamBytes = STREAM_CONTENT.getBytes("UTF-8");
        ByteArrayInputStream stream = new ByteArrayInputStream(streamBytes);
        cs = new ContentStreamImpl("foo.txt", BigInteger.valueOf(streamBytes.length), "text/plain; charset=UTF-8",
                stream);
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
            // TODO fix AtomPub/Browser case where the length is unknown
            // (streaming)
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

    private static final int THUMBNAIL_SIZE = 394;

    @Test
    public void testRenditions() throws Exception {
        boolean checkStream = !(isAtomPub || isBrowser);

        CmisObject ob = session.getObjectByPath("/testfolder1/testfile1");
        List<Rendition> renditions = ob.getRenditions();
        assertTrue(renditions == null || renditions.isEmpty());

        // no renditions by default with object

        session.clear();
        OperationContext oc = session.createOperationContext();
        ob = session.getObject(session.createObjectId(ob.getId()), oc);
        renditions = ob.getRenditions();
        assertTrue(renditions == null || renditions.isEmpty());

        // check rendition content stream requested directly
        // even though the doc has no renditions requested
        ContentStream cs = ((Document) ob).getContentStream("nuxeo:icon");
        assertNotNull(cs);
        assertEquals("image/png", cs.getMimeType());
        assertTrue(cs.getFileName().endsWith(".png"));
        if (!(isAtomPub || isBrowser)) {
            assertEquals(THUMBNAIL_SIZE, cs.getLength());
        }

        // get renditions with object

        session.clear();
        oc = session.createOperationContext();
        oc.setRenditionFilterString("*");
        ob = session.getObject(session.createObjectId(ob.getId()), oc);
        renditions = ob.getRenditions();
        assertEquals(4, renditions.size());
        Collections.sort(renditions, RENDITION_CMP);
        check(renditions.get(0), checkStream);

        // get renditions with query

        String q = "SELECT cmis:objectId FROM cmis:document WHERE cmis:name = 'testfile1_Title'";
        ItemIterable<QueryResult> results = session.query(q, true, oc);
        assertEquals(1, results.getTotalNumItems());
        renditions = results.iterator().next().getRenditions();
        assertEquals(4, renditions.size());
        Collections.sort(renditions, RENDITION_CMP);
        check(renditions.get(0), false);
        // no rendition stream, Chemistry deficiency (QueryResultImpl
        // constructor call to of.convertRendition with null)
    }

    protected void check(Rendition ren, boolean checkStream) {
        assertEquals("cmis:thumbnail", ren.getKind());
        assertEquals("nuxeo:icon", ren.getStreamId());
        assertEquals("image/png", ren.getMimeType());
        assertTrue(ren.getTitle().endsWith(".png"));
        assertEquals(THUMBNAIL_SIZE, ren.getLength());
        if (checkStream) {
            // get rendition stream
            ContentStream cs = ren.getContentStream();
            assertEquals("image/png", cs.getMimeType());
            assertTrue(cs.getFileName().endsWith(".png"));
            assertEquals(THUMBNAIL_SIZE, cs.getLength());
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
                Collections.singletonMap("dc:title", "new title"), null, null, null, null, session.getDefaultContext());
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
        checkValue(PropertyIds.IS_VERSION_SERIES_CHECKED_OUT, Boolean.FALSE, ver);
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

        // wait for fulltext before a cancelCheckOut, which does a copy of the fulltext rows as well
        waitForAsyncCompletion();

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
        } catch (CmisConstraintException e) {
            // ok
        }
    }

    @Test
    public void testDeleteObjectOrCancelCheckOut() throws Exception {
        // test cancelCheckOut
        CmisObject ob = session.getObjectByPath("/testfolder1/testfile1");

        ((Document) ob).checkIn(true, null, null, "comment");
        ((Document) ob).checkOut();

        Map<String, Object> map = new HashMap<>();
        map.put("dc:title", "new title");
        map.put("dc:subjects", Arrays.asList("a", "b", "c"));
        ob.updateProperties(map);

        // wait for fulltext before a cancelCheckOut, which does a copy of the fulltext rows as well
        waitForAsyncCompletion();

        ((Document) ob).cancelCheckOut();

        session.clear();
        ob = session.getObjectByPath("/testfolder1/testfile1");
        assertFalse("new title".equals(ob.getPropertyValue("dc:title")));

        // test deleteObject
        ob = session.getObjectByPath("/testfolder1/testfile2");

        map = new HashMap<>();
        map.put("dc:title", "new title");
        map.put("dc:subjects", Arrays.asList("a", "b", "c"));
        ob.updateProperties(map);

        // wait for fulltext before a cancelCheckOut, which does a copy of the fulltext rows as well
        waitForAsyncCompletion();

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
        Map<String, Serializable> props = new HashMap<>();
        props.put("dc:title", "newtitle");
        byte[] bytes = "foo-bar".getBytes("UTF-8");
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        ContentStream cs = session.getObjectFactory().createContentStream("test.pdf", bytes.length, "application/pdf",
                in);

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
    public void testUserWorkspace() {
        String wsPath = Helper.createUserWorkspace(coreSession, (isAtomPub || isBrowser) ? USERNAME : "Administrator");
        Folder ws = (Folder) session.getObjectByPath(wsPath);
        assertNotNull(ws);
    }

    @Test
    public void testLastModifiedServiceWrapper() throws Exception {
        if (!(isAtomPub || isBrowser)) {
            // test only makes sense in the context of REST HTTP
            return;
        }

        // deploy the LastModifiedServiceWrapper
        deployer.deploy("org.nuxeo.ecm.core.opencmis.tests.tests:OSGI-INF/test-servicefactorymanager-contrib.xml");
        //session = cmisFeatureSession.setUpCmisSession(coreSession.getRepositoryName());

        Document doc = (Document) session.getObjectByPath("/testfolder1/testfile1");
        GregorianCalendar lastModifiedCalendar = doc.getPropertyValue("dc:modified");

        // check Last-Modified Cache Response Header
        RepositoryInfo ri = session.getRepositoryInfo();
        String uri = ri.getThinClientUri() + ri.getId() + "/";
        uri += isAtomPub ? "content?id=" : "root?objectId=";
        uri += doc.getId();
        String lastModified = RFC_1123_DATE_TIME.format(lastModifiedCalendar.toZonedDateTime());
        String encoding = Base64.encodeBytes(new String(USERNAME + ":" + PASSWORD).getBytes());
        DefaultHttpClient client = new DefaultHttpClient();
        HttpGet request = new HttpGet(uri);
        HttpResponse response;
        request.setHeader("Authorization", "Basic " + encoding);
        try {
            response = client.execute(request);
            assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            // TODO NXP-18731 there are still timezone issues here
            // @Ignore
            // assertEquals(lastModified, response.getLastHeader("Last-Modified").getValue());
        } finally {
            client.getConnectionManager().shutdown();
        }
    }

    protected void checkValue(String prop, Object expected, CmisObject ob) {
        Object value = ob.getPropertyValue(prop);
        if (expected == NOT_NULL) {
            assertNotNull(value);
        } else {
            assertEquals(expected, value);
        }
    }

    private boolean isDocumentLocked(CmisObject ob) {
        return coreSession.getDocument(new IdRef(ob.getId())).isLocked();
    }

    private Lock lockDocument(CmisObject ob) {
        return coreSession.getDocument(new IdRef(ob.getId())).setLock();
    }

    protected static Set<String> set(String... strings) {
        return new HashSet<>(Arrays.asList(strings));
    }

    /** Get ACL, using * suffix on username to denote non-direct. */
    protected static Map<String, Set<String>> getActualAcl(Acl acl) {
        Map<String, Set<String>> actual = new HashMap<>();
        for (Ace ace : acl.getAces()) {
            actual.put(ace.getPrincipalId() + (ace.isDirect() ? "" : "*"), new HashSet<>(ace.getPermissions()));
        }
        return actual;
    }

    @Test
    public void testGetACLBase() throws Exception {
        String file1Id = session.getObjectByPath("/testfolder1/testfile1").getId();

        Acl acl = session.getAcl(session.createObjectId(file1Id), false);
        if (!(isAtomPub || isBrowser)) { // OpenCMIS 0.12 bug
            assertEquals(Boolean.TRUE, acl.isExact());
        }
        Map<String, Set<String>> actual = getActualAcl(acl);
        Map<String, Set<String>> expected = new HashMap<>();
        expected.put("bob", set("Browse"));
        expected.put("members*", set(READ, "Read"));
        expected.put("Administrator*", set(READ, WRITE, ALL, "Everything"));
        assertEquals(expected, actual);

        // with only basic permissions

        acl = session.getAcl(session.createObjectId(file1Id), true);
        if (!(isAtomPub || isBrowser)) { // OpenCMIS 0.12 bug
            assertEquals(Boolean.FALSE, acl.isExact());
        }
        actual = getActualAcl(acl);
        expected = new HashMap<>();
        expected.put("members*", set(READ));
        expected.put("Administrator*", set(READ, WRITE, ALL));
        assertEquals(expected, actual);
    }

    @Test
    public void testGetACL() throws Exception {
        String folder1Id = coreSession.getDocument(new PathRef("/testfolder1")).getId();
        String file1Id = coreSession.getDocument(new PathRef("/testfolder1/testfile1")).getId();
        String file4Id = coreSession.getDocument(new PathRef("/testfolder2/testfolder3/testfile4")).getId();

        // set more complex ACLs

        {
            // file1
            ACP acp = new ACPImpl();
            ACL acl = new ACLImpl();
            acl.add(new ACE("pete", SecurityConstants.READ_WRITE, true));
            acl.add(new ACE("john", SecurityConstants.WRITE, true));
            acp.addACL(acl);
            // other ACL
            acl = new ACLImpl("workflow");
            acl.add(new ACE("steve", SecurityConstants.READ, true));
            acp.addACL(acl);
            coreSession.setACP(new IdRef(file1Id), acp, true);

            // folder1
            acp = new ACPImpl();
            acl = new ACLImpl();
            acl.add(new ACE("mary", SecurityConstants.READ, true));
            acp.addACL(acl);
            coreSession.setACP(new IdRef(folder1Id), acp, true);

            // block on testfile4
            acp = new ACPImpl();
            acl = new ACLImpl();
            acl.add(new ACE(SecurityConstants.ADMINISTRATOR, SecurityConstants.READ, true));
            acl.add(new ACE(SecurityConstants.EVERYONE, SecurityConstants.EVERYTHING, false));
            acp.addACL(acl);
            coreSession.setACP(new IdRef(file4Id), acp, true);

            coreSession.save();
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }

        Acl acl = session.getAcl(session.createObjectId(file1Id), false);
        if (!(isAtomPub || isBrowser)) { // OpenCMIS 0.12 bug
            assertEquals(Boolean.TRUE, acl.isExact());
        }
        Map<String, Set<String>> actual = getActualAcl(acl);
        Map<String, Set<String>> expected = new HashMap<>();
        expected.put("pete", set(READ, WRITE, "ReadWrite"));
        expected.put("john", set("Write"));
        // * for inherited or not local acl
        expected.put("steve*", set(READ, "Read"));
        expected.put("mary*", set(READ, "Read"));
        expected.put("members*", set(READ, "Read"));
        expected.put("Administrator*", set(READ, WRITE, ALL, "Everything"));
        assertEquals(expected, actual);

        // direct Object API

        OperationContext oc = session.createOperationContext();
        oc.setIncludeAcls(true);
        Document ob = (Document) session.getObjectByPath("/testfolder1/testfile1", oc);
        acl = ob.getAcl();
        if (!(isAtomPub || isBrowser)) { // OpenCMIS 0.12 bug
            assertEquals(Boolean.TRUE, acl.isExact());
        }
        actual = getActualAcl(acl);
        assertEquals(expected, actual);

        // check blocking

        acl = session.getAcl(session.createObjectId(file4Id), false);
        if (!(isAtomPub || isBrowser)) { // OpenCMIS 0.12 bug
            assertEquals(Boolean.TRUE, acl.isExact());
        }
        actual = getActualAcl(acl);
        expected = new HashMap<>();
        expected.put("Administrator", set(READ, "Read"));
        expected.put("Everyone", set("Nothing"));
        assertEquals(expected, actual);
    }

    @Test
    public void testApplyACL() throws Exception {
        String file1Id = session.getObjectByPath("/testfolder1/testfile1").getId();

        // file1 already has a bob -> Browse permission from setUp

        // add

        Principal p = new AccessControlPrincipalDataImpl("mary");
        Ace ace = new AccessControlEntryImpl(p, Arrays.asList(READ));
        List<Ace> addAces = Arrays.asList(ace);
        List<Ace> removeAces = null;
        Acl acl = session.applyAcl(session.createObjectId(file1Id), addAces, removeAces, null);

        if (!(isAtomPub || isBrowser)) { // OpenCMIS 0.12 bug
            assertEquals(Boolean.TRUE, acl.isExact());
        }
        Map<String, Set<String>> actual = getActualAcl(acl);
        Map<String, Set<String>> expected = new HashMap<>();
        expected.put("bob", set("Browse"));
        expected.put("mary", set(READ, "Read"));
        expected.put("members*", set(READ, "Read"));
        expected.put("Administrator*", set(READ, WRITE, ALL, "Everything"));
        assertEquals(expected, actual);

        // remove

        ace = new AccessControlEntryImpl(p, Arrays.asList(READ, "Read"));
        addAces = null;
        removeAces = Arrays.asList(ace);
        acl = session.applyAcl(session.createObjectId(file1Id), addAces, removeAces, null);

        if (!(isAtomPub || isBrowser)) { // OpenCMIS 0.12 bug
            assertEquals(Boolean.TRUE, acl.isExact());
        }
        actual = getActualAcl(acl);
        expected = new HashMap<>();
        expected.put("bob", set("Browse"));
        expected.put("members*", set(READ, "Read"));
        expected.put("Administrator*", set(READ, WRITE, ALL, "Everything"));
        assertEquals(expected, actual);
    }

    @Test
    public void testRecoverableException() throws Exception {
        // listener that will cause a RecoverableClientException to be thrown
        // when a doc whose name starts with "throw" is created
        deployer.deploy("org.nuxeo.ecm.core.opencmis.tests.tests:OSGI-INF/recoverable-exc-listener-contrib.xml");

        Map<String, Serializable> properties = new HashMap<>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, "File");
        properties.put(PropertyIds.NAME, "throw_foo");
        try {
            session.getRootFolder().createDocument(properties, null, null, null, null, null,
                    NuxeoSession.DEFAULT_CONTEXT);
            fail("should throw RecoverableClientException");
        } catch (CmisInvalidArgumentException e) {
            // ok, this is what we get for a 400
            // check message
            assertEquals("bad name", e.getMessage());
        } catch (CmisRuntimeException e) {
            // check status code
            if (isHttp) {
                fail("should have thrown CmisInvalidArgumentException");
                // int status = StatusLoggingDefaultHttpInvoker.lastStatus;
                // assertEquals(400, status);
            } else {
                Throwable cause = e.getCause();
                if (!(cause instanceof RecoverableClientException)) {
                    throw e;
                }
            }
            // check message
            assertEquals("bad name", e.getMessage());
        }
    }

    @Test
    public void testComplexProperties() throws Exception {
        // Enable complex properties
        String ENABLE_COMPLEX_PROPERTIES = "org.nuxeo.cmis.enableComplexProperties";
        Framework.getProperties().setProperty(ENABLE_COMPLEX_PROPERTIES, "true");

        cmisFeatureSession.tearDownCmisSession();
        Thread.sleep(1000); // otherwise sometimes fails to set up again
        session = cmisFeatureSession.setUpCmisSession(coreSession.getRepositoryName());

        ObjectMapper om = new ObjectMapper();
        Map<String, Object> inputMap = new HashMap<>();
        inputMap.put("stringProp", "testString1");
        Long dateAsLong = Long.valueOf(1234500000000L);
        String dateAsString = "2009-02-13T04:40:00.000Z";

        Map<String, Serializable> properties = new HashMap<>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, "ComplexFile");
        properties.put(PropertyIds.NAME, "complexfile");
        Document doc;
        List<String> docIds = new ArrayList<>();

        // date as long timestamp
        inputMap.put("dateProp", dateAsLong);
        properties.put("complexTest:complexItem", om.writeValueAsString(inputMap));
        doc = session.getRootFolder().createDocument(properties, null, null, null, null, null,
                NuxeoSession.DEFAULT_CONTEXT);
        docIds.add(doc.getId());

        // date as w3c string
        inputMap.put("dateProp", dateAsString);
        properties.put("complexTest:complexItem", om.writeValueAsString(inputMap));
        doc = session.getRootFolder().createDocument(properties, null, null, null, null, null,
                NuxeoSession.DEFAULT_CONTEXT);
        docIds.add(doc.getId());

        Map<String, Object> expectedMap = new HashMap<>();
        expectedMap.put("stringProp", "testString1");
        expectedMap.put("dateProp", isBrowser ? dateAsLong : dateAsString);
        expectedMap.put("boolProp", null);
        expectedMap.put("enumProp", null);
        expectedMap.put("arrayProp", new ArrayList<>(0));
        expectedMap.put("intProp", null);
        expectedMap.put("floatProp", null);

        for (String docId : docIds) {
            doc = (Document) session.getObject(docId);
            String res = (String) doc.getPropertyValue("complexTest:complexItem");
            assertEquals(expectedMap, om.readValue(res, Map.class));
        }

        if (!isBrowser) {
            return;
        }

        expectedMap.put("dateProp", dateAsString);

        session = createBrowserCmisSession(coreSession.getRepositoryName(),
                ((CmisFeatureSessionHttp) cmisFeatureSession).serverURI);
        try {
            for (String docId : docIds) {
                doc = (Document) session.getObject(docId);
                String res = (String) doc.getPropertyValue("complexTest:complexItem");
                assertEquals(expectedMap, om.readValue(res, Map.class));
            }
        } finally {
            session.clear();
        }
    }

    private Session createBrowserCmisSession(String repositoryName, URI serverURI) {

        SessionFactory sf = SessionFactoryImpl.newInstance();
        Map<String, String> params = new HashMap<>();

        params.put(SessionParameter.AUTHENTICATION_PROVIDER_CLASS, CmisBindingFactory.STANDARD_AUTHENTICATION_PROVIDER);

        params.put(SessionParameter.CACHE_SIZE_REPOSITORIES, "10");
        params.put(SessionParameter.CACHE_SIZE_TYPES, "100");
        params.put(SessionParameter.CACHE_SIZE_OBJECTS, "100");

        params.put(SessionParameter.REPOSITORY_ID, repositoryName);
        params.put(SessionParameter.USER, USERNAME);
        params.put(SessionParameter.PASSWORD, PASSWORD);

        params.put(SessionParameter.HTTP_INVOKER_CLASS, StatusLoggingDefaultHttpInvoker.class.getName());

        params.put(SessionParameter.BINDING_TYPE, BindingType.BROWSER.value());
        params.put(SessionParameter.BROWSER_URL, serverURI.toString());

        params.put(SessionParameter.BROWSER_DATETIME_FORMAT, DateTimeFormat.EXTENDED.value());

        session = sf.createSession(params);
        return session;
    }

    @Test
    public void testRollbackOnException() throws Exception {
        Folder root = session.getRootFolder();
        Map<String, Serializable> properties = new HashMap<>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, "File");
        properties.put(PropertyIds.NAME, ThrowExceptionListener.CRASHME_NAME); // listener on this throws exception
        try {
            root.createDocument(properties, null, null, null, null, null, NuxeoSession.DEFAULT_CONTEXT);
            fail("creation should fail");
        } catch (CmisRuntimeException e) {
            // ok
        }

        // for local binding, rollback here
        if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }

        try {
            session.getObjectByPath("/" + ThrowExceptionListener.CRASHME_NAME);
            fail("doc should not have been created");
        } catch (CmisObjectNotFoundException e) {
            // ok
        }
        try {
            session.getObjectByPath("/" + ThrowExceptionListener.AFTERCRASH_NAME);
            fail("second doc should not have been created");
        } catch (CmisObjectNotFoundException e) {
            // ok
        }
    }

}
