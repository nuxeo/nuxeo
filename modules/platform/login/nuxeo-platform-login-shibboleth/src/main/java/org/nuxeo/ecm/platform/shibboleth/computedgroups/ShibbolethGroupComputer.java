/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Arnaud Kervern
 */

package org.nuxeo.ecm.platform.shibboleth.computedgroups;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.computedgroups.AbstractGroupComputer;
import org.nuxeo.ecm.platform.shibboleth.ShibbolethConstants;
import org.nuxeo.ecm.platform.shibboleth.ShibbolethGroupHelper;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.runtime.api.Framework;

public class ShibbolethGroupComputer extends AbstractGroupComputer {

    protected String getDirectoryName() {
        return ShibbolethConstants.SHIBBOLETH_DIRECTORY;
    }

    @Override
    public List<String> getAllGroupIds() {
        List<String> groupsId = new ArrayList<>();
        for (DocumentModel group : getAllGroups()) {
            groupsId.add(group.getId());
        }
        return groupsId;
    }

    @Override
    public List<String> getGroupMembers(String arg0) {
        // Cannot retrieve group member for a specific group, cause it's
        // assigned at user login.
        return null;
    }

    @Override
    public List<String> getGroupsForUser(NuxeoPrincipalImpl nxPrincipal) {
        List<String> groupsId = new ArrayList<>();
        for (DocumentModel group : getAllGroups()) {
            String el = (String) group.getPropertyValue(ShibbolethConstants.SHIBBOLETH_SCHEMA + ":"
                    + ShibbolethConstants.GROUP_EL_PROPERTY);
            if (ELGroupComputerHelper.isUserInGroup(nxPrincipal.getModel(), el)) {
                groupsId.add(group.getId());
            }
        }
        return groupsId;
    }

    @Override
    public List<String> getParentsGroupNames(String arg0) {
        return ShibbolethGroupHelper.getParentsGroups(arg0);
    }

    @Override
    public List<String> getSubGroupsNames(String arg0) {
        return null;
    }

    /**
     * Get current Directory Service
     */
    private DirectoryService getDS() {
        return Framework.getService(DirectoryService.class);
    }

    /**
     * List all Shibboleth Group in a DocumentModelList
     */
    private DocumentModelList getAllGroups() {
        try (Session shibGroupDirectory = getDS().open(getDirectoryName())) {
            return shibGroupDirectory.getEntries();
        }
    }
}
