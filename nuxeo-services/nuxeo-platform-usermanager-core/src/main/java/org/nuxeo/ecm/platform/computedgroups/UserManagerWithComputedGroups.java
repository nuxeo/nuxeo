/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 *
 */

package org.nuxeo.ecm.platform.computedgroups;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.common.collections.ScopedMap;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.directory.BaseSession;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.usermanager.UserManagerImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * {@link UserManager} implementation that is aware of {@link ComputedGroup}.
 *
 * @author Thierry Delprat
 */
public class UserManagerWithComputedGroups extends UserManagerImpl {

    private static final long serialVersionUID = 1L;

    protected static ComputedGroupsService cgs;

    protected static Boolean useComputedGroup;

    public static final String VIRTUAL_GROUP_MARKER = "__virtualGroup";

    protected ComputedGroupsService getService() {
        if (cgs == null) {
            cgs = Framework.getLocalService(ComputedGroupsService.class);
        }
        return cgs;
    }

    protected boolean activateComputedGroup() {
        // NXP-8133: recompute during tests, need to find a cleaner fix
        if (useComputedGroup == null || Framework.isTestModeSet()) {
            useComputedGroup = getService().activateComputedGroups();
        }
        return useComputedGroup;
    }

    @Override
    protected NuxeoPrincipal makePrincipal(DocumentModel userEntry, boolean anonymous, List<String> groups)
            {

        NuxeoPrincipal principal = super.makePrincipal(userEntry, anonymous, groups);
        if (activateComputedGroup() && principal instanceof NuxeoPrincipalImpl) {
            NuxeoPrincipalImpl nuxPrincipal = (NuxeoPrincipalImpl) principal;

            List<String> vGroups = getService().computeGroupsForUser(nuxPrincipal);

            if (vGroups == null) {
                vGroups = new ArrayList<String>();
            }

            List<String> origVGroups = nuxPrincipal.getVirtualGroups();
            if (origVGroups == null) {
                origVGroups = new ArrayList<String>();
            }

            // MERGE!
            origVGroups.addAll(vGroups);

            nuxPrincipal.setVirtualGroups(origVGroups);

            // This a hack to work around the problem of running tests
            if (!Framework.isTestModeSet()) {
                nuxPrincipal.updateAllGroups();
            } else {
                List<String> allGroups = nuxPrincipal.getGroups();
                for (String vGroup : vGroups) {
                    if (!allGroups.contains(vGroup)) {
                        allGroups.add(vGroup);
                    }
                }
                nuxPrincipal.setGroups(allGroups);
            }
        }
        return principal;
    }

    @Override
    public NuxeoGroup getGroup(String groupName) {
        NuxeoGroup grp = super.getGroup(groupName);
        if (activateComputedGroup() && (grp == null || getService().allowGroupOverride())) {
            NuxeoGroup computed = getService().getComputedGroup(groupName);
            if (computed != null) {
                grp = computed;
            }
        }
        return grp;
    }

    @Override
    public NuxeoGroup getGroup(String groupName, DocumentModel context) {
        NuxeoGroup grp = super.getGroup(groupName, context);
        if (activateComputedGroup() && (grp == null || getService().allowGroupOverride())) {
            NuxeoGroup computed = getService().getComputedGroup(groupName);
            if (computed != null) {
                grp = computed;
            }
        }
        return grp;
    }

    @Override
    public List<String> getGroupIds() {
        List<String> ids = super.getGroupIds();
        if (activateComputedGroup()) {
            List<String> vGroups = getService().computeGroupIds();
            for (String vGroup : vGroups) {
                if (!ids.contains(vGroup)) {
                    ids.add(vGroup);
                }
            }
        }
        return ids;
    }

    @Override
    public DocumentModel getGroupModel(String groupName) {
        DocumentModel grpDoc = super.getGroupModel(groupName);
        if (activateComputedGroup() && grpDoc == null) {
            return getComputedGroupAsDocumentModel(groupName);
        }
        return grpDoc;
    }

    @Override
    public DocumentModelList searchGroups(Map<String, Serializable> filter, Set<String> fulltext)
            {
        return searchGroups(filter, fulltext, null);
    }

    @Override
    public DocumentModelList searchGroups(Map<String, Serializable> filter, Set<String> fulltext, DocumentModel context)
            {

        boolean searchInVirtualGroups = activateComputedGroup();
        if (Boolean.FALSE.equals(filter.get(VIRTUAL_GROUP_MARKER))) {
            searchInVirtualGroups = false;
        }

        removeVirtualFilters(filter);
        DocumentModelList groups = super.searchGroups(filter, fulltext, context);

        if (searchInVirtualGroups) {
            for (String vGroupName : getService().searchComputedGroups(filter, fulltext)) {
                DocumentModel vGroup = getComputedGroupAsDocumentModel(vGroupName);
                if (vGroup != null) {
                    if (!groups.contains(vGroup)) {
                        groups.add(vGroup);
                    }
                }
            }
        }
        return groups;
    }

    protected DocumentModel getComputedGroupAsDocumentModel(String grpName) {
        NuxeoGroup grp = getService().getComputedGroup(grpName);
        if (grp == null) {
            return null;
        }

        String schemaName = getGroupSchemaName();
        String id = getGroupIdField();
        DocumentModel groupDoc = BaseSession.createEntryModel(null, schemaName, grpName, null);

        groupDoc.setProperty(schemaName, getGroupMembersField(), grp.getMemberUsers());
        groupDoc.setProperty(schemaName, id, grp.getName());
        groupDoc.setProperty(schemaName, getGroupIdField(), grp.getName());

        final ScopedMap contextData = groupDoc.getContextData();
        contextData.putScopedValue("virtual", Boolean.TRUE);

        return groupDoc;
    }

}
