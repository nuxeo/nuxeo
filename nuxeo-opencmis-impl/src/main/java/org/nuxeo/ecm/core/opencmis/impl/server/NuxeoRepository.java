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

    protected final String repositoryId;

    protected final String rootFolderId;

    protected TypeManagerImpl typeManager;

    public NuxeoRepository(String repositoryId, String rootFolderId) {
        this.repositoryId = repositoryId;
        this.rootFolderId = rootFolderId;

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
            if (typeManager.getTypeById(parentTypeId) == null) {
                // if parent was ignored, reparent under cmis:document
                parentTypeId = BaseTypeId.CMIS_DOCUMENT.value();
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

    public RepositoryInfo getRepositoryInfo(String latestChangeLogToken) {
        RepositoryInfoImpl repositoryInfo = new RepositoryInfoImpl();
        repositoryInfo.setId(repositoryId);
        repositoryInfo.setName("Nuxeo Repository " + repositoryId);
        repositoryInfo.setDescription("Nuxeo Repository " + repositoryId);
        repositoryInfo.setCmisVersionSupported("1.0");
        repositoryInfo.setPrincipalAnonymous("Guest"); // TODO
        repositoryInfo.setPrincipalAnyone(SecurityConstants.EVERYONE);
        repositoryInfo.setThinClientUri(null); // TODO
        repositoryInfo.setChangesIncomplete(Boolean.FALSE);
        repositoryInfo.setChangesOnType(Arrays.asList(BaseTypeId.CMIS_DOCUMENT,
                BaseTypeId.CMIS_FOLDER));
        repositoryInfo.setLatestChangeLogToken(latestChangeLogToken);
        repositoryInfo.setVendorName("Nuxeo");
        repositoryInfo.setProductName("Nuxeo OpenCMIS Connector");
        String version = Framework.getProperty(NUXEO_VERSION_PROP, "5.4 dev");
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
        caps.setCapabilityJoin(CapabilityJoin.INNERANDOUTER);
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
}
