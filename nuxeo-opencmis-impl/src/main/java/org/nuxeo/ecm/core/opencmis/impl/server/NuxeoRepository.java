/*
 * (C) Copyright 2006-2017 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.opencmis.impl.server;

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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.chemistry.opencmis.commons.data.ExtensionFeature;
import org.apache.chemistry.opencmis.commons.data.PermissionMapping;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.definitions.PermissionDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinitionContainer;
import org.apache.chemistry.opencmis.commons.enums.AclPropagation;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.CapabilityAcl;
import org.apache.chemistry.opencmis.commons.enums.CapabilityChanges;
import org.apache.chemistry.opencmis.commons.enums.CapabilityContentStreamUpdates;
import org.apache.chemistry.opencmis.commons.enums.CapabilityJoin;
import org.apache.chemistry.opencmis.commons.enums.CapabilityQuery;
import org.apache.chemistry.opencmis.commons.enums.CapabilityRenditions;
import org.apache.chemistry.opencmis.commons.enums.CmisVersion;
import org.apache.chemistry.opencmis.commons.enums.SupportedPermissions;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.AclCapabilitiesDataImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.CreatablePropertyTypesImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.NewTypeSettableAttributesImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PermissionDefinitionDataImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PermissionMappingDataImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.RepositoryCapabilitiesImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.RepositoryInfoImpl;
import org.apache.chemistry.opencmis.commons.server.CallContext;

import org.nuxeo.common.Environment;
import org.nuxeo.ecm.core.api.security.PermissionProvider;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.opencmis.impl.util.TypeManagerImpl;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.CompositeType;
import org.nuxeo.ecm.core.security.DefaultPermissionProvider;
import org.nuxeo.ecm.core.security.PermissionVisibilityDescriptor;
import org.nuxeo.runtime.api.Framework;

/**
 * Information about a Nuxeo repository.
 */
public class NuxeoRepository {

    /**
     * @deprecated Since 7.10. Use {@link Environment#DISTRIBUTION_VERSION}
     */
    @Deprecated
    public static final String NUXEO_VERSION_PROP = Environment.DISTRIBUTION_VERSION;

    public static final String NUXEO_URL_PROP = "nuxeo.url";

    public static final String SUPPORTS_JOINS_PROP = "org.nuxeo.cmis.joins";

    public static final String SUPPORTS_PROXIES_PROP = "org.nuxeo.cmis.proxies";

    public static final String ELASTICSEARCH_PROP = "org.nuxeo.cmis.elasticsearch";

    private static final String NUXEO_CONTEXT_PATH_PROP = "org.nuxeo.ecm.contextPath";

    private static final String NUXEO_CONTEXT_PATH_DEFAULT = "/nuxeo";

    private static final String X_FORWARDED_HOST = "x-forwarded-host";

    private static final String NUXEO_VH_HEADER = "nuxeo-virtual-host";

    private static final String VH_PARAM = "nuxeo.virtual.host";

    public static final String NUXEO_READ_REMOVE = "ReadRemove";

    protected final String repositoryId;

    protected final String rootFolderId;

    protected boolean supportsJoins;

    protected boolean supportsProxies;

    protected boolean useElasticsearch;

    protected Map<CmisVersion, TypeManagerImpl> typeManagerByCmisVersion = new HashMap<>();

    public NuxeoRepository(String repositoryId, String rootFolderId) {
        this.repositoryId = repositoryId;
        this.rootFolderId = rootFolderId;
        if (Framework.isBooleanPropertyTrue(SUPPORTS_JOINS_PROP)) {
            setSupportsJoins(true);
        }
        if (Framework.isBooleanPropertyTrue(SUPPORTS_PROXIES_PROP)) {
            setSupportsProxies(true);
        }
        if (Framework.isBooleanPropertyTrue(ELASTICSEARCH_PROP)) {
            setUseElasticsearch(true);
        }
    }

    public void setSupportsJoins(boolean supportsJoins) {
        this.supportsJoins = supportsJoins;
    }

    public boolean supportsJoins() {
        return supportsJoins;
    }

    public void setSupportsProxies(boolean supportsProxies) {
        this.supportsProxies = supportsProxies;
    }

    public boolean supportsProxies() {
        return supportsProxies;
    }

    public void setUseElasticsearch(boolean useElasticsearch) {
        this.useElasticsearch = useElasticsearch;
    }

    public boolean useElasticsearch() {
        return useElasticsearch;
    }

    public String getId() {
        return repositoryId;
    }

    // no need to have it synchronized
    public TypeManagerImpl getTypeManager(CmisVersion cmisVersion) {
        TypeManagerImpl typeManager = typeManagerByCmisVersion.get(cmisVersion);
        if (typeManager == null) {
            typeManager = initializeTypes(cmisVersion);
            typeManagerByCmisVersion.put(cmisVersion, typeManager);
        }
        return typeManager;
    }

    protected TypeManagerImpl initializeTypes(CmisVersion cmisVersion) {
        SchemaManager schemaManager = Framework.getService(SchemaManager.class);
        // scan the types to find super/inherited relationships
        Map<String, List<String>> typesChildren = new HashMap<>();
        for (DocumentType dt : schemaManager.getDocumentTypes()) {
            org.nuxeo.ecm.core.schema.types.Type st = dt.getSuperType();
            if (st == null) {
                continue;
            }
            String name = st.getName();
            List<String> siblings = typesChildren.get(name);
            if (siblings == null) {
                siblings = new LinkedList<>();
                typesChildren.put(name, siblings);
            }
            siblings.add(dt.getName());
        }
        // convert the transitive closure for Folder and Document subtypes
        Set<String> done = new HashSet<>();
        TypeManagerImpl typeManager = new TypeManagerImpl();
        typeManager.addTypeDefinition(NuxeoTypeHelper.constructCmisBase(BaseTypeId.CMIS_DOCUMENT, schemaManager, cmisVersion));
        typeManager.addTypeDefinition(NuxeoTypeHelper.constructCmisBase(BaseTypeId.CMIS_FOLDER, schemaManager, cmisVersion));
        typeManager.addTypeDefinition(NuxeoTypeHelper.constructCmisBase(BaseTypeId.CMIS_RELATIONSHIP, schemaManager, cmisVersion));
        if (cmisVersion != CmisVersion.CMIS_1_0) {
            typeManager.addTypeDefinition(NuxeoTypeHelper.constructCmisBase(BaseTypeId.CMIS_SECONDARY, schemaManager, cmisVersion));
        }
        addTypesRecursively(typeManager, NuxeoTypeHelper.NUXEO_DOCUMENT, typesChildren, done, schemaManager, cmisVersion);
        addTypesRecursively(typeManager, NuxeoTypeHelper.NUXEO_FOLDER, typesChildren, done, schemaManager, cmisVersion);
        addTypesRecursively(typeManager, NuxeoTypeHelper.NUXEO_RELATION, typesChildren, done, schemaManager, cmisVersion);
        if (cmisVersion != CmisVersion.CMIS_1_0) {
            addSecondaryTypes(typeManager, schemaManager, cmisVersion);
        }
        return typeManager;
    }

    protected void addTypesRecursively(TypeManagerImpl typeManager, String name,
            Map<String, List<String>> typesChildren, Set<String> done, SchemaManager schemaManager,
            CmisVersion cmisVersion) {
        if (done.contains(name)) {
            return;
        }
        done.add(name);
        DocumentType dt = schemaManager.getDocumentType(name);
        String parentTypeId = NuxeoTypeHelper.getParentTypeId(dt);
        if (parentTypeId != null) {
            TypeDefinitionContainer parentType = typeManager.getTypeById(parentTypeId);
            if (parentType == null) {
                // if parent was ignored, reparent under cmis:document
                parentTypeId = BaseTypeId.CMIS_DOCUMENT.value();
            } else {
                if (parentType.getTypeDefinition().getBaseTypeId() != BaseTypeId.CMIS_FOLDER && dt.isFolder()) {
                    // reparent Folderish but child of Document under
                    // cmis:folder
                    parentTypeId = BaseTypeId.CMIS_FOLDER.value();
                }
            }
            typeManager.addTypeDefinition(NuxeoTypeHelper.constructDocumentType(dt, parentTypeId, cmisVersion));
        }
        // recurse in children
        List<String> children = typesChildren.get(name);
        if (children == null) {
            return;
        }
        for (String sub : children) {
            addTypesRecursively(typeManager, sub, typesChildren, done, schemaManager, cmisVersion);
        }
    }

    protected void addSecondaryTypes(TypeManagerImpl typeManager, SchemaManager schemaManager,
            CmisVersion cmisVersion) {
        for (CompositeType type : schemaManager.getFacets()) {
            typeManager.addTypeDefinition(NuxeoTypeHelper.constructSecondaryType(type, cmisVersion), false);
        }
    }

    public String getRootFolderId() {
        return rootFolderId;
    }

    public RepositoryInfo getRepositoryInfo(String latestChangeLogToken, CallContext callContext) {
        CmisVersion cmisVersion = callContext.getCmisVersion();

        RepositoryInfoImpl repositoryInfo = new RepositoryInfoImpl();
        repositoryInfo.setId(repositoryId);
        repositoryInfo.setName("Nuxeo Repository " + repositoryId);
        repositoryInfo.setDescription("Nuxeo Repository " + repositoryId);
        repositoryInfo.setCmisVersionSupported(cmisVersion.value());
        repositoryInfo.setPrincipalAnonymous("Guest"); // TODO
        repositoryInfo.setPrincipalAnyone(SecurityConstants.EVERYONE);
        repositoryInfo.setThinClientUri(getBaseURL(callContext));
        repositoryInfo.setChangesIncomplete(Boolean.FALSE);
        repositoryInfo.setChangesOnType(Arrays.asList(BaseTypeId.CMIS_DOCUMENT, BaseTypeId.CMIS_FOLDER));
        repositoryInfo.setLatestChangeLogToken(latestChangeLogToken);
        repositoryInfo.setVendorName("Nuxeo");
        repositoryInfo.setProductName("Nuxeo OpenCMIS Connector");
        String version = Framework.getProperty(Environment.DISTRIBUTION_VERSION, "5.5 dev");
        repositoryInfo.setProductVersion(version);
        repositoryInfo.setRootFolder(rootFolderId);
        repositoryInfo.setExtensionFeature(Collections.<ExtensionFeature> emptyList());

        // capabilities

        RepositoryCapabilitiesImpl caps = new RepositoryCapabilitiesImpl();
        caps.setAllVersionsSearchable(Boolean.TRUE);
        caps.setCapabilityAcl(CapabilityAcl.MANAGE);
        caps.setCapabilityChanges(CapabilityChanges.OBJECTIDSONLY);
        caps.setCapabilityContentStreamUpdates(CapabilityContentStreamUpdates.PWCONLY);
        caps.setCapabilityJoin(supportsJoins ? CapabilityJoin.INNERANDOUTER : CapabilityJoin.NONE);
        caps.setCapabilityQuery(CapabilityQuery.BOTHCOMBINED);
        caps.setCapabilityRendition(CapabilityRenditions.READ);
        caps.setIsPwcSearchable(Boolean.TRUE);
        caps.setIsPwcUpdatable(Boolean.TRUE);
        caps.setSupportsGetDescendants(Boolean.TRUE);
        caps.setSupportsGetFolderTree(Boolean.TRUE);
        caps.setSupportsMultifiling(Boolean.FALSE);
        caps.setSupportsUnfiling(Boolean.FALSE);
        caps.setSupportsVersionSpecificFiling(Boolean.FALSE);
        caps.setNewTypeSettableAttributes(new NewTypeSettableAttributesImpl());
        caps.setCreatablePropertyTypes(new CreatablePropertyTypesImpl());
        repositoryInfo.setCapabilities(caps);

        // ACL capabilities

        AclCapabilitiesDataImpl aclCaps = new AclCapabilitiesDataImpl();
        aclCaps.setAclPropagation(AclPropagation.PROPAGATE);
        aclCaps.setSupportedPermissions(SupportedPermissions.REPOSITORY);

        List<PermissionDefinition> permDefs = new ArrayList<>();
        addPermissionDefinitions(permDefs);
        aclCaps.setPermissionDefinitionData(permDefs);

        Map<String, PermissionMapping> permMap = new HashMap<>();
        addPermissionMapping(permMap, CAN_GET_DESCENDENTS_FOLDER, READ);
        addPermissionMapping(permMap, CAN_GET_CHILDREN_FOLDER, READ);
        addPermissionMapping(permMap, CAN_GET_PARENTS_FOLDER, READ);
        addPermissionMapping(permMap, CAN_GET_FOLDER_PARENT_OBJECT, READ);
        addPermissionMapping(permMap, CAN_CREATE_DOCUMENT_FOLDER, WRITE);
        addPermissionMapping(permMap, CAN_CREATE_FOLDER_FOLDER, WRITE);
        // no CAN_CREATE_POLICY_FOLDER due to spec bug
        addPermissionMapping(permMap, CAN_CREATE_RELATIONSHIP_SOURCE, READ);
        addPermissionMapping(permMap, CAN_CREATE_RELATIONSHIP_TARGET, READ);
        addPermissionMapping(permMap, CAN_GET_PROPERTIES_OBJECT, READ);
        addPermissionMapping(permMap, CAN_VIEW_CONTENT_OBJECT, READ);
        addPermissionMapping(permMap, CAN_UPDATE_PROPERTIES_OBJECT, WRITE);
        addPermissionMapping(permMap, CAN_MOVE_OBJECT, WRITE);
        addPermissionMapping(permMap, CAN_MOVE_TARGET, WRITE);
        addPermissionMapping(permMap, CAN_MOVE_SOURCE, WRITE);
        addPermissionMapping(permMap, CAN_DELETE_OBJECT, WRITE);
        addPermissionMapping(permMap, CAN_DELETE_TREE_FOLDER, WRITE);
        addPermissionMapping(permMap, CAN_SET_CONTENT_DOCUMENT, WRITE);
        addPermissionMapping(permMap, CAN_DELETE_CONTENT_DOCUMENT, WRITE);
        addPermissionMapping(permMap, CAN_ADD_TO_FOLDER_OBJECT, WRITE);
        addPermissionMapping(permMap, CAN_ADD_TO_FOLDER_FOLDER, WRITE);
        addPermissionMapping(permMap, CAN_REMOVE_FROM_FOLDER_OBJECT, WRITE);
        addPermissionMapping(permMap, CAN_REMOVE_FROM_FOLDER_FOLDER, WRITE);
        addPermissionMapping(permMap, CAN_CHECKOUT_DOCUMENT, WRITE);
        addPermissionMapping(permMap, CAN_CANCEL_CHECKOUT_DOCUMENT, WRITE);
        addPermissionMapping(permMap, CAN_CHECKIN_DOCUMENT, WRITE);
        addPermissionMapping(permMap, CAN_GET_ALL_VERSIONS_VERSION_SERIES, READ);
        addPermissionMapping(permMap, CAN_GET_OBJECT_RELATIONSHIPS_OBJECT, READ);
        addPermissionMapping(permMap, CAN_ADD_POLICY_OBJECT, WRITE);
        addPermissionMapping(permMap, CAN_ADD_POLICY_POLICY, WRITE);
        addPermissionMapping(permMap, CAN_REMOVE_POLICY_OBJECT, WRITE);
        addPermissionMapping(permMap, CAN_REMOVE_POLICY_POLICY, WRITE);
        addPermissionMapping(permMap, CAN_GET_APPLIED_POLICIES_OBJECT, READ);
        addPermissionMapping(permMap, CAN_GET_ACL_OBJECT, READ);
        addPermissionMapping(permMap, CAN_APPLY_ACL_OBJECT, ALL);
        aclCaps.setPermissionMappingData(permMap);

        repositoryInfo.setAclCapabilities(aclCaps);

        return repositoryInfo;
    }

    @SuppressWarnings("unchecked")
    protected static void addPermissionDefinitions(List<PermissionDefinition> permDefs) {
        addPermissionDefinition(permDefs, READ, "Read"); // = Nuxeo Read
        addPermissionDefinition(permDefs, WRITE, "Write"); // = Nuxeo ReadWrite
        addPermissionDefinition(permDefs, ALL, "All"); // = Nuxeo Everything
        addPermissionDefinition(permDefs, NUXEO_READ_REMOVE, "Remove");

        Set<String> done = new HashSet<>();
        done.add(SecurityConstants.READ);
        done.add(SecurityConstants.READ_WRITE);
        done.add(SecurityConstants.EVERYTHING);
        done.add(NUXEO_READ_REMOVE);

        /*
         * Add Nuxeo-specific permissions registered through the permissionsVisibility extension point.
         */

        DefaultPermissionProvider permissionProvider = (DefaultPermissionProvider) Framework.getService(PermissionProvider.class);
        permissionProvider.getUserVisiblePermissionDescriptors(); // init var
        Map<String, PermissionVisibilityDescriptor> map;
        try {
            Field f;
            f = DefaultPermissionProvider.class.getDeclaredField("mergedPermissionsVisibility");
            f.setAccessible(true);
            map = (Map<String, PermissionVisibilityDescriptor>) f.get(permissionProvider);
        } catch (NoSuchFieldException | SecurityException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        // iterate for all types regisited, not just the default ""
        for (Entry<String, PermissionVisibilityDescriptor> en : map.entrySet()) {
            for (String permission : en.getValue().getSortedItems()) {
                if (!done.add(permission)) {
                    continue;
                }
                addPermissionDefinition(permDefs, permission, permission);
            }
        }
    }

    protected static void addPermissionDefinition(List<PermissionDefinition> permDefs, String permission,
            String description) {
        PermissionDefinitionDataImpl pd = new PermissionDefinitionDataImpl();
        pd.setId(permission);
        pd.setDescription(description);
        permDefs.add(pd);
    }

    protected static void addPermissionMapping(Map<String, PermissionMapping> permMap, String key, String permission) {
        PermissionMappingDataImpl pm = new PermissionMappingDataImpl();
        pm.setKey(key);
        pm.setPermissions(Collections.singletonList(permission));
        permMap.put(key, pm);
    }

    /** Returns the server base URL (including context). */
    private static String getBaseURL(CallContext callContext) {
        HttpServletRequest request = (HttpServletRequest) callContext.get(CallContext.HTTP_SERVLET_REQUEST);
        if (request != null) {
            String baseURL = getServerURL(request);
            String contextPath = request.getContextPath();
            if (contextPath == null) {
                contextPath = Framework.getProperty(NUXEO_CONTEXT_PATH_PROP, NUXEO_CONTEXT_PATH_DEFAULT);
            }
            // add context path
            return baseURL + contextPath + '/';
        } else {
            return Framework.getProperty(NUXEO_URL_PROP);
        }
    }

    /**
     * Returns the server URL according to virtual hosting headers (without trailing slash).
     */
    private static String getServerURL(HttpServletRequest request) {
        String url = null;
        // Detect Nuxeo specific header for VH
        String nuxeoVH = request.getHeader(NUXEO_VH_HEADER);
        if (nuxeoVH == null) {
            nuxeoVH = Framework.getProperty(VH_PARAM);
        }
        if (nuxeoVH != null && nuxeoVH.startsWith("http")) {
            url = nuxeoVH;
        } else {
            // default values
            String scheme = request.getScheme();
            String serverName = request.getServerName();
            int serverPort = request.getServerPort();
            // Detect virtual hosting based in standard header
            String forwardedHost = request.getHeader(X_FORWARDED_HOST);
            if (forwardedHost != null) {
                if (forwardedHost.contains(":")) {
                    String[] split = forwardedHost.split(":");
                    serverName = split[0];
                    serverPort = Integer.parseInt(split[1]);
                } else {
                    serverName = forwardedHost;
                    serverPort = 80; // fallback
                }
            }
            url = buildURL(scheme, serverName, serverPort);
        }
        // strip trailing slash
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    /** Builds an URL (without trailing slash). */
    private static String buildURL(String scheme, String serverName, int serverPort) {
        StringBuilder sb = new StringBuilder();
        sb.append(scheme);
        sb.append("://");
        sb.append(serverName);
        if (serverPort != 0) {
            if ("http".equals(scheme) && serverPort != 80 || "https".equals(scheme) && serverPort != 443) {
                sb.append(':');
                sb.append(serverPort);
            }
        }
        return sb.toString();
    }

}
