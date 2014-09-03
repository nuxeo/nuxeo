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
package org.nuxeo.ecm.core.opencmis.impl.server;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.chemistry.opencmis.commons.data.PermissionMapping;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.definitions.PermissionDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinitionContainer;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinitionList;
import org.apache.chemistry.opencmis.commons.enums.AclPropagation;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.CapabilityAcl;
import org.apache.chemistry.opencmis.commons.enums.CapabilityChanges;
import org.apache.chemistry.opencmis.commons.enums.CapabilityContentStreamUpdates;
import org.apache.chemistry.opencmis.commons.enums.CapabilityJoin;
import org.apache.chemistry.opencmis.commons.enums.CapabilityQuery;
import org.apache.chemistry.opencmis.commons.enums.CapabilityRenditions;
import org.apache.chemistry.opencmis.commons.enums.SupportedPermissions;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.AclCapabilitiesDataImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.RepositoryCapabilitiesImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.RepositoryInfoImpl;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.opencmis.impl.util.TypeManagerImpl;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Information about a Nuxeo repository.
 */
public class NuxeoRepository {

    public static final String NUXEO_VERSION_PROP = "org.nuxeo.distribution.version";

    public static final String NUXEO_URL_PROP = "nuxeo.url";

    public static final String SUPPORTS_JOINS_PROP = "org.nuxeo.cmis.joins";

    private static final String NUXEO_CONTEXT_PATH_PROP = "org.nuxeo.ecm.contextPath";

    private static final String NUXEO_CONTEXT_PATH_DEFAULT = "/nuxeo";

    private static final String X_FORWARDED_HOST = "x-forwarded-host";

    private static final String NUXEO_VH_HEADER = "nuxeo-virtual-host";

    protected final String repositoryId;

    protected final String rootFolderId;

    protected boolean supportsJoins;

    protected TypeManagerImpl typeManager;

    public NuxeoRepository(String repositoryId, String rootFolderId) {
        this.repositoryId = repositoryId;
        this.rootFolderId = rootFolderId;
        if (Framework.isBooleanPropertyTrue(SUPPORTS_JOINS_PROP)) {
            setSupportsJoins(true);
        }
    }

    public void setSupportsJoins(boolean supportsJoins) {
        this.supportsJoins = supportsJoins;
    }

    public boolean supportsJoins() {
        return supportsJoins;
    }

    public String getId() {
        return repositoryId;
    }

    // no need to have it synchronized
    public TypeManagerImpl getTypeManager() {
        if (typeManager == null) {
            typeManager = initializeTypes();
        }
        return typeManager;
    }

    protected static TypeManagerImpl initializeTypes() {
        SchemaManager schemaManager;
        try {
            schemaManager = Framework.getService(SchemaManager.class);
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException(e.toString(), e); // TODO
        }
        // scan the types to find super/inherited relationships
        Map<String, List<String>> typesChildren = new HashMap<String, List<String>>();
        for (DocumentType dt : schemaManager.getDocumentTypes()) {
            org.nuxeo.ecm.core.schema.types.Type st = dt.getSuperType();
            if (st == null) {
                continue;
            }
            String name = st.getName();
            List<String> siblings = typesChildren.get(name);
            if (siblings == null) {
                siblings = new LinkedList<String>();
                typesChildren.put(name, siblings);
            }
            siblings.add(dt.getName());
        }
        // convert the transitive closure for Folder and Document subtypes
        Set<String> done = new HashSet<String>();
        TypeManagerImpl typeManager = new TypeManagerImpl();
        typeManager.addTypeDefinition(NuxeoTypeHelper.constructCmisBase(
                BaseTypeId.CMIS_DOCUMENT, schemaManager));
        typeManager.addTypeDefinition(NuxeoTypeHelper.constructCmisBase(
                BaseTypeId.CMIS_FOLDER, schemaManager));
        typeManager.addTypeDefinition(NuxeoTypeHelper.constructCmisBase(
                BaseTypeId.CMIS_RELATIONSHIP, schemaManager));
        addTypesRecursively(typeManager, NuxeoTypeHelper.NUXEO_DOCUMENT,
                typesChildren, done, schemaManager);
        addTypesRecursively(typeManager, NuxeoTypeHelper.NUXEO_FOLDER,
                typesChildren, done, schemaManager);
        addTypesRecursively(typeManager, NuxeoTypeHelper.NUXEO_RELATION,
                typesChildren, done, schemaManager);
        return typeManager;
    }

    protected static void addTypesRecursively(TypeManagerImpl typeManager,
            String name, Map<String, List<String>> typesChildren,
            Set<String> done, SchemaManager schemaManager) {
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
                if (parentType.getTypeDefinition().getBaseTypeId() != BaseTypeId.CMIS_FOLDER
                        && dt.isFolder()) {
                    // reparent Folderish but child of Document under cmis:folder
                    parentTypeId = BaseTypeId.CMIS_FOLDER.value();
                }
            }
            typeManager.addTypeDefinition(NuxeoTypeHelper.construct(dt,
                    parentTypeId));
        }
        // recurse in children
        List<String> children = typesChildren.get(name);
        if (children == null) {
            return;
        }
        for (String sub : children) {
            addTypesRecursively(typeManager, sub, typesChildren, done,
                    schemaManager);
        }
    }

    public String getRootFolderId() {
        return rootFolderId;
    }

    public RepositoryInfo getRepositoryInfo(String latestChangeLogToken,
            CallContext callContext) {
        RepositoryInfoImpl repositoryInfo = new RepositoryInfoImpl();
        repositoryInfo.setId(repositoryId);
        repositoryInfo.setName("Nuxeo Repository " + repositoryId);
        repositoryInfo.setDescription("Nuxeo Repository " + repositoryId);
        repositoryInfo.setCmisVersionSupported("1.1");
        repositoryInfo.setPrincipalAnonymous("Guest"); // TODO
        repositoryInfo.setPrincipalAnyone(SecurityConstants.EVERYONE);
        repositoryInfo.setThinClientUri(getBaseURL(callContext));
        repositoryInfo.setChangesIncomplete(Boolean.FALSE);
        repositoryInfo.setChangesOnType(Arrays.asList(BaseTypeId.CMIS_DOCUMENT,
                BaseTypeId.CMIS_FOLDER));
        repositoryInfo.setLatestChangeLogToken(latestChangeLogToken);
        repositoryInfo.setVendorName("Nuxeo");
        repositoryInfo.setProductName("Nuxeo OpenCMIS Connector");
        String version = Framework.getProperty(NUXEO_VERSION_PROP, "5.5 dev");
        repositoryInfo.setProductVersion(version);
        repositoryInfo.setRootFolder(rootFolderId);
        RepositoryCapabilitiesImpl caps = new RepositoryCapabilitiesImpl();
        repositoryInfo.setCapabilities(caps);
        AclCapabilitiesDataImpl aclCaps = new AclCapabilitiesDataImpl();
        repositoryInfo.setAclCapabilities(aclCaps);
        caps.setAllVersionsSearchable(Boolean.TRUE);
        caps.setCapabilityAcl(CapabilityAcl.NONE);
        caps.setCapabilityChanges(CapabilityChanges.OBJECTIDSONLY);
        caps.setCapabilityContentStreamUpdates(CapabilityContentStreamUpdates.PWCONLY);
        caps.setCapabilityJoin(supportsJoins ? CapabilityJoin.INNERANDOUTER
                : CapabilityJoin.NONE);
        caps.setCapabilityQuery(CapabilityQuery.BOTHCOMBINED);
        caps.setCapabilityRendition(CapabilityRenditions.READ);
        caps.setIsPwcSearchable(Boolean.TRUE);
        caps.setIsPwcUpdatable(Boolean.TRUE);
        caps.setSupportsGetDescendants(Boolean.TRUE);
        caps.setSupportsGetFolderTree(Boolean.TRUE);
        caps.setSupportsMultifiling(Boolean.FALSE);
        caps.setSupportsUnfiling(Boolean.FALSE);
        caps.setSupportsVersionSpecificFiling(Boolean.FALSE);
        aclCaps.setAclPropagation(AclPropagation.REPOSITORYDETERMINED);
        aclCaps.setPermissionDefinitionData(new ArrayList<PermissionDefinition>(
                0));
        aclCaps.setPermissionMappingData(new HashMap<String, PermissionMapping>());
        aclCaps.setSupportedPermissions(SupportedPermissions.BASIC);
        return repositoryInfo;
    }

    // Structures are not copied when returned
    public TypeDefinition getTypeDefinition(String typeId) {
        TypeDefinitionContainer typec = getTypeManager().getTypeById(typeId);
        return typec == null ? null : typec.getTypeDefinition();
    }

    public boolean hasType(String typeId) {
        return getTypeManager().hasType(typeId);
    }

    // Structures are not copied when returned
    public TypeDefinitionList getTypeChildren(String typeId,
            Boolean includePropertyDefinitions, BigInteger maxItems,
            BigInteger skipCount) {
        return getTypeManager().getTypeChildren(typeId,
                includePropertyDefinitions, maxItems, skipCount);
    }

    public List<TypeDefinitionContainer> getTypeDescendants(String typeId,
            int depth, Boolean includePropertyDefinitions) {
        return getTypeManager().getTypeDescendants(typeId, depth,
                includePropertyDefinitions);
    }

    /** Returns the server base URL (including context). */
    private static String getBaseURL(CallContext callContext) {
        HttpServletRequest request = (HttpServletRequest) callContext.get(CallContext.HTTP_SERVLET_REQUEST);
        if (request != null) {
            String baseURL = getServerURL(request);
            String contextPath = request.getContextPath();
            if (contextPath == null) {
                contextPath = Framework.getProperty(NUXEO_CONTEXT_PATH_PROP,
                        NUXEO_CONTEXT_PATH_DEFAULT);
            }
            // add context path
            return baseURL + contextPath + '/';
        } else {
            return Framework.getProperty(NUXEO_URL_PROP);
        }
    }

    /**
     * Returns the server URL according to virtual hosting headers (without
     * trailing slash).
     */
    private static String getServerURL(HttpServletRequest request) {
        String url = null;
        // Detect Nuxeo specific header for VH
        String nuxeoVH = request.getHeader(NUXEO_VH_HEADER);
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
    private static String buildURL(String scheme, String serverName,
            int serverPort) {
        StringBuilder sb = new StringBuilder();
        sb.append(scheme);
        sb.append("://");
        sb.append(serverName);
        if (serverPort != 0) {
            if ("http".equals(scheme) && serverPort != 80
                    || "https".equals(scheme) && serverPort != 443) {
                sb.append(':');
                sb.append(serverPort);
            }
        }
        return sb.toString();
    }

}
