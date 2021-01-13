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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.query.sql.model.QueryBuilder;
import org.nuxeo.ecm.platform.usermanager.GroupConfig;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.runtime.RuntimeMessage.Level;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * {@link ComputedGroupsService} implementation
 *
 * @author Thierry Delprat
 */
public class ComputedGroupsServiceImpl extends DefaultComponent implements ComputedGroupsService {

    public static final String COMPUTER_EP = "computer";

    public static final String CHAIN_EP = "computerChain";

    protected Map<String, GroupComputer> computers;

    protected List<String> computerNames;

    protected boolean allowOverride = true;

    @Override
    public void start(ComponentContext context) {
        computers = this.<GroupComputerDescriptor> getRegistryContributions(COMPUTER_EP)
                        .stream()
                        .collect(Collectors.toConcurrentMap(GroupComputerDescriptor::getName,
                                GroupComputerDescriptor::getComputer));
        computerNames = this.<GroupComputerChainDescriptor> getRegistryContribution(CHAIN_EP)
                            .map(GroupComputerChainDescriptor::getComputerNames)
                            .orElse(Collections.emptyList());
        List<String> missingComputers = computerNames.stream()
                                                     .filter(Predicate.not(computers::containsKey))
                                                     .collect(Collectors.toList());
        if (!missingComputers.isEmpty()) {
            addRuntimeMessage(Level.ERROR, String.format("Missing group computers: %s", missingComputers));
        }
    }

    @Override
    public void stop(ComponentContext context) throws InterruptedException {
        computers = null;
        computerNames = null;
    }

    @Override
    public List<String> computeGroupsForUser(NuxeoPrincipalImpl nuxeoPrincipal) {
        List<String> userGroups = new ArrayList<>();
        for (String computerName : computerNames) {
            userGroups.addAll(computers.get(computerName).getGroupsForUser(nuxeoPrincipal));
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
            GroupComputer computer = computers.get(name);
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
            List<String> foundGroupIds = computers.get(name).getAllGroupIds();
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
            List<String> foundMembers = computers.get(name).getGroupMembers(groupName);
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
            List<String> foundParents = computers.get(name).getParentsGroupNames(groupName);
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
            List<String> foundSubGroups = computers.get(name).getSubGroupsNames(groupName);
            if (foundSubGroups != null) {
                subGroups.addAll(foundSubGroups);
            }
        }
        return subGroups;
    }

    public List<GroupComputerDescriptor> getComputerDescriptors() {
        return getRegistryContributions(COMPUTER_EP);
    }

    @Override
    public boolean activateComputedGroups() {
        return !computerNames.isEmpty();
    }

    @Override
    public List<String> searchComputedGroups(Map<String, Serializable> filter, Set<String> fulltext) {
        List<String> foundGroups = new ArrayList<>();
        for (String name : computerNames) {
            foundGroups.addAll(computers.get(name).searchGroups(filter, fulltext));
        }
        Collections.sort(foundGroups);
        return foundGroups;
    }

    @Override
    public List<String> searchComputedGroups(QueryBuilder queryBuilder) {
        List<String> groups = new ArrayList<>();
        for (String name : computerNames) {
            groups.addAll(computers.get(name).searchGroups(queryBuilder));
        }
        Collections.sort(groups);
        return groups;
    }

}
