/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 * *
 */

package org.nuxeo.ecm.platform.computedgroups;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * {@link ComputedGroupsService} implementation
 *
 * @author Thierry Delprat
 *
 */
public class ComputedGroupsServiceImpl extends DefaultComponent implements
        ComputedGroupsService {

    public static final String COMPUTER_EP = "computer";

    public static final String CHAIN_EP = "computerChain";

    protected static Map<String, GroupComputerDescriptor> computers = new HashMap<String, GroupComputerDescriptor>();

    protected static List<String> computerNames = new ArrayList<String>();

    protected boolean allowOverride = true;

    protected static Log log = LogFactory.getLog(ComputedGroupsServiceImpl.class);

    public void activate(ComponentContext context) throws Exception {
        super.activate(context);
        computers = new HashMap<String, GroupComputerDescriptor>();
        computerNames = new ArrayList<String>();
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {

        if (COMPUTER_EP.equals(extensionPoint)) {
            GroupComputerDescriptor desc = (GroupComputerDescriptor) contribution;
            computers.put(desc.getName(), desc);
        } else if (CHAIN_EP.equals(extensionPoint)) {
            GroupComputerChainDescriptor desc = (GroupComputerChainDescriptor) contribution;
            if (desc.isAppend()) {
                computerNames.addAll(desc.getComputerNames());
            } else {
                computerNames = desc.getComputerNames();
            }

        }
    }

    public List<String> computeGroupsForUser(NuxeoPrincipalImpl nuxeoPrincipal) {

        List<String> userGroups = new ArrayList<String>();
        try {
            for (String computerName : computerNames) {
                userGroups.addAll(computers.get(computerName).getComputer().getGroupsForUser(
                        nuxeoPrincipal));
            }
        } catch (Exception e) {
            log.error("Error while getting virtual groups for user "
                    + nuxeoPrincipal.getName(), e);
        }
        return userGroups;
    }

    public boolean allowGroupOverride() {
        return allowOverride;
    }

    public NuxeoGroup getComputedGroup(String groupName) {
        try {
            boolean groupExists = false;
            for (String name : computerNames) {
                GroupComputerDescriptor desc = computers.get(name);
                if (desc.getComputer().hasGroup(groupName)) {
                    groupExists = true;
                    break;
                }
            }
            if (groupExists) {
                return new NuxeoComputedGroup(groupName);
            }
        } catch (Exception e) {
            log.error("Error while getting virtual group " + groupName, e);
        }
        return null;
    }

    public List<String> computeGroupIds() {

        List<String> groupIds = new ArrayList<String>();
        try {
            for (String name : computerNames) {
                GroupComputerDescriptor desc = computers.get(name);
                List<String> foundGroupIds = desc.getComputer().getAllGroupIds();
                if (foundGroupIds != null) {
                    groupIds.addAll(foundGroupIds);
                }
            }
        } catch (Exception e) {
            log.error("Error while listing virtual groups ids ", e);
            return new ArrayList<String>();
        }
        return groupIds;
    }

    public List<String> getComputedGroupMembers(String groupName) {
        try {
            List<String> members = new ArrayList<String>();

            for (String name : computerNames) {
                GroupComputerDescriptor desc = computers.get(name);
                List<String> foundMembers = desc.getComputer().getGroupMembers(
                        groupName);
                if (foundMembers != null) {
                    members.addAll(foundMembers);
                }
            }
            return members;
        } catch (Exception e) {
            log.error("Error while getting members of virtual group "
                    + groupName, e);
            return new ArrayList<String>();
        }
    }

    public List<String> getComputedGroupParent(String groupName) {
        try {
            List<String> parents = new ArrayList<String>();

            for (String name : computerNames) {
                GroupComputerDescriptor desc = computers.get(name);
                List<String> foundParents = desc.getComputer().getParentsGroupNames(
                        groupName);
                if (foundParents != null) {
                    parents.addAll(foundParents);
                }
            }
            return parents;
        } catch (Exception e) {
            log.error("Error while getting parent of virtual group "
                    + groupName, e);
        }
        return null;
    }

    public List<String> getComputedGroupSubGroups(String groupName) {
        try {
            List<String> subGroups = new ArrayList<String>();
            for (String name : computerNames) {
                GroupComputerDescriptor desc = computers.get(name);
                List<String> foundSubGroups = desc.getComputer().getSubGroupsNames(
                        groupName);
                if (foundSubGroups != null) {
                    subGroups.addAll(foundSubGroups);
                }
            }
            return subGroups;
        } catch (Exception e) {
            log.error("Error while getting subgroups of virtual group "
                    + groupName, e);
        }
        return null;
    }

    public List<GroupComputerDescriptor> getComputerDescriptors() {

        List<GroupComputerDescriptor> result = new ArrayList<GroupComputerDescriptor>();
        for (String name : computerNames) {
            result.add(computers.get(name));
        }
        return result;
    }

    public boolean activateComputedGroups() {
        return computerNames.size() > 0;
    }

    public List<String> searchComputedGroups(Map<String, Serializable> filter,
            HashSet<String> fulltext) {

        List<String> foundGroups = new ArrayList<String>();
        try {
            for (String name : computerNames) {
                GroupComputerDescriptor desc = computers.get(name);
                foundGroups.addAll(desc.getComputer().searchGroups(filter,
                        fulltext));
            }
            Collections.sort(foundGroups);
        } catch (Exception e) {
            log.error("Error while searching computed groups", e);
        }
        return foundGroups;
    }

    public void deactivate(ComponentContext context) throws Exception {
        super.deactivate(context);

    }
}
