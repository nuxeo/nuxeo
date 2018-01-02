/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.core.security;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.security.UserVisiblePermission;

/**
 * @author Bogdan Stefanescu
 * @author Olivier Grisel
 */
public class DefaultPermissionProvider implements PermissionProviderLocal {

    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(DefaultPermissionProvider.class);

    private final List<PermissionDescriptor> registeredPermissions = new LinkedList<>();

    // to be recomputed each time a new PermissionDescriptor is registered -
    // null means invalidated
    private Map<String, MergedPermissionDescriptor> mergedPermissions;

    private Map<String, Set<String>> mergedGroups;

    private final List<PermissionVisibilityDescriptor> registeredPermissionsVisibility = new LinkedList<>();

    private Map<String, PermissionVisibilityDescriptor> mergedPermissionsVisibility;

    public DefaultPermissionProvider() {
        mergedPermissionsVisibility = null;
    }

    @Override
    public synchronized List<UserVisiblePermission> getUserVisiblePermissionDescriptors(String typeName) {
        if (mergedPermissionsVisibility == null) {
            computeMergedPermissionsVisibility();
        }
        // grab the default items (type is "")
        PermissionVisibilityDescriptor defaultVisibility = mergedPermissionsVisibility.get(typeName);
        if (defaultVisibility == null) {
            // fallback to default
            defaultVisibility = mergedPermissionsVisibility.get("");
        }
        if (defaultVisibility == null) {
            throw new NuxeoException("no permission visibility configuration registered");
        }
        return defaultVisibility.getSortedUIPermissionDescriptor();
    }

    @Override
    public List<UserVisiblePermission> getUserVisiblePermissionDescriptors() {
        return getUserVisiblePermissionDescriptors("");
    }

    // called synchronized
    protected void computeMergedPermissionsVisibility() {
        mergedPermissionsVisibility = new HashMap<>();
        for (PermissionVisibilityDescriptor pvd : registeredPermissionsVisibility) {
            PermissionVisibilityDescriptor mergedPvd = mergedPermissionsVisibility.get(pvd.getTypeName());
            if (mergedPvd == null) {
                mergedPvd = new PermissionVisibilityDescriptor(pvd);
                if (!StringUtils.isEmpty(pvd.getTypeName())) {
                    PermissionVisibilityDescriptor defaultPerms = new PermissionVisibilityDescriptor(
                            mergedPermissionsVisibility.get(""));
                    defaultPerms.merge(mergedPvd);
                    mergedPvd.setPermissionUIItems(
                            defaultPerms.getPermissionUIItems().toArray(new PermissionUIItemDescriptor[] {}));
                }
                mergedPermissionsVisibility.put(mergedPvd.getTypeName(), mergedPvd);
            } else {
                mergedPvd.merge(pvd);
            }
        }
    }

    @Override
    public synchronized String[] getSubPermissions(String perm) {
        List<String> permissions = getPermission(perm).getSubPermissions();
        return permissions.toArray(new String[permissions.size()]);
    }

    @Override
    public synchronized String[] getAliasPermissions(String perm) {
        List<String> permissions = getPermission(perm).getSubPermissions();
        return permissions.toArray(new String[permissions.size()]);
    }

    // called synchronized
    protected MergedPermissionDescriptor getPermission(String perm) {
        if (mergedPermissions == null) {
            computeMergedPermissions();
        }
        MergedPermissionDescriptor mpd = mergedPermissions.get(perm);
        if (mpd == null) {
            throw new NuxeoException(perm + " is not a registered permission");
        }
        return mpd;
    }

    // OG: this is an awkward method prototype left unchanged for BBB
    @Override
    public synchronized String[] getPermissionGroups(String perm) {
        if (mergedGroups == null) {
            computeMergedGroups();
        }
        Set<String> groups = mergedGroups.get(perm);
        if (groups != null && !groups.isEmpty()) {
            // OG: why return null instead of an empty array
            return groups.toArray(new String[groups.size()]);
        }
        return null;
    }

    // called synchronized
    protected void computeMergedGroups() {
        if (mergedPermissions == null) {
            computeMergedPermissions();
        }
        mergedGroups = new HashMap<>();

        // scanning sub permissions to collect direct group membership
        for (MergedPermissionDescriptor mpd : mergedPermissions.values()) {
            for (String subPermission : mpd.getSubPermissions()) {
                Set<String> groups = mergedGroups.get(subPermission);
                if (groups == null) {
                    groups = new TreeSet<>();
                    groups.add(mpd.getName());
                    mergedGroups.put(subPermission, groups);
                } else {
                    if (!groups.contains(mpd.getName())) {
                        groups.add(mpd.getName());
                    }
                }
            }
        }

        // building the transitive closure on groups membership with a recursive
        // method
        Set<String> alreadyProcessed = new HashSet<>();
        for (Entry<String, Set<String>> groupEntry : mergedGroups.entrySet()) {
            String permissionName = groupEntry.getKey();
            Set<String> groups = groupEntry.getValue();
            Set<String> allGroups = computeAllGroups(permissionName, alreadyProcessed);
            groups.addAll(allGroups);
        }
    }

    // called synchronized
    protected Set<String> computeAllGroups(String permissionName, Set<String> alreadyProcessed) {
        Set<String> allGroups = mergedGroups.get(permissionName);
        if (allGroups == null) {
            allGroups = new TreeSet<>();
        }
        if (alreadyProcessed.contains(permissionName)) {
            return allGroups;
        } else {
            // marking it processed early to avoid infinite loops in case of
            // recursive inclusion
            alreadyProcessed.add(permissionName);
            for (String directGroupName : new TreeSet<>(allGroups)) {
                allGroups.addAll(computeAllGroups(directGroupName, alreadyProcessed));
            }
            return allGroups;
        }
    }

    // OG: this is an awkward method prototype left unchanged for BBB
    @Override
    public synchronized String[] getPermissions() {
        if (mergedPermissions == null) {
            computeMergedPermissions();
        }
        // TODO OG: should we add aliased permissions here as well?
        return mergedPermissions.keySet().toArray(new String[mergedPermissions.size()]);
    }

    // called synchronized
    protected void computeMergedPermissions() {
        mergedPermissions = new HashMap<>();
        for (PermissionDescriptor pd : registeredPermissions) {
            MergedPermissionDescriptor mpd = mergedPermissions.get(pd.getName());
            if (mpd == null) {
                mpd = new MergedPermissionDescriptor(pd);
                mergedPermissions.put(mpd.getName(), mpd);
            } else {
                mpd.mergeDescriptor(pd);
            }
        }
    }

    @Override
    public synchronized void registerDescriptor(PermissionDescriptor descriptor) {
        // check that all included permission have previously been registered
        Set<String> alreadyRegistered = new HashSet<>();
        for (PermissionDescriptor registeredPerm : registeredPermissions) {
            alreadyRegistered.add(registeredPerm.getName());
        }
        for (String includePerm : descriptor.getIncludePermissions()) {
            if (!alreadyRegistered.contains(includePerm)) {
                throw new NuxeoException(
                        String.format("Permission '%s' included by '%s' is not a registered permission", includePerm,
                                descriptor.getName()));
            }
        }
        // invalidate merged permission
        mergedPermissions = null;
        mergedGroups = null;
        // append the new descriptor
        registeredPermissions.add(descriptor);
    }

    @Override
    public synchronized void unregisterDescriptor(PermissionDescriptor descriptor) {
        int lastOccurence = registeredPermissions.lastIndexOf(descriptor);
        if (lastOccurence != -1) {
            // invalidate merged permission
            mergedPermissions = null;
            mergedGroups = null;
            // remove the last occurrence of the descriptor
            registeredPermissions.remove(lastOccurence);
        }
    }

    @Override
    public synchronized void registerDescriptor(PermissionVisibilityDescriptor descriptor) {
        // invalidate cached merged descriptors
        mergedPermissionsVisibility = null;
        registeredPermissionsVisibility.add(descriptor);
    }

    @Override
    public synchronized void unregisterDescriptor(PermissionVisibilityDescriptor descriptor) {
        int lastOccurence = registeredPermissionsVisibility.lastIndexOf(descriptor);
        if (lastOccurence != -1) {
            // invalidate merged descriptors
            mergedPermissionsVisibility = null;
            // remove the last occurrence of the descriptor
            registeredPermissionsVisibility.remove(lastOccurence);
        }
    }

}
