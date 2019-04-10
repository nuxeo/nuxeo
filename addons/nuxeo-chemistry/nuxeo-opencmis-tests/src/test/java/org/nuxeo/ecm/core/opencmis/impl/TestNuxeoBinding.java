/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.core.opencmis.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.AclCapabilities;
import org.apache.chemistry.opencmis.commons.data.AllowableActions;
import org.apache.chemistry.opencmis.commons.data.ChangeEventInfo;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.commons.data.ObjectInFolderContainer;
import org.apache.chemistry.opencmis.commons.data.ObjectList;
import org.apache.chemistry.opencmis.commons.data.ObjectParentData;
import org.apache.chemistry.opencmis.commons.data.Properties;
import org.apache.chemistry.opencmis.commons.data.PropertyData;
import org.apache.chemistry.opencmis.commons.data.PropertyString;
import org.apache.chemistry.opencmis.commons.data.RenditionData;
import org.apache.chemistry.opencmis.commons.data.RepositoryCapabilities;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinitionContainer;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinitionList;
import org.apache.chemistry.opencmis.commons.enums.AclPropagation;
import org.apache.chemistry.opencmis.commons.enums.Action;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.CapabilityAcl;
import org.apache.chemistry.opencmis.commons.enums.CapabilityChanges;
import org.apache.chemistry.opencmis.commons.enums.CapabilityContentStreamUpdates;
import org.apache.chemistry.opencmis.commons.enums.CapabilityJoin;
import org.apache.chemistry.opencmis.commons.enums.CapabilityQuery;
import org.apache.chemistry.opencmis.commons.enums.CapabilityRenditions;
import org.apache.chemistry.opencmis.commons.enums.ChangeType;
import org.apache.chemistry.opencmis.commons.enums.IncludeRelationships;
import org.apache.chemistry.opencmis.commons.enums.SupportedPermissions;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.exceptions.CmisConstraintException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisInvalidArgumentException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ContentStreamImpl;
import org.apache.chemistry.opencmis.commons.spi.BindingsObjectFactory;
import org.apache.chemistry.opencmis.commons.spi.DiscoveryService;
import org.apache.chemistry.opencmis.commons.spi.Holder;
import org.apache.chemistry.opencmis.commons.spi.MultiFilingService;
import org.apache.chemistry.opencmis.commons.spi.NavigationService;
import org.apache.chemistry.opencmis.commons.spi.ObjectService;
import org.apache.chemistry.opencmis.commons.spi.RepositoryService;
import org.apache.chemistry.opencmis.commons.spi.VersioningService;
import org.apache.chemistry.opencmis.server.support.query.CalendarHelper;
import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoRepository;
import org.nuxeo.ecm.core.opencmis.tests.Helper;
import org.nuxeo.runtime.api.Framework;

/**
 * Tests that hit directly the server APIs.
 */
public class TestNuxeoBinding extends NuxeoBindingTestCase {

    public static final String NUXEO_ROOT_TYPE = "Root"; // from Nuxeo

    public static final String NUXEO_ROOT_NAME = ""; // NuxeoPropertyDataName;

    // stream content with non-ASCII characters
    public static final String STREAM_CONTENT = "Caf\u00e9 Diem\none\0two";

    public static final String COMPLEX_TITLE = "Is this my/your caf\u00e9?";

    protected RepositoryService repoService;

    protected ObjectService objService;

    protected NavigationService navService;

    protected MultiFilingService filingService;

    protected DiscoveryService discService;

    protected VersioningService verService;

    protected String file5id;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        Map<String, String> info = Helper.makeNuxeoRepository(nuxeotc.getSession());
        file5id = info.get("file5id");
    }

    @Override
    public void init() throws Exception {
        super.init();
        repoService = binding.getRepositoryService();
        objService = binding.getObjectService();
        navService = binding.getNavigationService();
        filingService = binding.getMultiFilingService();
        discService = binding.getDiscoveryService();
        verService = binding.getVersioningService();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    protected String createDocument(String name, String folderId, String typeId) {
        return objService.createDocument(repositoryId,
                createBaseDocumentProperties(name, typeId), folderId, null,
                null, null, null, null, null);
    }

    protected String createFolder(String name, String folderId, String typeId) {
        return objService.createFolder(repositoryId,
                createBaseDocumentProperties(name, typeId), folderId, null,
                null, null, null);
    }

    protected Properties createBaseDocumentProperties(String name, String typeId) {
        BindingsObjectFactory factory = binding.getObjectFactory();
        List<PropertyData<?>> props = new ArrayList<PropertyData<?>>();
        props.add(factory.createPropertyIdData(PropertyIds.NAME, name));
        props.add(factory.createPropertyIdData(PropertyIds.OBJECT_TYPE_ID,
                typeId));
        return factory.createPropertiesData(props);
    }

    protected Properties createProperties(String key, String value) {
        BindingsObjectFactory factory = binding.getObjectFactory();
        PropertyString prop = factory.createPropertyStringData(key, value);
        return factory.createPropertiesData(Collections.<PropertyData<?>> singletonList(prop));
    }

    protected ObjectData getObject(String id) {
        return objService.getObject(repositoryId, id, null, Boolean.FALSE,
                IncludeRelationships.NONE, null, Boolean.FALSE, Boolean.FALSE,
                null);
    }

    protected ObjectData getObjectByPath(String path) {
        return objService.getObjectByPath(repositoryId, path, null, null, null,
                null, null, null, null);
    }

    protected ObjectList query(String statement) {
        return discService.query(repositoryId, statement, null, null, null,
                null, null, null, null);
    }

    protected static Object getValue(ObjectData data, String key) {
        return data.getProperties().getProperties().get(key).getFirstValue();
    }

    protected static Object getValues(ObjectData data, String key) {
        return data.getProperties().getProperties().get(key).getValues();
    }

    protected static String getString(ObjectData data, String key) {
        return (String) getValue(data, key);
    }

    protected static Object getQueryValue(ObjectData data, String queryName) {
        Properties properties = data.getProperties();
        for (PropertyData<?> pd : properties.getPropertyList()) {
            if (queryName.equals(pd.getQueryName())) {
                return pd.getFirstValue();
            }
        }
        return null;
    }

    @Test
    public void testGetRepositoryInfos() {
        List<RepositoryInfo> infos = repoService.getRepositoryInfos(null);
        assertEquals(1, infos.size());
        checkInfo(infos.get(0));
    }

    @Test
    public void testGetRepositoryInfo() {
        RepositoryInfo info = repoService.getRepositoryInfo(repositoryId, null);
        checkInfo(info);
    }

    protected void checkInfo(RepositoryInfo info) {
        assertEquals(repositoryId, info.getId());
        assertEquals("Nuxeo Repository " + repositoryId, info.getName());
        assertEquals("Nuxeo Repository " + repositoryId, info.getDescription());
        assertEquals("Nuxeo", info.getVendorName());
        assertEquals("Nuxeo OpenCMIS Connector", info.getProductName());
        String version = Framework.getProperty(
                NuxeoRepository.NUXEO_VERSION_PROP, "5.4 dev");
        assertEquals(version, info.getProductVersion());
        assertEquals(rootFolderId, info.getRootFolderId());
        assertEquals("Guest", info.getPrincipalIdAnonymous());
        assertEquals("1.0", info.getCmisVersionSupported());
        // TODO assertEquals("...", info.getThinClientUri());
        assertNotNull(info.getLatestChangeLogToken());
        assertEquals(Boolean.FALSE, info.getChangesIncomplete());
        assertEquals(
                Arrays.asList(BaseTypeId.CMIS_DOCUMENT, BaseTypeId.CMIS_FOLDER),
                info.getChangesOnType());
        assertEquals(SecurityConstants.EVERYONE, info.getPrincipalIdAnyone());
        RepositoryCapabilities caps = info.getCapabilities();
        assertEquals(CapabilityAcl.NONE, caps.getAclCapability());
        assertEquals(CapabilityChanges.OBJECTIDSONLY,
                caps.getChangesCapability());
        assertEquals(CapabilityContentStreamUpdates.PWCONLY,
                caps.getContentStreamUpdatesCapability());
        assertEquals(CapabilityJoin.INNERANDOUTER, caps.getJoinCapability());
        assertEquals(CapabilityQuery.BOTHCOMBINED, caps.getQueryCapability());
        assertEquals(CapabilityRenditions.READ, caps.getRenditionsCapability());
        AclCapabilities aclCaps = info.getAclCapabilities();
        assertEquals(AclPropagation.REPOSITORYDETERMINED,
                aclCaps.getAclPropagation());
        assertEquals(Collections.emptyMap(), aclCaps.getPermissionMapping());
        assertEquals(Collections.emptyList(), aclCaps.getPermissions());
        assertEquals(SupportedPermissions.BASIC,
                aclCaps.getSupportedPermissions());
    }

    @Test
    public void testGetTypeDefinition() {
        TypeDefinition type;

        type = repoService.getTypeDefinition(repositoryId, "cmis:folder", null);
        assertEquals(Boolean.TRUE, type.isCreatable());
        assertNull(type.getParentTypeId());
        assertEquals("cmis:folder", type.getLocalName());
        assertTrue(type.getPropertyDefinitions().containsKey("dc:title"));

        type = repoService.getTypeDefinition(repositoryId, "Folder", null);
        assertEquals(Boolean.TRUE, type.isCreatable());
        assertEquals("cmis:folder", type.getParentTypeId());
        assertEquals("Folder", type.getLocalName());

        type = repoService.getTypeDefinition(repositoryId, "cmis:document",
                null);
        assertEquals(Boolean.TRUE, type.isCreatable());
        assertNull(type.getParentTypeId());
        assertEquals("cmis:document", type.getLocalName());
        assertTrue(type.getPropertyDefinitions().containsKey("dc:title"));
        assertTrue(type.getPropertyDefinitions().containsKey(
                "cmis:contentStreamFileName"));

        try {
            // nosuchtype, Document is mapped to cmis:document
            repoService.getTypeDefinition(repositoryId, "Document", null);
            fail();
        } catch (CmisInvalidArgumentException e) {
            // ok
        }

        type = repoService.getTypeDefinition(repositoryId, "Note", null);
        assertEquals(Boolean.TRUE, type.isCreatable());
        assertEquals("cmis:document", type.getParentTypeId());
        assertEquals("Note", type.getLocalName());
        assertTrue(type.getPropertyDefinitions().containsKey("note"));
    }

    public List<String> getIds(TypeDefinitionList types) {
        List<String> ids = new ArrayList<String>();
        for (TypeDefinition type : types.getList()) {
            ids.add(type.getId());
        }
        return ids;
    }

    @Test
    public void testGetTypeChildren() {
        TypeDefinitionList types = repoService.getTypeChildren(repositoryId,
                "cmis:folder", Boolean.FALSE, null, null, null);
        for (TypeDefinition type : types.getList()) {
            assertNull(type.getPropertyDefinitions());
        }
        List<String> ids = getIds(types);
        assertTrue(ids.contains("Folder"));
        assertTrue(ids.contains("Root"));
        assertTrue(ids.contains("Domain"));
        assertTrue(ids.contains("OrderedFolder"));
        assertTrue(ids.contains("Workspace"));
        assertTrue(ids.contains("Section"));

        // batching
        types = repoService.getTypeChildren(repositoryId, "cmis:folder",
                Boolean.FALSE, BigInteger.valueOf(4), BigInteger.valueOf(2),
                null);
        List<String> ids2 = getIds(types);
        assertEquals(4, ids2.size());
        assertFalse(ids2.contains(ids.get(0)));
        assertFalse(ids2.contains(ids.get(1)));
        // batching beyond max size
        types = repoService.getTypeChildren(repositoryId, "cmis:folder",
                Boolean.FALSE, BigInteger.valueOf(12), BigInteger.valueOf(5),
                null);
        List<String> ids3 = getIds(types);
        assertEquals(ids.size() - 5, ids3.size());
        assertFalse(ids3.contains(ids.get(0)));
        assertFalse(ids3.contains(ids.get(1)));
        assertFalse(ids3.contains(ids.get(2)));
        assertFalse(ids3.contains(ids.get(3)));
        assertFalse(ids3.contains(ids.get(4)));

        // check property definition inclusion
        types = repoService.getTypeChildren(repositoryId,
                BaseTypeId.CMIS_FOLDER.value(), Boolean.TRUE, null, null, null);
        for (TypeDefinition type : types.getList()) {
            Map<String, PropertyDefinition<?>> pd = type.getPropertyDefinitions();
            assertNotNull(pd);
            // dublincore in all types
            assertTrue(pd.keySet().contains("dc:title"));
        }

        types = repoService.getTypeChildren(repositoryId,
                BaseTypeId.CMIS_DOCUMENT.value(), Boolean.TRUE, null, null,
                null);
        for (TypeDefinition type : types.getList()) {
            Map<String, PropertyDefinition<?>> pd = type.getPropertyDefinitions();
            assertNotNull(pd);
            // dublincore in all types
            assertTrue(pd.keySet().contains("dc:title"));
        }
        ids = getIds(types);
        assertTrue(ids.contains("File"));
        assertTrue(ids.contains("Note"));
        assertTrue(ids.contains("MyDocType"));

        // nonexistent type
        try {
            repoService.getTypeChildren(repositoryId, "nosuchtype",
                    Boolean.TRUE, null, null, null);
            fail();
        } catch (CmisInvalidArgumentException e) {
            // ok
        }
    }

    @Test
    public void testGetTypeDescendants() {
        List<TypeDefinitionContainer> desc = repoService.getTypeDescendants(
                repositoryId, "cmis:folder", null, Boolean.FALSE, null);
        assertTrue(desc.size() > 2);
        TypeDefinition t = null;
        for (TypeDefinitionContainer tc : desc) {
            TypeDefinition type = tc.getTypeDefinition();
            if (type.getId().equals("OrderedFolder")) {
                t = type;
            }
        }
        assertNotNull(t);

        // nonexistent type
        try {
            repoService.getTypeDescendants(repositoryId, "nosuchtype", null,
                    Boolean.FALSE, null);
            fail();
        } catch (CmisInvalidArgumentException e) {
            // ok
        }
    }

    @Test
    public void testRoot() {
        ObjectData root = getObject(rootFolderId);
        assertNotNull(root.getId());
        assertEquals(NUXEO_ROOT_TYPE,
                getString(root, PropertyIds.OBJECT_TYPE_ID));
        assertEquals(NUXEO_ROOT_NAME, getString(root, PropertyIds.NAME));
        assertEquals("/", getString(root, PropertyIds.PATH));

        // root parent
        ObjectData parent = navService.getFolderParent(repositoryId,
                rootFolderId, null, null);
        assertNull(parent);
        List<ObjectParentData> parents = navService.getObjectParents(
                repositoryId, rootFolderId, null, null, null, null, null, null);
        assertEquals(0, parents.size());
    }

    @Test
    public void testGetObjectByPath() {
        ObjectData ob;

        ob = getObjectByPath("/testfolder1/testfile1");
        assertEquals("testfile1_Title", getString(ob, "dc:title"));

        // works by cmis:name too, needed for Adobe Drive 2
        ob = getObjectByPath("/testfolder1_Title/testfile1_Title");
        assertEquals("testfile1_Title", getString(ob, "dc:title"));

        // cannot mix both
        try {
            getObjectByPath("/testfolder1/testfile1_Title");
            fail();
        } catch (CmisObjectNotFoundException e) {
            // ok
        }
    }

    @Test
    public void testCreateDocument() {
        String id;
        ObjectData data;

        id = createDocument("newdoc", rootFolderId, "File");
        assertNotNull(id);
        data = getObject(id);
        assertEquals(id, data.getId());
        assertEquals("newdoc", getString(data, PropertyIds.NAME));

        // creation of a cmis:document (helps simple clients)

        id = createDocument("newdoc2", rootFolderId, "cmis:document");
        assertNotNull(id);
        data = getObject(id);
        assertEquals(id, data.getId());
        assertEquals("newdoc2", getString(data, PropertyIds.NAME));
        assertEquals("File", getString(data, PropertyIds.OBJECT_TYPE_ID));
    }

    @Test
    public void testCreateFolder() {
        String id = createFolder("newfold", rootFolderId, "Folder");
        assertNotNull(id);
        ObjectData data = getObject(id);
        assertEquals(id, data.getId());
        assertEquals("newfold", getString(data, PropertyIds.NAME));

        // creation of a cmis:folder (helps simple clients)

        id = createFolder("newfold2", rootFolderId, "cmis:folder");
        assertNotNull(id);
        data = getObject(id);
        assertEquals(id, data.getId());
        assertEquals("newfold2", getString(data, PropertyIds.NAME));
        assertEquals("Folder", getString(data, PropertyIds.OBJECT_TYPE_ID));
    }

    protected String createDocumentMyDocType() {
        BindingsObjectFactory factory = binding.getObjectFactory();
        List<PropertyData<?>> props = new ArrayList<PropertyData<?>>();
        props.add(factory.createPropertyIdData(PropertyIds.NAME, COMPLEX_TITLE));
        props.add(factory.createPropertyIdData(PropertyIds.OBJECT_TYPE_ID,
                "MyDocType"));
        props.add(factory.createPropertyStringData("my:string", "abc"));
        props.add(factory.createPropertyBooleanData("my:boolean", Boolean.TRUE));
        props.add(factory.createPropertyIntegerData("my:integer",
                BigInteger.valueOf(123)));
        props.add(factory.createPropertyIntegerData("my:long",
                BigInteger.valueOf(123)));
        props.add(factory.createPropertyDecimalData("my:double",
                BigDecimal.valueOf(123.456)));
        GregorianCalendar expectedDate = Helper.getCalendar(2010, 9, 30, 16, 4,
                55);
        props.add(factory.createPropertyDateTimeData("my:date", expectedDate));
        Properties properties = factory.createPropertiesData(props);
        String id = objService.createDocument(repositoryId, properties,
                rootFolderId, null, VersioningState.CHECKEDOUT, null, null,
                null, null);
        assertNotNull(id);
        return id;
    }

    @Test
    public void testCreateDocumentMyDocType() {
        String id = createDocumentMyDocType();
        ObjectData data = getObject(id);
        assertEquals(id, data.getId());
        assertEquals(COMPLEX_TITLE, getString(data, PropertyIds.NAME));
        assertEquals("MyDocType", getString(data, PropertyIds.OBJECT_TYPE_ID));
        assertEquals("abc", getString(data, "my:string"));
        assertEquals(Boolean.TRUE, getValue(data, "my:boolean"));
        assertEquals(BigInteger.valueOf(123), getValue(data, "my:integer"));
        assertEquals(BigInteger.valueOf(123), getValue(data, "my:long"));
        assertEquals(BigDecimal.valueOf(123.456), getValue(data, "my:double"));
        GregorianCalendar date = (GregorianCalendar) getValue(data, "my:date");
        GregorianCalendar expectedDate = Helper.getCalendar(2010, 9, 30, 16, 4,
                55);
        if (!CalendarHelper.toString(expectedDate).equals(
                CalendarHelper.toString(date))) {
            // there may be a timezone difference if the database
            // doesn't store timezones -> try with local timezone
            TimeZone tz = TimeZone.getDefault();
            GregorianCalendar localDate = Helper.getCalendar(2010, 9, 30, 16,
                    4, 55, tz);
            assertEquals(CalendarHelper.toString(localDate),
                    CalendarHelper.toString(date));
        }
        // check path segment created from name/title
        List<ObjectParentData> parents = navService.getObjectParents(
                repositoryId, id, null, null, null, null, Boolean.TRUE, null);
        assertEquals(1, parents.size());
        String pathSegment = parents.get(0).getRelativePathSegment();
        assertEquals(COMPLEX_TITLE.replace("/", "-"), pathSegment);
    }

    @Test
    public void testCreateDocumentWithContentStream() throws Exception {
        // null filename passed on purpose, size ignored by Nuxeo
        ContentStream cs = new ContentStreamImpl(null, "text/plain",
                Helper.FILE1_CONTENT);
        String id = objService.createDocument(repositoryId,
                createBaseDocumentProperties("doc1.txt", "File"), rootFolderId,
                cs, VersioningState.NONE, null, null, null, null);
        assertNotNull(id);
        ObjectData data = getObject(id);
        assertEquals(id, data.getId());
        assertEquals("doc1.txt", getString(data, PropertyIds.NAME));
        cs = objService.getContentStream(repositoryId, id, null, null, null,
                null);
        assertNotNull(cs);
        assertEquals("text/plain", cs.getMimeType());
        assertEquals("doc1.txt", cs.getFileName());
        assertEquals(Helper.FILE1_CONTENT.length(), cs.getLength());
        assertEquals(Helper.FILE1_CONTENT, Helper.read(cs.getStream(), "UTF-8"));
    }

    @Test
    public void testUpdateProperties() throws Exception {
        ObjectData ob = getObjectByPath("/testfolder1/testfile1");
        assertEquals("testfile1_Title", getString(ob, "dc:title"));

        Properties props = createProperties("dc:title", "new title");
        Holder<String> objectIdHolder = new Holder<String>(ob.getId());
        objService.updateProperties(repositoryId, objectIdHolder, null, props,
                null);
        assertEquals(ob.getId(), objectIdHolder.getValue());

        ob = getObject(ob.getId());
        assertEquals("new title", getString(ob, "dc:title"));
    }

    @Test
    public void testGetProperties() throws Exception {
        Properties p;
        ObjectData ob = getObjectByPath("/testfolder1/testfile1");

        p = objService.getProperties(repositoryId, ob.getId(), null, null);
        assertNotNull(p);
        assertEquals("testfile1_Title",
                p.getProperties().get("dc:title").getFirstValue());

        // with filter
        p = objService.getProperties(repositoryId, ob.getId(), "cmis:name",
                null);
        assertNull(p.getProperties().get("dc:title"));
        assertEquals("testfile1_Title",
                p.getProperties().get("cmis:name").getFirstValue());
    }

    @Test
    public void testContentStream() throws Exception {
        ObjectData ob = getObjectByPath("/testfolder1/testfile1");

        // get stream
        ContentStream cs = objService.getContentStream(repositoryId,
                ob.getId(), null, null, null, null);
        assertNotNull(cs);
        assertEquals("text/plain", cs.getMimeType());
        assertEquals("testfile.txt", cs.getFileName());
        assertEquals(Helper.FILE1_CONTENT.length(), cs.getLength());
        assertEquals(Helper.FILE1_CONTENT, Helper.read(cs.getStream(), "UTF-8"));

        // set stream

        cs = new ContentStreamImpl("foo.txt", "text/plain; charset=UTF-8",
                STREAM_CONTENT);
        Holder<String> objectIdHolder = new Holder<String>(ob.getId());
        objService.setContentStream(repositoryId, objectIdHolder, Boolean.TRUE,
                null, cs, null);
        assertEquals(ob.getId(), objectIdHolder.getValue());

        // refetch
        cs = objService.getContentStream(repositoryId, ob.getId(), null, null,
                null, null);
        assertNotNull(cs);
        assertEquals("text/plain; charset=UTF-8", cs.getMimeType());
        assertEquals("foo.txt", cs.getFileName());
        assertEquals(STREAM_CONTENT.getBytes("UTF-8").length, cs.getLength());
        assertEquals(STREAM_CONTENT, Helper.read(cs.getStream(), "UTF-8"));

        // delete
        objService.deleteContentStream(repositoryId, objectIdHolder, null, null);

        // refetch
        try {
            cs = objService.getContentStream(repositoryId, ob.getId(), null,
                    null, null, null);
            fail("Should have no content stream");
        } catch (CmisConstraintException e) {
            // ok
        }
    }

    // flatten and order children
    protected static List<String> flatTree(List<ObjectInFolderContainer> tree)
            throws Exception {
        if (tree == null) {
            return null;
        }
        List<String> r = new LinkedList<String>();
        for (Iterator<ObjectInFolderContainer> it = tree.iterator(); it.hasNext();) {
            ObjectInFolderContainer child = it.next();
            String name = getString(child.getObject().getObject(),
                    PropertyIds.NAME);
            String elem = name;
            List<String> sub = flatTree(child.getChildren());
            if (sub != null) {
                elem += "[" + StringUtils.join(sub, ", ") + "]";
            }
            r.add(elem);
        }
        Collections.sort(r);
        return r;
    }

    protected static String flat(List<ObjectInFolderContainer> tree)
            throws Exception {
        return StringUtils.join(flatTree(tree), ", ");
    }

    @Test
    public void testGetDescendants() throws Exception {
        List<ObjectInFolderContainer> tree;

        try {
            navService.getDescendants(repositoryId, rootFolderId,
                    BigInteger.valueOf(0), null, null, null, null, null, null);
            fail("Depth 0 should be forbidden");
        } catch (CmisInvalidArgumentException e) {
            // ok
        }

        tree = navService.getDescendants(repositoryId, rootFolderId,
                BigInteger.valueOf(1), null, null, null, null, null, null);
        assertEquals("testfolder1_Title, " //
                + "testfolder2_Title", flat(tree));

        tree = navService.getDescendants(repositoryId, rootFolderId,
                BigInteger.valueOf(2), null, null, null, null, null, null);
        assertEquals("testfolder1_Title[" //
                + /* */"testfile1_Title, " //
                + /* */"testfile2_Title, " //
                + /* */"testfile3_Title], " //
                + "testfolder2_Title[" //
                + /* */"testfolder3_Title, " //
                + /* */"testfolder4_Title]", //
                flat(tree));

        tree = navService.getDescendants(repositoryId, rootFolderId,
                BigInteger.valueOf(3), null, null, null, null, null, null);
        assertEquals("testfolder1_Title[" //
                + /* */"testfile1_Title[], " //
                + /* */"testfile2_Title[], " //
                + /* */"testfile3_Title[]], " //
                + "testfolder2_Title[" //
                + /* */"testfolder3_Title[testfile4_Title], " //
                + /* */"testfolder4_Title[]]", //
                flat(tree));

        tree = navService.getDescendants(repositoryId, rootFolderId,
                BigInteger.valueOf(4), null, null, null, null, null, null);
        assertEquals("testfolder1_Title[" //
                + /* */"testfile1_Title[], " //
                + /* */"testfile2_Title[], " //
                + /* */"testfile3_Title[]], " //
                + "testfolder2_Title[" //
                + /* */"testfolder3_Title[testfile4_Title[]], " //
                + /* */"testfolder4_Title[]]", //
                flat(tree));

        tree = navService.getDescendants(repositoryId, rootFolderId,
                BigInteger.valueOf(-1), null, null, null, null, null, null);
        assertEquals("testfolder1_Title[testfile1_Title[], "
                + /* */"testfile2_Title[], " //
                + /* */"testfile3_Title[]], " //
                + "testfolder2_Title[" //
                + /* */"testfolder3_Title[testfile4_Title[]], " //
                + /* */"testfolder4_Title[]]", //
                flat(tree));

        ObjectData ob = getObjectByPath("/testfolder2");
        String folder2Id = ob.getId();

        tree = navService.getDescendants(repositoryId, folder2Id,
                BigInteger.valueOf(1), null, null, null, null, null, null);
        assertEquals("testfolder3_Title, testfolder4_Title", flat(tree));

        tree = navService.getDescendants(repositoryId, folder2Id,
                BigInteger.valueOf(2), null, null, null, null, null, null);
        assertEquals("testfolder3_Title[testfile4_Title], testfolder4_Title[]",
                flat(tree));

        tree = navService.getDescendants(repositoryId, folder2Id,
                BigInteger.valueOf(3), null, null, null, null, null, null);
        assertEquals(
                "testfolder3_Title[testfile4_Title[]], testfolder4_Title[]",
                flat(tree));

        tree = navService.getDescendants(repositoryId, folder2Id,
                BigInteger.valueOf(-1), null, null, null, null, null, null);
        assertEquals(
                "testfolder3_Title[testfile4_Title[]], testfolder4_Title[]",
                flat(tree));
    }

    @Test
    public void testGetFolderTree() throws Exception {
        List<ObjectInFolderContainer> tree;

        try {
            navService.getFolderTree(repositoryId, rootFolderId,
                    BigInteger.valueOf(0), null, null, null, null, null, null);
            fail("Depth 0 should be forbidden");
        } catch (CmisInvalidArgumentException e) {
            // ok
        }

        tree = navService.getFolderTree(repositoryId, rootFolderId,
                BigInteger.valueOf(1), null, null, null, null, null, null);
        assertEquals("testfolder1_Title, " //
                + "testfolder2_Title", flat(tree));

        tree = navService.getFolderTree(repositoryId, rootFolderId,
                BigInteger.valueOf(2), null, null, null, null, null, null);
        assertEquals("testfolder1_Title[], " //
                + "testfolder2_Title[" //
                + /* */"testfolder3_Title, " //
                + /* */"testfolder4_Title]", //
                flat(tree));

        tree = navService.getFolderTree(repositoryId, rootFolderId,
                BigInteger.valueOf(3), null, null, null, null, null, null);
        assertEquals("testfolder1_Title[], " //
                + "testfolder2_Title[" //
                + /* */"testfolder3_Title[], " //
                + /* */"testfolder4_Title[]]", //
                flat(tree));

        tree = navService.getFolderTree(repositoryId, rootFolderId,
                BigInteger.valueOf(4), null, null, null, null, null, null);
        assertEquals("testfolder1_Title[], " //
                + "testfolder2_Title[" //
                + /* */"testfolder3_Title[], " //
                + /* */"testfolder4_Title[]]", //
                flat(tree));

        tree = navService.getFolderTree(repositoryId, rootFolderId,
                BigInteger.valueOf(-1), null, null, null, null, null, null);
        assertEquals("testfolder1_Title[], " //
                + "testfolder2_Title[" //
                + /* */"testfolder3_Title[], " //
                + /* */"testfolder4_Title[]]", //
                flat(tree));

        ObjectData ob = getObjectByPath("/testfolder2");
        String folder2Id = ob.getId();

        tree = navService.getFolderTree(repositoryId, folder2Id,
                BigInteger.valueOf(1), null, null, null, null, null, null);
        assertEquals("testfolder3_Title, testfolder4_Title", flat(tree));

        tree = navService.getFolderTree(repositoryId, folder2Id,
                BigInteger.valueOf(2), null, null, null, null, null, null);
        assertEquals("testfolder3_Title[], testfolder4_Title[]", flat(tree));

        tree = navService.getFolderTree(repositoryId, folder2Id,
                BigInteger.valueOf(3), null, null, null, null, null, null);
        assertEquals("testfolder3_Title[], testfolder4_Title[]", flat(tree));

        tree = navService.getFolderTree(repositoryId, folder2Id,
                BigInteger.valueOf(-1), null, null, null, null, null, null);
        assertEquals("testfolder3_Title[], testfolder4_Title[]", flat(tree));
    }

    @Test
    public void testCreateDocumentFromSource() throws Exception {
        ObjectData ob = getObjectByPath("/testfolder1/testfile1");
        String key = "dc:title";
        String value = "new title";
        Properties props = createProperties(key, value);
        String id = objService.createDocumentFromSource(repositoryId,
                ob.getId(), props, rootFolderId, null, null, null, null, null);
        assertNotNull(id);
        assertNotSame(id, ob.getId());
        // fetch
        ObjectData copy = getObjectByPath("/testfile1");
        assertNotNull(copy);
        assertEquals(value, getString(copy, key));
    }

    @Test
    public void testDeleteObject() throws Exception {
        ObjectData ob = getObjectByPath("/testfolder1/testfile1");
        objService.deleteObject(repositoryId, ob.getId(), Boolean.TRUE, null);
        try {
            ob = getObjectByPath("/testfolder1/testfile1");
            fail("Document should be deleted");
        } catch (CmisObjectNotFoundException e) {
            // ok
        }

        ob = getObjectByPath("/testfolder2");
        try {
            objService.deleteObject(repositoryId, ob.getId(), Boolean.TRUE,
                    null);
            fail("Should not be able to delete non-empty folder");
        } catch (CmisConstraintException e) {
            // ok to fail, still has children
        }
        ob = getObjectByPath("/testfolder2");
        assertNotNull(ob);

        try {
            objService.deleteObject(repositoryId, "nosuchid", Boolean.TRUE,
                    null);
            fail("Should not be able to delete nonexistent object");
        } catch (CmisObjectNotFoundException e) {
            // ok
        }
    }

    @Test
    public void testRemoveObjectFromFolder1() throws Exception {
        ObjectData ob = getObjectByPath("/testfolder1/testfile1");
        filingService.removeObjectFromFolder(repositoryId, ob.getId(), null,
                null);
        try {
            ob = getObjectByPath("/testfolder1/testfile1");
            fail("Document should be deleted");
        } catch (CmisObjectNotFoundException e) {
            // ok
        }
    }

    @Test
    public void testRemoveObjectFromFolder2() throws Exception {
        ObjectData ob = getObjectByPath("/testfolder1/testfile1");
        ObjectData folder = getObjectByPath("/testfolder1");
        filingService.removeObjectFromFolder(repositoryId, ob.getId(),
                folder.getId(), null);
        try {
            ob = getObjectByPath("/testfolder1/testfile1");
            fail("Document should be deleted");
        } catch (CmisObjectNotFoundException e) {
            // ok
        }
    }

    @Test
    public void testDeleteTree() throws Exception {
        ObjectData ob = getObjectByPath("/testfolder1");
        objService.deleteTree(repositoryId, ob.getId(), null, null, null, null);
        try {
            getObjectByPath("/testfolder1");
            fail("Folder should be deleted");
        } catch (CmisObjectNotFoundException e) {
            // ok
        }
        try {
            getObjectByPath("/testfolder1/testfile1");
            fail("Folder should be deleted");
        } catch (CmisObjectNotFoundException e) {
            // ok
        }
        assertNotNull(getObjectByPath("/testfolder2"));
    }

    @Test
    public void testGetAllowableActions() throws Exception {
        Set<Action> expected;
        ObjectData ob;
        AllowableActions aa;

        // folder

        ob = getObjectByPath("/testfolder1");
        aa = objService.getAllowableActions(repositoryId, ob.getId(), null);
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
                Action.CAN_ADD_OBJECT_TO_FOLDER, //
                Action.CAN_REMOVE_OBJECT_FROM_FOLDER, //
                Action.CAN_UPDATE_PROPERTIES, //
                Action.CAN_MOVE_OBJECT, //
                Action.CAN_DELETE_OBJECT);
        assertEquals(expected, aa.getAllowableActions());

        // checked out doc

        ob = getObjectByPath("/testfolder1/testfile1");
        aa = objService.getAllowableActions(repositoryId, ob.getId(), null);
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
                Action.CAN_GET_RENDITIONS, //
                Action.CAN_GET_ALL_VERSIONS, //
                Action.CAN_CANCEL_CHECK_OUT, //
                Action.CAN_CHECK_IN);
        assertEquals(expected, aa.getAllowableActions());

        // checked in doc

        Holder<String> idHolder = new Holder<String>(ob.getId());
        verService.checkIn(repositoryId, idHolder, Boolean.TRUE, null, null,
                "comment", null, null, null, null);

        aa = objService.getAllowableActions(repositoryId, ob.getId(), null);
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
                Action.CAN_GET_RENDITIONS, //
                Action.CAN_GET_ALL_VERSIONS, //
                Action.CAN_CHECK_OUT);
        assertEquals(expected, aa.getAllowableActions());

    }

    @Test
    public void testMoveObject() throws Exception {
        ObjectData fold = getObjectByPath("/testfolder1");
        ObjectData ob = getObjectByPath("/testfolder2/testfolder3/testfile4");
        Holder<String> objectIdHolder = new Holder<String>(ob.getId());
        objService.moveObject(repositoryId, objectIdHolder, fold.getId(), null,
                null);
        assertEquals(ob.getId(), objectIdHolder.getValue());
        try {
            getObjectByPath("/testfolder2/testfolder3/testfile4");
            fail("Object should be moved away");
        } catch (CmisObjectNotFoundException e) {
            // ok
        }
        ObjectData ob2 = getObjectByPath("/testfolder1/testfile4");
        assertEquals(ob.getId(), ob2.getId());
    }

    @Test
    public void testQueryBasic() throws Exception {
        String statement;
        ObjectList res;

        statement = "SELECT cmis:objectId, cmis:name" //
                + " FROM File"; // no WHERE clause
        res = query(statement);
        assertEquals(3, res.getNumItems().intValue());

        statement = "SELECT cmis:objectId, cmis:name" //
                + " FROM File" //
                + " WHERE cmis:name <> 'testfile1_Title'";
        res = query(statement);
        assertEquals(2, res.getNumItems().intValue());

        // spec says names are case-insensitive
        // statement = "SELECT CMIS:OBJECTid, DC:DESCRIPTion" //
        // + " FROM FILE" //
        // + " WHERE DC:TItle = 'testfile1_Title'";
        // res = query(statement);
        // assertEquals(1, res.getNumItems().intValue());

        // STAR
        statement = "SELECT * FROM cmis:document";
        res = query(statement);
        assertEquals(4, res.getNumItems().intValue());
        statement = "SELECT * FROM cmis:folder";
        res = query(statement);
        assertEquals(4, res.getNumItems().intValue());

        statement = "SELECT cmis:objectId, dc:description" //
                + " FROM File" //
                + " WHERE dc:title = 'testfile1_Title'";
        res = query(statement);
        assertEquals(1, res.getNumItems().intValue());

        statement = "SELECT cmis:objectId, dc:description" //
                + " FROM File" //
                + " WHERE dc:title = 'testfile1_Title'"
                + " AND dc:description <> 'argh'"
                + " AND dc:coverage <> 'zzzzz'";
        res = query(statement);
        assertEquals(1, res.getNumItems().intValue());

        // IN
        statement = "SELECT cmis:objectId" //
                + " FROM File" //
                + " WHERE dc:title IN ('testfile1_Title', 'xyz')";
        res = query(statement);
        assertEquals(1, res.getNumItems().intValue());
    }

    protected String NOT_NULL = new String("__NOTNULL__");

    protected void checkWhereTerm(String type, String prop, String value) {
        if (value == NOT_NULL) {
            checkQueriedValue(type, prop + " IS NOT NULL");
        } else {
            checkQueriedValue(type, prop + " = " + value);
        }
    }

    @SuppressWarnings("boxing")
    protected void checkQueriedValue(String type, String term) {
        String statement = String.format(
                "SELECT cmis:objectId FROM %s WHERE %s", type, term);
        ObjectList res = query(statement);
        assertNotSame(0, res.getNumItems().intValue());
    }

    @Test
    public void testQueryWhereProperties() throws Exception {
        String statement;
        ObjectList res;

        createDocumentMyDocType();

        // STAR
        statement = "SELECT * FROM MyDocType";
        res = query(statement);
        assertEquals(1, res.getNumItems().intValue());

        checkQueriedValue("MyDocType", "my:string = 'abc'");
        checkQueriedValue("MyDocType", "my:string <> 'def'");
        checkQueriedValue("MyDocType", "my:boolean = true");
        checkQueriedValue("MyDocType", "my:boolean <> FALSE");
        checkQueriedValue("MyDocType", "my:integer = 123");
        checkQueriedValue("MyDocType", "my:integer <> 456");
        checkQueriedValue("MyDocType", "my:double = 123.456");
        checkQueriedValue("MyDocType", "my:double <> 123");
        checkQueriedValue("MyDocType",
                "my:date = TIMESTAMP '2010-09-30T16:04:55-02:00'");
        checkQueriedValue("MyDocType",
                "my:date <> TIMESTAMP '1999-09-09T01:01:01Z'");
        try {
            statement = "SELECT cmis:objectId FROM MyDocType WHERE my:date <> TIMESTAMP 'foobar'";
            query(statement);
            fail("Should be invalid Timestamp");
        } catch (CmisRuntimeException e) {
            // ok
        }
    }

    @Test
    public void testQueryWhereSystemProperties() throws Exception {

        // ----- Object -----

        checkWhereTerm("File", PropertyIds.NAME, "'testfile1_Title'");
        checkWhereTerm("File", PropertyIds.OBJECT_ID, NOT_NULL);
        checkWhereTerm("File", PropertyIds.OBJECT_TYPE_ID, "'File'");
        // checkWhereTerm("File", PropertyIds.BASE_TYPE_ID,
        // "'cmis:document'");
        checkWhereTerm("File", PropertyIds.CREATED_BY, "'michael'");
        checkWhereTerm("File", PropertyIds.CREATION_DATE, NOT_NULL);
        checkWhereTerm("File", PropertyIds.LAST_MODIFIED_BY, "'bob'");
        checkWhereTerm("File", PropertyIds.LAST_MODIFICATION_DATE, NOT_NULL);
        // checkWhereTerm("File", PropertyIds.CHANGE_TOKEN, null);

        // ----- Folder -----

        checkWhereTerm("Folder", PropertyIds.PARENT_ID, NOT_NULL);
        // checkWhereTerm("Folder", PropertyIds.PATH, NOT_NULL);
        // checkWhereTerm("Folder", PropertyIds.ALLOWED_CHILD_OBJECT_TYPE_IDS,
        // NOT_NULL);

        // ----- Document -----

        // checkWhereTerm("File", PropertyIds.IS_IMMUTABLE, "FALSE");
        // checkWhereTerm("File", PropertyIds.IS_LATEST_VERSION, "TRUE");
        // checkWhereTerm("File", PropertyIds.IS_MAJOR_VERSION, "TRUE");
        // checkWhereTerm("File", PropertyIds.IS_LATEST_MAJOR_VERSION, "FALSE");
        // checkWhereTerm("File", PropertyIds.VERSION_LABEL, NOT_NULL);
        // checkWhereTerm("File", PropertyIds.VERSION_SERIES_ID, NOT_NULL);
        // checkWhereTerm("File", PropertyIds.VERSION_SERIES_CHECKED_OUT_BY,
        // NOT_NULL);
        // checkWhereTerm("File", PropertyIds.VERSION_SERIES_CHECKED_OUT_ID,
        // NOT_NULL);
        // checkWhereTerm("File", PropertyIds.CHECKIN_COMMENT, "xyz");
        // checkWhereTerm("File", PropertyIds.CONTENT_STREAM_LENGTH, NOT_NULL);
        // checkWhereTerm("File", PropertyIds.CONTENT_STREAM_MIME_TYPE,
        // "text/plain");
        // checkWhereTerm("File", PropertyIds.CONTENT_STREAM_FILE_NAME,
        // "testfile.txt");
        // checkWhereTerm("File", PropertyIds.CONTENT_STREAM_ID, NOT_NULL);
    }

    protected void checkReturnedValue(String prop, Object expected) {
        checkReturnedValue(prop, expected, "File", "testfile1_Title");
    }

    protected void checkReturnedValue(String prop, Object expected,
            String type, String name) {
        String statement = String.format(
                "SELECT %s FROM %s WHERE cmis:name = '%s'", prop, type, name);
        ObjectList res = query(statement);
        assertEquals(1, res.getNumItems().intValue());
        ObjectData data = res.getObjects().get(0);
        checkValue(prop, expected, data);
    }

    protected void checkValue(String prop, Object expected, ObjectData data) {
        Object value = expected instanceof List ? getValues(data, prop)
                : getValue(data, prop);
        if (expected == NOT_NULL) {
            assertNotNull(value);
        } else {
            assertEquals(expected, value);
        }
    }

    @Test
    public void testQueryReturnedProperties() throws Exception {
        checkReturnedValue("dc:title", "testfile1_Title");
        checkReturnedValue("dc:modified", NOT_NULL);
        checkReturnedValue("dc:lastContributor", "john");
        // multi-valued
        checkReturnedValue("dc:subjects", Arrays.asList("foo", "gee/moo"));
        checkReturnedValue("dc:contributors", Arrays.asList("pete", "bob"),
                "File", "testfile2_Title");
    }

    @Test
    public void testQueryReturnedSystemProperties() throws Exception {

        // ----- Object -----

        checkReturnedValue(PropertyIds.NAME, "testfile1_Title");
        checkReturnedValue(PropertyIds.OBJECT_ID, NOT_NULL);
        checkReturnedValue(PropertyIds.OBJECT_TYPE_ID, "File");
        checkReturnedValue(PropertyIds.BASE_TYPE_ID, "cmis:document");
        checkReturnedValue(PropertyIds.CREATED_BY, "michael");
        checkReturnedValue(PropertyIds.CREATION_DATE, NOT_NULL);
        checkReturnedValue(PropertyIds.LAST_MODIFIED_BY, "john");
        checkReturnedValue(PropertyIds.LAST_MODIFICATION_DATE, NOT_NULL);
        checkReturnedValue(PropertyIds.CHANGE_TOKEN, null);

        // ----- Folder -----

        checkReturnedValue(PropertyIds.PARENT_ID, rootFolderId, "Folder",
                "testfolder1_Title");
        checkReturnedValue(PropertyIds.PATH, "/testfolder1", "Folder",
                "testfolder1_Title");
        checkReturnedValue(PropertyIds.ALLOWED_CHILD_OBJECT_TYPE_IDS, null,
                "Folder", "testfolder1_Title");

        // ----- Document -----

        checkReturnedValue(PropertyIds.IS_IMMUTABLE, Boolean.FALSE);
        checkReturnedValue(PropertyIds.IS_LATEST_VERSION, Boolean.FALSE);
        checkReturnedValue(PropertyIds.IS_MAJOR_VERSION, Boolean.FALSE);
        checkReturnedValue(PropertyIds.IS_LATEST_MAJOR_VERSION, Boolean.FALSE);
        checkReturnedValue(PropertyIds.VERSION_LABEL, null);
        checkReturnedValue(PropertyIds.VERSION_SERIES_ID, NOT_NULL);
        checkReturnedValue(PropertyIds.IS_VERSION_SERIES_CHECKED_OUT,
                Boolean.TRUE);
        checkReturnedValue(PropertyIds.VERSION_SERIES_CHECKED_OUT_ID, NOT_NULL);
        checkReturnedValue(PropertyIds.VERSION_SERIES_CHECKED_OUT_BY, "system");
        checkReturnedValue(PropertyIds.CHECKIN_COMMENT, null);
        checkReturnedValue(
                PropertyIds.CONTENT_STREAM_LENGTH,
                new ContentStreamImpl(null, "text/plain", Helper.FILE1_CONTENT).getBigLength());
        checkReturnedValue(PropertyIds.CONTENT_STREAM_MIME_TYPE, "text/plain");
        checkReturnedValue(PropertyIds.CONTENT_STREAM_FILE_NAME, "testfile.txt");
        checkReturnedValue(PropertyIds.CONTENT_STREAM_ID, null);
    }

    @Test
    public void testQueryAny() throws Exception {
        String statement;
        ObjectList res;

        // ... = ANY ...
        statement = "SELECT cmis:name FROM File WHERE 'pete' = ANY dc:contributors";
        res = query(statement);
        assertEquals(1, res.getNumItems().intValue());
        statement = "SELECT cmis:name FROM File WHERE 'bob' = ANY dc:contributors";
        res = query(statement);
        assertEquals(1, res.getNumItems().intValue());

        // ANY ... IN ...
        statement = "SELECT cmis:name FROM File WHERE ANY dc:contributors IN ('pete')";
        res = query(statement);
        assertEquals(1, res.getNumItems().intValue());
        statement = "SELECT cmis:name FROM File WHERE ANY dc:contributors IN ('pete', 'bob')";
        res = query(statement);
        assertEquals(1, res.getNumItems().intValue());

        // ANY ... NOT IN ...
        statement = "SELECT cmis:name FROM File WHERE ANY dc:contributors NOT IN ('pete')";
        res = query(statement);
        assertEquals(1, res.getNumItems().intValue());
        statement = "SELECT cmis:name FROM File WHERE ANY dc:contributors NOT IN ('john')";
        res = query(statement);
        assertEquals(1, res.getNumItems().intValue());
        statement = "SELECT cmis:name FROM File WHERE ANY dc:contributors NOT IN ('pete', 'bob')";
        res = query(statement);
        assertEquals(0, res.getNumItems().intValue());
    }

    @Test
    public void testQueryOrderBy() throws Exception {
        String statement;
        ObjectList res;
        ObjectData data;

        statement = "SELECT cmis:objectId, cmis:name" //
                + " FROM File" //
                + " ORDER BY cmis:name";
        res = query(statement);
        assertEquals(3, res.getNumItems().intValue());
        data = res.getObjects().get(0);
        assertEquals("testfile1_Title", getString(data, PropertyIds.NAME));

        // now change order
        res = query(statement + " DESC");
        assertEquals(3, res.getNumItems().intValue());
        data = res.getObjects().get(0);
        assertEquals("testfile4_Title", getString(data, PropertyIds.NAME));
    }

    @Test
    public void testQueryInFolder() throws Exception {
        ObjectData f1 = getObjectByPath("/testfolder1");
        String statementPattern = "SELECT cmis:name FROM File" //
                + " WHERE IN_FOLDER('%s')" //
                + " ORDER BY cmis:name";
        String statement = String.format(statementPattern, f1.getId());
        ObjectList res = query(statement);
        assertEquals(2, res.getNumItems().intValue());
        assertEquals("testfile1_Title",
                getString(res.getObjects().get(0), PropertyIds.NAME));
        assertEquals("testfile2_Title",
                getString(res.getObjects().get(1), PropertyIds.NAME));

        // missing/illegal ID
        statement = String.format(statementPattern, "nosuchid");
        res = query(statement);
        assertEquals(0, res.getNumItems().intValue());
    }

    @Test
    public void testQueryInTree() throws Exception {
        ObjectList res;
        String statement;

        ObjectData f2 = getObjectByPath("/testfolder2");
        String statementPattern = "SELECT cmis:name FROM File" //
                + " WHERE IN_TREE('%s')";

        statement = String.format(statementPattern, f2.getId());
        res = query(statement);
        assertEquals(1, res.getNumItems().intValue());
        assertEquals("testfile4_Title",
                getString(res.getObjects().get(0), PropertyIds.NAME));

        // missing/illegal ID
        statement = String.format(statementPattern, "nosuchid");
        res = query(statement);
        assertEquals(0, res.getNumItems().intValue());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testQueryContains() throws Exception {

        ObjectData ob = getObjectByPath("/testfolder1/testfile1");
        assertEquals("testfile1_Title", getString(ob, "dc:title"));

        BindingsObjectFactory factory = binding.getObjectFactory();
        PropertyData<?> propTitle = factory.createPropertyStringData(
                "dc:title", "new title1");
        PropertyData<?> propDescription = factory.createPropertyStringData(
                "dc:description", "new description1");
        Properties properties = factory.createPropertiesData(Arrays.asList(
                propTitle, propDescription));

        Holder<String> objectIdHolder = new Holder<String>(ob.getId());
        objService.updateProperties(repositoryId, objectIdHolder, null,
                properties, null);

        ObjectList res;
        String statement;

        statement = "SELECT cmis:name FROM File WHERE CONTAINS('title1')";
        res = query(statement);
        assertEquals(1, res.getNumItems().intValue());
        assertEquals("new title1",
                getString(res.getObjects().get(0), PropertyIds.NAME));

        statement = "SELECT cmis:name FROM File"
                + " WHERE CONTAINS('description1')";
        res = query(statement);
        assertEquals(1, res.getNumItems().intValue());
        assertEquals("new title1",
                getString(res.getObjects().get(0), PropertyIds.NAME));

        // specific query for title index (the description token do not match)
        statement = "SELECT cmis:name FROM File"
                + " WHERE CONTAINS('nx:title:description1')";
        res = query(statement);
        assertEquals(0, res.getNumItems().intValue());

        statement = "SELECT cmis:name FROM File"
                + " WHERE CONTAINS('nx:title:title1')";
        res = query(statement);
        assertEquals(1, res.getNumItems().intValue());
        assertEquals("new title1",
                getString(res.getObjects().get(0), PropertyIds.NAME));

        // specific query for invalid index name should not break (but log a
        // warning instead and fallback to the default index)
        statement = "SELECT cmis:name FROM File" //
                + " WHERE CONTAINS('nx:borked:title1')";
        res = query(statement);
        assertEquals(1, res.getNumItems().intValue());
        assertEquals("new title1",
                getString(res.getObjects().get(0), PropertyIds.NAME));
    }

    @Test
    public void testQueryScore() throws Exception {
        ObjectList res;
        String statement;
        ObjectData data;

        statement = "SELECT cmis:name, SCORE() FROM File" //
                + " WHERE CONTAINS('testfile1_Title')";
        res = query(statement);
        assertEquals(1, res.getNumItems().intValue());
        data = res.getObjects().get(0);
        assertEquals("testfile1_Title", getString(data, PropertyIds.NAME));
        assertNotNull(getValue(data, "SEARCH_SCORE")); // name from spec

        // using an alias for the score
        statement = "SELECT cmis:name, SCORE() AS priority FROM File" //
                + " WHERE CONTAINS('testfile1_Title')";
        res = query(statement);
        assertEquals(1, res.getNumItems().intValue());
        data = res.getObjects().get(0);
        assertEquals("testfile1_Title", getString(data, PropertyIds.NAME));
        assertNotNull(getValue(data, "priority"));

        // ORDER BY score
        statement = "SELECT cmis:name, SCORE() importance FROM File" //
                + " WHERE CONTAINS('testfile1_Title')" //
                + " ORDER BY importance DESC";
        res = query(statement);
        assertEquals(1, res.getNumItems().intValue());
        data = res.getObjects().get(0);
        assertEquals("testfile1_Title", getString(data, PropertyIds.NAME));
        assertNotNull(getValue(data, "importance"));
    }

    @Test
    public void testQueryJoin() throws Exception {
        String statement;
        ObjectList res;
        ObjectData data;

        String folder2id = getObjectByPath("/testfolder2").getId();
        String folder3id = getObjectByPath("/testfolder2/testfolder3").getId();
        String folder4id = getObjectByPath("/testfolder2/testfolder4").getId();

        statement = "SELECT A.cmis:objectId, A.dc:title, B.cmis:objectId, B.dc:title" //
                + " FROM cmis:folder A" //
                + " JOIN cmis:folder B ON A.cmis:objectId = B.cmis:parentId" //
                + " WHERE A.cmis:name = 'testfolder2_Title'" //
                + " ORDER BY B.dc:title";
        res = query(statement);
        assertEquals(2, res.getNumItems().intValue());

        data = res.getObjects().get(0);
        assertEquals(folder2id, getQueryValue(data, "A.cmis:objectId"));
        assertEquals("testfolder2_Title", getQueryValue(data, "A.dc:title"));
        assertEquals(folder3id, getQueryValue(data, "B.cmis:objectId"));
        assertEquals("testfolder3_Title", getQueryValue(data, "B.dc:title"));

        data = res.getObjects().get(1);
        assertEquals(folder2id, getQueryValue(data, "A.cmis:objectId"));
        assertEquals("testfolder2_Title", getQueryValue(data, "A.dc:title"));
        assertEquals(folder4id, getQueryValue(data, "B.cmis:objectId"));
        assertEquals("testfolder4_Title", getQueryValue(data, "B.dc:title"));
    }

    @Test
    public void testQueryJoinWithSecurity() throws Exception {
        nuxeotc.closeSession();
        nuxeotc.session = nuxeotc.openSessionAs("bob");
        init();

        String statement = "SELECT A.cmis:objectId, A.dc:title, B.cmis:objectId, B.dc:title" //
                + " FROM cmis:folder A" //
                + " JOIN cmis:folder B ON A.cmis:objectId = B.cmis:parentId" //
                + " WHERE A.cmis:name = 'testfolder2_Title'" //
                + " ORDER BY B.dc:title";
        ObjectList res = query(statement);
        assertEquals(0, res.getNumItems().intValue());
    }

    @Test
    public void testQueryBad() throws Exception {
        try {
            query("SELECT foo bar baz");
            fail();
        } catch (CmisRuntimeException e) {
            assertTrue(e.getMessage().contains(
                    "line 1:15 missing FROM at 'baz'"));
        }
        try {
            query("SELECT foo FROM bar");
            fail();
        } catch (CmisRuntimeException e) {
            assertTrue(e.getMessage().contains(
                    "bar is neither a type query name nor an alias"));
        }
        try {
            query("SELECT foo FROM cmis:folder");
            fail();
        } catch (CmisRuntimeException e) {
            assertTrue(e.getMessage().contains(
                    "foo is not a property query name in any of the types"));
        }
    }

    @Test
    public void testQueryBatching() throws Exception {
        int NUM = 20;
        for (int i = 0; i < NUM; i++) {
            String name = String.format("somedoc%03d", Integer.valueOf(i));
            objService.createDocument(repositoryId,
                    createBaseDocumentProperties(name, "cmis:document"),
                    rootFolderId, null, VersioningState.CHECKEDOUT, null, null,
                    null, null);
        }
        ObjectList res;
        List<ObjectData> objects;
        String statement = "SELECT cmis:name FROM cmis:document"
                + " WHERE cmis:name LIKE 'somedoc%' ORDER BY cmis:name";
        res = discService.query(repositoryId, statement, null, null, null,
                null, null, null, null);
        assertEquals(NUM, res.getNumItems().intValue());
        objects = res.getObjects();
        assertEquals(NUM, objects.size());
        assertEquals("somedoc000", getString(objects.get(0), PropertyIds.NAME));
        assertEquals("somedoc019",
                getString(objects.get(objects.size() - 1), PropertyIds.NAME));
        // batch
        res = discService.query(repositoryId, statement, null, null, null,
                null, BigInteger.valueOf(10), BigInteger.valueOf(5), null);
        assertEquals(NUM, res.getNumItems().intValue());
        objects = res.getObjects();
        assertEquals(10, objects.size());
        assertEquals("somedoc005", getString(objects.get(0), PropertyIds.NAME));
        assertEquals("somedoc014",
                getString(objects.get(objects.size() - 1), PropertyIds.NAME));
    }

    @Test
    public void testVersioning() throws Exception {
        ObjectData ob = getObjectByPath("/testfolder1/testfile1");
        String id = ob.getId();

        // checked out

        checkValue(PropertyIds.IS_LATEST_VERSION, Boolean.FALSE, ob);
        checkValue(PropertyIds.IS_MAJOR_VERSION, Boolean.FALSE, ob);
        checkValue(PropertyIds.IS_LATEST_MAJOR_VERSION, Boolean.FALSE, ob);
        checkValue(PropertyIds.VERSION_LABEL, null, ob);
        checkValue(PropertyIds.VERSION_SERIES_ID, NOT_NULL, ob);
        checkValue(PropertyIds.IS_VERSION_SERIES_CHECKED_OUT, Boolean.TRUE, ob);
        checkValue(PropertyIds.VERSION_SERIES_CHECKED_OUT_ID, id, ob);
        checkValue(PropertyIds.VERSION_SERIES_CHECKED_OUT_BY, "system", ob);
        checkValue(PropertyIds.CHECKIN_COMMENT, null, ob);
        String series = (String) getValue(ob, PropertyIds.VERSION_SERIES_ID);

        // check in major -> version 1.0

        Holder<String> idHolder = new Holder<String>(id);
        verService.checkIn(repositoryId, idHolder, Boolean.TRUE, null, null,
                "comment", null, null, null, null);

        String vid = idHolder.getValue();
        ObjectData ver = getObject(vid);

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

        ObjectData ci = getObject(id);

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

        Holder<Boolean> cchold = new Holder<Boolean>();
        verService.checkOut(repositoryId, idHolder, null, cchold);
        String coid = idHolder.getValue();
        ObjectData co = getObject(coid);

        assertEquals(id, coid); // Nuxeo invariant
        assertEquals(Boolean.TRUE, cchold.getValue()); // copied
        checkValue(PropertyIds.IS_LATEST_VERSION, Boolean.FALSE, co);
        checkValue(PropertyIds.IS_MAJOR_VERSION, Boolean.FALSE, co);
        checkValue(PropertyIds.IS_LATEST_MAJOR_VERSION, Boolean.FALSE, co);
        checkValue(PropertyIds.VERSION_LABEL, null, co);
        checkValue(PropertyIds.VERSION_SERIES_ID, series, co);
        checkValue(PropertyIds.IS_VERSION_SERIES_CHECKED_OUT, Boolean.TRUE, co);
        checkValue(PropertyIds.VERSION_SERIES_CHECKED_OUT_ID, coid, co);
        checkValue(PropertyIds.VERSION_SERIES_CHECKED_OUT_BY, "system", co);
        checkValue(PropertyIds.CHECKIN_COMMENT, null, co);

        // check in minor -> version 1.1

        idHolder.setValue(coid);
        verService.checkIn(repositoryId, idHolder, Boolean.FALSE, null, null,
                "comment2", null, null, null, null);

        String v2id = idHolder.getValue();
        ObjectData ver2 = getObject(v2id);

        checkValue(PropertyIds.IS_LATEST_VERSION, Boolean.TRUE, ver2);
        checkValue(PropertyIds.IS_MAJOR_VERSION, Boolean.FALSE, ver2);
        checkValue(PropertyIds.IS_LATEST_MAJOR_VERSION, Boolean.FALSE, ver2);
        checkValue(PropertyIds.VERSION_LABEL, "1.1", ver2);
        checkValue(PropertyIds.VERSION_SERIES_ID, series, ver2);
        checkValue(PropertyIds.IS_VERSION_SERIES_CHECKED_OUT, Boolean.FALSE,
                ver2);
        checkValue(PropertyIds.VERSION_SERIES_CHECKED_OUT_ID, null, ver2);
        checkValue(PropertyIds.VERSION_SERIES_CHECKED_OUT_BY, null, ver2);
        checkValue(PropertyIds.CHECKIN_COMMENT, "comment2", ver2);

        // check out again (with no content copied holder)

        verService.checkOut(repositoryId, idHolder, null, null);
        coid = idHolder.getValue();
        co = getObject(coid);
        assertEquals(id, coid); // Nuxeo invariant

        // cancel check out

        verService.cancelCheckOut(repositoryId, coid, null);
        checkValue(PropertyIds.IS_VERSION_SERIES_CHECKED_OUT, Boolean.FALSE,
                ver2);
        ci = getObject(id);
        checkValue(PropertyIds.IS_LATEST_VERSION, Boolean.TRUE, ci);

        // list all versions
        // TODO check this when no live document exists

        // atompub passes just object id, soap just version series id
        List<ObjectData> vers = verService.getAllVersions(null, id, null, null,
                null, null);
        assertEquals(2, vers.size());
        assertEquals(ver2.getId(), vers.get(0).getId());
        assertEquals(ver.getId(), vers.get(1).getId());

        // get latest version

        Boolean major = Boolean.FALSE;
        ObjectData l = verService.getObjectOfLatestVersion(id, id, null, major,
                null, null, null, null, null, null, null);
        assertEquals(ver2.getId(), l.getId());
        major = Boolean.TRUE;
        l = verService.getObjectOfLatestVersion(id, id, null, major, null,
                null, null, null, null, null, null);
        assertEquals(ver.getId(), l.getId());

        major = Boolean.FALSE;
        Properties p = verService.getPropertiesOfLatestVersion(repositoryId,
                id, null, major, null, null);
        assertEquals(ver2.getId(),
                p.getProperties().get(PropertyIds.OBJECT_ID).getFirstValue());
    }

    @Test
    public void testVersioningInitialState() {

        // creation as major version (default, per spec)

        String id = objService.createDocument(repositoryId,
                createBaseDocumentProperties("newdoc2", "cmis:document"),
                rootFolderId, null, VersioningState.MAJOR, null, null, null,
                null);
        ObjectData ob = getObject(id);

        checkValue(PropertyIds.IS_LATEST_VERSION, Boolean.TRUE, ob);
        checkValue(PropertyIds.IS_MAJOR_VERSION, Boolean.TRUE, ob);
        checkValue(PropertyIds.IS_LATEST_MAJOR_VERSION, Boolean.TRUE, ob);
        checkValue(PropertyIds.VERSION_LABEL, "1.0", ob);
        checkValue(PropertyIds.VERSION_SERIES_ID, NOT_NULL, ob);
        checkValue(PropertyIds.IS_VERSION_SERIES_CHECKED_OUT, Boolean.FALSE, ob);
        checkValue(PropertyIds.VERSION_SERIES_CHECKED_OUT_ID, null, ob);
        checkValue(PropertyIds.VERSION_SERIES_CHECKED_OUT_BY, null, ob);
        checkValue(PropertyIds.CHECKIN_COMMENT, null, ob);

        // creation as minor version

        id = objService.createDocument(repositoryId,
                createBaseDocumentProperties("newdoc2", "cmis:document"),
                rootFolderId, null, VersioningState.MINOR, null, null, null,
                null);
        ob = getObject(id);

        checkValue(PropertyIds.IS_LATEST_VERSION, Boolean.TRUE, ob);
        checkValue(PropertyIds.IS_MAJOR_VERSION, Boolean.FALSE, ob);
        checkValue(PropertyIds.IS_LATEST_MAJOR_VERSION, Boolean.FALSE, ob);
        checkValue(PropertyIds.VERSION_LABEL, "0.1", ob);
        checkValue(PropertyIds.VERSION_SERIES_ID, NOT_NULL, ob);
        checkValue(PropertyIds.IS_VERSION_SERIES_CHECKED_OUT, Boolean.FALSE, ob);
        checkValue(PropertyIds.VERSION_SERIES_CHECKED_OUT_ID, null, ob);
        checkValue(PropertyIds.VERSION_SERIES_CHECKED_OUT_BY, null, ob);
        checkValue(PropertyIds.CHECKIN_COMMENT, null, ob);

        // creation checked out

        id = objService.createDocument(repositoryId,
                createBaseDocumentProperties("newdoc3", "cmis:document"),
                rootFolderId, null, VersioningState.CHECKEDOUT, null, null,
                null, null);
        ob = getObject(id);

        checkValue(PropertyIds.IS_LATEST_VERSION, Boolean.FALSE, ob);
        checkValue(PropertyIds.IS_MAJOR_VERSION, Boolean.FALSE, ob);
        checkValue(PropertyIds.IS_LATEST_MAJOR_VERSION, Boolean.FALSE, ob);
        checkValue(PropertyIds.VERSION_LABEL, null, ob);
        checkValue(PropertyIds.VERSION_SERIES_ID, NOT_NULL, ob);
        checkValue(PropertyIds.IS_VERSION_SERIES_CHECKED_OUT, Boolean.TRUE, ob);
        checkValue(PropertyIds.VERSION_SERIES_CHECKED_OUT_ID, id, ob);
        checkValue(PropertyIds.VERSION_SERIES_CHECKED_OUT_BY, "system", ob);
        checkValue(PropertyIds.CHECKIN_COMMENT, null, ob);

    }

    @Test
    public void testRenditions() throws Exception {
        ObjectData ob = getObjectByPath("/testfolder1/testfile1");

        // list renditions

        List<RenditionData> renditions = objService.getRenditions(repositoryId,
                ob.getId(), null, null, null, null);
        assertEquals(1, renditions.size());
        RenditionData ren = renditions.get(0);
        assertEquals("cmis:thumbnail", ren.getKind());
        assertEquals("nx:icon", ren.getStreamId()); // nuxeo
        assertEquals("image/png", ren.getMimeType());
        assertEquals("text.png", ren.getTitle());

        // get rendition stream
        ContentStream cs = objService.getContentStream(repositoryId,
                ob.getId(), ren.getStreamId(), null, null, null);
        assertEquals("image/png", cs.getMimeType());
        assertEquals("text.png", cs.getFileName());
        assertEquals(394, cs.getBigLength().longValue());
    }

    @Ignore
    // has some problems when run from maven
    @Test
    public void testGetContentChanges() throws Exception {
        ObjectList changes;
        List<ObjectData> objects;
        Holder<String> changeLogTokenHolder = new Holder<String>();

        sleepForAudit();
        String clt1 = repoService.getRepositoryInfo(repositoryId, null).getLatestChangeLogToken();
        assertNotNull(clt1);

        // read all log
        List<ObjectData> allObjects = new ArrayList<ObjectData>();
        changeLogTokenHolder.setValue(null); // start at beginning
        boolean skipFirst = false;
        do {
            int maxItems = 5;
            changes = discService.getContentChanges(repositoryId,
                    changeLogTokenHolder, Boolean.TRUE, null, null, null,
                    BigInteger.valueOf(maxItems), null);
            objects = changes.getObjects();
            if (skipFirst) {
                // already got the first one as part of the last batch
                objects = objects.subList(1, objects.size());
            }
            allObjects.addAll(objects);
            skipFirst = true;
        } while (Boolean.TRUE.equals(changes.hasMoreItems()));
        assertEquals(clt1, changeLogTokenHolder.getValue());

        assertTrue(allObjects.size() >= 10);
        objects = allObjects.subList(allObjects.size() - 10, allObjects.size());
        checkChange(objects.get(0), "/testfolder1", //
                ChangeType.CREATED, "Folder");
        checkChange(objects.get(1), "/testfolder1/testfile1",
                ChangeType.CREATED, "File");
        checkChange(objects.get(2), "/testfolder1/testfile2",
                ChangeType.CREATED, "File");
        checkChange(objects.get(3), "/testfolder1/testfile3",
                ChangeType.CREATED, "Note");
        checkChange(objects.get(4), "/testfolder2", //
                ChangeType.CREATED, "Folder");
        checkChange(objects.get(5), "/testfolder2/testfolder3",
                ChangeType.CREATED, "Folder");
        checkChange(objects.get(6), "/testfolder2/testfolder4",
                ChangeType.CREATED, "Folder");
        checkChange(objects.get(7), "/testfolder2/testfolder3/testfile4",
                ChangeType.CREATED, "File");
        checkChange(objects.get(8), file5id, ChangeType.CREATED, "File");
        checkChange(objects.get(9), file5id, ChangeType.UPDATED, "File");

        // remove a doc

        ObjectData ob1 = getObjectByPath("/testfolder1/testfile1");
        objService.deleteObject(repositoryId, ob1.getId(), Boolean.TRUE, null);

        // get latest change log token
        sleepForAudit();
        String clt2 = repoService.getRepositoryInfo(repositoryId, null).getLatestChangeLogToken();
        assertNotNull(clt2);
        assertNotSame(clt2, clt1);

        changeLogTokenHolder.setValue(clt2); // just the last
        changes = discService.getContentChanges(repositoryId,
                changeLogTokenHolder, Boolean.TRUE, null, null, null,
                BigInteger.valueOf(100), null);
        objects = changes.getObjects();
        assertEquals(1, objects.size());
        checkChange(objects.get(0), ob1.getId(), ChangeType.DELETED, "File");
    }

    protected void sleepForAudit() throws InterruptedException {
        Thread.sleep(5 * 1000); // wait for audit log to catch up
    }

    protected void checkChange(ObjectData data, String id,
            ChangeType changeType, String type) throws Exception {
        Map<String, PropertyData<?>> properties;
        ChangeEventInfo cei;
        cei = data.getChangeEventInfo();
        properties = data.getProperties().getProperties();
        String expectedId = id.startsWith("/") ? getObjectByPath(id).getId()
                : id;
        assertEquals(expectedId, data.getId());
        assertEquals(changeType, cei.getChangeType());
        assertEquals(type,
                properties.get(PropertyIds.OBJECT_TYPE_ID).getFirstValue());
    }

    @Test
    public void testRelationship() throws Exception {
        String id1 = getObjectByPath("/testfolder1/testfile1").getId();
        String id2 = getObjectByPath("/testfolder1/testfile2").getId();

        // create relationship
        String statement;
        ObjectList res;
        BindingsObjectFactory factory = binding.getObjectFactory();
        List<PropertyData<?>> props = new ArrayList<PropertyData<?>>();
        props.add(factory.createPropertyIdData(PropertyIds.NAME, "rel"));
        props.add(factory.createPropertyIdData(PropertyIds.OBJECT_TYPE_ID,
                "Relation"));
        props.add(factory.createPropertyIdData(PropertyIds.SOURCE_ID, id1));
        props.add(factory.createPropertyIdData(PropertyIds.TARGET_ID, id2));
        Properties properties = factory.createPropertiesData(props);
        String relid = objService.createRelationship(repositoryId, properties,
                null, null, null, null);

        // query relationship
        statement = "SELECT cmis:objectId, cmis:name, cmis:sourceId, cmis:targetId FROM Relation";
        res = query(statement);
        assertEquals(1, res.getNumItems().intValue());
        ObjectData od = res.getObjects().get(0);
        assertEquals(relid, getValue(od, PropertyIds.OBJECT_ID));
        assertEquals("rel", getValue(od, PropertyIds.NAME));
        assertEquals(id1, getValue(od, PropertyIds.SOURCE_ID));
        assertEquals(id2, getValue(od, PropertyIds.TARGET_ID));

        // normal user has security applied to its queries
        nuxeotc.closeSession();
        nuxeotc.session = nuxeotc.openSessionAs("john");
        init();

        statement = "SELECT A.cmis:objectId, B.cmis:objectId"
                + " FROM cmis:document A"
                + " JOIN cmis:relationship R ON R.cmis:sourceId = A.cmis:objectId"
                + " JOIN cmis:document B ON R.cmis:targetId = B.cmis:objectId";
        res = query(statement);
        // no access to testfile1 or testfile2 by john
        assertEquals(0, res.getNumItems().intValue());

        // bob has Browse on testfile1 and testfile2
        nuxeotc.closeSession();
        nuxeotc.session = nuxeotc.openSessionAs("bob");
        init();

        // no security check on relationship itself
        statement = "SELECT A.cmis:objectId, B.cmis:objectId"
                + " FROM cmis:document A"
                + " JOIN cmis:relationship R ON R.cmis:sourceId = A.cmis:objectId"
                + " JOIN cmis:document B ON R.cmis:targetId = B.cmis:objectId";
        res = query(statement);
        assertEquals(1, res.getNumItems().intValue());
    }

}
