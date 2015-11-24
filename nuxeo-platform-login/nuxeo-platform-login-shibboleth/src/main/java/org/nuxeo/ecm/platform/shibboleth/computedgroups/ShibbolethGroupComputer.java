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
    public List<String> getAllGroupIds() throws Exception {
        List<String> groupsId = new ArrayList<String>();
        for (DocumentModel group : getAllGroups()) {
            groupsId.add(group.getId());
        }
        return groupsId;
    }

    @Override
    public List<String> getGroupMembers(String arg0) throws Exception {
        // Cannot retrieve group member for a specific group, cause it's
        // assigned at user login.
        return null;
    }

    @Override
    public List<String> getGroupsForUser(NuxeoPrincipalImpl nxPrincipal)
            throws Exception {
        List<String> groupsId = new ArrayList<String>();
        for (DocumentModel group : getAllGroups()) {
            String el = (String) group.getPropertyValue(ShibbolethConstants.SHIBBOLETH_SCHEMA
                    + ":" + ShibbolethConstants.GROUP_EL_PROPERTY);
            if (ELGroupComputerHelper.isUserInGroup(nxPrincipal.getModel(), el)) {
                groupsId.add(group.getId());
            }
        }
        return groupsId;
    }

    @Override
    public List<String> getParentsGroupNames(String arg0) throws Exception {
        return ShibbolethGroupHelper.getParentsGroups(arg0);
    }

    @Override
    public List<String> getSubGroupsNames(String arg0) throws Exception {
        return null;
    }

    /**
     * Get current Directory Service
     *
     * @return
     * @throws Exception
     */
    private DirectoryService getDS() throws Exception {
        return Framework.getService(DirectoryService.class);
    }

    /**
     * List all Shibbolet Group in a DocumentModelList
     *
     * @return
     * @throws Exception
     */
    private DocumentModelList getAllGroups() throws Exception {
        Session shibGroupDirectory = null;
        try {
            shibGroupDirectory = getDS().open(getDirectoryName());
            return shibGroupDirectory.getEntries();
        } finally {
            if (shibGroupDirectory != null) {
                shibGroupDirectory.close();
            }
        }
    }
}
