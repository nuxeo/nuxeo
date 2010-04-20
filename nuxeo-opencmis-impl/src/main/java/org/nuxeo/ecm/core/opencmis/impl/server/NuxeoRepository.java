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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.chemistry.opencmis.commons.api.PermissionDefinition;
import org.apache.chemistry.opencmis.commons.api.PermissionMapping;
import org.apache.chemistry.opencmis.commons.api.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.api.TypeDefinition;
import org.apache.chemistry.opencmis.commons.enums.AclPropagation;
import org.apache.chemistry.opencmis.commons.enums.CapabilityAcl;
import org.apache.chemistry.opencmis.commons.enums.CapabilityChanges;
import org.apache.chemistry.opencmis.commons.enums.CapabilityContentStreamUpdates;
import org.apache.chemistry.opencmis.commons.enums.CapabilityJoin;
import org.apache.chemistry.opencmis.commons.enums.CapabilityQuery;
import org.apache.chemistry.opencmis.commons.enums.CapabilityRenditions;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.AclCapabilitiesDataImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.RepositoryCapabilitiesImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.RepositoryInfoImpl;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.opencmis.impl.util.SimpleTypeManager;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.runtime.api.Framework;

public class NuxeoRepository {

    protected final String repositoryId;

    protected SimpleTypeManager typeManager;

    protected String rootFolderId;

    protected final RepositoryCapabilitiesImpl caps;

    protected final AclCapabilitiesDataImpl aclCaps;

    public NuxeoRepository(String repositoryId) {
        this.repositoryId = repositoryId;
        caps = new RepositoryCapabilitiesImpl();
        caps.setAllVersionsSearchable(Boolean.TRUE);
        caps.setCapabilityAcl(CapabilityAcl.NONE);
        caps.setCapabilityChanges(CapabilityChanges.PROPERTIES);
        caps.setCapabilityContentStreamUpdates(CapabilityContentStreamUpdates.PWCONLY);
        caps.setCapabilityJoin(CapabilityJoin.INNERANDOUTER);
        caps.setCapabilityQuery(CapabilityQuery.BOTHCOMBINED);
        caps.setCapabilityRendition(CapabilityRenditions.NONE);
        caps.setIsPwcSearchable(Boolean.FALSE);
        caps.setIsPwcUpdatable(Boolean.FALSE);
        caps.setSupportsGetDescendants(Boolean.TRUE);
        caps.setSupportsGetFolderTree(Boolean.TRUE);
        caps.setSupportsMultifiling(Boolean.FALSE);
        caps.setSupportsUnfiling(Boolean.FALSE);
        caps.setSupportsVersionSpecificFiling(Boolean.FALSE);
        aclCaps = new AclCapabilitiesDataImpl();
        aclCaps.setAclPropagation(AclPropagation.REPOSITORYDETERMINED);
        aclCaps.setPermissionDefinitionData(new ArrayList<PermissionDefinition>(
                0));
        aclCaps.setPermissionMappingData(new HashMap<String, PermissionMapping>());
    }

    protected void initializeTypes() {
        if (typeManager != null) {
            return;
        }
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
        typeManager = new SimpleTypeManager();
        addTypeRecursively("Folder", typesChildren, done, schemaManager);
        addTypeRecursively("Document", typesChildren, done, schemaManager);
    }

    protected void addTypeRecursively(String name,
            Map<String, List<String>> typesChildren, Set<String> done,
            SchemaManager schemaManager) {
        if (done.contains(name)) {
            return;
        }
        done.add(name);
        DocumentType dt = schemaManager.getDocumentType(name);
        typeManager.addType(NuxeoTypeHelper.construct(dt));
        // recurse in children
        List<String> children = typesChildren.get(name);
        if (children == null) {
            return;
        }
        for (String sub : children) {
            addTypeRecursively(sub, typesChildren, done, schemaManager);
        }
    }

    public String getId() {
        return repositoryId;
    }

    public String getRootFolderId(CoreSession coreSession) {
        if (rootFolderId == null) {
            try {
                rootFolderId = coreSession.getRootDocument().getId();
            } catch (ClientException e) {
                throw new CmisRuntimeException("Cannot get root id", e);
            }
        }
        return rootFolderId;
    }

    public RepositoryInfo getRepositoryInfo(CoreSession coreSession) {
        RepositoryInfoImpl info = new RepositoryInfoImpl();
        info.setRepositoryId(repositoryId);
        info.setRepositoryName("Nuxeo Repository"); // TODO
        info.setRepositoryDescription("Nuxeo Repository"); // TODO
        info.setCmisVersionSupported("1.0");
        info.setPrincipalAnonymous("Guest"); // TODO
        info.setPrincipalAnyone(SecurityConstants.EVERYONE);
        info.setThinClientUri(null); // TODO
        info.setChangesIncomplete(Boolean.TRUE);
        info.setChangesOnType(null);
        info.setVendorName("Nuxeo");
        info.setProductName("Nuxeo CMIS Connector");
        info.setProductVersion("5.3-SNAPSHOT");
        info.setRepositoryCapabilities(caps);
        info.setAclCapabilities(aclCaps);
        //
        info.setRootFolder(getRootFolderId(coreSession));
        info.setLatestChangeLogToken(null); // TODO XXX variable
        return info;
    }

    public TypeDefinition getTypeDefinition(String typeId) {
        initializeTypes();
        return typeManager.getTypeDefinition(typeId);
    }

}
