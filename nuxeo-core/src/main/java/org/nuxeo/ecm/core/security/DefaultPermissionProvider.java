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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.security.UserVisiblePermission;

/**
 * @author Bogdan Stefanescu
 * @author Olivier Grisel
 */
public class DefaultPermissionProvider implements PermissionProviderLocal {

    @SuppressWarnings("unused")
    private static final Log log = LogFactory
            .getLog(DefaultPermissionProvider.class);

    private final List<PermissionDescriptor> registeredPermissions = new LinkedList<PermissionDescriptor>();

    // to be recomputed each time a new PermissionDescriptor is registered -
    // null means invalidated
    private Map<String, MergedPermissionDescriptor> mergedPermissions;

    private Map<String, Set<String>> mergedGroups;

    private final List<PermissionVisibilityDescriptor> registeredPermissionsVisibility
            = new LinkedList<PermissionVisibilityDescriptor>();

    private Map<String, PermissionVisibilityDescriptor> mergedPermissionsVisibility;

    public DefaultPermissionProvider() {
        mergedPermissionsVisibility = null;
    }

    public List<UserVisiblePermission> getUserVisiblePermissionDescriptors(
            String typeName) throws ClientException {
        if (mergedPermissionsVisibility == null) {
            try {
                computeMergedPermissionsVisibility();
            } catch (Exception e) {
                throw new ClientException(e);
            }
        }
        // grab the default items (type is "")
        PermissionVisibilityDescriptor defaultVisibility = mergedPermissionsVisibility
                .get(typeName);
        if (defaultVisibility == null) {
            // fallback to default
            defaultVisibility = mergedPermissionsVisibility.get("");
        }
        if (defaultVisibility == null) {
            throw new ClientException(
                    "no permission visibility configuration registered");
        }
        return defaultVisibility.getSortedUIPermissionDescriptor();
    }

    public List<UserVisiblePermission> getUserVisiblePermissionDescriptors()
            throws ClientException {
        return getUserVisiblePermissionDescriptors("");
    }

    @Deprecated // use getUserVisiblePermissionDescriptors instead
    public String[] getUserVisiblePermissions() throws ClientException {
        if (mergedPermissionsVisibility == null) {
            try {
                computeMergedPermissionsVisibility();
            } catch (Exception e) {
                throw new ClientException(e);
            }
        }
        // grab the default items (type is "")
        PermissionVisibilityDescriptor defaultVisibility = mergedPermissionsVisibility
                .get("");
        if (defaultVisibility == null) {
            throw new ClientException(
                    "no permission visibility configuration registered");
        }
        return defaultVisibility.getSortedItems();
    }

    @Deprecated // use getUserVisiblePermissionDescriptors instead
    public String[] getUserVisiblePermissions(String typeName)
            throws ClientException {
        if (mergedPermissionsVisibility == null) {
            try {
                computeMergedPermissionsVisibility();
            } catch (Exception e) {
                throw new ClientException(e);
            }
        }
        PermissionVisibilityDescriptor defaultVisibility = mergedPermissionsVisibility
                .get(typeName);
        if (defaultVisibility == null) {
            // fallback to default conf
            return getUserVisiblePermissions();
        }
        return defaultVisibility.getSortedItems();
    }

    private void computeMergedPermissionsVisibility() throws Exception {
        mergedPermissionsVisibility = new HashMap<String, PermissionVisibilityDescriptor>();
        for (PermissionVisibilityDescriptor pvd : registeredPermissionsVisibility) {
            PermissionVisibilityDescriptor mergedPvd = mergedPermissionsVisibility
                    .get(pvd.getTypeName());
            if (mergedPvd == null) {
                mergedPvd = new PermissionVisibilityDescriptor(pvd);
                mergedPermissionsVisibility.put(mergedPvd.getTypeName(),
                        mergedPvd);
            } else {
                mergedPvd.merge(pvd);
            }
        }
    }

    public String[] getSubPermissions(String perm) throws ClientException {
        List<String> permissions = getPermission(perm).getSubPermissions();
        return permissions.toArray(new String[permissions.size()]);
    }

    public String[] getAliasPermissions(String perm) throws ClientException {
        List<String> permissions = getPermission(perm).getSubPermissions();
        return permissions.toArray(new String[permissions.size()]);
    }

    private MergedPermissionDescriptor getPermission(String perm)
            throws ClientException {
        if (mergedPermissions == null) {
            computeMergedPermissions();
        }
        MergedPermissionDescriptor mpd = mergedPermissions.get(perm);
        if (mpd == null) {
            throw new ClientException(perm + " is not a registered permission");
        }
        return mpd;
    }

    // OG: this is an awkward method prototype left unchanged for BBB
    public String[] getPermissionGroups(String perm) {
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

    protected synchronized void computeMergedGroups() {
        if (mergedPermissions == null) {
            computeMergedPermissions();
        }
        mergedGroups = new HashMap<String, Set<String>>();

        // scanning sub permissions to collect direct group membership
        for (MergedPermissionDescriptor mpd : mergedPermissions.values()) {
            for (String subPermission : mpd.getSubPermissions()) {
                Set<String> groups = mergedGroups.get(subPermission);
                if (groups == null) {
                    groups = new TreeSet<String>();
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
        Set<String> alreadyProcessed = new HashSet<String>();
        for (Entry<String, Set<String>> groupEntry : mergedGroups.entrySet()) {
            String permissionName = groupEntry.getKey();
            Set<String> groups = groupEntry.getValue();
            Set<String> allGroups = computeAllGroups(permissionName,
                    alreadyProcessed);
            groups.addAll(allGroups);
        }
    }

    protected Set<String> computeAllGroups(String permissionName,
            Set<String> alreadyProcessed) {
        Set<String> allGroups = mergedGroups.get(permissionName);
        if (allGroups == null) {
            allGroups = new TreeSet<String>();
        }
        if (alreadyProcessed.contains(permissionName)) {
            return allGroups;
        } else {
            // marking it processed early to avoid infinite loops in case of
            // recursive inclusion
            alreadyProcessed.add(permissionName);
            for (String directGroupName : new TreeSet<String>(allGroups)) {
                allGroups.addAll(computeAllGroups(directGroupName,
                        alreadyProcessed));
            }
            return allGroups;
        }
    }

    // OG: this is an awkward method prototype left unchanged for BBB
    public String[] getPermissions() {
        if (mergedPermissions == null) {
            computeMergedPermissions();
        }
        // TODO OG: should we add aliased permissions here as well?
        return mergedPermissions.keySet().toArray(
                new String[mergedPermissions.size()]);
    }

    protected synchronized void computeMergedPermissions() {
        mergedPermissions = new HashMap<String, MergedPermissionDescriptor>();
        for (PermissionDescriptor pd : registeredPermissions) {
            MergedPermissionDescriptor mpd = mergedPermissions
                    .get(pd.getName());
            if (mpd == null) {
                mpd = new MergedPermissionDescriptor(pd);
                mergedPermissions.put(mpd.getName(), mpd);
            } else {
                mpd.mergeDescriptor(pd);
            }
        }
    }

    public synchronized void registerDescriptor(
            PermissionDescriptor descriptor) throws Exception {
        // check that all included permission have previously been registered
        Set<String> alreadyRegistered = new HashSet<String>();
        for (PermissionDescriptor registeredPerm : registeredPermissions) {
            alreadyRegistered.add(registeredPerm.getName());
        }
        for (String includePerm : descriptor.getIncludePermissions()) {
            if (!alreadyRegistered.contains(includePerm)) {
                // TODO: OG: use a specific exception sub class instead of the
                // base type
                throw new Exception(
                        String.format(
                                "Permission '%s' included by '%s' is not a registered permission",
                                 includePerm, descriptor.getName()));
            }
        }
        // invalidate merged permission
        mergedPermissions = null;
        mergedGroups = null;
        // append the new descriptor
        registeredPermissions.add(descriptor);
    }

    public synchronized void unregisterDescriptor(
            PermissionDescriptor descriptor) {
        int lastOccurence = registeredPermissions.lastIndexOf(descriptor);
        if (lastOccurence != -1) {
            // invalidate merged permission
            mergedPermissions = null;
            mergedGroups = null;
            // remove the last occurrence of the descriptor
            registeredPermissions.remove(lastOccurence);
        }
    }

    public synchronized void registerDescriptor(
            PermissionVisibilityDescriptor descriptor) throws Exception {
        // invalidate cached merged descriptors
        mergedPermissionsVisibility = null;
        registeredPermissionsVisibility.add(descriptor);
    }

    public synchronized void unregisterDescriptor(
            PermissionVisibilityDescriptor descriptor) throws Exception {
        int lastOccurence = registeredPermissionsVisibility
                .lastIndexOf(descriptor);
        if (lastOccurence != -1) {
            // invalidate merged descriptors
            mergedPermissionsVisibility = null;
            // remove the last occurrence of the descriptor
            registeredPermissionsVisibility.remove(lastOccurence);
        }
    }

}
