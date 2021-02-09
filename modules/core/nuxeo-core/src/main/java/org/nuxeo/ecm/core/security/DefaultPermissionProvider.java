/*
 * (C) Copyright 2006-2021 Nuxeo (http://nuxeo.com/) and others.
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
 *     Bogdan Stefanescu
 *     Olivier Grisel
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.core.security;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.security.PermissionProvider;
import org.nuxeo.ecm.core.api.security.UserVisiblePermission;

/**
 * Service handling permissions.
 */
public class DefaultPermissionProvider implements PermissionProvider {

    protected final Map<String, PermissionDescriptor> permissions;

    protected final Map<String, Set<String>> groups;

    protected final Map<String, PermissionVisibilityDescriptor> visibility;

    /**
     * Service initialized with contributed descriptors.
     *
     * @since 11.5
     */
    public DefaultPermissionProvider(Map<String, PermissionDescriptor> permissions,
            Map<String, PermissionVisibilityDescriptor> visibility) {
        checkPermissions(permissions);
        this.permissions = permissions;
        this.groups = computeGroups(permissions);
        this.visibility = visibility;
    }

    @Override
    public List<UserVisiblePermission> getUserVisiblePermissionDescriptors(String typeName) {
        // return permission using empty type name ("") as default
        PermissionVisibilityDescriptor defaultVisibility = visibility.getOrDefault(typeName,
                visibility.get(PermissionVisibilityRegistry.DEFAULT_ID));
        if (defaultVisibility == null) {
            throw new NuxeoException(
                    String.format("No permission visibility configuration registered for type '%s'", typeName));
        }
        return defaultVisibility.getSortedUIPermissionDescriptor();
    }

    @Override
    public List<UserVisiblePermission> getUserVisiblePermissionDescriptors() {
        return getUserVisiblePermissionDescriptors(PermissionVisibilityRegistry.DEFAULT_ID);
    }

    /**
     * Expose visibility map for CMIS needs.
     *
     * @since 11.5
     */
    public Map<String, PermissionVisibilityDescriptor> getPermissionsVisibility() {
        return Collections.unmodifiableMap(visibility);
    }

    @Override
    public String[] getSubPermissions(String perm) {
        List<String> permissions = getPermission(perm).getSubPermissions();
        return permissions.toArray(new String[permissions.size()]);
    }

    /**
     * @deprecated since 11.5: unused
     */
    @Deprecated(since = "11.5")
    @Override
    public String[] getAliasPermissions(String perm) {
        List<String> permissions = getPermission(perm).getAliasPermissions();
        return permissions.toArray(new String[permissions.size()]);
    }

    protected PermissionDescriptor getPermission(String perm) {
        PermissionDescriptor mpd = permissions.get(perm);
        if (mpd == null) {
            throw new NuxeoException(perm + " is not a registered permission");
        }
        return mpd;
    }

    @Override
    public synchronized String[] getPermissionGroups(String perm) {
        Set<String> groupList = groups.get(perm);
        if (groupList != null) {
            return groupList.toArray(new String[groupList.size()]);
        }
        return new String[0];
    }

    protected Map<String, Set<String>> computeGroups(Map<String, PermissionDescriptor> permissions) {
        Map<String, Set<String>> mergedGroups = new HashMap<>();
        // scanning sub permissions to collect direct group membership
        for (PermissionDescriptor mpd : permissions.values()) {
            for (String subPermission : mpd.getSubPermissions()) {
                Set<String> groups = mergedGroups.get(subPermission);
                if (groups == null) {
                    groups = new TreeSet<>();
                    groups.add(mpd.getName());
                    mergedGroups.put(subPermission, groups);
                } else if (!groups.contains(mpd.getName())) {
                    groups.add(mpd.getName());
                }
            }
        }

        // building the transitive closure on groups membership with a recursive method
        Set<String> alreadyProcessed = new HashSet<>();
        for (Entry<String, Set<String>> groupEntry : mergedGroups.entrySet()) {
            String permissionName = groupEntry.getKey();
            Set<String> groups = groupEntry.getValue();
            Set<String> allGroups = computeAllGroups(mergedGroups, permissionName, alreadyProcessed);
            groups.addAll(allGroups);
        }
        return mergedGroups;
    }

    protected Set<String> computeAllGroups(Map<String, Set<String>> mergedGroups, String permissionName,
            Set<String> alreadyProcessed) {
        Set<String> allGroups = mergedGroups.get(permissionName);
        if (allGroups == null) {
            allGroups = new TreeSet<>();
        }
        if (alreadyProcessed.contains(permissionName)) {
            return allGroups;
        } else {
            // marking it processed early to avoid infinite loops in case of recursive inclusion
            alreadyProcessed.add(permissionName);
            for (String directGroupName : new TreeSet<>(allGroups)) {
                allGroups.addAll(computeAllGroups(mergedGroups, directGroupName, alreadyProcessed));
            }
            return allGroups;
        }
    }

    @Override
    public synchronized String[] getPermissions() {
        return permissions.keySet().toArray(new String[permissions.size()]);
    }

    protected void checkPermissions(Map<String, PermissionDescriptor> permissions) {
        // check that all included permissions are defined
        for (PermissionDescriptor p : permissions.values()) {
            List<String> unknownPermissions = p.getSubPermissions()
                                               .stream()
                                               .filter(Predicate.not(permissions::containsKey))
                                               .collect(Collectors.toList());
            if (!unknownPermissions.isEmpty()) {
                throw new NuxeoException(String.format("Permission(s) %s included by '%s' are unknown",
                        unknownPermissions, p.getName()));
            }
        }
    }

}
