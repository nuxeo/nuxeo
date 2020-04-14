/*
 * (C) Copyright 2016-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Michael Vachette
 *     Florent Guillaume
 */
package org.nuxeo.ecm.automation.core.operations.users;

import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.directory.BaseSession;
import org.nuxeo.ecm.platform.usermanager.GroupConfig;
import org.nuxeo.ecm.platform.usermanager.NuxeoGroupImpl;
import org.nuxeo.ecm.platform.usermanager.UserManager;

/**
 * Operation to create or update a group.
 *
 * @since 9.1
 */
@Operation(id = CreateOrUpdateGroup.ID, //
        category = Constants.CAT_USERS_GROUPS, //
        label = "Create or Update Group", //
        description = "Create or Update Group")
public class CreateOrUpdateGroup {

    public static final String ID = "Group.CreateOrUpdate";

    public static final String CREATE_OR_UPDATE = "createOrUpdate";

    public static final String CREATE = "create";

    public static final String UPDATE = "update";

    public static final String GROUP_SCHEMA = "group";

    protected static final String GROUP_COLON = GROUP_SCHEMA + ':';

    public static final String GROUP_NAME = "groupname";

    public static final String GROUP_LABEL = "grouplabel";

    public static final String GROUP_DESCRIPTION = "description";

    public static final String MEMBERS = "members";

    public static final String SUB_GROUPS = "subGroups";

    public static final String PARENT_GROUPS = "parentGroups";

    public static final String GROUP_TENANTID = "tenantId";

    @Context
    protected UserManager userManager;

    @Context
    protected OperationContext ctx;

    @Param(name = "groupname")
    protected String groupName;

    @Param(name = "tenantId", required = false)
    protected String tenantId;

    @Param(name = "grouplabel", required = false)
    protected String groupLabel;

    @Param(name = "description", required = false)
    protected String groupDescription;

    @Param(name = "members", required = false)
    protected StringList members;

    @Param(name = "subGroups", required = false)
    protected StringList subGroups;

    @Param(name = "parentGroups", required = false)
    protected StringList parentGroups;

    @Param(name = "properties", required = false)
    protected Properties properties = new Properties();

    @Param(name = "mode", required = false, values = { CREATE_OR_UPDATE, CREATE, UPDATE })
    protected String mode;

    @OperationMethod
    public void run() throws OperationException {
        String tenantGroupName = getTenantGroupName(groupName, tenantId);
        boolean create;
        DocumentModel groupDoc = userManager.getGroupModel(tenantGroupName);
        if (groupDoc == null) {
            if (UPDATE.equals(mode)) {
                throw new OperationException("Cannot update non-existent group: " + groupName);
            }
            create = true;
            groupDoc = userManager.getBareGroupModel();
            groupDoc.setProperty(GROUP_SCHEMA, GROUP_NAME, tenantGroupName);
        } else {
            if (CREATE.equals(mode)) {
                throw new OperationException("Cannot create already-existing group: " + groupName);
            }
            create = false;

            // make sure that the group can be updated
            checkCanCreateOrUpdateGroup(groupDoc);
        }
        if (members != null) {
            groupDoc.setProperty(GROUP_SCHEMA, MEMBERS, members);
        }
        if (subGroups != null) {
            groupDoc.setProperty(GROUP_SCHEMA, SUB_GROUPS, subGroups);
        }
        if (parentGroups != null) {
            groupDoc.setProperty(GROUP_SCHEMA, PARENT_GROUPS, parentGroups);
        }
        for (Entry<String, String> entry : Arrays.asList( //
                new SimpleEntry<>(GROUP_TENANTID, tenantId), //
                new SimpleEntry<>(GROUP_LABEL, groupLabel), //
                new SimpleEntry<>(GROUP_DESCRIPTION, groupDescription))) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (StringUtils.isNotBlank(value)) {
                properties.put(key, value);
            }
        }
        for (Entry<String, String> entry : properties.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key.startsWith(GROUP_COLON)) {
                key = key.substring(GROUP_COLON.length());
            }
            groupDoc.setProperty(GROUP_SCHEMA, key, value);
        }

        // make sure that the new group can be created or updated
        checkCanCreateOrUpdateGroup(groupDoc);

        if (create) {
            userManager.createGroup(groupDoc);
        } else {
            userManager.updateGroup(groupDoc);
        }
    }

    /**
     * Use tenant_mytenant_mygroup instead of mygroup for groups having a tenant id.
     * <p>
     * This is done explicitly instead of the implicit computation done in SQLSession.createEntry which is based on a
     * tenant id deduced from the logged-in user.
     */
    public static String getTenantGroupName(String groupName, String tenantId) {
        if (StringUtils.isBlank(tenantId)) {
            return groupName;
        }
        return BaseSession.computeMultiTenantDirectoryId(tenantId, groupName);
    }

    protected void checkCanCreateOrUpdateGroup(DocumentModel groupDoc) {
        NuxeoPrincipal currentUser = ctx.getPrincipal();
        if (!currentUser.isAdministrator()
                && (!currentUser.isMemberOf("powerusers") || !canCreateOrUpdateGroup(groupDoc))) {
            throw new NuxeoException("User is not allowed to create or edit groups", HttpServletResponse.SC_FORBIDDEN);
        }
    }

    protected boolean canCreateOrUpdateGroup(DocumentModel groupDoc) {
        GroupConfig groupConfig = userManager.getGroupConfig();
        NuxeoGroup group = new NuxeoGroupImpl(groupDoc, groupConfig);
        Set<String> allGroups = computeAllGroups(group);
        List<String> administratorsGroups = userManager.getAdministratorsGroups();
        return allGroups.stream().noneMatch(administratorsGroups::contains);
    }

    protected Set<String> computeAllGroups(NuxeoGroup group) {
        Set<String> allGroups = new HashSet<>();
        Queue<NuxeoGroup> queue = new LinkedList<>();
        queue.add(group);

        while (!queue.isEmpty()) {
            NuxeoGroup nuxeoGroup = queue.poll();
            allGroups.add(nuxeoGroup.getName());
            nuxeoGroup.getParentGroups()
                      .stream()
                      .filter(pg -> !allGroups.contains(pg))
                      .map(userManager::getGroup)
                      .forEach(queue::add);
        }

        return allGroups;
    }

}
