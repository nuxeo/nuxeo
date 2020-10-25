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
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.query.sql.model.QueryBuilder;
import org.nuxeo.ecm.platform.usermanager.GroupConfig;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * {@link ComputedGroupsService} implementation
 *
 * @author Thierry Delprat
 */
public class ComputedGroupsServiceImpl extends DefaultComponent implements ComputedGroupsService {

    public static final String COMPUTER_EP = "computer";

    public static final String CHAIN_EP = "computerChain";

    protected Map<String, GroupComputerDescriptor> computers = new HashMap<>();

    protected List<String> computerNames = new ArrayList<>();

    protected boolean allowOverride = true;

    protected static Log log = LogFactory.getLog(ComputedGroupsServiceImpl.class);

    @Override
    public void activate(ComponentContext context) {
        super.activate(context);
        computers.clear();
        computerNames.clear();
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {

        if (COMPUTER_EP.equals(extensionPoint)) {
            if (contribution instanceof GroupComputerDescriptor) {
                GroupComputerDescriptor desc = (GroupComputerDescriptor) contribution;

                if (desc.isEnabled()) {
                    log.debug("Add " + desc.getName() + " from component " + contributor.getName());
                    computers.put(desc.getName(), desc);
                } else {
                    if (computers.containsKey(desc.getName())) {
                        log.debug("Remove " + desc.getName() + " from component " + contributor.getName());
                        computers.remove(desc.getName());
                    } else {
                        log.warn("Can't remove " + desc.getName() + " as not found, from component "
                                + contributor.getName());
                    }
                }
                return;
            } else {
                throw new RuntimeException("Waiting GroupComputerDescriptor contribution kind, please look component "
                        + contributor.getName());
            }
        }

        if (CHAIN_EP.equals(extensionPoint)) {
            GroupComputerChainDescriptor desc = (GroupComputerChainDescriptor) contribution;
            if (desc.isAppend()) {
                computerNames.addAll(desc.getComputerNames());
            } else {
                computerNames = desc.getComputerNames();
            }
            return;
        }

        log.warn("Unkown contribution, please check the component " + contributor.getName());
    }

    @Override
    public List<String> computeGroupsForUser(NuxeoPrincipalImpl nuxeoPrincipal) {
        List<String> userGroups = new ArrayList<>();
        for (String computerName : computerNames) {
            userGroups.addAll(computers.get(computerName).getComputer().getGroupsForUser(nuxeoPrincipal));
        }
        return userGroups;
    }

    @Override
    public void updateGroupsForUser(NuxeoPrincipalImpl nuxeoPrincipal) {
        List<String> computedGroups = computeGroupsForUser(nuxeoPrincipal);
        Set<String> virtualGroups = new HashSet<>(nuxeoPrincipal.getVirtualGroups());
        virtualGroups.addAll(computedGroups);
        nuxeoPrincipal.setVirtualGroups(new ArrayList<>(virtualGroups));
    }

    @Override
    public boolean allowGroupOverride() {
        return allowOverride;
    }

    @Override
    @Deprecated
    public NuxeoGroup getComputedGroup(String groupName) {
        return getComputedGroup(groupName, GroupConfig.DEFAULT);
    }

    @Override
    public NuxeoGroup getComputedGroup(String groupName, GroupConfig groupConfig) {
        for (String name : computerNames) {
            GroupComputer computer = computers.get(name).getComputer();
            if (computer.hasGroup(groupName)) {
                if (computer instanceof GroupComputerLabelled) {
                    String groupLabel = ((GroupComputerLabelled) computer).getLabel(groupName);
                    return new NuxeoComputedGroup(groupName, groupLabel, groupConfig);
                }
                return new NuxeoComputedGroup(groupName, groupConfig);
            }
        }
        return null;
    }

    @Override
    public List<String> computeGroupIds() {
        List<String> groupIds = new ArrayList<>();
        for (String name : computerNames) {
            GroupComputerDescriptor desc = computers.get(name);
            List<String> foundGroupIds = desc.getComputer().getAllGroupIds();
            if (foundGroupIds != null) {
                groupIds.addAll(foundGroupIds);
            }
        }
        return groupIds;
    }

    @Override
    public List<String> getComputedGroupMembers(String groupName) {
        List<String> members = new ArrayList<>();
        for (String name : computerNames) {
            GroupComputerDescriptor desc = computers.get(name);
            List<String> foundMembers = desc.getComputer().getGroupMembers(groupName);
            if (foundMembers != null) {
                members.addAll(foundMembers);
            }
        }
        return members;
    }

    @Override
    public List<String> getComputedGroupParent(String groupName) {
        List<String> parents = new ArrayList<>();
        for (String name : computerNames) {
            GroupComputerDescriptor desc = computers.get(name);
            List<String> foundParents = desc.getComputer().getParentsGroupNames(groupName);
            if (foundParents != null) {
                parents.addAll(foundParents);
            }
        }
        return parents;
    }

    @Override
    public List<String> getComputedGroupSubGroups(String groupName) {
        List<String> subGroups = new ArrayList<>();
        for (String name : computerNames) {
            GroupComputerDescriptor desc = computers.get(name);
            List<String> foundSubGroups = desc.getComputer().getSubGroupsNames(groupName);
            if (foundSubGroups != null) {
                subGroups.addAll(foundSubGroups);
            }
        }
        return subGroups;
    }

    public List<GroupComputerDescriptor> getComputerDescriptors() {
        List<GroupComputerDescriptor> result = new ArrayList<>();
        for (String name : computerNames) {
            result.add(computers.get(name));
        }
        return result;
    }

    @Override
    public boolean activateComputedGroups() {
        return computerNames.size() > 0;
    }

    @Override
    public List<String> searchComputedGroups(Map<String, Serializable> filter, Set<String> fulltext) {
        List<String> foundGroups = new ArrayList<>();
        for (String name : computerNames) {
            GroupComputerDescriptor desc = computers.get(name);
            foundGroups.addAll(desc.getComputer().searchGroups(filter, fulltext));
        }
        Collections.sort(foundGroups);
        return foundGroups;
    }

    @Override
    public List<String> searchComputedGroups(QueryBuilder queryBuilder) {
        List<String> groups = new ArrayList<>();
        for (String name : computerNames) {
            GroupComputerDescriptor desc = computers.get(name);
            groups.addAll(desc.getComputer().searchGroups(queryBuilder));
        }
        Collections.sort(groups);
        return groups;
    }

    @Override
    public void deactivate(ComponentContext context) {
        super.deactivate(context);

    }
}
