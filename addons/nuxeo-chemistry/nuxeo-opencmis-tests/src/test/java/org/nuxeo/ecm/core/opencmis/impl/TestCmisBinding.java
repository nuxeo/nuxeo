/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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

import static org.apache.chemistry.opencmis.commons.BasicPermissions.ALL;
import static org.apache.chemistry.opencmis.commons.BasicPermissions.READ;
import static org.apache.chemistry.opencmis.commons.BasicPermissions.WRITE;
import static org.apache.chemistry.opencmis.commons.data.PermissionMapping.CAN_ADD_POLICY_OBJECT;
import static org.apache.chemistry.opencmis.commons.data.PermissionMapping.CAN_ADD_POLICY_POLICY;
import static org.apache.chemistry.opencmis.commons.data.PermissionMapping.CAN_ADD_TO_FOLDER_FOLDER;
import static org.apache.chemistry.opencmis.commons.data.PermissionMapping.CAN_ADD_TO_FOLDER_OBJECT;
import static org.apache.chemistry.opencmis.commons.data.PermissionMapping.CAN_APPLY_ACL_OBJECT;
import static org.apache.chemistry.opencmis.commons.data.PermissionMapping.CAN_CANCEL_CHECKOUT_DOCUMENT;
import static org.apache.chemistry.opencmis.commons.data.PermissionMapping.CAN_CHECKIN_DOCUMENT;
import static org.apache.chemistry.opencmis.commons.data.PermissionMapping.CAN_CHECKOUT_DOCUMENT;
import static org.apache.chemistry.opencmis.commons.data.PermissionMapping.CAN_CREATE_DOCUMENT_FOLDER;
import static org.apache.chemistry.opencmis.commons.data.PermissionMapping.CAN_CREATE_FOLDER_FOLDER;
import static org.apache.chemistry.opencmis.commons.data.PermissionMapping.CAN_CREATE_RELATIONSHIP_SOURCE;
import static org.apache.chemistry.opencmis.commons.data.PermissionMapping.CAN_CREATE_RELATIONSHIP_TARGET;
import static org.apache.chemistry.opencmis.commons.data.PermissionMapping.CAN_DELETE_CONTENT_DOCUMENT;
import static org.apache.chemistry.opencmis.commons.data.PermissionMapping.CAN_DELETE_OBJECT;
import static org.apache.chemistry.opencmis.commons.data.PermissionMapping.CAN_DELETE_TREE_FOLDER;
import static org.apache.chemistry.opencmis.commons.data.PermissionMapping.CAN_GET_ACL_OBJECT;
import static org.apache.chemistry.opencmis.commons.data.PermissionMapping.CAN_GET_ALL_VERSIONS_VERSION_SERIES;
import static org.apache.chemistry.opencmis.commons.data.PermissionMapping.CAN_GET_APPLIED_POLICIES_OBJECT;
import static org.apache.chemistry.opencmis.commons.data.PermissionMapping.CAN_GET_CHILDREN_FOLDER;
import static org.apache.chemistry.opencmis.commons.data.PermissionMapping.CAN_GET_DESCENDENTS_FOLDER;
import static org.apache.chemistry.opencmis.commons.data.PermissionMapping.CAN_GET_FOLDER_PARENT_OBJECT;
import static org.apache.chemistry.opencmis.commons.data.PermissionMapping.CAN_GET_OBJECT_RELATIONSHIPS_OBJECT;
import static org.apache.chemistry.opencmis.commons.data.PermissionMapping.CAN_GET_PARENTS_FOLDER;
import static org.apache.chemistry.opencmis.commons.data.PermissionMapping.CAN_GET_PROPERTIES_OBJECT;
import static org.apache.chemistry.opencmis.commons.data.PermissionMapping.CAN_MOVE_OBJECT;
import static org.apache.chemistry.opencmis.commons.data.PermissionMapping.CAN_MOVE_SOURCE;
import static org.apache.chemistry.opencmis.commons.data.PermissionMapping.CAN_MOVE_TARGET;
import static org.apache.chemistry.opencmis.commons.data.PermissionMapping.CAN_REMOVE_FROM_FOLDER_FOLDER;
import static org.apache.chemistry.opencmis.commons.data.PermissionMapping.CAN_REMOVE_FROM_FOLDER_OBJECT;
import static org.apache.chemistry.opencmis.commons.data.PermissionMapping.CAN_REMOVE_POLICY_OBJECT;
import static org.apache.chemistry.opencmis.commons.data.PermissionMapping.CAN_REMOVE_POLICY_POLICY;
import static org.apache.chemistry.opencmis.commons.data.PermissionMapping.CAN_SET_CONTENT_DOCUMENT;
import static org.apache.chemistry.opencmis.commons.data.PermissionMapping.CAN_UPDATE_PROPERTIES_OBJECT;
import static org.apache.chemistry.opencmis.commons.data.PermissionMapping.CAN_VIEW_CONTENT_OBJECT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.Ace;
import org.apache.chemistry.opencmis.commons.data.Acl;
import org.apache.chemistry.opencmis.commons.data.AclCapabilities;
import org.apache.chemistry.opencmis.commons.data.AllowableActions;
import org.apache.chemistry.opencmis.commons.data.ChangeEventInfo;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.commons.data.ObjectInFolderContainer;
import org.apache.chemistry.opencmis.commons.data.ObjectInFolderList;
import org.apache.chemistry.opencmis.commons.data.ObjectList;
import org.apache.chemistry.opencmis.commons.data.ObjectParentData;
import org.apache.chemistry.opencmis.commons.data.PermissionMapping;
import org.apache.chemistry.opencmis.commons.data.Principal;
import org.apache.chemistry.opencmis.commons.data.Properties;
import org.apache.chemistry.opencmis.commons.data.PropertyData;
import org.apache.chemistry.opencmis.commons.data.PropertyString;
import org.apache.chemistry.opencmis.commons.data.RepositoryCapabilities;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.definitions.PermissionDefinition;
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
import org.apache.chemistry.opencmis.commons.impl.dataobjects.AccessControlEntryImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.AccessControlListImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.AccessControlPrincipalDataImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ContentStreamImpl;
import org.apache.chemistry.opencmis.commons.spi.Holder;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.Environment;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.RecoverableClientException;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoRepository;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoTypeHelper;
import org.nuxeo.ecm.core.opencmis.tests.Helper;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.RuntimeHarness;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Tests that hit directly the server APIs.
 * <p>
 * Uses CMISQL to NXQL conversion for queries, which disallows JOINs.
 */
@RunWith(FeaturesRunner.class)
@Features({ CmisFeature.class, CmisFeatureConfiguration.class })
@LocalDeploy("org.nuxeo.ecm.core.opencmis.tests.tests:OSGI-INF/types-contrib.xml")
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestCmisBinding extends TestCmisBindingBase {

    public static final String NUXEO_ROOT_TYPE = "Root"; // from Nuxeo

    public static final String NUXEO_ROOT_NAME = ""; // NuxeoPropertyDataName;

    // stream content with non-ASCII characters
    public static final String STREAM_CONTENT = "Caf\u00e9 Diem\none\0two";

    public static final String COMPLEX_TITLE = "Is this my/your caf\u00e9?";

    @Inject
    protected RuntimeHarness harness;

    @Inject
    protected CoreSession coreSession;

    @Inject
    protected WorkManager workManager;

    @Before
    public void setUp() throws Exception {
        // wait indexing of /default-domain as we need to delete it in setUpData
        waitForIndexing();
        setUpBinding(coreSession);
        setUpData(coreSession);
        waitForIndexing();
    }

    @After
    public void tearDown() throws Exception {
        tearDownBinding();
        waitForIndexing();
    }

    public void reSetUp(String username) {
        tearDownBinding();
        setUpBinding(coreSession, username);
    }

    // -----

    protected String createDocument(String name, String folderId, String typeId) {
        return objService.createDocument(repositoryId, createBaseDocumentProperties(name, typeId), folderId, null, null,
                null, null, null, null);
    }

    protected String createFolder(String name, String folderId, String typeId) {
        return objService.createFolder(repositoryId, createBaseDocumentProperties(name, typeId), folderId, null, null,
                null, null);
    }

    protected Properties createBaseDocumentProperties(String name, String typeId) {
        List<PropertyData<?>> props = new ArrayList<>();
        props.add(factory.createPropertyStringData(PropertyIds.NAME, name));
        props.add(factory.createPropertyIdData(PropertyIds.OBJECT_TYPE_ID, typeId));
        return factory.createPropertiesData(props);
    }

    protected Properties createProperties(String key, String value) {
        PropertyString prop = factory.createPropertyStringData(key, value);
        return factory.createPropertiesData(Collections.<PropertyData<?>> singletonList(prop));
    }

    protected ObjectData getObject(String id) {
        return objService.getObject(repositoryId, id, null, Boolean.FALSE, IncludeRelationships.BOTH, null,
                Boolean.FALSE, Boolean.FALSE, null);
    }

    protected ObjectData getObjectByPath(String path) {
        return objService.getObjectByPath(repositoryId, path, null, null, null, null, null, null, null);
    }

    protected ObjectList query(String statement) {
        return discService.query(repositoryId, statement, Boolean.TRUE, null, null, null, null, null, null);
    }

    protected static Object getValue(ObjectData data, String key) {
        PropertyData<?> pd = data.getProperties().getProperties().get(key);
        return pd == null ? null : pd.getFirstValue();
    }

    protected static Object getValues(ObjectData data, String key) {
        PropertyData<?> pd = data.getProperties().getProperties().get(key);
        return pd == null ? null : pd.getValues();
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

    protected static Set<String> set(String... strings) {
        return new HashSet<>(Arrays.asList(strings));
    }

    protected void checkInfo(RepositoryInfo info) {
        assertEquals(repositoryId, info.getId());
        assertEquals("Nuxeo Repository " + repositoryId, info.getName());
        assertEquals("Nuxeo Repository " + repositoryId, info.getDescription());
        assertEquals("Nuxeo", info.getVendorName());
        assertEquals("Nuxeo OpenCMIS Connector", info.getProductName());
        String version = Framework.getProperty(Environment.DISTRIBUTION_VERSION, "5.5 dev");
        assertEquals(version, info.getProductVersion());
        assertEquals(rootFolderId, info.getRootFolderId());
        assertEquals("Guest", info.getPrincipalIdAnonymous());
        assertEquals("1.1", info.getCmisVersionSupported());
        // TODO assertEquals("...", info.getThinClientUri());
        assertNotNull(info.getLatestChangeLogToken());
        assertEquals(Boolean.FALSE, info.getChangesIncomplete());
        assertEquals(Arrays.asList(BaseTypeId.CMIS_DOCUMENT, BaseTypeId.CMIS_FOLDER), info.getChangesOnType());
        assertEquals(SecurityConstants.EVERYONE, info.getPrincipalIdAnyone());

        // capabilities

        RepositoryCapabilities caps = info.getCapabilities();
        assertEquals(CapabilityAcl.MANAGE, caps.getAclCapability());
        assertEquals(CapabilityChanges.OBJECTIDSONLY, caps.getChangesCapability());
        assertEquals(CapabilityContentStreamUpdates.PWCONLY, caps.getContentStreamUpdatesCapability());
        assertEquals(supportsJoins() ? CapabilityJoin.INNERANDOUTER : CapabilityJoin.NONE, caps.getJoinCapability());
        assertEquals(CapabilityQuery.BOTHCOMBINED, caps.getQueryCapability());
        assertEquals(CapabilityRenditions.READ, caps.getRenditionsCapability());

        // ACL capabilities

        AclCapabilities aclCaps = info.getAclCapabilities();
        assertEquals(AclPropagation.PROPAGATE, aclCaps.getAclPropagation());
        assertEquals(SupportedPermissions.REPOSITORY, aclCaps.getSupportedPermissions());

        Map<String, String> permDefs = new HashMap<>();
        for (PermissionDefinition pd : aclCaps.getPermissions()) {
            permDefs.put(pd.getId(), pd.getDescription());
        }
        Map<String, String> expectedPermDefs = new HashMap<>();
        expectedPermDefs.put(READ, "Read");
        expectedPermDefs.put(WRITE, "Write");
        expectedPermDefs.put(ALL, "All");
        expectedPermDefs.put(NuxeoRepository.NUXEO_READ_REMOVE, "Remove");
        assertEquals(expectedPermDefs, permDefs);

        Map<String, Set<String>> permMap = new HashMap<>();
        for (PermissionMapping permissonMapping : aclCaps.getPermissionMapping().values()) {
            String key = permissonMapping.getKey();
            List<String> perms = permissonMapping.getPermissions();
            permMap.put(key, new HashSet<>(perms));
        }
        Map<String, Set<String>> expectedPermMap = new HashMap<>();
        expectedPermMap.put(CAN_GET_DESCENDENTS_FOLDER, set(READ));
        expectedPermMap.put(CAN_GET_CHILDREN_FOLDER, set(READ));
        expectedPermMap.put(CAN_GET_PARENTS_FOLDER, set(READ));
        expectedPermMap.put(CAN_GET_FOLDER_PARENT_OBJECT, set(READ));
        expectedPermMap.put(CAN_CREATE_DOCUMENT_FOLDER, set(WRITE));
        expectedPermMap.put(CAN_CREATE_FOLDER_FOLDER, set(WRITE));
        expectedPermMap.put(CAN_CREATE_RELATIONSHIP_SOURCE, set(READ));
        expectedPermMap.put(CAN_CREATE_RELATIONSHIP_TARGET, set(READ));
        expectedPermMap.put(CAN_GET_PROPERTIES_OBJECT, set(READ));
        expectedPermMap.put(CAN_VIEW_CONTENT_OBJECT, set(READ));
        expectedPermMap.put(CAN_UPDATE_PROPERTIES_OBJECT, set(WRITE));
        expectedPermMap.put(CAN_MOVE_OBJECT, set(WRITE));
        expectedPermMap.put(CAN_MOVE_TARGET, set(WRITE));
        expectedPermMap.put(CAN_MOVE_SOURCE, set(WRITE));
        expectedPermMap.put(CAN_DELETE_OBJECT, set(WRITE));
        expectedPermMap.put(CAN_DELETE_TREE_FOLDER, set(WRITE));
        expectedPermMap.put(CAN_SET_CONTENT_DOCUMENT, set(WRITE));
        expectedPermMap.put(CAN_DELETE_CONTENT_DOCUMENT, set(WRITE));
        expectedPermMap.put(CAN_ADD_TO_FOLDER_OBJECT, set(WRITE));
        expectedPermMap.put(CAN_ADD_TO_FOLDER_FOLDER, set(WRITE));
        expectedPermMap.put(CAN_REMOVE_FROM_FOLDER_OBJECT, set(WRITE));
        expectedPermMap.put(CAN_REMOVE_FROM_FOLDER_FOLDER, set(WRITE));
        expectedPermMap.put(CAN_CHECKOUT_DOCUMENT, set(WRITE));
        expectedPermMap.put(CAN_CANCEL_CHECKOUT_DOCUMENT, set(WRITE));
        expectedPermMap.put(CAN_CHECKIN_DOCUMENT, set(WRITE));
        expectedPermMap.put(CAN_GET_ALL_VERSIONS_VERSION_SERIES, set(READ));
        expectedPermMap.put(CAN_GET_OBJECT_RELATIONSHIPS_OBJECT, set(READ));
        expectedPermMap.put(CAN_ADD_POLICY_OBJECT, set(WRITE));
        expectedPermMap.put(CAN_ADD_POLICY_POLICY, set(WRITE));
        expectedPermMap.put(CAN_REMOVE_POLICY_OBJECT, set(WRITE));
        expectedPermMap.put(CAN_REMOVE_POLICY_POLICY, set(WRITE));
        expectedPermMap.put(CAN_GET_APPLIED_POLICIES_OBJECT, set(READ));
        expectedPermMap.put(CAN_GET_ACL_OBJECT, set(READ));
        expectedPermMap.put(CAN_APPLY_ACL_OBJECT, set(ALL));

        assertEquals(expectedPermMap, permMap);
    }

    @Test
    public void testGetTypeDefinition() {
        TypeDefinition type;

        type = repoService.getTypeDefinition(repositoryId, "cmis:folder", null);
        assertEquals(Boolean.TRUE, type.isCreatable());
        assertNull(type.getParentTypeId());
        assertEquals("cmis:folder", type.getLocalName());
        assertTrue(type.getPropertyDefinitions().containsKey("dc:title"));
        assertTrue(type.getPropertyDefinitions().containsKey("nuxeo:lifecycleState"));
        assertTrue(type.getPropertyDefinitions().containsKey("nuxeo:secondaryObjectTypeIds"));
        assertFalse(type.getPropertyDefinitions().containsKey("nuxeo:isVersion"));
        assertFalse(type.getPropertyDefinitions().containsKey("nuxeo:contentStreamDigest"));

        type = repoService.getTypeDefinition(repositoryId, "Folder", null);
        assertEquals(Boolean.TRUE, type.isCreatable());
        assertEquals("cmis:folder", type.getParentTypeId());
        assertEquals("Folder", type.getLocalName());
        assertTrue(type.getPropertyDefinitions().containsKey("nuxeo:lifecycleState"));
        assertTrue(type.getPropertyDefinitions().containsKey("nuxeo:secondaryObjectTypeIds"));
        assertFalse(type.getPropertyDefinitions().containsKey("nuxeo:isVersion"));
        assertFalse(type.getPropertyDefinitions().containsKey("nuxeo:contentStreamDigest"));

        type = repoService.getTypeDefinition(repositoryId, "cmis:document", null);
        assertEquals(Boolean.TRUE, type.isCreatable());
        assertNull(type.getParentTypeId());
        assertEquals("cmis:document", type.getLocalName());
        assertTrue(type.getPropertyDefinitions().containsKey("dc:title"));
        assertTrue(type.getPropertyDefinitions().containsKey("cmis:contentStreamFileName"));
        assertTrue(type.getPropertyDefinitions().containsKey("nuxeo:lifecycleState"));
        assertTrue(type.getPropertyDefinitions().containsKey("nuxeo:secondaryObjectTypeIds"));
        assertTrue(type.getPropertyDefinitions().containsKey("nuxeo:isVersion"));
        assertTrue(type.getPropertyDefinitions().containsKey("nuxeo:contentStreamDigest"));

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
        assertTrue(type.getPropertyDefinitions().containsKey("nuxeo:lifecycleState"));
        assertTrue(type.getPropertyDefinitions().containsKey("nuxeo:secondaryObjectTypeIds"));
        assertTrue(type.getPropertyDefinitions().containsKey("nuxeo:isVersion"));
        assertTrue(type.getPropertyDefinitions().containsKey("nuxeo:contentStreamDigest"));

        type = repoService.getTypeDefinition(repositoryId, "MyForum", null);
        assertEquals(BaseTypeId.CMIS_FOLDER, type.getBaseTypeId());
        assertEquals("cmis:folder", type.getParentTypeId());

        type = repoService.getTypeDefinition(repositoryId, "MyForum2", null);
        assertEquals(BaseTypeId.CMIS_FOLDER, type.getBaseTypeId());
        assertEquals("cmis:folder", type.getParentTypeId());
    }

    public List<String> getTypeIds(TypeDefinitionList types) {
        List<String> ids = new ArrayList<>();
        for (TypeDefinition type : types.getList()) {
            ids.add(type.getId());
        }
        return ids;
    }

    @Test
    public void testGetTypeChildrenBase() {
        TypeDefinitionList types = repoService.getTypeChildren(repositoryId, null, Boolean.FALSE, null, null, null);
        List<String> ids = getTypeIds(types);
        assertEquals(4, ids.size());
        assertTrue(ids.contains(BaseTypeId.CMIS_DOCUMENT.value()));
        assertTrue(ids.contains(BaseTypeId.CMIS_FOLDER.value()));
        assertTrue(ids.contains(BaseTypeId.CMIS_RELATIONSHIP.value()));
        assertTrue(ids.contains(BaseTypeId.CMIS_SECONDARY.value()));
    }

    @Test
    public void testGetTypeChildren() {
        TypeDefinitionList types = repoService.getTypeChildren(repositoryId, "cmis:folder", Boolean.FALSE, null, null,
                null);
        for (TypeDefinition type : types.getList()) {
            Map<String, PropertyDefinition<?>> pd = type.getPropertyDefinitions();
            assertNotNull(pd);
            assertEquals(0, pd.size());
        }
        List<String> ids = getTypeIds(types);
        assertTrue(ids.contains("Folder"));
        assertTrue(ids.contains("Root"));
        assertTrue(ids.contains("Domain"));
        assertTrue(ids.contains("OrderedFolder"));
        assertTrue(ids.contains("Workspace"));
        assertTrue(ids.contains("Section"));

        // batching
        types = repoService.getTypeChildren(repositoryId, "cmis:folder", Boolean.FALSE, BigInteger.valueOf(4),
                BigInteger.valueOf(2), null);
        List<String> ids2 = getTypeIds(types);
        assertEquals(4, ids2.size());
        assertFalse(ids2.contains(ids.get(0)));
        assertFalse(ids2.contains(ids.get(1)));
        // batching beyond max size
        types = repoService.getTypeChildren(repositoryId, "cmis:folder", Boolean.FALSE, BigInteger.valueOf(12),
                BigInteger.valueOf(5), null);
        List<String> ids3 = getTypeIds(types);
        assertEquals(ids.size() - 5, ids3.size());
        assertFalse(ids3.contains(ids.get(0)));
        assertFalse(ids3.contains(ids.get(1)));
        assertFalse(ids3.contains(ids.get(2)));
        assertFalse(ids3.contains(ids.get(3)));
        assertFalse(ids3.contains(ids.get(4)));

        // check property definition inclusion
        types = repoService.getTypeChildren(repositoryId, BaseTypeId.CMIS_FOLDER.value(), Boolean.TRUE, null, null,
                null);
        for (TypeDefinition type : types.getList()) {
            Map<String, PropertyDefinition<?>> pd = type.getPropertyDefinitions();
            assertNotNull(pd);
            // dublincore in all types
            assertTrue(pd.keySet().contains("dc:title"));
        }
        ids = getTypeIds(types);
        assertTrue(ids.contains("MyForum"));
        assertTrue(ids.contains("MyForum2"));

        types = repoService.getTypeChildren(repositoryId, BaseTypeId.CMIS_DOCUMENT.value(), Boolean.TRUE, null, null,
                null);
        for (TypeDefinition type : types.getList()) {
            Map<String, PropertyDefinition<?>> pd = type.getPropertyDefinitions();
            assertNotNull(pd);
            // dublincore in all types
            assertTrue(pd.keySet().contains("dc:title"));
        }
        ids = getTypeIds(types);
        assertTrue(ids.contains("File"));
        assertTrue(ids.contains("Note"));
        assertTrue(ids.contains("MyDocType"));

        // nonexistent type
        try {
            repoService.getTypeChildren(repositoryId, "nosuchtype", Boolean.TRUE, null, null, null);
            fail();
        } catch (CmisInvalidArgumentException e) {
            // ok
        }
    }

    @Test
    public void testGetTypeChildrenSecondary() {
        TypeDefinitionList types = repoService.getTypeChildren(repositoryId, "cmis:secondary", Boolean.FALSE, null,
                null, null);
        for (TypeDefinition type : types.getList()) {
            Map<String, PropertyDefinition<?>> pd = type.getPropertyDefinitions();
            assertNotNull(pd);
            assertEquals(0, pd.size());
        }
        List<String> ids = getTypeIds(types);
        assertTrue(ids.contains("facet:CustomFacetWithMySchema2"));
        assertTrue(ids.contains("facet:CustomFacetWithoutSchema"));
        assertTrue(ids.contains("facet:ComplexTest"));
        assertTrue(ids.contains("facet:HasRelatedText"));
        assertTrue(ids.contains("facet:Versionable"));
        assertTrue(ids.contains("facet:Folderish"));
    }

    @Test
    public void testGetTypeDescendants() {
        List<TypeDefinitionContainer> desc = repoService.getTypeDescendants(repositoryId, "cmis:folder", null,
                Boolean.FALSE, null);
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
            repoService.getTypeDescendants(repositoryId, "nosuchtype", null, Boolean.FALSE, null);
            fail();
        } catch (CmisInvalidArgumentException e) {
            // ok
        }
    }

    @Test
    public void testRoot() {
        ObjectData root = getObject(rootFolderId);
        assertNotNull(root.getId());
        assertEquals(NUXEO_ROOT_TYPE, getString(root, PropertyIds.OBJECT_TYPE_ID));
        assertEquals(NUXEO_ROOT_NAME, getString(root, PropertyIds.NAME));
        assertEquals("/", getString(root, PropertyIds.PATH));

        // root parent
        assertNull(getString(root, PropertyIds.PARENT_ID));
        ObjectData parent = navService.getFolderParent(repositoryId, rootFolderId, null, null);
        assertNull(parent);
        List<ObjectParentData> parents = navService.getObjectParents(repositoryId, rootFolderId, null, null, null, null,
                null, null);
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
        assertEquals(Boolean.TRUE, getValue(data, PropertyIds.IS_LATEST_MAJOR_VERSION));
        assertEquals(Boolean.FALSE, getValue(data, PropertyIds.IS_IMMUTABLE));
        assertEquals("File", getString(data, PropertyIds.OBJECT_TYPE_ID));
        assertEquals(Boolean.FALSE, // ...
                getValue(data, NuxeoTypeHelper.NX_ISVERSION));
        assertEquals("project", getValue(data, NuxeoTypeHelper.NX_LIFECYCLE_STATE));
        assertEquals(rootFolderId, getValue(data, NuxeoTypeHelper.NX_PARENT_ID));
        @SuppressWarnings("unchecked")
        List<String> facets = (List<String>) getValues(data, NuxeoTypeHelper.NX_FACETS);
        assertEquals(set( //
                "Commentable", //
                "Downloadable", //
                "HasRelatedText", //
                "Publishable", //
                "Versionable" //
        ), new HashSet<>(facets));
        assertEquals(null, getString(data, NuxeoTypeHelper.NX_DIGEST));
        @SuppressWarnings("unchecked")
        List<String> hashes = (List<String>) getValues(data, PropertyIds.CONTENT_STREAM_HASH);
        assertEquals(0, hashes.size());

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
        assertEquals("Folder", getString(data, PropertyIds.OBJECT_TYPE_ID));
        assertEquals("project", getValue(data, NuxeoTypeHelper.NX_LIFECYCLE_STATE));
        assertEquals(Arrays.asList("Folderish"), getValues(data, NuxeoTypeHelper.NX_FACETS));

        // creation of a cmis:folder (helps simple clients)

        id = createFolder("newfold2", rootFolderId, "cmis:folder");
        assertNotNull(id);
        data = getObject(id);
        assertEquals(id, data.getId());
        assertEquals("newfold2", getString(data, PropertyIds.NAME));
        assertEquals("Folder", getString(data, PropertyIds.OBJECT_TYPE_ID));
    }

    protected String createDocumentMyDocType() {
        List<PropertyData<?>> props = new ArrayList<>();
        props.add(factory.createPropertyStringData(PropertyIds.NAME, COMPLEX_TITLE));
        props.add(factory.createPropertyIdData(PropertyIds.OBJECT_TYPE_ID, "MyDocType"));
        props.add(factory.createPropertyStringData("my:string", "abc"));
        props.add(factory.createPropertyBooleanData("my:boolean", Boolean.TRUE));
        props.add(factory.createPropertyIntegerData("my:integer", BigInteger.valueOf(123)));
        props.add(factory.createPropertyIntegerData("my:long", BigInteger.valueOf(123)));
        props.add(factory.createPropertyDecimalData("my:double", BigDecimal.valueOf(123.456)));
        GregorianCalendar expectedDate = Helper.getCalendar(2010, 9, 30, 16, 4, 55);
        props.add(factory.createPropertyDateTimeData("my:date", expectedDate));
        Properties properties = factory.createPropertiesData(props);
        String id = objService.createDocument(repositoryId, properties, rootFolderId, null, VersioningState.CHECKEDOUT,
                null, null, null, null);
        assertNotNull(id);
        if (TransactionHelper.isTransactionActive()) {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
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
        GregorianCalendar expectedDate = Helper.getCalendar(2010, 9, 30, 16, 4, 55);
        if (expectedDate.getTimeInMillis() != date.getTimeInMillis()) {
            // there may be a timezone difference if the database
            // doesn't store timezones -> try with local timezone
            TimeZone tz = TimeZone.getDefault();
            GregorianCalendar localDate = Helper.getCalendar(2010, 9, 30, 16, 4, 55, tz);
            assertEquals(localDate.getTimeInMillis(), date.getTimeInMillis());
        }
        // check path segment created from name/title
        List<ObjectParentData> parents = navService.getObjectParents(repositoryId, id, null, null, null, null,
                Boolean.TRUE, null);
        assertEquals(1, parents.size());
        String pathSegment = parents.get(0).getRelativePathSegment();
        assertEquals(COMPLEX_TITLE.replace("/", "-"), pathSegment);
    }

    @Test
    public void testCreateDocumentWithContentStream() throws Exception {
        // null filename passed on purpose, size ignored by Nuxeo
        ContentStream cs = new ContentStreamImpl(null, "text/plain", Helper.FILE1_CONTENT);
        String id = objService.createDocument(repositoryId, createBaseDocumentProperties("doc1.txt", "File"),
                rootFolderId, cs, VersioningState.NONE, null, null, null, null);
        assertNotNull(id);
        ObjectData data = getObject(id);
        assertEquals(id, data.getId());
        assertEquals("doc1.txt", getString(data, PropertyIds.NAME));
        assertEquals("bde9eb59c76cb432a0f8d02057a19923", getString(data, NuxeoTypeHelper.NX_DIGEST));
        @SuppressWarnings("unchecked")
        List<String> hashes = (List<String>) getValues(data, PropertyIds.CONTENT_STREAM_HASH);
        assertEquals("{md5}bde9eb59c76cb432a0f8d02057a19923", hashes.get(0));
        cs = objService.getContentStream(repositoryId, id, null, null, null, null);
        assertNotNull(cs);
        assertEquals("text/plain", cs.getMimeType());
        assertEquals("doc1.txt", cs.getFileName());
        assertEquals(Helper.FILE1_CONTENT.length(), cs.getLength());
        assertEquals(Helper.FILE1_CONTENT, Helper.read(cs.getStream(), "UTF-8"));
    }

    @Test
    public void testCreateDocumentImplicitType() throws Exception {
        List<PropertyData<?>> props = new ArrayList<>();
        props.add(factory.createPropertyStringData(PropertyIds.NAME, "doc.txt"));
        props.add(factory.createPropertyIdData(PropertyIds.OBJECT_TYPE_ID, "cmis:document"));
        props.add(factory.createPropertyStringData("dc:description", "my doc"));
        Properties properties = factory.createPropertiesData(props);

        String id = objService.createDocument(repositoryId, properties, rootFolderId, null, VersioningState.CHECKEDOUT,
                null, null, null, null);
        ObjectData data = getObject(id);
        // check that the filename was enough to detect that we need a more
        // specific type than File
        assertEquals("Note", getValue(data, PropertyIds.OBJECT_TYPE_ID));
        // other props were set
        assertEquals("my doc", getValue(data, "dc:description"));
    }

    @Test
    public void testCreateDocumentWithoutName() throws Exception {
        List<PropertyData<?>> props = new ArrayList<>();
        props.add(factory.createPropertyIdData(PropertyIds.OBJECT_TYPE_ID, "cmis:document"));
        Properties properties = factory.createPropertiesData(props);
        try {
            objService.createDocument(repositoryId, properties, rootFolderId, null, VersioningState.NONE, null, null,
                    null, null);
            fail("Creation without cmis:name should fail");
        } catch (CmisConstraintException e) {
            // ok
        }
    }

    @Test
    public void testUpdateProperties() throws Exception {
        ObjectData ob = getObjectByPath("/testfolder1/testfile1");
        assertEquals("testfile1_Title", getString(ob, "dc:title"));

        Properties props = createProperties("dc:title", "new title");
        Holder<String> objectIdHolder = new Holder<>(ob.getId());
        objService.updateProperties(repositoryId, objectIdHolder, null, props, null);
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
        assertEquals("testfile1_Title", p.getProperties().get("dc:title").getFirstValue());

        // null value from nuxeo property
        PropertyData<?> v;
        v = p.getProperties().get("dc:nature");
        assertNull(v.getFirstValue());
        assertEquals(Collections.emptyList(), v.getValues());

        // null value from NuxeoPropertyStringDataFixed
        v = p.getProperties().get("cmis:changeToken");
        assertNull(v.getFirstValue());
        assertEquals(Collections.emptyList(), v.getValues());

        // with filter
        p = objService.getProperties(repositoryId, ob.getId(), "cmis:name", null);
        assertNull(p.getProperties().get("dc:title"));
        assertEquals("testfile1_Title", p.getProperties().get("cmis:name").getFirstValue());
    }

    @Test
    public void testPropertyFromSecondaryType() throws Exception {
        DocumentModel doc = coreSession.getDocument(new PathRef("/testfolder1/testfile1"));
        doc.addFacet("CustomFacetWithMySchema2");
        doc.setPropertyValue("my2:string", "foo");
        coreSession.saveDocument(doc);
        coreSession.save();
        nextTransaction();
        waitForIndexing();

        ObjectData ob = getObjectByPath("/testfolder1/testfile1");

        Properties p = objService.getProperties(repositoryId, ob.getId(), null, null);
        Map<String, PropertyData<?>> properties = p.getProperties();
        PropertyData<?> pd = properties.get("cmis:secondaryObjectTypeIds");
        assertNotNull(pd);
        @SuppressWarnings("unchecked")
        List<String> stl = (List<String>) pd.getValues();
        assertNotNull(stl);
        assertTrue(stl.contains("facet:CustomFacetWithMySchema2"));
        pd = properties.get("my2:string");
        assertNotNull(pd);
        assertEquals("foo", pd.getFirstValue());

        // change secondary prop
        Properties props = createProperties("my2:string", "bar");
        Holder<String> objectIdHolder = new Holder<String>(ob.getId());
        objService.updateProperties(repositoryId, objectIdHolder, null, props, null);

        // re-fetch
        p = objService.getProperties(repositoryId, ob.getId(), null, null);
        pd = p.getProperties().get("my2:string");
        assertEquals("bar", pd.getFirstValue());
    }

    @Test
    public void testContentStream() throws Exception {
        ObjectData ob = getObjectByPath("/testfolder1/testfile1");
        assertEquals("testfile1_Title", getString(ob, PropertyIds.NAME));
        assertEquals("bde9eb59c76cb432a0f8d02057a19923", getString(ob, NuxeoTypeHelper.NX_DIGEST));
        @SuppressWarnings("unchecked")
        List<String> hashes = (List<String>) getValues(ob, PropertyIds.CONTENT_STREAM_HASH);
        assertEquals("{md5}bde9eb59c76cb432a0f8d02057a19923", hashes.get(0));

        // get stream
        ContentStream cs = objService.getContentStream(repositoryId, ob.getId(), null, null, null, null);
        assertNotNull(cs);
        assertEquals("text/plain", cs.getMimeType());
        assertEquals("testfile.txt", cs.getFileName());
        assertEquals(Helper.FILE1_CONTENT.length(), cs.getLength());
        assertEquals(Helper.FILE1_CONTENT, Helper.read(cs.getStream(), "UTF-8"));

        // set stream

        cs = new ContentStreamImpl("foo.txt", "text/plain; charset=UTF-8", STREAM_CONTENT);
        Holder<String> objectIdHolder = new Holder<>(ob.getId());
        objService.setContentStream(repositoryId, objectIdHolder, Boolean.TRUE, null, cs, null);
        assertEquals(ob.getId(), objectIdHolder.getValue());

        // refetch
        cs = objService.getContentStream(repositoryId, ob.getId(), null, null, null, null);
        assertNotNull(cs);
        assertEquals("text/plain; charset=UTF-8", cs.getMimeType());
        assertEquals("foo.txt", cs.getFileName());
        assertEquals(STREAM_CONTENT.getBytes("UTF-8").length, cs.getLength());
        assertEquals(STREAM_CONTENT, Helper.read(cs.getStream(), "UTF-8"));

        // delete
        objService.deleteContentStream(repositoryId, objectIdHolder, null, null);

        // refetch
        try {
            cs = objService.getContentStream(repositoryId, ob.getId(), null, null, null, null);
            fail("Should have no content stream");
        } catch (CmisConstraintException e) {
            // ok
        }
    }

    @Test
    public void testGetChildren() {
        ObjectInFolderList res;
        String orderBy;

        orderBy = "cmis:name";
        res = navService.getChildren(repositoryId, rootFolderId, null, orderBy, null, null, null, null, null, null,
                null);
        assertEquals("testfolder1_Title", getValue(res.getObjects().get(0).getObject(), "cmis:name"));
        assertEquals("testfolder2_Title", getValue(res.getObjects().get(1).getObject(), "cmis:name"));

        orderBy = "cmis:name DESC";
        res = navService.getChildren(repositoryId, rootFolderId, null, orderBy, null, null, null, null, null, null,
                null);
        assertEquals("testfolder2_Title", getValue(res.getObjects().get(0).getObject(), "cmis:name"));
        assertEquals("testfolder1_Title", getValue(res.getObjects().get(1).getObject(), "cmis:name"));
    }

    // flatten and order children
    protected static List<String> flatTree(List<ObjectInFolderContainer> tree) throws Exception {
        if (tree == null) {
            return null;
        }
        List<String> r = new LinkedList<>();
        for (Iterator<ObjectInFolderContainer> it = tree.iterator(); it.hasNext();) {
            ObjectInFolderContainer child = it.next();
            String name = getString(child.getObject().getObject(), PropertyIds.NAME);
            String elem = name;
            List<String> sub = flatTree(child.getChildren());
            if (sub != null) {
                elem += "[" + StringUtils.join(sub, ", ") + "]";
            }
            r.add(elem);
        }
        Collections.sort(r);
        return r.isEmpty() ? null : r;
    }

    protected static String flat(List<ObjectInFolderContainer> tree) throws Exception {
        return StringUtils.join(flatTree(tree), ", ");
    }

    @Test
    public void testGetDescendants() throws Exception {
        List<ObjectInFolderContainer> tree;

        try {
            navService.getDescendants(repositoryId, rootFolderId, BigInteger.valueOf(0), null, null, null, null, null,
                    null);
            fail("Depth 0 should be forbidden");
        } catch (CmisInvalidArgumentException e) {
            // ok
        }

        tree = navService.getDescendants(repositoryId, rootFolderId, BigInteger.valueOf(1), null, null, null, null,
                null, null);
        assertEquals("testfolder1_Title, " //
                + "testfolder2_Title", flat(tree));

        tree = navService.getDescendants(repositoryId, rootFolderId, BigInteger.valueOf(2), null, null, null, null,
                null, null);
        assertEquals(
                "testfolder1_Title[" //
                        + /* */"testfile1_Title, " //
                        + /* */"testfile2_Title, " //
                        + /* */"testfile3_Title], " //
                        + "testfolder2_Title[" //
                        + /* */"testfolder3_Title, " //
                        + /* */"testfolder4_Title" //
                        + /* */(supportsProxies() ? ", title6" : "") //
                        + "]", //
                flat(tree));

        tree = navService.getDescendants(repositoryId, rootFolderId, BigInteger.valueOf(3), null, null, null, null,
                null, null);
        assertEquals(
                "testfolder1_Title[" //
                        + /* */"testfile1_Title, " //
                        + /* */"testfile2_Title, " //
                        + /* */"testfile3_Title], " //
                        + "testfolder2_Title[" //
                        + /* */"testfolder3_Title[testfile4_Title, title6], " //
                        + /* */"testfolder4_Title" //
                        + /* */(supportsProxies() ? ", title6" : "") //
                        + "]", //
                flat(tree));

        tree = navService.getDescendants(repositoryId, rootFolderId, BigInteger.valueOf(4), null, null, null, null,
                null, null);
        assertEquals(
                "testfolder1_Title[" //
                        + /* */"testfile1_Title, " //
                        + /* */"testfile2_Title, " //
                        + /* */"testfile3_Title], " //
                        + "testfolder2_Title[" //
                        + /* */"testfolder3_Title[testfile4_Title, title6], " //
                        + /* */"testfolder4_Title" //
                        + /* */(supportsProxies() ? ", title6" : "") //
                        + "]", //
                flat(tree));

        tree = navService.getDescendants(repositoryId, rootFolderId, BigInteger.valueOf(-1), null, null, null, null,
                null, null);
        assertEquals(
                "testfolder1_Title[testfile1_Title, " + /* */"testfile2_Title, " //
                        + /* */"testfile3_Title], " //
                        + "testfolder2_Title[" //
                        + /* */"testfolder3_Title[testfile4_Title, title6], " //
                        + /* */"testfolder4_Title" //
                        + /* */(supportsProxies() ? ", title6" : "") //
                        + "]", //
                flat(tree));

        ObjectData ob = getObjectByPath("/testfolder2");
        String folder2Id = ob.getId();

        tree = navService.getDescendants(repositoryId, folder2Id, BigInteger.valueOf(1), null, null, null, null, null,
                null);
        assertEquals("testfolder3_Title, testfolder4_Title" + (supportsProxies() ? ", title6" : ""), flat(tree));

        tree = navService.getDescendants(repositoryId, folder2Id, BigInteger.valueOf(2), null, null, null, null, null,
                null);
        assertEquals(
                "testfolder3_Title[testfile4_Title, title6], testfolder4_Title" + (supportsProxies() ? ", title6" : ""),
                flat(tree));

        tree = navService.getDescendants(repositoryId, folder2Id, BigInteger.valueOf(3), null, null, null, null, null,
                null);
        assertEquals(
                "testfolder3_Title[testfile4_Title, title6], testfolder4_Title" + (supportsProxies() ? ", title6" : ""),
                flat(tree));

        tree = navService.getDescendants(repositoryId, folder2Id, BigInteger.valueOf(-1), null, null, null, null, null,
                null);
        assertEquals(
                "testfolder3_Title[testfile4_Title, title6], testfolder4_Title" + (supportsProxies() ? ", title6" : ""),
                flat(tree));
    }

    @Test
    public void testGetFolderTree() throws Exception {
        List<ObjectInFolderContainer> tree;

        try {
            navService.getFolderTree(repositoryId, rootFolderId, BigInteger.valueOf(0), null, null, null, null, null,
                    null);
            fail("Depth 0 should be forbidden");
        } catch (CmisInvalidArgumentException e) {
            // ok
        }

        tree = navService.getFolderTree(repositoryId, rootFolderId, BigInteger.valueOf(1), null, null, null, null, null,
                null);
        assertEquals("testfolder1_Title, " //
                + "testfolder2_Title", flat(tree));

        tree = navService.getFolderTree(repositoryId, rootFolderId, BigInteger.valueOf(2), null, null, null, null, null,
                null);
        assertEquals(
                "testfolder1_Title, " //
                        + "testfolder2_Title[" //
                        + /* */"testfolder3_Title, " //
                        + /* */"testfolder4_Title]", //
                flat(tree));

        tree = navService.getFolderTree(repositoryId, rootFolderId, BigInteger.valueOf(3), null, null, null, null, null,
                null);
        assertEquals(
                "testfolder1_Title, " //
                        + "testfolder2_Title[" //
                        + /* */"testfolder3_Title, " //
                        + /* */"testfolder4_Title]", //
                flat(tree));

        tree = navService.getFolderTree(repositoryId, rootFolderId, BigInteger.valueOf(4), null, null, null, null, null,
                null);
        assertEquals(
                "testfolder1_Title, " //
                        + "testfolder2_Title[" //
                        + /* */"testfolder3_Title, " //
                        + /* */"testfolder4_Title]", //
                flat(tree));

        tree = navService.getFolderTree(repositoryId, rootFolderId, BigInteger.valueOf(-1), null, null, null, null,
                null, null);
        assertEquals(
                "testfolder1_Title, " //
                        + "testfolder2_Title[" //
                        + /* */"testfolder3_Title, " //
                        + /* */"testfolder4_Title]", //
                flat(tree));

        ObjectData ob = getObjectByPath("/testfolder2");
        String folder2Id = ob.getId();

        tree = navService.getFolderTree(repositoryId, folder2Id, BigInteger.valueOf(1), null, null, null, null, null,
                null);
        assertEquals("testfolder3_Title, testfolder4_Title", flat(tree));

        tree = navService.getFolderTree(repositoryId, folder2Id, BigInteger.valueOf(2), null, null, null, null, null,
                null);
        assertEquals("testfolder3_Title, testfolder4_Title", flat(tree));

        tree = navService.getFolderTree(repositoryId, folder2Id, BigInteger.valueOf(3), null, null, null, null, null,
                null);
        assertEquals("testfolder3_Title, testfolder4_Title", flat(tree));

        tree = navService.getFolderTree(repositoryId, folder2Id, BigInteger.valueOf(-1), null, null, null, null, null,
                null);
        assertEquals("testfolder3_Title, testfolder4_Title", flat(tree));
    }

    @Test
    public void testCreateDocumentFromSource() throws Exception {
        ObjectData ob = getObjectByPath("/testfolder1/testfile1");
        String key = "dc:title";
        String value = "new title";
        Properties props = createProperties(key, value);
        String id = objService.createDocumentFromSource(repositoryId, ob.getId(), props, rootFolderId, null, null, null,
                null, null);
        assertNotNull(id);
        assertNotEquals(id, ob.getId());
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
            objService.deleteObject(repositoryId, ob.getId(), Boolean.TRUE, null);
            fail("Should not be able to delete non-empty folder");
        } catch (CmisConstraintException e) {
            // ok to fail, still has children
        }
        ob = getObjectByPath("/testfolder2");
        assertNotNull(ob);

        try {
            objService.deleteObject(repositoryId, "nosuchid", Boolean.TRUE, null);
            fail("Should not be able to delete nonexistent object");
        } catch (CmisObjectNotFoundException e) {
            // ok
        }
    }

    @Test
    public void testRemoveObjectFromFolder1() throws Exception {
        ObjectData ob = getObjectByPath("/testfolder1/testfile1");
        filingService.removeObjectFromFolder(repositoryId, ob.getId(), null, null);
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
        filingService.removeObjectFromFolder(repositoryId, ob.getId(), folder.getId(), null);
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
                Action.CAN_ADD_OBJECT_TO_FOLDER, //
                Action.CAN_REMOVE_OBJECT_FROM_FOLDER, //
                Action.CAN_GET_RENDITIONS, //
                Action.CAN_GET_ALL_VERSIONS, //
                Action.CAN_CANCEL_CHECK_OUT, //
                Action.CAN_CHECK_IN);
        assertEquals(expected, aa.getAllowableActions());

        // checked in doc

        Holder<String> idHolder = new Holder<>(ob.getId());
        verService.checkIn(repositoryId, idHolder, Boolean.TRUE, null, null, "comment", null, null, null, null);

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
                Action.CAN_ADD_OBJECT_TO_FOLDER, //
                Action.CAN_REMOVE_OBJECT_FROM_FOLDER, //
                Action.CAN_GET_RENDITIONS, //
                Action.CAN_GET_ALL_VERSIONS, //
                Action.CAN_CHECK_OUT);
        assertEquals(expected, aa.getAllowableActions());

    }

    @Test
    public void testMoveObject() throws Exception {
        ObjectData fold = getObjectByPath("/testfolder1");
        ObjectData ob = getObjectByPath("/testfolder2/testfolder3/testfile4");
        Holder<String> objectIdHolder = new Holder<>(ob.getId());
        objService.moveObject(repositoryId, objectIdHolder, fold.getId(), null, null);
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

        waitForIndexing();

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
        assertEquals(supportsProxies() ? 7 : 6, res.getNumItems().intValue()); // 5 docs, 1 version, 1 proxy
        statement = "SELECT * FROM cmis:folder";
        res = query(statement);
        assertEquals(returnsRootInFolderQueries() ? 5 : 4, res.getNumItems().intValue());

        statement = "SELECT cmis:objectId, dc:description" //
                + " FROM File" //
                + " WHERE dc:title = 'testfile1_Title'";
        res = query(statement);
        assertEquals(1, res.getNumItems().intValue());

        statement = "SELECT cmis:objectId, dc:description" //
                + " FROM File" //
                + " WHERE dc:title = 'testfile1_Title'" + " AND dc:description <> 'argh'"
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

    protected void checkQueriedValue(String type, String term) {
        String statement = String.format("SELECT cmis:objectId FROM %s WHERE %s", type, term);
        ObjectList res = query(statement);
        int num = res.getNumItems().intValue();
        if (num == 0) {
            fail("no result for: " + statement);
        }
    }

    @Test
    public void testQuerySecurity() throws Exception {
        String statement;
        ObjectList res;

        waitForIndexing();

        statement = "SELECT cmis:objectId FROM File";
        res = query(statement);
        assertEquals(3, res.getNumItems().intValue());

        reSetUp("bob");

        statement = "SELECT cmis:objectId FROM File";
        res = query(statement);
        // only testfile1 and testfile2 are accessible by bob
        assertEquals(2, res.getNumItems().intValue());
    }

    /**
     * Wait for async worker completion then wait for indexing completion
     */
    public void waitForIndexing() throws Exception {
        if (!useElasticsearch()) {
            return;
        }
        TransactionHelper.commitOrRollbackTransaction();
        workManager.awaitCompletion(20, TimeUnit.SECONDS);
        ElasticSearchAdmin esa = Framework.getService(ElasticSearchAdmin.class);
        esa.prepareWaitForIndexing().get(20, TimeUnit.SECONDS);
        esa.refresh();
        TransactionHelper.startTransaction();
    }

    @Test
    public void testQueryWhereProperties() throws Exception {
        String statement;
        ObjectList res;

        createDocumentMyDocType();
        waitForIndexing();

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
        // TODO fix timezone issues (PostgreSQL)
        // checkQueriedValue("MyDocType",
        // "my:date = TIMESTAMP '2010-09-30T16:04:55-02:00'");
        checkQueriedValue("MyDocType", "my:date <> TIMESTAMP '1999-09-09T01:01:01Z'");
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
        waitForIndexing();

        // ----- Object -----

        checkWhereTerm("File", PropertyIds.NAME, "'testfile1_Title'");
        checkWhereTerm("File", PropertyIds.DESCRIPTION, "'testfile1_description'");
        checkWhereTerm("File", PropertyIds.OBJECT_ID, NOT_NULL);
        checkWhereTerm("File", PropertyIds.OBJECT_TYPE_ID, "'File'");
        // checkWhereTerm("File", PropertyIds.BASE_TYPE_ID,
        // "'cmis:document'");
        checkWhereTerm("File", PropertyIds.CREATED_BY, "'michael'");
        checkWhereTerm("File", PropertyIds.CREATION_DATE, NOT_NULL);
        checkWhereTerm("File", PropertyIds.LAST_MODIFIED_BY, "'bob'");
        checkWhereTerm("File", PropertyIds.LAST_MODIFICATION_DATE, NOT_NULL);
        // checkWhereTerm("File", PropertyIds.CHANGE_TOKEN, null);

        checkWhereTerm("File", NuxeoTypeHelper.NX_ISVERSION, "false");
        checkWhereTerm("File", NuxeoTypeHelper.NX_LIFECYCLE_STATE, "'project'");
        checkWhereTerm("File", NuxeoTypeHelper.NX_PARENT_ID, NOT_NULL);

        // ----- Folder -----

        checkWhereTerm("Folder", PropertyIds.PARENT_ID, NOT_NULL);
        checkWhereTerm("Folder", NuxeoTypeHelper.NX_LIFECYCLE_STATE, "'project'");

        // checkWhereTerm("Folder", PropertyIds.PATH, NOT_NULL);
        // checkWhereTerm("Folder", PropertyIds.ALLOWED_CHILD_OBJECT_TYPE_IDS,
        // NOT_NULL);

        // ----- Document -----

        // checkWhereTerm("File", PropertyIds.IS_IMMUTABLE, "FALSE");
        checkWhereTerm("File", PropertyIds.IS_LATEST_VERSION, "FALSE");
        // checkWhereTerm("File", PropertyIds.IS_MAJOR_VERSION, "TRUE");
        checkWhereTerm("File", PropertyIds.IS_LATEST_MAJOR_VERSION, "FALSE");
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
        // checkWhereTerm("File", NuxeoTypeHelper.NX_ECM_DIGEST,
        // "'bde9eb59c76cb432a0f8d02057a19923'");
    }

    protected void checkReturnedValue(String prop, Object expected) {
        checkReturnedValue(prop, expected, "File", "testfile1_Title");
    }

    protected void checkReturnedValue(String prop, Object expected, String type, String name) {
        String statement = String.format("SELECT %s FROM %s WHERE cmis:name = '%s'", prop, type, name);
        ObjectList res = query(statement);
        assertEquals(1, res.getNumItems().intValue());
        ObjectData data = res.getObjects().get(0);
        checkValue(prop, expected, data);
    }

    protected void checkValue(String prop, Object expected, ObjectData data) {
        Object value = expected instanceof List ? getValues(data, prop) : getValue(data, prop);
        if (expected == NOT_NULL) {
            assertNotNull(value);
        } else {
            assertEquals(expected, value);
        }
    }

    @Test
    public void testQueryReturnedProperties() throws Exception {
        waitForIndexing();
        checkReturnedValue("dc:title", "testfile1_Title");
        checkReturnedValue("dc:modified", NOT_NULL);
        checkReturnedValue("dc:lastContributor", "john");
        // multi-valued
        checkReturnedValue("dc:subjects", Arrays.asList("foo", "gee/moo"));
        checkReturnedValue("dc:contributors", Arrays.asList("pete", "bob"), "File", "testfile2_Title");
    }

    @Test
    public void testQueryReturnedSystemProperties() throws Exception {
        waitForIndexing();

        // ----- Object -----

        checkReturnedValue(PropertyIds.NAME, "testfile1_Title");
        checkReturnedValue(PropertyIds.DESCRIPTION, "testfile1_description");
        checkReturnedValue(PropertyIds.OBJECT_ID, NOT_NULL);
        checkReturnedValue(PropertyIds.OBJECT_TYPE_ID, "File");
        checkReturnedValue(PropertyIds.BASE_TYPE_ID, "cmis:document");
        checkReturnedValue(PropertyIds.CREATED_BY, "michael");
        checkReturnedValue(PropertyIds.CREATION_DATE, NOT_NULL);
        checkReturnedValue(PropertyIds.LAST_MODIFIED_BY, "john");
        checkReturnedValue(PropertyIds.LAST_MODIFICATION_DATE, NOT_NULL);
        checkReturnedValue(PropertyIds.CHANGE_TOKEN, null);
        checkReturnedValue(NuxeoTypeHelper.NX_PARENT_ID, NOT_NULL);

        // ----- Folder -----

        checkReturnedValue(PropertyIds.PARENT_ID, rootFolderId, "Folder", "testfolder1_Title");
        checkReturnedValue(PropertyIds.PATH, "/testfolder1", "Folder", "testfolder1_Title");
        checkReturnedValue(PropertyIds.ALLOWED_CHILD_OBJECT_TYPE_IDS, null, "Folder", "testfolder1_Title");
        checkReturnedValue(NuxeoTypeHelper.NX_FACETS, NOT_NULL, "Folder", "testfolder1_Title");
        checkReturnedValue(NuxeoTypeHelper.NX_LIFECYCLE_STATE, "project", "Folder", "testfolder1_Title");

        // ----- Document -----

        checkReturnedValue(PropertyIds.IS_IMMUTABLE, Boolean.FALSE);
        checkReturnedValue(PropertyIds.IS_LATEST_VERSION, Boolean.FALSE);
        checkReturnedValue(PropertyIds.IS_MAJOR_VERSION, Boolean.FALSE);
        checkReturnedValue(PropertyIds.IS_LATEST_MAJOR_VERSION, Boolean.FALSE);
        checkReturnedValue(PropertyIds.VERSION_LABEL, null);
        checkReturnedValue(PropertyIds.VERSION_SERIES_ID, NOT_NULL);
        checkReturnedValue(PropertyIds.IS_VERSION_SERIES_CHECKED_OUT, Boolean.TRUE);
        checkReturnedValue(PropertyIds.IS_PRIVATE_WORKING_COPY, Boolean.TRUE);
        checkReturnedValue(NuxeoTypeHelper.NX_ISVERSION, Boolean.FALSE);
        checkReturnedValue(NuxeoTypeHelper.NX_ISCHECKEDIN, Boolean.FALSE);
        checkReturnedValue(NuxeoTypeHelper.NX_FACETS, NOT_NULL);
        checkReturnedValue(NuxeoTypeHelper.NX_LIFECYCLE_STATE, "project");
        checkReturnedValue(NuxeoTypeHelper.NX_DIGEST, NOT_NULL);
        checkReturnedValue(PropertyIds.CONTENT_STREAM_HASH, NOT_NULL);
        checkReturnedValue(PropertyIds.VERSION_SERIES_CHECKED_OUT_ID, NOT_NULL);
        checkReturnedValue(PropertyIds.VERSION_SERIES_CHECKED_OUT_BY, USERNAME);
        checkReturnedValue(PropertyIds.CHECKIN_COMMENT, null);
        checkReturnedValue(PropertyIds.CONTENT_STREAM_LENGTH,
                new ContentStreamImpl(null, "text/plain", Helper.FILE1_CONTENT).getBigLength());
        checkReturnedValue(PropertyIds.CONTENT_STREAM_MIME_TYPE, "text/plain");
        checkReturnedValue(PropertyIds.CONTENT_STREAM_FILE_NAME, "testfile.txt");
        checkReturnedValue(PropertyIds.CONTENT_STREAM_ID, null);
    }

    @Test
    public void testQueryReturnedStar() throws Exception {
        waitForIndexing();

        String statement = "SELECT * FROM File WHERE cmis:name = 'testfile1_Title'";
        ObjectList res = query(statement);
        assertEquals(1, res.getNumItems().intValue());
        ObjectData data = res.getObjects().get(0);
        checkValue(PropertyIds.OBJECT_ID, NOT_NULL, data);
        checkValue(PropertyIds.OBJECT_TYPE_ID, "File", data);
        checkValue(PropertyIds.BASE_TYPE_ID, "cmis:document", data); // returned
        checkValue(PropertyIds.NAME, "testfile1_Title", data);
        checkValue(PropertyIds.CREATED_BY, "michael", data);
        checkValue(PropertyIds.CREATION_DATE, NOT_NULL, data);
        checkValue(PropertyIds.LAST_MODIFIED_BY, "john", data);
        checkValue(PropertyIds.LAST_MODIFICATION_DATE, NOT_NULL, data);
        checkValue(PropertyIds.CHANGE_TOKEN, null, data);
    }

    @Test
    public void testQueryLifecycle() throws Exception {
        String statement;
        ObjectList res;

        waitForIndexing();

        statement = "SELECT cmis:name FROM File";
        res = query(statement);
        int initiallyQueryableFilesCount = res.getNumItems().intValue();

        // all files are in state 'project'
        statement = "SELECT cmis:name FROM File WHERE nuxeo:lifecycleState = 'project'";
        res = query(statement);
        assertEquals(initiallyQueryableFilesCount, res.getNumItems().intValue());

        // delete another file:
        coreSession.followTransition(new PathRef("/testfolder1/testfile1"), "delete");
        coreSession.save();
        nextTransaction();
        waitForIndexing();

        // by default 'deleted' files are filtered out
        statement = "SELECT cmis:name FROM File";
        res = query(statement);
        assertEquals(initiallyQueryableFilesCount - 1, res.getNumItems().intValue());

        // but it is nevertheless possible to perform explicit queries on the
        // lifecycle state
        statement = "SELECT cmis:name FROM File" + " WHERE nuxeo:lifecycleState = 'project'";
        res = query(statement);
        assertEquals(initiallyQueryableFilesCount - 1, res.getNumItems().intValue());

        statement = "SELECT cmis:name FROM File" + " WHERE nuxeo:lifecycleState = 'deleted'" + " ORDER BY cmis:name";
        res = query(statement);
        assertEquals(2, res.getNumItems().intValue());
        assertEquals("testfile1_Title",
                res.getObjects().get(0).getProperties().getProperties().get(PropertyIds.NAME).getFirstValue());
        // file5 was deleted in the setup function of the test case
        assertEquals("title5",
                res.getObjects().get(1).getProperties().getProperties().get(PropertyIds.NAME).getFirstValue());

        statement = "SELECT cmis:name FROM File"
                + " WHERE nuxeo:lifecycleState IN ('project', 'deleted', 'somethingelse')";
        res = query(statement);
        assertEquals(initiallyQueryableFilesCount + 1, res.getNumItems().intValue());
    }

    @Test
    public void testQueryPathSegment() throws Exception {
        String statement;
        ObjectList res;
        List<ObjectData> objects;

        waitForIndexing();

        statement = "SELECT nuxeo:pathSegment FROM File ORDER BY nuxeo:pathSegment";
        res = query(statement);
        objects = res.getObjects();
        assertEquals(3, res.getNumItems().intValue());
        assertEquals("testfile1", getValue(objects.get(0), NuxeoTypeHelper.NX_PATH_SEGMENT));
        assertEquals("testfile2", getValue(objects.get(1), NuxeoTypeHelper.NX_PATH_SEGMENT));
        assertEquals("testfile4", getValue(objects.get(2), NuxeoTypeHelper.NX_PATH_SEGMENT));

        statement = "SELECT cmis:name FROM File WHERE nuxeo:pathSegment = 'testfile1'";
        res = query(statement);
        assertEquals(1, res.getNumItems().intValue());
        assertEquals("testfile1_Title", getValue(res.getObjects().get(0), "cmis:name"));
    }

    @Test
    public void testQueryPos() throws Exception {
        String statement;
        ObjectList res;
        List<ObjectData> objects;

        DocumentModel ofolder = coreSession.createDocumentModel("/", "ordered", "OrderedFolder");
        coreSession.createDocument(ofolder);
        DocumentModel odoc1 = coreSession.createDocumentModel("/ordered", "odoc1", "File");
        coreSession.createDocument(odoc1);
        DocumentModel odoc2 = coreSession.createDocumentModel("/ordered", "odoc2", "File");
        coreSession.createDocument(odoc2);
        coreSession.save();
        nextTransaction();
        waitForIndexing();

        statement = "SELECT nuxeo:pos FROM File WHERE nuxeo:pos >= 0 ORDER BY nuxeo:pos";
        res = query(statement);
        objects = res.getObjects();
        assertEquals(2, res.getNumItems().intValue());
        assertEquals(BigInteger.valueOf(0), getValue(objects.get(0), NuxeoTypeHelper.NX_POS));
        assertEquals(BigInteger.valueOf(1), getValue(objects.get(1), NuxeoTypeHelper.NX_POS));
    }

    @Test
    public void testQueryVersions() throws Exception {
        String statement;
        ObjectList res;

        waitForIndexing();

        // count all documents (for reference)
        statement = "SELECT cmis:name FROM File";
        res = query(statement);
        int initialFileCount = res.getNumItems().intValue();

        // checkin testfile1 as an archived version
        ObjectData ob = getObjectByPath("/testfolder1/testfile1");
        String id = ob.getId();
        Holder<String> idHolder = new Holder<>(id);
        verService.checkIn(repositoryId, idHolder, Boolean.TRUE, null, null, "this is the comment", null, null, null,
                null);

        waitForIndexing();

        // by default CMISQL queries will return both live documents and
        // archived versions
        res = query(statement);
        assertEquals(initialFileCount + 1, res.getNumItems().intValue());

        // it is however possible to fetch only the archived versions using the
        // nuxeo:isVersion system property
        statement = "SELECT cmis:name, nuxeo:isVersion FROM File" + " WHERE nuxeo:isVersion = true ORDER BY cmis:name";
        res = query(statement);
        assertEquals(1, res.getNumItems().intValue());
        checkValue(PropertyIds.NAME, "testfile1_Title", res.getObjects().get(0));
        checkValue(NuxeoTypeHelper.NX_ISVERSION, Boolean.TRUE, res.getObjects().get(0));

        // this should be equivalent to
        statement = "SELECT cmis:name, nuxeo:isVersion FROM File"
                + " WHERE nuxeo:isVersion <> false ORDER BY cmis:name";
        res = query(statement);
        assertEquals(1, res.getNumItems().intValue());
        checkValue(PropertyIds.NAME, "testfile1_Title", res.getObjects().get(0));
        checkValue(NuxeoTypeHelper.NX_ISVERSION, Boolean.TRUE, res.getObjects().get(0));

        // conversely one can select only live documents by negating this
        // predicate
        statement = "SELECT cmis:name, nuxeo:isVersion FROM File" + " WHERE nuxeo:isVersion = false ORDER BY cmis:name";
        res = query(statement);
        assertEquals(initialFileCount, res.getNumItems().intValue());
        checkValue(PropertyIds.NAME, "testfile1_Title", res.getObjects().get(0));
        checkValue(NuxeoTypeHelper.NX_ISVERSION, Boolean.FALSE, res.getObjects().get(0));

        // this should be equivalent to
        statement = "SELECT cmis:name, nuxeo:isVersion  FROM File"
                + " WHERE nuxeo:isVersion <> true ORDER BY cmis:name";
        res = query(statement);
        assertEquals(initialFileCount, res.getNumItems().intValue());
        checkValue(PropertyIds.NAME, "testfile1_Title", res.getObjects().get(0));
        checkValue(NuxeoTypeHelper.NX_ISVERSION, Boolean.FALSE, res.getObjects().get(0));
    }

    @Test
    public void testQueryLatestsVersions() throws Exception {
        String statement;
        ObjectList res;
        ObjectData first;

        waitForIndexing();

        // check that there is only one version of the document with title
        // 'testfile1_Title' (for reference)
        statement = "SELECT * FROM File WHERE cmis:name = 'testfile1_Title'";
        res = query(statement);
        assertEquals(1, res.getNumItems().intValue());

        // checkin testfile1 as an archived version
        ObjectData ob = getObjectByPath("/testfolder1/testfile1");
        String id = ob.getId();
        Holder<String> idHolder = new Holder<>(id);
        verService.checkIn(repositoryId, idHolder, Boolean.TRUE, null, null, "this is the comment", null, null, null,
                null);

        waitForIndexing();

        // by default CMISQL queries will return both live documents and
        // archived versions
        res = query(statement);
        assertEquals(2, res.getNumItems().intValue());

        // it is however possible to fetch only the last version using the
        // cmis:isLatestVersion system property
        statement = "SELECT cmis:isLatestVersion, nuxeo:isVersion FROM File"
                + " WHERE cmis:isLatestVersion = true AND cmis:name = 'testfile1_Title'";
        res = query(statement);
        assertEquals(1, res.getNumItems().intValue());
        first = res.getObjects().get(0);
        checkValue(PropertyIds.IS_LATEST_VERSION, Boolean.TRUE, first);
        checkValue(NuxeoTypeHelper.NX_ISVERSION, Boolean.TRUE, first);

        // this should be equivalent to
        statement = "SELECT cmis:isLatestVersion, nuxeo:isVersion FROM File"
                + " WHERE cmis:isLatestVersion <> false AND cmis:name = 'testfile1_Title'";
        res = query(statement);
        assertEquals(1, res.getNumItems().intValue());
        first = res.getObjects().get(0);
        checkValue(PropertyIds.IS_LATEST_VERSION, Boolean.TRUE, first);
        checkValue(NuxeoTypeHelper.NX_ISVERSION, Boolean.TRUE, first);

        // we can check out the last version, edit it and try again:
        verService.checkOut(repositoryId, idHolder, null, null);
        waitForIndexing();

        ob = getObjectByPath("/testfolder1/testfile1");
        assertEquals("testfile1_Title", getString(ob, "dc:title"));

        Properties props = createProperties("dc:description", "new description");
        idHolder = new Holder<>(ob.getId());
        objService.updateProperties(repositoryId, idHolder, null, props, null);
        assertEquals(ob.getId(), idHolder.getValue());

        waitForIndexing();

        ob = getObject(ob.getId());
        assertEquals("new description", getString(ob, "dc:description"));

        // the latest major version is still the archived version, not the
        // checkouted document
        statement = "SELECT * FROM File WHERE cmis:isLatestVersion = true" + " AND cmis:name = 'testfile1_Title'";
        res = query(statement);
        assertEquals(1, res.getNumItems().intValue());
        first = res.getObjects().get(0);
        checkValue(PropertyIds.IS_LATEST_VERSION, Boolean.TRUE, first);
        checkValue(NuxeoTypeHelper.NX_ISVERSION, Boolean.TRUE, first);
        checkValue("dc:description", "testfile1_description", first);

        // is also possible to query for versions that are not the latests, in
        // this case we only get the checkouted document
        statement = "SELECT * FROM File" + " WHERE cmis:isLatestVersion = false" + " AND cmis:name = 'testfile1_Title'";
        res = query(statement);
        assertEquals(1, res.getNumItems().intValue());
        first = res.getObjects().get(0);
        checkValue(PropertyIds.IS_LATEST_VERSION, Boolean.FALSE, first);
        checkValue(NuxeoTypeHelper.NX_ISVERSION, Boolean.FALSE, res.getObjects().get(0));
        checkValue("dc:description", "new description", first);
    }

    @Test
    public void testQueryAny() throws Exception {
        String statement;
        ObjectList res;

        waitForIndexing();

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
        // with qualifier
        statement = "SELECT f.cmis:objectId FROM File f WHERE ANY f.dc:subjects IN ('foo')";
        res = query(statement);
        assertEquals(1, res.getNumItems().intValue());

        // ANY ... NOT IN ...
        statement = "SELECT cmis:name FROM File WHERE ANY dc:contributors NOT IN ('pete')";
        res = query(statement);
        assertEquals(emptyListNegativeMatch() ? 2 : 1, res.getNumItems().intValue());
        statement = "SELECT cmis:name FROM File WHERE ANY dc:contributors NOT IN ('john')";
        res = query(statement);
        assertEquals(emptyListNegativeMatch() ? 3 : 1, res.getNumItems().intValue());
        statement = "SELECT cmis:name FROM File WHERE ANY dc:contributors NOT IN ('pete', 'bob')";
        res = query(statement);
        assertEquals(emptyListNegativeMatch() ? 2 : 0, res.getNumItems().intValue());
    }

    @Test
    public void testQueryIsNullMuti() throws Exception {
        String statement;
        ObjectList res;

        waitForIndexing();

        statement = "SELECT cmis:objectId FROM cmis:document" + " WHERE dc:subjects IS NULL";
        res = query(statement);
        assertEquals(supportsProxies() ? 6 : 5, res.getNumItems().intValue()); // 4 docs, 1 version, 1 proxy

        // with qualifier
        statement = "SELECT A.cmis:objectId FROM cmis:document A" + " WHERE A.dc:subjects IS NULL";
        res = query(statement);
        assertEquals(supportsProxies() ? 6 : 5, res.getNumItems().intValue()); // 4 docs, 1 version, 1 proxy
    }

    @Test
    public void testQueryIsNotNullMuti() throws Exception {
        waitForIndexing();

        String statement = "SELECT cmis:objectId FROM cmis:document" + " WHERE dc:subjects IS NOT NULL";
        ObjectList res = query(statement);
        assertEquals(1, res.getNumItems().intValue());
    }

    @SuppressWarnings("boxing")
    @Test
    public void testQueryMixinTypes() throws Exception {
        String statement;
        ObjectList res;

        // add some instance facets on 2 documents
        DocumentModel doc1 = coreSession.getDocument(new PathRef("/testfolder1/testfile1"));
        assertTrue(doc1.addFacet("CustomFacetWithoutSchema"));
        coreSession.saveDocument(doc1);
        DocumentModel doc2 = coreSession.getDocument(new PathRef("/testfolder1/testfile2"));
        assertTrue(doc2.addFacet("CustomFacetWithMySchema2"));
        doc2.setPropertyValue("my2:long", 12);
        coreSession.saveDocument(doc2);
        coreSession.save();
        nextTransaction();
        waitForIndexing();

        // ... = ANY ...
        statement = "SELECT nuxeo:secondaryObjectTypeIds FROM File WHERE 'Versionable' = ANY nuxeo:secondaryObjectTypeIds";
        res = query(statement);
        assertEquals(3, res.getNumItems().intValue());
        statement = "SELECT nuxeo:secondaryObjectTypeIds FROM File WHERE 'Downloadable' = ANY nuxeo:secondaryObjectTypeIds";
        res = query(statement);
        assertEquals(3, res.getNumItems().intValue());
        statement = "SELECT nuxeo:secondaryObjectTypeIds FROM File WHERE 'CustomFacetWithoutSchema' = ANY nuxeo:secondaryObjectTypeIds";
        res = query(statement);
        assertEquals(1, res.getNumItems().intValue());
        checkValue(NuxeoTypeHelper.NX_FACETS, Arrays.asList("Commentable", "CustomFacetWithoutSchema", "Downloadable",
                "HasRelatedText", "Publishable", "Thumbnail", "Versionable"), res.getObjects().get(0));
        statement = "SELECT * FROM File WHERE 'CustomFacetWithMySchema2' = ANY nuxeo:secondaryObjectTypeIds";
        res = query(statement);
        assertEquals(1, res.getNumItems().intValue());
        // additional test with JOIN in next method

        // ANY ... IN ...
        statement = "SELECT nuxeo:secondaryObjectTypeIds FROM File WHERE ANY nuxeo:secondaryObjectTypeIds IN ('Versionable')";
        res = query(statement);
        assertEquals(3, res.getNumItems().intValue());
        statement = "SELECT nuxeo:secondaryObjectTypeIds FROM File WHERE ANY nuxeo:secondaryObjectTypeIds IN ('CustomFacetWithMySchema2')";
        res = query(statement);
        assertEquals(1, res.getNumItems().intValue());
        checkValue(NuxeoTypeHelper.NX_FACETS, Arrays.asList("Commentable", "CustomFacetWithMySchema2", "Downloadable",
                "HasRelatedText", "Publishable", "Versionable"), res.getObjects().get(0));
        statement = "SELECT nuxeo:secondaryObjectTypeIds FROM File WHERE ANY nuxeo:secondaryObjectTypeIds IN ('CustomFacetWithoutSchema', 'CustomFacetWithMySchema2')";
        res = query(statement);
        assertEquals(2, res.getNumItems().intValue());
        statement = "SELECT nuxeo:secondaryObjectTypeIds FROM File WHERE ANY nuxeo:secondaryObjectTypeIds IN ('Versionable', 'CustomFacetWithoutSchema')";
        res = query(statement);
        assertEquals(3, res.getNumItems().intValue());

        // ANY ... NOT IN ...
        statement = "SELECT nuxeo:secondaryObjectTypeIds FROM File WHERE ANY nuxeo:secondaryObjectTypeIds NOT IN ('Versionable')";
        res = query(statement);
        assertEquals(0, res.getNumItems().intValue());
        statement = "SELECT nuxeo:secondaryObjectTypeIds FROM File WHERE ANY nuxeo:secondaryObjectTypeIds NOT IN ('CustomFacetWithoutSchema')";
        res = query(statement);
        assertEquals(2, res.getNumItems().intValue());
        statement = "SELECT nuxeo:secondaryObjectTypeIds FROM File WHERE ANY nuxeo:secondaryObjectTypeIds NOT IN ('CustomFacetWithoutSchema', 'CustomFacetWithMySchema2')";
        res = query(statement);
        assertEquals(1, res.getNumItems().intValue());
        checkValue(NuxeoTypeHelper.NX_FACETS,
                Arrays.asList("Commentable", "Downloadable", "HasRelatedText", "Publishable", "Versionable"),
                res.getObjects().get(0));
        statement = "SELECT nuxeo:secondaryObjectTypeIds FROM File WHERE ANY nuxeo:secondaryObjectTypeIds NOT IN ('Versionable', 'CustomFacetWithoutSchema')";
        res = query(statement);
        assertEquals(0, res.getNumItems().intValue());
    }

    @SuppressWarnings("boxing")
    @Test
    public void testQueryMixinTypesJoin() throws Exception {
        assumeSupportsJoins();

        String statement;
        ObjectList res;

        // add some instance facets on 2 documents
        DocumentModel doc1 = coreSession.getDocument(new PathRef("/testfolder1/testfile1"));
        assertTrue(doc1.addFacet("CustomFacetWithoutSchema"));
        coreSession.saveDocument(doc1);
        DocumentModel doc2 = coreSession.getDocument(new PathRef("/testfolder1/testfile2"));
        assertTrue(doc2.addFacet("CustomFacetWithMySchema2"));
        doc2.setPropertyValue("my2:long", 12);
        coreSession.saveDocument(doc2);
        coreSession.save();
        nextTransaction();
        waitForIndexing();

        // ... = ANY ...
        // with several qualifiers (therefore a JOIN)
        statement = "SELECT A.cmis:objectId FROM cmis:document A"
                + " JOIN cmis:folder B ON A.nuxeo:parentId = B.cmis:objectId"
                + " WHERE 'Versionable' = ANY A.nuxeo:secondaryObjectTypeIds";
        res = query(statement);
        assertEquals(5, res.getNumItems().intValue()); // 5 docs
    }

    @Test
    public void testQueryOrderBy() throws Exception {
        String statement;
        ObjectList res;
        ObjectData data;

        waitForIndexing();

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
        waitForIndexing();

        ObjectData f1 = getObjectByPath("/testfolder1");
        String statementPattern = "SELECT cmis:name FROM File" //
                + " WHERE IN_FOLDER('%s')" //
                + " ORDER BY cmis:name";
        String statement = String.format(statementPattern, f1.getId());
        ObjectList res = query(statement);
        assertEquals(2, res.getNumItems().intValue());
        assertEquals("testfile1_Title", getString(res.getObjects().get(0), PropertyIds.NAME));
        assertEquals("testfile2_Title", getString(res.getObjects().get(1), PropertyIds.NAME));

        // missing/illegal ID
        statement = String.format(statementPattern, "nosuchid");
        res = query(statement);
        assertEquals(0, res.getNumItems().intValue());
    }

    @Test
    public void testQueryInTree() throws Exception {
        ObjectList res;
        String statement;

        waitForIndexing();

        ObjectData f2 = getObjectByPath("/testfolder2");
        String statementPattern = "SELECT cmis:name FROM File" //
                + " WHERE IN_TREE('%s')";

        statement = String.format(statementPattern, f2.getId());
        res = query(statement);
        assertEquals(1, res.getNumItems().intValue());
        assertEquals("testfile4_Title", getString(res.getObjects().get(0), PropertyIds.NAME));

        // missing/illegal ID
        statement = String.format(statementPattern, "nosuchid");
        res = query(statement);
        assertEquals(0, res.getNumItems().intValue());
    }

    @Test
    public void testQueryInTreeQualifier() throws Exception {
        ObjectList res;
        String statement;
        String statementPattern; // qual is type
        ObjectData f2 = getObjectByPath("/testfolder2");

        waitForIndexing();

        statementPattern = "SELECT cmis:name FROM File" // no alias
                + " WHERE IN_TREE(File, '%s')"; // qual is type
        statement = String.format(statementPattern, f2.getId());
        res = query(statement);
        assertEquals(1, res.getNumItems().intValue());
        assertEquals("testfile4_Title", getString(res.getObjects().get(0), PropertyIds.NAME));

        statementPattern = "SELECT cmis:name FROM File f" // alias
                + " WHERE IN_TREE(f, '%s')"; // qual is alias
        statement = String.format(statementPattern, f2.getId());
        res = query(statement);
        assertEquals(1, res.getNumItems().intValue());
        assertEquals("testfile4_Title", getString(res.getObjects().get(0), PropertyIds.NAME));

        statementPattern = "SELECT cmis:name FROM File f" // alias
                + " WHERE IN_TREE(File, '%s')"; // qual is type
        statement = String.format(statementPattern, f2.getId());
        res = query(statement);
        assertEquals(1, res.getNumItems().intValue());
        assertEquals("testfile4_Title", getString(res.getObjects().get(0), PropertyIds.NAME));

        statementPattern = "SELECT cmis:name FROM File f" // alias
                + " WHERE IN_TREE('%s')"; // no qual
        statement = String.format(statementPattern, f2.getId());
        res = query(statement);
        assertEquals(1, res.getNumItems().intValue());
        assertEquals("testfile4_Title", getString(res.getObjects().get(0), PropertyIds.NAME));

        try {
            statement = "SELECT cmis:name FROM File" + " WHERE IN_TREE(g, 'abc')"; // invalid qual
            query(statement);
            fail("should fail");
        } catch (CmisInvalidArgumentException e) {
            assertTrue(e.getMessage().contains("g is neither a type query name nor an alias"));
        }

        try {
            statement = "SELECT cmis:name FROM File f" + " WHERE IN_TREE(g, 'abc')"; // invalid qual
            query(statement);
            fail("should fail");
        } catch (CmisInvalidArgumentException e) {
            assertTrue(e.getMessage().contains("g is neither a type query name nor an alias"));
        }
    }

    @Test
    public void testQueryQualifiers() throws Exception {
        ObjectList res;
        String statement;

        waitForIndexing();

        statement = "SELECT cmis:name FROM File"; // default
        res = query(statement);
        assertEquals(3, res.getNumItems().intValue());

        statement = "SELECT File.cmis:name FROM File"; // type qual
        res = query(statement);
        assertEquals(3, res.getNumItems().intValue());

        statement = "SELECT File.cmis:name, cmis:name FROM File";
        res = query(statement);
        assertEquals(3, res.getNumItems().intValue());

        statement = "SELECT File.cmis:name, cmis:objectTypeId FROM File";
        res = query(statement);
        assertEquals(3, res.getNumItems().intValue());

        statement = "SELECT cmis:name FROM File f"; // no qual
        res = query(statement);
        assertEquals(3, res.getNumItems().intValue());

        statement = "SELECT f.cmis:name FROM File f"; // alias qual
        res = query(statement);
        assertEquals(3, res.getNumItems().intValue());

        statement = "SELECT File.cmis:name FROM File f"; // alias qual
        res = query(statement);
        assertEquals(3, res.getNumItems().intValue());

        statement = "SELECT f.cmis:name, cmis:objectTypeId FROM File f";
        res = query(statement);
        assertEquals(3, res.getNumItems().intValue());

        statement = "SELECT File.cmis:name, f.cmis:objectId FROM File f";
        res = query(statement);
        assertEquals(3, res.getNumItems().intValue());

        statement = "SELECT File.cmis:name, cmis:objectTypeId FROM File f";
        res = query(statement);
        assertEquals(3, res.getNumItems().intValue());

        statement = "SELECT File.cmis:name, f.cmis:objectId, cmis:objectTypeId FROM File f";
        res = query(statement);
        assertEquals(3, res.getNumItems().intValue());
    }

    @Test
    public void testQueryContains() throws Exception {

        ObjectData ob = getObjectByPath("/testfolder1/testfile1");
        assertEquals("testfile1_Title", getString(ob, "dc:title"));

        PropertyData<?> propTitle = factory.createPropertyStringData("dc:title", "new title1");
        PropertyData<?> propDescription = factory.createPropertyStringData("dc:description", "new description1");
        Properties properties = factory.createPropertiesData(Arrays.asList(propTitle, propDescription));

        Holder<String> objectIdHolder = new Holder<>(ob.getId());
        objService.updateProperties(repositoryId, objectIdHolder, null, properties, null);

        sleepForFulltext();

        waitForIndexing();

        ObjectList res;
        String statement;

        statement = "SELECT cmis:name FROM File WHERE CONTAINS('title1')";
        res = query(statement);
        assertEquals(1, res.getNumItems().intValue());
        assertEquals("new title1", getString(res.getObjects().get(0), PropertyIds.NAME));

        statement = "SELECT cmis:name FROM File" + " WHERE CONTAINS('description1')";
        res = query(statement);
        assertEquals(1, res.getNumItems().intValue());
        assertEquals("new title1", getString(res.getObjects().get(0), PropertyIds.NAME));

        if (supportsMultipleFulltextIndexes()) {
            // specific query for title index (the description token do not
            // match)
            statement = "SELECT cmis:name FROM File" + " WHERE CONTAINS('nx:title:description1')";
            res = query(statement);
            assertEquals(0, res.getNumItems().intValue());

            statement = "SELECT cmis:name FROM File" + " WHERE CONTAINS('nx:title:title1')";
            res = query(statement);
            assertEquals(1, res.getNumItems().intValue());
            assertEquals("new title1", getString(res.getObjects().get(0), PropertyIds.NAME));

            // query for invalid index name
            try {
                statement = "SELECT cmis:name FROM File" //
                        + " WHERE CONTAINS('nx:borked:title1')";
                res = query(statement);
                if (!useElasticsearch()) { // ES turns this into the regular fulltext query
                    fail();
                }
            } catch (CmisInvalidArgumentException e) {
                assertTrue(e.getMessage(), e.getMessage().contains("No such fulltext index: borked"));
            }
        }
    }

    /**
     * Test the relax mode having multiple {@code CONTAINS()}s in CMISQL. The relax mode does not follow the CMIS
     * specification 1.1 where at most one {@code CONTAINS()} function MUST be included in a single query statement
     * (section 2.1.14.2.4.4). {@code JOIN}s are not supported yet.
     *
     * @see https://jira.nuxeo.com/browse/NXP-19858
     */
    @Test
    @LocalDeploy("org.nuxeo.ecm.core.opencmis.tests.tests:OSGI-INF/test-relax-cmis-spec.xml")
    public void testQueryMultiContainsRelaxingSpec() throws Exception {

        assumeFalse("DBS does not support multiple CONTAINS", coreFeature.getStorageConfiguration().isDBS());
        // when using JOINs, we use the CMISQLQueryMaker which hasn't been updated to allow multiple CONTAINs
        assumeFalse("JOINs are not supported", supportsJoins());

        ConfigurationService configService = Framework.getService(ConfigurationService.class);
        assertTrue(configService.isBooleanPropertyTrue(NuxeoRepository.RELAX_CMIS_SPEC));

        ObjectData ob = getObjectByPath("/testfolder1/testfile1");
        assertEquals("testfile1_Title", getString(ob, "dc:title"));

        PropertyData<?> propTitle = factory.createPropertyStringData("dc:title", "new title1");
        PropertyData<?> propDescription = factory.createPropertyStringData("dc:description", "new description1");
        Properties properties = factory.createPropertiesData(Arrays.asList(propTitle, propDescription));
        Holder<String> objectIdHolder = new Holder<>(ob.getId());
        objService.updateProperties(repositoryId, objectIdHolder, null, properties, null);

        sleepForFulltext();
        waitForIndexing();
        ObjectList res;

        res = query("SELECT cmis:name FROM File WHERE CONTAINS('title1') OR CONTAINS('anotherTitle')");
        assertEquals(1, res.getNumItems().intValue());
        assertEquals("new title1", getString(res.getObjects().get(0), PropertyIds.NAME));

        res = query("SELECT cmis:name FROM File WHERE CONTAINS('description1') OR CONTAINS('anotherDescription')");
        assertEquals(1, res.getNumItems().intValue());
        assertEquals("new title1", getString(res.getObjects().get(0), PropertyIds.NAME));
    }

    @Test
    public void testQueryMultiConainsFollowingSpec() throws Exception {

        ConfigurationService configService = Framework.getService(ConfigurationService.class);
        assertFalse(configService.isBooleanPropertyTrue(NuxeoRepository.RELAX_CMIS_SPEC));

        ObjectData ob = getObjectByPath("/testfolder1/testfile1");
        assertEquals("testfile1_Title", getString(ob, "dc:title"));

        String statement = "SELECT cmis:name FROM File WHERE CONTAINS('testfile1_Title') OR CONTAINS('anotherTitle')";
        try {
            query(statement);
            fail();
        } catch (CmisInvalidArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("At most one CONTAINS() is allowed"));
        }
    }

    @Test
    public void testQueryContainsQualifier() throws Exception {

        ObjectData ob = getObjectByPath("/testfolder1/testfile1");
        assertEquals("testfile1_Title", getString(ob, "dc:title"));

        PropertyData<?> propTitle = factory.createPropertyStringData("dc:title", "new title1");
        PropertyData<?> propDescription = factory.createPropertyStringData("dc:description", "new description1");
        Properties properties = factory.createPropertiesData(Arrays.asList(propTitle, propDescription));

        Holder<String> objectIdHolder = new Holder<>(ob.getId());
        objService.updateProperties(repositoryId, objectIdHolder, null, properties, null);

        sleepForFulltext();
        waitForIndexing();

        // this failed in CMISQL -> SQL mode (NXP-17512)
        String statement = "SELECT f.* FROM File f WHERE CONTAINS(f, 'title1')";
        ObjectList res = query(statement);
        assertEquals(1, res.getNumItems().intValue());
        assertEquals("new title1", getString(res.getObjects().get(0), PropertyIds.NAME));
    }

    @Test
    public void testQueryContainsSyntax() throws Exception {
        ObjectData ob = getObjectByPath("/testfolder1/testfile1");
        assertEquals("testfile1_Title", getString(ob, "dc:title"));

        PropertyData<?> propTitle = factory.createPropertyStringData("dc:title", "new title1");
        PropertyData<?> propDescription = factory.createPropertyStringData("dc:description", "new description1");
        Properties properties = factory.createPropertiesData(Arrays.asList(propTitle, propDescription));

        Holder<String> objectIdHolder = new Holder<>(ob.getId());
        objService.updateProperties(repositoryId, objectIdHolder, null, properties, null);

        sleepForFulltext();

        waitForIndexing();

        ObjectList res;
        String statement;

        statement = "SELECT cmis:name FROM File WHERE CONTAINS('title1 description1')";
        res = query(statement);
        assertEquals(1, res.getNumItems().intValue());
        assertEquals("new title1", getString(res.getObjects().get(0), PropertyIds.NAME));

        statement = "SELECT cmis:name FROM File WHERE CONTAINS('title1 AND description1')";
        res = query(statement);
        assertEquals(1, res.getNumItems().intValue());
        assertEquals("new title1", getString(res.getObjects().get(0), PropertyIds.NAME));

        statement = "SELECT cmis:name FROM File WHERE CONTAINS('title1 OR blorgzap')";
        res = query(statement);
        assertEquals(1, res.getNumItems().intValue());
        assertEquals("new title1", getString(res.getObjects().get(0), PropertyIds.NAME));
    }

    @Test
    public void testQueryScore() throws Exception {
        sleepForFulltext();

        waitForIndexing();

        ObjectList res;
        String statement;
        ObjectData data;

        // Oracle cannot match on testfile2_Title, because it gets split
        // so match on a single word "football"

        statement = "SELECT cmis:name, SCORE() FROM File" //
                + " WHERE CONTAINS('football')";
        res = query(statement);
        assertEquals(1, res.getNumItems().intValue());
        data = res.getObjects().get(0);
        assertEquals("testfile2_Title", getString(data, PropertyIds.NAME));
        assertNotNull(getValue(data, "SEARCH_SCORE")); // name from spec

        // using an alias for the score
        statement = "SELECT cmis:name, SCORE() AS priority FROM File" //
                + " WHERE CONTAINS('football')";
        res = query(statement);
        assertEquals(1, res.getNumItems().intValue());
        data = res.getObjects().get(0);
        assertEquals("testfile2_Title", getString(data, PropertyIds.NAME));
        assertNotNull(getValue(data, "priority"));

        // ORDER BY score
        statement = "SELECT cmis:name, SCORE() importance FROM File" //
                + " WHERE CONTAINS('football')" //
                + " ORDER BY importance DESC";
        res = query(statement);
        assertEquals(1, res.getNumItems().intValue());
        data = res.getObjects().get(0);
        assertEquals("testfile2_Title", getString(data, PropertyIds.NAME));
        assertNotNull(getValue(data, "importance"));
    }

    @Test
    public void testQueryJoin() throws Exception {
        assumeSupportsJoins();

        String statement;
        ObjectList res;
        ObjectData data;

        String folder2id = getObjectByPath("/testfolder2").getId();
        String folder3id = getObjectByPath("/testfolder2/testfolder3").getId();
        String folder4id = getObjectByPath("/testfolder2/testfolder4").getId();

        waitForIndexing();

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
    public void testQueryJoinWithSubQueryMulti() throws Exception {
        assumeSupportsJoins();

        waitForIndexing();

        String statement = "SELECT A.cmis:objectId, B.cmis:objectId" //
                + " FROM cmis:document A" //
                + " LEFT JOIN File B ON A.cmis:objectId = B.cmis:objectId" //
                + " WHERE 'foo' = ANY B.dc:subjects";
        ObjectList res = query(statement);
        assertEquals(1, res.getNumItems().intValue());
    }

    @Test
    public void testQueryJoinWithSubQueryMultiIsNull() throws Exception {
        assumeSupportsJoins();

        waitForIndexing();

        String statement = "SELECT A.cmis:objectId, B.cmis:objectId" //
                + " FROM cmis:document A" //
                + " LEFT JOIN File B ON A.cmis:objectId = B.cmis:objectId" //
                + " WHERE B.dc:subjects IS NULL";
        ObjectList res = query(statement);
        assertEquals(5, res.getNumItems().intValue()); // 4 docs, 1 version
    }

    @Test
    public void testQueryJoinWithSecurity() throws Exception {
        assumeSupportsJoins();

        reSetUp("bob");
        // only testfile1 and testfile2 are accessible by bob

        String statement;
        ObjectList res;

        waitForIndexing();

        // INNER JOIN
        statement = "SELECT A.cmis:objectId, A.dc:title, B.cmis:objectId, B.dc:title" //
                + " FROM cmis:folder A" //
                + " JOIN cmis:folder B ON A.cmis:objectId = B.cmis:parentId" //
                + " WHERE A.cmis:name = 'testfolder2_Title'" //
                + " ORDER BY B.dc:title";
        res = query(statement);
        assertEquals(0, res.getNumItems().intValue());

        // INNER JOIN
        statement = "SELECT A.cmis:objectId, B.cmis:objectId" //
                + " FROM cmis:document A" //
                + " JOIN File B ON A.cmis:objectId = B.cmis:objectId" //
                + " WHERE B.cmis:name NOT IN ('testfile3_Title', 'testfile4_Title')";
        res = query(statement);
        assertEquals(2, res.getNumItems().intValue());

        // LEFT JOIN
        statement = "SELECT A.cmis:objectId, B.cmis:objectId" //
                + " FROM cmis:document A" //
                + " LEFT JOIN File B ON A.cmis:objectId = B.cmis:objectId" //
                + " WHERE B.cmis:name NOT IN ('testfile3_Title', 'testfile4_Title')";
        res = query(statement);
        assertEquals(2, res.getNumItems().intValue());

        statement = "SELECT A.cmis:objectId, A.cmis:name, B.filename, C.note" //
                + " FROM cmis:document A" //
                + " LEFT JOIN File B ON A.cmis:objectId = B.cmis:objectId" //
                + " LEFT JOIN Note C ON A.cmis:objectId = C.cmis:objectId" //
                + " WHERE (A.cmis:objectTypeId NOT IN ('File')" //
                + "     OR B.cmis:name NOT IN ('testfile3_Title', 'testfile4_Title'))";
        res = query(statement);
        assertEquals(2, res.getNumItems().intValue());
    }

    @Test
    public void testQueryJoinWithFacets() throws Exception {
        assumeSupportsJoins();

        waitForIndexing();

        String statement = "SELECT A.cmis:objectId" //
                + " FROM cmis:folder A" //
                + " JOIN cmis:folder B ON A.cmis:objectId = B.cmis:parentId" //
                + " WHERE ANY A.nuxeo:secondaryObjectTypeIds NOT IN ('Foo')";
        ObjectList res = query(statement);
        assertEquals(4, res.getNumItems().intValue()); // root too
    }

    @Test
    public void testQueryJoinReturnVirtualColumns() throws Exception {
        assumeSupportsJoins();

        waitForIndexing();

        String statement = "SELECT A.cmis:objectId, A.nuxeo:contentStreamDigest, B.cmis:path" //
                + " FROM cmis:document A" //
                + " JOIN cmis:folder B ON A.nuxeo:parentId = B.cmis:objectId" //
                + " WHERE A.cmis:name = 'testfile1_Title'";
        ObjectList res = query(statement);
        assertEquals(1, res.getNumItems().intValue());

        ObjectData data = res.getObjects().get(0);
        assertNotNull(getQueryValue(data, "A.nuxeo:contentStreamDigest"));
        assertEquals("/testfolder1", getQueryValue(data, "B.cmis:path"));
    }

    @Test
    public void testQueryJoinWithMultipleTypes() throws Exception {
        assumeSupportsJoins();

        waitForIndexing();

        String statement = "SELECT A.cmis:objectId, A.cmis:name, B.filename, C.note" //
                + " FROM cmis:document A" //
                + " LEFT JOIN File B ON A.cmis:objectId = B.cmis:objectId" //
                + " LEFT JOIN Note C ON A.cmis:objectId = C.cmis:objectId" //
                + " WHERE ANY A.nuxeo:secondaryObjectTypeIds NOT IN ('Foo')" //
                + "   AND (A.cmis:objectTypeId NOT IN ('File')" //
                + "     OR B.cmis:name NOT IN ('testfile3_Title', 'testfile4_Title'))";
        ObjectList res = query(statement);
        assertEquals(5, res.getNumItems().intValue()); // 4 docs, 1 version
    }

    @Test
    public void testQueryJoinWithMultipleTypes2() throws Exception {
        assumeSupportsJoins();

        waitForIndexing();

        String statement = "SELECT A.cmis:objectId, B.cmis:objectId, C.cmis:objectId" //
                + " FROM cmis:document A" //
                + " LEFT JOIN File B ON A.cmis:objectId = B.cmis:objectId" //
                + " LEFT JOIN Note C ON A.cmis:objectId = C.cmis:objectId" //
                + " WHERE B.cmis:name NOT IN ('testfile3_Title', 'testfile4_Title')";
        ObjectList res = query(statement);
        assertEquals(2, res.getNumItems().intValue());
    }

    @Test
    public void testQueryJoinSecondaryType() throws Exception {
        // this is a JOIN with secondary type, always valid even if JOINs are not supported

        // not implemented for direct CMISQL -> SQL translation
        assumeFalse("not implemented", supportsJoins());

        DocumentModel doc = coreSession.getDocument(new PathRef("/testfolder1/testfile1"));
        doc.addFacet("CustomFacetWithMySchema2");
        doc.setPropertyValue("my2:string", "foo");
        coreSession.saveDocument(doc);
        coreSession.save();
        nextTransaction();
        waitForIndexing();

        String statement = "SELECT *" //
                + " FROM cmis:document D" //
                + " JOIN facet:CustomFacetWithMySchema2 F" //
                + " ON D.cmis:objectId = F.cmis:objectId" //
                + " WHERE D.cmis:name = 'testfile1_Title'" //
                + " AND F.my2:string = 'foo'";
        ObjectList res = query(statement);
        assertEquals(1, res.getNumItems().intValue());

        statement = "SELECT *" //
                + " FROM cmis:document D" //
                + " JOIN facet:CustomFacetWithMySchema2 F" //
                + " ON D.cmis:objectId = F.cmis:objectId" //
                + " WHERE F.my2:string = 'notfoo'";
        res = query(statement);
        assertEquals(0, res.getNumItems().intValue());
    }

    @Test
    public void testQueryBad() throws Exception {
        try {
            query("SELECT foo bar baz");
            fail();
        } catch (CmisInvalidArgumentException e) {
            assertTrue(e.getMessage().contains("line 1:15 missing FROM at 'baz'"));
        }
        try {
            query("SELECT foo FROM bar");
            fail();
        } catch (CmisInvalidArgumentException e) {
            assertTrue(e.getMessage().contains("bar is neither a type query name nor an alias"));
        }
        try {
            query("SELECT foo FROM cmis:folder");
            fail();
        } catch (CmisInvalidArgumentException e) {
            assertTrue(e.getMessage().contains("foo is not a property query name in any of the types"));
        }
    }

    @Test
    public void testQueryBatching() throws Exception {
        int NUM = 20;
        for (int i = 0; i < NUM; i++) {
            String name = String.format("somedoc%03d", Integer.valueOf(i));
            objService.createDocument(repositoryId, createBaseDocumentProperties(name, "cmis:document"), rootFolderId,
                    null, VersioningState.CHECKEDOUT, null, null, null, null);
        }

        waitForIndexing();

        ObjectList res;
        List<ObjectData> objects;
        String statement = "SELECT cmis:name FROM cmis:document"
                + " WHERE cmis:name LIKE 'somedoc%' ORDER BY cmis:name";
        res = discService.query(repositoryId, statement, Boolean.TRUE, null, null, null, null, null, null);
        assertEquals(NUM, res.getNumItems().intValue());
        objects = res.getObjects();
        assertEquals(NUM, objects.size());
        assertEquals("somedoc000", getString(objects.get(0), PropertyIds.NAME));
        assertEquals("somedoc019", getString(objects.get(objects.size() - 1), PropertyIds.NAME));
        // batch
        res = discService.query(repositoryId, statement, Boolean.TRUE, null, null, null, BigInteger.valueOf(10),
                BigInteger.valueOf(5), null);
        assertEquals(NUM, res.getNumItems().intValue());
        objects = res.getObjects();
        assertEquals(10, objects.size());
        assertEquals("somedoc005", getString(objects.get(0), PropertyIds.NAME));
        assertEquals("somedoc014", getString(objects.get(objects.size() - 1), PropertyIds.NAME));
    }

    @Test
    public void testQueryPWC() throws Exception {
        waitForIndexing();

        ObjectList list = navService.getCheckedOutDocs(repositoryId, null, null, null, null, null, null, null, null,
                null);
        // TODO XXX proxy shouldn't be considered checked out
        assertEquals(supportsProxies() ? 5 : 4, list.getNumItems().intValue()); // 4 docs, 1 proxy

        ObjectData ob = getObjectByPath("/testfolder1/testfile1");
        String id = ob.getId();
        Holder<String> idHolder = new Holder<>(id);
        verService.checkIn(repositoryId, idHolder, Boolean.TRUE, null, null, "comment", null, null, null, null);

        waitForIndexing();

        list = navService.getCheckedOutDocs(repositoryId, null, null, null, null, null, null, null, null, null);
        // TODO XXX proxy shouldn't be considered checked out
        assertEquals(supportsProxies() ? 4 : 3, list.getNumItems().intValue()); // 3 docs, 1 proxy

        verService.checkOut(repositoryId, idHolder, null, null);

        waitForIndexing();

        // re-checkout (ecm:isCheckedIn now false instead of null earlier)
        list = navService.getCheckedOutDocs(repositoryId, null, null, null, null, null, null, null, null, null);
        // TODO XXX proxy shouldn't be considered checked out
        assertEquals(supportsProxies() ? 5 : 4, list.getNumItems().intValue()); // 4 docs, 1 proxy

        // with folder and filter and order
        ObjectData f1 = getObjectByPath("/testfolder1");
        list = navService.getCheckedOutDocs(repositoryId, f1.getId(), "cmis:name", "cmis:name DESC", null, null, null,
                null, null, null);
        assertEquals(3, list.getNumItems().intValue());
        List<ObjectData> objects = list.getObjects();
        assertEquals("testfile3_Title", getValue(objects.get(0), "cmis:name"));
        assertEquals("testfile2_Title", getValue(objects.get(1), "cmis:name"));
        assertEquals("testfile1_Title", getValue(objects.get(2), "cmis:name"));
    }

    @Test
    public void testQueryAllVersions() throws Exception {
        ObjectData ob = getObjectByPath("/testfolder1/testfile1");
        String id = ob.getId();

        // two versions
        Holder<String> idHolder = new Holder<>(id);
        verService.checkIn(repositoryId, idHolder, Boolean.TRUE, null, null, "comment", null, null, null, null);
        verService.checkOut(repositoryId, idHolder, null, null);
        verService.checkIn(repositoryId, idHolder, Boolean.TRUE, null, null, "comment", null, null, null, null);

        waitForIndexing();

        ObjectList res;
        String statement = "SELECT cmis:objectId FROM cmis:document" + " WHERE cmis:name = 'testfile1_Title'";

        // search all versions
        res = discService.query(repositoryId, statement, Boolean.TRUE, null, null, null, null, null, null);
        assertEquals(3, res.getNumItems().intValue());

        // do not search all versions (only latest)
        res = discService.query(repositoryId, statement, Boolean.FALSE, null, null, null, null, null, null);
        assertEquals(1, res.getNumItems().intValue());
        res = discService.query(repositoryId, statement, null, null, null, null, null, null, null);
        assertEquals(1, res.getNumItems().intValue());

        statement = "SELECT major_version, minor_version, cmis:versionLabel FROM File WHERE cmis:name = 'testfile1_Title'";
        res = discService.query(repositoryId, statement, Boolean.FALSE, null, null, null, null, null, null);
        assertEquals(1, res.getNumItems().intValue());
        ObjectData singleResult = res.getObjects().get(0);
        assertEquals("2.0", getValue(singleResult, PropertyIds.VERSION_LABEL));
        assertEquals(BigInteger.valueOf(2), getValue(singleResult, "major_version"));
        assertEquals(BigInteger.valueOf(0), getValue(singleResult, "minor_version"));
    }

    @Test
    public void testQueryAllVersionsFolders() throws Exception {
        ObjectList res;
        Boolean searchAllVersions;

        waitForIndexing();

        String statement = "SELECT cmis:objectId FROM cmis:folder" + " WHERE cmis:name = 'testfolder2_Title'";

        searchAllVersions = Boolean.TRUE;
        res = discService.query(repositoryId, statement, searchAllVersions, null, null, null, null, null, null);
        assertEquals(1, res.getNumItems().intValue());

        searchAllVersions = Boolean.FALSE;
        res = discService.query(repositoryId, statement, searchAllVersions, null, null, null, null, null, null);
        assertEquals(1, res.getNumItems().intValue());

        searchAllVersions = null;
        res = discService.query(repositoryId, statement, searchAllVersions, null, null, null, null, null, null);
        assertEquals(1, res.getNumItems().intValue());
    }


    @Test
    //NXP-23164
    public void testNonPrefixedFields() throws Exception {
        ObjectList res;
        String statement;

        String id = objService.createDocument(repositoryId, createBaseDocumentProperties("nonpref_doc", "File"),
                rootFolderId, null, VersioningState.MINOR, null, null, null, null);
        assertNotNull(id);
        ObjectData ob = getObject(id);

        //Initially icon is null
        checkValue("icon", null, ob);
        Properties props = createProperties("icon", "my/icon");
        Holder idHolder = new Holder<>(ob.getId());
        objService.updateProperties(repositoryId, idHolder, null, props, null);

        waitForIndexing();

        statement = "SELECT icon, minor_version FROM File WHERE cmis:objectId = '"+id+"'";
        res = query(statement);
        assertEquals(1, res.getNumItems().intValue());
        ObjectData singleResult = res.getObjects().get(0);
        assertEquals(BigInteger.valueOf(1), getValue(singleResult, "minor_version"));
        assertEquals("my/icon", getValue(singleResult, "icon"));
    }

    @Test
    // NXP-12776 randomly failing
    @Ignore
    public void testVersioning() throws Exception {
        ObjectData ob = getObjectByPath("/testfolder1/testfile1");
        String id = ob.getId();

        waitForIndexing();

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
        checkValue(PropertyIds.IS_PRIVATE_WORKING_COPY, Boolean.TRUE, ob);
        checkValue(NuxeoTypeHelper.NX_ISVERSION, Boolean.FALSE, ob);
        checkValue(NuxeoTypeHelper.NX_ISCHECKEDIN, Boolean.FALSE, ob);
        String series = (String) getValue(ob, PropertyIds.VERSION_SERIES_ID);

        // check in major -> version 1.0

        Holder<String> idHolder = new Holder<>(id);
        verService.checkIn(repositoryId, idHolder, Boolean.TRUE, null, null, "comment", null, null, null, null);

        waitForIndexing();

        String vid = idHolder.getValue();
        ObjectData ver = getObject(vid);

        checkValue(PropertyIds.IS_LATEST_VERSION, Boolean.TRUE, ver);
        checkValue(PropertyIds.IS_MAJOR_VERSION, Boolean.TRUE, ver);
        checkValue(PropertyIds.IS_LATEST_MAJOR_VERSION, Boolean.TRUE, ver);
        checkValue(PropertyIds.VERSION_LABEL, "1.0", ver);
        checkValue(PropertyIds.VERSION_SERIES_ID, series, ver);
        checkValue(PropertyIds.IS_VERSION_SERIES_CHECKED_OUT, Boolean.FALSE, ver);
        checkValue(PropertyIds.VERSION_SERIES_CHECKED_OUT_ID, null, ver);
        checkValue(PropertyIds.VERSION_SERIES_CHECKED_OUT_BY, null, ver);
        checkValue(PropertyIds.CHECKIN_COMMENT, "comment", ver);
        checkValue(PropertyIds.IS_PRIVATE_WORKING_COPY, Boolean.FALSE, ver);
        checkValue(NuxeoTypeHelper.NX_ISVERSION, Boolean.TRUE, ver);
        checkValue(NuxeoTypeHelper.NX_ISCHECKEDIN, Boolean.TRUE, ver); // hm

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

        // not viewed as a version according to Nuxeo semantics though
        ob = getObjectByPath("/testfolder1/testfile1");
        checkValue(PropertyIds.IS_PRIVATE_WORKING_COPY, Boolean.FALSE, ob);
        checkValue(NuxeoTypeHelper.NX_ISVERSION, Boolean.FALSE, ob);
        checkValue(NuxeoTypeHelper.NX_ISCHECKEDIN, Boolean.TRUE, ob);

        // check out

        Holder<Boolean> cchold = new Holder<>();
        verService.checkOut(repositoryId, idHolder, null, cchold);

        waitForIndexing();

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
        checkValue(PropertyIds.VERSION_SERIES_CHECKED_OUT_BY, USERNAME, co);
        checkValue(PropertyIds.CHECKIN_COMMENT, null, co);
        checkValue(PropertyIds.IS_PRIVATE_WORKING_COPY, Boolean.TRUE, co);
        checkValue(NuxeoTypeHelper.NX_ISVERSION, Boolean.FALSE, co);
        checkValue(NuxeoTypeHelper.NX_ISCHECKEDIN, Boolean.FALSE, co);

        // check in minor -> version 1.1

        idHolder.setValue(coid);
        verService.checkIn(repositoryId, idHolder, Boolean.FALSE, null, null, "comment2", null, null, null, null);

        waitForIndexing();

        String v2id = idHolder.getValue();
        ObjectData ver2 = getObject(v2id);

        checkValue(PropertyIds.IS_LATEST_VERSION, Boolean.TRUE, ver2);
        checkValue(PropertyIds.IS_MAJOR_VERSION, Boolean.FALSE, ver2);
        checkValue(PropertyIds.IS_LATEST_MAJOR_VERSION, Boolean.FALSE, ver2);
        checkValue(PropertyIds.VERSION_LABEL, "1.1", ver2);
        checkValue(PropertyIds.VERSION_SERIES_ID, series, ver2);
        checkValue(PropertyIds.IS_VERSION_SERIES_CHECKED_OUT, Boolean.FALSE, ver2);
        checkValue(PropertyIds.VERSION_SERIES_CHECKED_OUT_ID, null, ver2);
        checkValue(PropertyIds.VERSION_SERIES_CHECKED_OUT_BY, null, ver2);
        checkValue(PropertyIds.CHECKIN_COMMENT, "comment2", ver2);
        checkValue(PropertyIds.IS_PRIVATE_WORKING_COPY, Boolean.FALSE, ver2);
        checkValue(NuxeoTypeHelper.NX_ISVERSION, Boolean.TRUE, ver2);
        checkValue(NuxeoTypeHelper.NX_ISCHECKEDIN, Boolean.TRUE, ver2);

        // check out again (with no content copied holder)

        verService.checkOut(repositoryId, idHolder, null, null);
        coid = idHolder.getValue();
        co = getObject(coid);
        assertEquals(id, coid); // Nuxeo invariant

        // cancel check out

        waitForAsyncCompletion();
        verService.cancelCheckOut(repositoryId, coid, null);
        checkValue(PropertyIds.IS_VERSION_SERIES_CHECKED_OUT, Boolean.FALSE, ver2);
        ci = getObject(id);
        checkValue(PropertyIds.IS_LATEST_VERSION, Boolean.TRUE, ci);

        // not viewed as a version according to Nuxeo semantics
        checkValue(NuxeoTypeHelper.NX_ISVERSION, Boolean.FALSE, ci);

        // list all versions
        // TODO check this when no live document exists

        // have a checked out doc
        idHolder.setValue(id);
        verService.checkOut(repositoryId, idHolder, null, null);
        // atompub passes just object id, soap just version series id
        List<ObjectData> vers = verService.getAllVersions(repositoryId, id, null, null, null, null);
        assertEquals(3, vers.size());
        assertEquals(id, vers.get(0).getId());
        assertEquals(ver2.getId(), vers.get(1).getId());
        assertEquals(ver.getId(), vers.get(2).getId());

        // get latest version

        Boolean major = Boolean.FALSE;
        ObjectData l = verService.getObjectOfLatestVersion(repositoryId, id, null, major, null, null, null, null, null,
                null, null);
        assertEquals(ver2.getId(), l.getId());
        // also works on a version object
        l = verService.getObjectOfLatestVersion(repositoryId, ver.getId(), null, major, null, null, null, null, null,
                null, null);
        assertEquals(ver2.getId(), l.getId());
        // latest major version
        major = Boolean.TRUE;
        l = verService.getObjectOfLatestVersion(repositoryId, id, null, major, null, null, null, null, null, null,
                null);
        assertEquals(ver.getId(), l.getId());
        l = verService.getObjectOfLatestVersion(repositoryId, ver2.getId(), null, major, null, null, null, null, null,
                null, null);
        assertEquals(ver.getId(), l.getId());

        major = Boolean.FALSE;
        Properties p = verService.getPropertiesOfLatestVersion(repositoryId, id, null, major, null, null);
        assertEquals(ver2.getId(), p.getProperties().get(PropertyIds.OBJECT_ID).getFirstValue());
    }

    @Test
    public void testCancelCheckout() throws Exception {
        ObjectData ob = getObjectByPath("/testfolder1/testfile1");
        String id = ob.getId();
        waitForAsyncCompletion();
        verService.cancelCheckOut(repositoryId, id, null);
        try {
            getObject(id);
            fail("Document should be deleted");
        } catch (CmisObjectNotFoundException e) {
            // ok
        }
    }

    @Test
    public void testCheckInWithChanges() throws Exception {
        ObjectData ob = getObjectByPath("/testfolder1/testfile1");
        String id = ob.getId();

        // check in with data
        Properties props = createProperties("dc:title", "newtitle");
        byte[] bytes = "foo-bar".getBytes("UTF-8");
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        ContentStream cs = new ContentStreamImpl("test.txt", BigInteger.valueOf(bytes.length), "text/plain", in);

        Holder<String> idHolder = new Holder<>(id);
        harness.deployContrib("org.nuxeo.ecm.core.opencmis.tests.tests", "OSGI-INF/comment-listener-contrib.xml");
        try {
            CommentListener.clearComments();
            verService.checkIn(repositoryId, idHolder, Boolean.TRUE, props, cs, "comment", null, null, null, null);
        } finally {
            harness.undeployContrib("org.nuxeo.ecm.core.opencmis.tests.tests", "OSGI-INF/comment-listener-contrib.xml");
        }
        List<String> comments = CommentListener.getComments();
        assertEquals(Arrays.asList("documentModified:comment=comment,checkInComment=null",
                "documentCheckedIn:comment=1.0 comment,checkInComment=comment",
                "documentCreated:comment=1.0 comment,checkInComment=comment"), comments);

        String vid = idHolder.getValue();
        ObjectData ver = getObject(vid);

        checkValue(PropertyIds.IS_LATEST_VERSION, Boolean.TRUE, ver);
        checkValue(PropertyIds.VERSION_LABEL, "1.0", ver);
        checkValue(PropertyIds.CHECKIN_COMMENT, "comment", ver);

        // check changes applied
        checkValue("dc:title", "newtitle", ver);
        ContentStream cs2 = objService.getContentStream(repositoryId, ver.getId(), null, null, null, null);
        assertEquals("text/plain", cs2.getMimeType());
        assertEquals(bytes.length, cs2.getLength());
        assertEquals("test.txt", cs2.getFileName());
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        IOUtils.copy(cs2.getStream(), os);
        assertEquals("foo-bar", os.toString("UTF-8"));
    }

    @Test
    public void testVersioningInitialState() {

        // creation as major version (default, per spec)

        String id = objService.createDocument(repositoryId, createBaseDocumentProperties("newdoc2", "cmis:document"),
                rootFolderId, null, VersioningState.MAJOR, null, null, null, null);
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
        checkValue(NuxeoTypeHelper.NX_ISVERSION, Boolean.FALSE, ob); // ...

        // copy from checked in source as checked out

        id = objService.createDocumentFromSource(repositoryId, id, null, rootFolderId, VersioningState.CHECKEDOUT, null,
                null, null, null);
        ob = getObject(id);
        checkValue(PropertyIds.IS_LATEST_VERSION, Boolean.FALSE, ob);
        checkValue(PropertyIds.IS_MAJOR_VERSION, Boolean.FALSE, ob);
        checkValue(PropertyIds.IS_LATEST_MAJOR_VERSION, Boolean.FALSE, ob);
        checkValue(PropertyIds.VERSION_LABEL, null, ob);
        checkValue(PropertyIds.VERSION_SERIES_ID, NOT_NULL, ob);
        checkValue(PropertyIds.IS_VERSION_SERIES_CHECKED_OUT, Boolean.TRUE, ob);
        checkValue(PropertyIds.VERSION_SERIES_CHECKED_OUT_ID, id, ob);
        checkValue(PropertyIds.VERSION_SERIES_CHECKED_OUT_BY, USERNAME, ob);
        checkValue(PropertyIds.CHECKIN_COMMENT, null, ob);
        checkValue(NuxeoTypeHelper.NX_ISVERSION, Boolean.FALSE, ob);

        // creation as minor version

        id = objService.createDocument(repositoryId, createBaseDocumentProperties("newdoc2", "cmis:document"),
                rootFolderId, null, VersioningState.MINOR, null, null, null, null);
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
        checkValue(NuxeoTypeHelper.NX_ISVERSION, Boolean.FALSE, ob); // ...

        // creation checked out

        id = objService.createDocument(repositoryId, createBaseDocumentProperties("newdoc3", "cmis:document"),
                rootFolderId, null, VersioningState.CHECKEDOUT, null, null, null, null);
        ob = getObject(id);

        checkValue(PropertyIds.IS_LATEST_VERSION, Boolean.FALSE, ob);
        checkValue(PropertyIds.IS_MAJOR_VERSION, Boolean.FALSE, ob);
        checkValue(PropertyIds.IS_LATEST_MAJOR_VERSION, Boolean.FALSE, ob);
        checkValue(PropertyIds.VERSION_LABEL, null, ob);
        checkValue(PropertyIds.VERSION_SERIES_ID, NOT_NULL, ob);
        checkValue(PropertyIds.IS_VERSION_SERIES_CHECKED_OUT, Boolean.TRUE, ob);
        checkValue(PropertyIds.VERSION_SERIES_CHECKED_OUT_ID, id, ob);
        checkValue(PropertyIds.VERSION_SERIES_CHECKED_OUT_BY, USERNAME, ob);
        checkValue(PropertyIds.CHECKIN_COMMENT, null, ob);
        checkValue(NuxeoTypeHelper.NX_ISVERSION, Boolean.FALSE, ob);

        // copy from checked out source as checked in

        id = objService.createDocumentFromSource(repositoryId, id, null, rootFolderId, VersioningState.MAJOR, null,
                null, null, null);
        ob = getObject(id);
        checkValue(PropertyIds.IS_LATEST_VERSION, Boolean.TRUE, ob);
        checkValue(PropertyIds.IS_MAJOR_VERSION, Boolean.TRUE, ob);
        checkValue(PropertyIds.IS_LATEST_MAJOR_VERSION, Boolean.TRUE, ob);
        checkValue(PropertyIds.VERSION_LABEL, "1.0", ob);
        checkValue(PropertyIds.VERSION_SERIES_ID, NOT_NULL, ob);
        checkValue(PropertyIds.IS_VERSION_SERIES_CHECKED_OUT, Boolean.FALSE, ob);
        checkValue(PropertyIds.VERSION_SERIES_CHECKED_OUT_ID, null, ob);
        checkValue(PropertyIds.VERSION_SERIES_CHECKED_OUT_BY, null, ob);
        checkValue(PropertyIds.CHECKIN_COMMENT, null, ob);
        checkValue(NuxeoTypeHelper.NX_ISVERSION, Boolean.FALSE, ob); // ...
    }

    @Test
    public void testProxyVersionProperties() throws Exception {
        assumeSupportsProxies();

        // check proxy to a version

        ObjectData ob = getObjectByPath("/testfolder2/testfile6");
        checkValue("dc:title", "title6", ob);
        checkValue(PropertyIds.IS_LATEST_VERSION, Boolean.TRUE, ob);
        checkValue(PropertyIds.IS_MAJOR_VERSION, Boolean.FALSE, ob);
        checkValue(PropertyIds.IS_LATEST_MAJOR_VERSION, Boolean.FALSE, ob);
        checkValue(PropertyIds.VERSION_LABEL, "0.1", ob);
        checkValue(PropertyIds.VERSION_SERIES_ID, NOT_NULL, ob);
        checkValue(PropertyIds.IS_VERSION_SERIES_CHECKED_OUT, Boolean.FALSE, ob);
        checkValue(PropertyIds.VERSION_SERIES_CHECKED_OUT_ID, null, ob);
        checkValue(PropertyIds.VERSION_SERIES_CHECKED_OUT_BY, null, ob);
        checkValue(PropertyIds.CHECKIN_COMMENT, null, ob);
        checkValue(NuxeoTypeHelper.NX_ISVERSION, Boolean.FALSE, ob); // ...

        // create live proxy

        Helper.sleepForAuditGranularity();
        DocumentModel proxy = coreSession.createProxy(new PathRef("/testfolder1/testfile1"),
                new PathRef("/testfolder2"));
        coreSession.save();
        nextTransaction();
        waitForIndexing();

        // check live proxy

        ob = getObjectByPath("/testfolder2/testfile1");
        checkValue("dc:title", "testfile1_Title", ob);
        checkValue(PropertyIds.IS_LATEST_VERSION, Boolean.FALSE, ob);
        checkValue(PropertyIds.IS_MAJOR_VERSION, Boolean.FALSE, ob);
        checkValue(PropertyIds.IS_LATEST_MAJOR_VERSION, Boolean.FALSE, ob);
        checkValue(PropertyIds.VERSION_LABEL, null, ob);
        checkValue(PropertyIds.VERSION_SERIES_ID, NOT_NULL, ob);
        checkValue(PropertyIds.IS_VERSION_SERIES_CHECKED_OUT, Boolean.TRUE, ob);
        checkValue(PropertyIds.VERSION_SERIES_CHECKED_OUT_ID, NOT_NULL, ob);
        checkValue(PropertyIds.VERSION_SERIES_CHECKED_OUT_BY, USERNAME, ob);
        checkValue(PropertyIds.CHECKIN_COMMENT, null, ob);
        checkValue(NuxeoTypeHelper.NX_ISVERSION, Boolean.FALSE, ob);
    }

    /*
     * NXP-22253
     */
    @Test
    public void testProxyOnNonReadableWorkingCopy() throws Exception {
        assumeSupportsProxies();

        // create a proxy to testfile6 version
        Helper.sleepForAuditGranularity();
        DocumentModel proxy = coreSession.createProxy(new PathRef("/testfolder2/testfolder3/testfile6"),
                new PathRef("/testfolder2/testfolder4"));

        // Grant read right on root for john
        ACP acp = new ACPImpl();
        acp.addACE(ACL.LOCAL_ACL, new ACE("john", SecurityConstants.READ, true));
        coreSession.setACP(new PathRef("/"), acp,  false);

        // Deny read right on testfolder3 for john
        PathRef folder3Ref = new PathRef("/testfolder2/testfolder3");
        acp = coreSession.getACP(folder3Ref);
        acp.blockInheritance(ACL.LOCAL_ACL, "Administrator");
        coreSession.setACP(folder3Ref, acp,  true);

        coreSession.save();
        nextTransaction();
        waitForIndexing();

        reSetUp("john");

        // Try to access the proxy
        ObjectData ob = getObjectByPath("/testfolder2/testfolder4/testfile6");
        checkValue("dc:title", "title6", ob);
        checkValue(PropertyIds.IS_LATEST_VERSION, Boolean.TRUE, ob);
        checkValue(PropertyIds.IS_MAJOR_VERSION, Boolean.FALSE, ob);
        checkValue(PropertyIds.IS_LATEST_MAJOR_VERSION, Boolean.FALSE, ob);
        checkValue(PropertyIds.VERSION_LABEL, "0.1", ob);
        checkValue(PropertyIds.VERSION_SERIES_ID, NOT_NULL, ob);
        checkValue(PropertyIds.IS_VERSION_SERIES_CHECKED_OUT, Boolean.FALSE, ob);
        checkValue(PropertyIds.VERSION_SERIES_CHECKED_OUT_ID, null, ob);
        checkValue(PropertyIds.VERSION_SERIES_CHECKED_OUT_BY, null, ob);
        checkValue(NuxeoTypeHelper.NX_ISVERSION, Boolean.FALSE, ob);

        // Check we can not access the target
        try {
            getObjectByPath("/testfolder2/testfolder3/testfile6");
            fail("We shouldn't have access to this document with the user 'john'");
        } catch (CmisObjectNotFoundException confe) {
            assertEquals("/testfolder2/testfolder3/testfile6", confe.getMessage());
        }

        // check that getAllVersions still returns the proxy
        List<ObjectData> vers = verService.getAllVersions(repositoryId, proxy.getId(), null, null, null, null);
        assertEquals(1, vers.size());
        assertEquals(proxy.getId(), vers.get(0).getId());
    }

    /*
     * NXP-22252
     */
    @Test
    public void testProxyOnNonReadableCheckedOutWorkingCopy() throws Exception {
        assumeSupportsProxies();

        // create a proxy to testfile4 which is a regular document (checked out)
        Helper.sleepForAuditGranularity();
        DocumentModel proxy = coreSession.createProxy(new PathRef("/testfolder2/testfolder3/testfile4"),
                new PathRef("/testfolder2/testfolder4"));

        // Grant read right on root for john
        ACP acp = new ACPImpl();
        acp.addACE(ACL.LOCAL_ACL, new ACE("john", SecurityConstants.READ, true));
        coreSession.setACP(new PathRef("/"), acp,  false);

        // Deny read right on testfolder3 for john
        PathRef folder3Ref = new PathRef("/testfolder2/testfolder3");
        acp = coreSession.getACP(folder3Ref);
        acp.blockInheritance(ACL.LOCAL_ACL, "Administrator");
        coreSession.setACP(folder3Ref, acp,  true);

        coreSession.save();
        nextTransaction();
        waitForIndexing();

        reSetUp("john");

        // Try to access the proxy
        ObjectData ob = getObjectByPath("/testfolder2/testfolder4/testfile4");
        checkValue("dc:title", "testfile4_Title", ob);
        checkValue(PropertyIds.IS_LATEST_VERSION, Boolean.FALSE, ob);
        checkValue(PropertyIds.IS_MAJOR_VERSION, Boolean.FALSE, ob);
        checkValue(PropertyIds.IS_LATEST_MAJOR_VERSION, Boolean.FALSE, ob);
        checkValue(PropertyIds.VERSION_LABEL, null, ob);
        checkValue(PropertyIds.VERSION_SERIES_ID, NOT_NULL, ob);
        checkValue(PropertyIds.IS_VERSION_SERIES_CHECKED_OUT, Boolean.TRUE, ob);
        checkValue(NuxeoTypeHelper.NX_ISVERSION, Boolean.FALSE, ob);
        // NXP-22252 check unavailable properties due to permission
        checkValue(PropertyIds.VERSION_SERIES_CHECKED_OUT_ID, NOT_NULL, ob);
        checkValue(PropertyIds.VERSION_SERIES_CHECKED_OUT_BY, "john", ob);
        checkValue(PropertyIds.CHECKIN_COMMENT, null, ob);

        // check that getAllVersions still returns the proxy
        List<ObjectData> vers = verService.getAllVersions(repositoryId, proxy.getId(), null, null, null, null);
        assertEquals(1, vers.size());
        assertEquals(proxy.getId(), vers.get(0).getId());
    }

    @Test
    public void testGetContentChanges() throws Exception {
        doTestGetContentChanges(false);
    }

    @Test
    public void testGetContentChangesHiddenType() throws Exception {
        doTestGetContentChanges(true);
    }

    protected void doTestGetContentChanges(boolean addHidden) throws Exception {
        List<ObjectData> objects;
        Holder<String> changeLogTokenHolder = new Holder<>();

        if (addHidden) {
            // add a doc whose type is not known to CMIS
            DocumentModel doc = coreSession.createDocumentModel("/", "hidden", "HiddenFolder");
            Helper.sleepForAuditGranularity();
            coreSession.createDocument(doc);
            coreSession.save();
            nextTransaction();
        }

        sleepForAudit();
        String clt1 = repoService.getRepositoryInfo(repositoryId, null).getLatestChangeLogToken();
        assertNotNull(clt1);

        List<ObjectData> allObjects = readAllContentChanges(changeLogTokenHolder);
        if (!addHidden) {
            assertEquals(clt1, changeLogTokenHolder.getValue());
        }

        int n = 13; // last n events
        assertTrue(allObjects.size() >= n);
        objects = allObjects.subList(allObjects.size() - n, allObjects.size());
        checkChange(objects.get(0), "/testfolder1", //
                ChangeType.CREATED, "Folder");
        checkChange(objects.get(1), "/testfolder1/testfile1", ChangeType.CREATED, "File");
        checkChange(objects.get(2), "/testfolder1/testfile2", ChangeType.CREATED, "File");
        checkChange(objects.get(3), "/testfolder1/testfile3", ChangeType.CREATED, "Note");
        checkChange(objects.get(4), "/testfolder2", //
                ChangeType.CREATED, "Folder");
        checkChange(objects.get(5), "/testfolder2/testfolder3", ChangeType.CREATED, "Folder");
        checkChange(objects.get(6), "/testfolder2/testfolder4", ChangeType.CREATED, "Folder");
        checkChange(objects.get(7), "/testfolder2/testfolder3/testfile4", ChangeType.CREATED, "File");
        checkChange(objects.get(8), file5id, ChangeType.CREATED, "File");
        checkChange(objects.get(9), file5id, ChangeType.UPDATED, "File");
        checkChange(objects.get(10), "/testfolder2/testfolder3/testfile6", ChangeType.CREATED, "Note");
        checkChange(objects.get(11), file6verid, ChangeType.CREATED, "Note");
        checkChange(objects.get(12), proxyid, ChangeType.CREATED, "Note");

        // remove a doc

        ObjectData ob1 = getObjectByPath("/testfolder1/testfile1");
        objService.deleteObject(repositoryId, ob1.getId(), Boolean.TRUE, null);
        nextTransaction();

        // get latest change log token
        sleepForAudit();
        String clt2 = repoService.getRepositoryInfo(repositoryId, null).getLatestChangeLogToken();
        assertNotNull(clt2);
        assertNotEquals(clt2, clt1);

        changeLogTokenHolder.setValue(clt2); // just the last
        ObjectList changes;
        changes = discService.getContentChanges(repositoryId, changeLogTokenHolder, Boolean.TRUE, null, null, null,
                BigInteger.valueOf(100), null);
        objects = changes.getObjects();
        assertEquals(1, objects.size());
        checkChange(objects.get(0), ob1.getId(), ChangeType.DELETED, "File");
    }

    @Test
    public void testGetContentChangesBatchHiddenType() throws Exception {
        Holder<String> changeLogTokenHolder = new Holder<>();

        // add docs whose type is not known to CMIS
        for (int i = 0; i < 15; i++) {
            DocumentModel doc = coreSession.createDocumentModel("/", "hidden" + i, "HiddenFolder");
            Helper.sleepForAuditGranularity();
            doc = coreSession.createDocument(doc);
            coreSession.save();
        }
        // add a regular doc
        DocumentModel doc = coreSession.createDocumentModel("/", "regular", "File");
        Helper.sleepForAuditGranularity();
        doc = coreSession.createDocument(doc);
        coreSession.save();
        nextTransaction();

        sleepForAudit();
        String clt1 = repoService.getRepositoryInfo(repositoryId, null).getLatestChangeLogToken();
        assertNotNull(clt1);

        List<ObjectData> allObjects = readAllContentChanges(changeLogTokenHolder);
        assertEquals(clt1, changeLogTokenHolder.getValue());

        int n = 14; // last n events
        assertTrue(allObjects.size() >= n);
        List<ObjectData> objects = allObjects.subList(allObjects.size() - n, allObjects.size());
        checkChange(objects.get(0), "/testfolder1", //
                ChangeType.CREATED, "Folder");
        checkChange(objects.get(1), "/testfolder1/testfile1", ChangeType.CREATED, "File");
        checkChange(objects.get(2), "/testfolder1/testfile2", ChangeType.CREATED, "File");
        checkChange(objects.get(3), "/testfolder1/testfile3", ChangeType.CREATED, "Note");
        checkChange(objects.get(4), "/testfolder2", //
                ChangeType.CREATED, "Folder");
        checkChange(objects.get(5), "/testfolder2/testfolder3", ChangeType.CREATED, "Folder");
        checkChange(objects.get(6), "/testfolder2/testfolder4", ChangeType.CREATED, "Folder");
        checkChange(objects.get(7), "/testfolder2/testfolder3/testfile4", ChangeType.CREATED, "File");
        checkChange(objects.get(8), file5id, ChangeType.CREATED, "File");
        checkChange(objects.get(9), file5id, ChangeType.UPDATED, "File");
        checkChange(objects.get(10), "/testfolder2/testfolder3/testfile6", ChangeType.CREATED, "Note");
        checkChange(objects.get(11), file6verid, ChangeType.CREATED, "Note");
        checkChange(objects.get(12), proxyid, ChangeType.CREATED, "Note");
        checkChange(objects.get(13), doc.getId(), ChangeType.CREATED, "File");
    }

    protected List<ObjectData> readAllContentChanges(Holder<String> changeLogTokenHolder) {
        List<ObjectData> allObjects = new ArrayList<>();
        changeLogTokenHolder.setValue(null); // start at beginning
        boolean skipFirst = false;
        ObjectList changes;
        do {
            int maxItems = 5;
            changes = discService.getContentChanges(repositoryId, changeLogTokenHolder, Boolean.TRUE, null, null, null,
                    BigInteger.valueOf(maxItems), null);
            List<ObjectData> objects = changes.getObjects();
            if (skipFirst) {
                // already got the first one as part of the last batch
                objects = objects.subList(1, objects.size());
            }
            allObjects.addAll(objects);
            skipFirst = true;
        } while (Boolean.TRUE.equals(changes.hasMoreItems()));
        return allObjects;
    }

    protected void sleepForAudit() throws InterruptedException {
        Thread.sleep(5 * 1000); // wait for audit log to catch up
    }

    protected void checkChange(ObjectData data, String id, ChangeType changeType, String type) throws Exception {
        Map<String, PropertyData<?>> properties;
        ChangeEventInfo cei;
        cei = data.getChangeEventInfo();
        properties = data.getProperties().getProperties();
        String expectedId = id.startsWith("/") ? getObjectByPath(id).getId() : id;
        assertEquals(expectedId, data.getId());
        assertEquals(changeType, cei.getChangeType());
        assertEquals(type, properties.get(PropertyIds.OBJECT_TYPE_ID).getFirstValue());
    }

    @Test
    public void testRelationship() throws Exception {
        assumeSupportsJoins();

        String id1 = getObjectByPath("/testfolder1/testfile1").getId();
        String id2 = getObjectByPath("/testfolder1/testfile2").getId();

        // create relationship
        String statement;
        ObjectList res;
        List<PropertyData<?>> props = new ArrayList<>();
        props.add(factory.createPropertyIdData(PropertyIds.NAME, "rel"));
        props.add(factory.createPropertyIdData(PropertyIds.OBJECT_TYPE_ID, "Relation"));
        props.add(factory.createPropertyIdData(PropertyIds.SOURCE_ID, id1));
        props.add(factory.createPropertyIdData(PropertyIds.TARGET_ID, id2));
        Properties properties = factory.createPropertiesData(props);
        String relid = objService.createRelationship(repositoryId, properties, null, null, null, null);

        waitForIndexing();

        // must be superuser...
        // ObjectData rel = getObject(relid);
        // assertEquals("rel", getValue(rel, PropertyIds.NAME));
        // assertNull(getValue(rel, NuxeoTypeHelper.NX_PARENT_ID));

        // objects have relationship info
        ObjectData od1 = getObject(id1);
        List<ObjectData> rels1 = od1.getRelationships();
        assertNotNull(rels1);
        assertEquals(1, rels1.size());
        // check relation base type id present
        assertNotNull(getValue(rels1.get(0), PropertyIds.BASE_TYPE_ID));
        ObjectData od2 = getObject(id2);
        List<ObjectData> rels2 = od2.getRelationships();
        assertNotNull(rels2);
        assertEquals(1, rels2.size());

        // object from query have relationship info
        statement = "SELECT cmis:objectId FROM File WHERE cmis:name = 'testfile1_Title'";
        res = discService.query(repositoryId, statement, Boolean.TRUE, null, IncludeRelationships.BOTH, null, null,
                null, null);
        assertEquals(1, res.getNumItems().intValue());
        od1 = res.getObjects().get(0);
        rels1 = od1.getRelationships();
        assertNotNull(rels1);
        assertEquals(1, rels1.size());
        // check relation base type id present
        assertNotNull(getValue(rels1.get(0), PropertyIds.BASE_TYPE_ID));

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
        reSetUp("john");

        statement = "SELECT A.cmis:objectId, B.cmis:objectId" + " FROM cmis:document A"
                + " JOIN cmis:relationship R ON R.cmis:sourceId = A.cmis:objectId"
                + " JOIN cmis:document B ON R.cmis:targetId = B.cmis:objectId";
        res = query(statement);
        // no access to testfile1 or testfile2 by john
        assertEquals(0, res.getNumItems().intValue());

        // bob has Browse on testfile1 and testfile2
        reSetUp("bob");

        // no security check on relationship itself
        statement = "SELECT A.cmis:objectId, B.cmis:objectId" + " FROM cmis:document A"
                + " JOIN cmis:relationship R ON R.cmis:sourceId = A.cmis:objectId"
                + " JOIN cmis:document B ON R.cmis:targetId = B.cmis:objectId";
        res = query(statement);
        assertEquals(1, res.getNumItems().intValue());

        // with LEFT JOIN on relation
        statement = "SELECT A.cmis:objectId, B.cmis:objectId" + " FROM cmis:document A"
                + " LEFT JOIN cmis:relationship R ON R.cmis:sourceId = A.cmis:objectId"
                + " LEFT JOIN cmis:document B ON R.cmis:targetId = B.cmis:objectId";
        res = query(statement);
        assertEquals(2, res.getNumItems().intValue());
    }

    @Test
    public void testQueryWithSecurityPolicy() throws Exception {
        DocumentModel doc = coreSession.getDocument(new PathRef("/testfolder1/testfile1"));
        doc.setPropertyValue("dc:title", "SECRET should not be listed");
        coreSession.saveDocument(doc);
        coreSession.save();
        nextTransaction();
        waitForIndexing();

        ObjectList res = query("SELECT cmis:objectId FROM File");
        assertEquals(3, res.getNumItems().intValue());

        // manually check
        res = query("SELECT cmis:objectId FROM File WHERE dc:title NOT LIKE 'SECRET%'");
        assertEquals(2, res.getNumItems().intValue());

        if (!supportsNXQLQueryTransformers()) {
            // deploy a security policy with a non-trivial query transformer
            // that has no CMISQL equivalent
            harness.deployContrib("org.nuxeo.ecm.core.opencmis.tests.tests", "OSGI-INF/security-policy-contrib.xml");
            // check that queries now fail
            try {
                query("SELECT cmis:objectId FROM File");
                fail("Should be denied due to security policy");
            } catch (CmisRuntimeException e) {
                String msg = e.getMessage();
                assertTrue(msg, msg.contains("Security policy"));
            } finally {
                harness.undeployContrib("org.nuxeo.ecm.core.opencmis.tests.tests",
                        "OSGI-INF/security-policy-contrib.xml");
            }

            // without it it works again
            res = query("SELECT cmis:objectId FROM File");
            assertEquals(3, res.getNumItems().intValue());
        }

        if (!useElasticsearch()) {
            // deploy a security policy with a transformer
            String contrib = supportsNXQLQueryTransformers() ? "OSGI-INF/security-policy-contrib3.xml"
                    : "OSGI-INF/security-policy-contrib2.xml";
            harness.deployContrib("org.nuxeo.ecm.core.opencmis.tests.tests", contrib);
            try {
                res = query("SELECT cmis:objectId FROM File");
                assertEquals(2, res.getNumItems().intValue());
                res = query("SELECT cmis:objectId FROM File WHERE dc:title <> 'something'");
                assertEquals(2, res.getNumItems().intValue());
            } finally {
                harness.undeployContrib("org.nuxeo.ecm.core.opencmis.tests.tests", contrib);
            }
        }
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
        String file1Id = getObjectByPath("/testfolder1/testfile1").getId();

        Acl acl = aclService.getAcl(repositoryId, file1Id, Boolean.FALSE, null);
        assertEquals(Boolean.TRUE, acl.isExact());
        Map<String, Set<String>> actual = getActualAcl(acl);
        Map<String, Set<String>> expected = new HashMap<>();
        expected.put("bob", set("Browse"));
        expected.put("members*", set(READ, "Read"));
        expected.put("Administrator*", set(READ, WRITE, ALL, "Everything"));
        assertEquals(expected, actual);

        // with only basic permissions

        acl = aclService.getAcl(repositoryId, file1Id, Boolean.TRUE, null);
        assertEquals(Boolean.FALSE, acl.isExact());
        actual = getActualAcl(acl);
        expected = new HashMap<>();
        expected.put("members*", set(READ));
        expected.put("Administrator*", set(READ, WRITE, ALL));
        assertEquals(expected, actual);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testGetACL() throws Exception {
        String folder1Id = getObjectByPath("/testfolder1").getId();
        String file1Id = getObjectByPath("/testfolder1/testfile1").getId();
        String file4Id = getObjectByPath("/testfolder2/testfolder3/testfile4").getId();

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
            nextTransaction();
            // // process invalidations
            // ((NuxeoBinding) binding).getCoreSession().save();
        }

        Acl acl = aclService.getAcl(repositoryId, file1Id, Boolean.FALSE, null);
        assertEquals(Boolean.TRUE, acl.isExact());
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

        ObjectData ob = objService.getObjectByPath(repositoryId, "/testfolder1/testfile1", null, null, null, null, null,
                Boolean.TRUE, null); // includeAcl
        acl = ob.getAcl();
        assertEquals(Boolean.TRUE, acl.isExact());
        actual = getActualAcl(acl);
        assertEquals(expected, actual);

        // check blocking

        acl = aclService.getAcl(repositoryId, file4Id, Boolean.FALSE, null);
        assertEquals(Boolean.TRUE, acl.isExact());
        actual = getActualAcl(acl);
        expected = new HashMap<>();
        expected.put("Administrator", set(READ, "Read"));
        expected.put("Everyone", set("Nothing"));
        assertEquals(expected, actual);
    }

    @Test
    public void testApplyACL() throws Exception {
        String file1Id = getObjectByPath("/testfolder1/testfile1").getId();

        // file1 already has a bob -> Browse permission from setUp

        // add

        Principal p = new AccessControlPrincipalDataImpl("mary");
        Ace ace = new AccessControlEntryImpl(p, Arrays.asList(READ));
        Acl addAces = new AccessControlListImpl(Arrays.asList(ace));
        Acl removeAces = null;
        Acl acl = aclService.applyAcl(repositoryId, file1Id, addAces, removeAces, AclPropagation.REPOSITORYDETERMINED,
                null);

        assertEquals(Boolean.TRUE, acl.isExact());
        Map<String, Set<String>> actual = getActualAcl(acl);
        Map<String, Set<String>> expected = new HashMap<>();
        expected.put("bob", set("Browse"));
        expected.put("mary", set(READ, "Read"));
        expected.put("members*", set(READ, "Read"));
        expected.put("Administrator*", set(READ, WRITE, ALL, "Everything"));
        assertEquals(expected, actual);

        // remove

        ace = new AccessControlEntryImpl(p, Arrays.asList(READ));
        addAces = null;
        removeAces = new AccessControlListImpl(Arrays.asList(ace));
        acl = aclService.applyAcl(repositoryId, file1Id, addAces, removeAces, AclPropagation.REPOSITORYDETERMINED,
                null);

        assertEquals(Boolean.TRUE, acl.isExact());
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
        harness.deployContrib("org.nuxeo.ecm.core.opencmis.tests.tests",
                "OSGI-INF/recoverable-exc-listener-contrib.xml");
        try {
            createDocument("throw_foo", rootFolderId, "File");
            fail("should throw RecoverableClientException");
        } catch (CmisRuntimeException e) {
            Throwable cause = e.getCause();
            assertTrue(String.valueOf(cause), cause instanceof RecoverableClientException);
        } finally {
            harness.undeployContrib("org.nuxeo.ecm.core.opencmis.tests.tests",
                    "OSGI-INF/recoverable-exc-listener-contrib.xml");
        }
    }

    @Test
    public void testQueryProxy() throws Exception {

        waitForIndexing();

        // getChildren

        DocumentModel folder = coreSession.getDocument(new PathRef("/testfolder2"));
        ObjectInFolderList children = navService.getChildren(repositoryId, folder.getId(), null, null, null, null, null,
                null, null, null, null);
        assertEquals(supportsProxies() ? 3 : 2, children.getNumItems().intValue()); // 2 folders, 1 proxy

        // query

        String query = "SELECT cmis:objectId FROM Note WHERE cmis:name = 'title6'";
        ObjectList res = query(query);
        assertEquals(supportsProxies() ? 3 : 2, res.getNumItems().intValue()); // 1 live doc, 1 version, 1 proxy
    }

}
