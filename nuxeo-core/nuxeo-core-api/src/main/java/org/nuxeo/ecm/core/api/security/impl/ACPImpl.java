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

package org.nuxeo.ecm.core.api.security.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.Access;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.UserAccess;
import org.nuxeo.ecm.core.api.security.UserEntry;

/**
 * The ACP implementation uses a cache used when calling getAccess().
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ACPImpl implements ACP {

    private static final long serialVersionUID = -2640696060701197284L;

    private final ArrayList<String> owners;

    private final List<ACL> acls;

    private transient Map<String, Access> cache;

    public ACPImpl() {
        owners = new ArrayList<String>();
        acls = new ArrayList<ACL>();
        cache = new HashMap<String, Access>();
    }

    // Owners.

    public String[] getOwners() {
        return owners.toArray(new String[owners.size()]);
    }

    public boolean isOwner(String username) {
        return owners.contains(username);
    }

    public void addOwner(String owner) {
        owners.add(owner);
        cache.clear();
    }

    public void removeOwner(String owner) {
        owners.remove(owner);
        cache.clear();
    }

    public void setOwners(String[] owners) {
        this.owners.clear();
        this.owners.addAll(Arrays.asList(owners));
        cache.clear();
    }

    // ACLs.

    /**
     * This method must append the ACL and not insert it since it is used to
     * append the inherited ACL which is the less significant ACL.
     */
    public void addACL(ACL acl) {
        assert acl != null;
        ACL oldACL = getACL(acl.getName());
        if (!acl.equals(oldACL)) {
            // replace existing ACL instance different from acl having the same
            // name, if any
            if (oldACL != null) {
                oldACL.clear();
                oldACL.addAll(acl);
            } else {
                acls.add(acl);
            }
        }
        // if oldACL and ACL are the same instance, we just need to clear
        // the cache
        cache.clear();
    }

    public void addACL(int pos, ACL acl) {
        acls.add(pos, acl);
        cache.clear();
    }

    public void addACL(String afterMe, ACL acl) {
        if (afterMe == null) {
            addACL(0, acl);
        } else {
            int i;
            int len = acls.size();
            for (i = 0; i < len; i++) {
                if (acls.get(i).getName().equals(afterMe)) {
                    break;
                }
            }
            addACL(i + 1, acl);
        }
    }

    public ACL getACL(String name) {
        if (name == null) {
            name = ACL.LOCAL_ACL;
        }
        int len = acls.size();
        for (int i = 0; i < len; i++) {
            ACL acl = acls.get(i);
            if (acl.getName().equals(name)) {
                return acl;
            }
        }
        return null;
    }

    public ACL[] getACLs() {
        return acls.toArray(new ACL[acls.size()]);
    }

    public ACL getMergedACLs(String name) {
        ACL mergedAcl = new ACLImpl(name, true);
        for (ACL acl : acls) {
            mergedAcl.addAll(acl);
        }
        return mergedAcl;
    }

    public static ACL newACL(String name) {
        return new ACLImpl(name);
    }

    public ACL removeACL(String name) {
        for (int i = 0, len = acls.size(); i < len; i++) {
            ACL acl = acls.get(i);
            if (acl.getName().equals(name)) {
                cache.clear();
                return acls.remove(i);
            }
        }
        return null;
    }

    public Access getAccess(String principal, String permission) {
        // check first the cache
        String key = principal + ':' + permission;
        Access access = cache.get(key);
        if (access == null) {
            // TODO: process owners
            access = Access.UNKNOWN;
            FOUND_ACE: for (ACL acl : acls) {
                for (ACE ace : acl) {
                    if (permissionsMatch(ace, permission)
                            && principalsMatch(ace, principal)) {
                        access = ace.isGranted() ? Access.GRANT : Access.DENY;
                        break FOUND_ACE;
                    }
                }
            }
            cache.put(key, access);
        }
        return access;
    }

    public Access getAccess(String[] principals, String[] permissions) {
        for (ACL acl : acls) {
            for (ACE ace : acl) {
                // fully check ACE in turn against username/permissions
                // and usergroups/permgroups
                Access access = getAccess(ace, principals, permissions);
                if (access != Access.UNKNOWN) {
                    return access;
                }
            }
        }
        return Access.UNKNOWN;
    }

    public static Access getAccess(ACE ace, String[] principals,
            String[] permissions) {
        String acePerm = ace.getPermission();
        String aceUser = ace.getUsername();

        for (String principal : principals) {
            if (principalsMatch(aceUser, principal)) {
                // check permission match only if principal is matching
                for (String permission : permissions) {
                    if (permissionsMatch(acePerm, permission)) {
                        return ace.isGranted() ? Access.GRANT : Access.DENY;
                    } // end permissionMatch
                } // end perm for
            } // end principalMatch
        } // end princ for
        return Access.UNKNOWN;
    }

    private static boolean permissionsMatch(ACE ace, String permission) {
        String acePerm = ace.getPermission();

        // RESTRICTED_READ needs special handling, is not implied by EVERYTHING.
        if (!permission.equals(SecurityConstants.RESTRICTED_READ)) {
            if (acePerm.equals(SecurityConstants.EVERYTHING)) {
                return true;
            }
        }

        return acePerm.equals(permission);
    }

    private static boolean permissionsMatch(String acePerm, String permission) {
        // RESTRICTED_READ needs special handling, is not implied by EVERYTHING.
        if (acePerm.equals(SecurityConstants.EVERYTHING)) {
            if (!permission.equals(SecurityConstants.RESTRICTED_READ)) {
                return true;
            }
        }
        return acePerm.equals(permission);
    }

    private static boolean principalsMatch(ACE ace, String principal) {
        String acePrincipal = ace.getUsername();
        if (acePrincipal.equals(SecurityConstants.EVERYONE)) {
            return true;
        }
        return acePrincipal.equals(principal);
    }

    private static boolean principalsMatch(String aceUser, String principal) {
        if (aceUser.equals(SecurityConstants.EVERYONE)) {
            return true;
        }
        return aceUser.equals(principal);
    }

    public void addAccessRule(String aclName, ACE ace) {
        ACL acl = getACL(aclName);
        if (acl == null) {
            acl = new ACLImpl(aclName);
            addACL(acl);
        }
        acl.add(ace);
    }

    public ACL getOrCreateACL(String name) {
        ACL acl = getACL(name);
        if (acl == null) {
            acl = new ACLImpl(name);
            addACL(acl);
        }
        return acl;
    }

    public ACL getOrCreateACL() {
        return getOrCreateACL(ACL.LOCAL_ACL);
    }

    // Rules.

    public void setRules(String aclName, UserEntry[] userEntries) {
        setRules(aclName, userEntries, true);
    }

    public void setRules(String aclName, UserEntry[] userEntries,
            boolean overwrite) {

        ACL acl = getACL(aclName);
        if (acl == null) { // create the loca ACL
            acl = new ACLImpl(aclName);
            addACL(acl);
        } else if (overwrite) {
            // :XXX: Should not overwrite entries not given as parameters here.
            acl.clear();
        }
        for (UserEntry entry : userEntries) {
            for (String permission : entry.getPermissions()) {
                UserAccess userAccess = entry.getAccess(permission);
                if (userAccess.isReadOnly()) {
                    continue; // avoid setting read only rules
                }
                ACE ace = new ACE(entry.getUserName(), permission,
                        userAccess.isGranted());
                acl.add(ace);
            }
        }
        cache.clear();
    }

    public void setRules(UserEntry[] userEntries) {
        setRules(ACL.LOCAL_ACL, userEntries);
    }

    public void setRules(UserEntry[] userEntries, boolean overwrite) {
        setRules(ACL.LOCAL_ACL, userEntries, overwrite);
    }

    // Serialization.

    private void readObject(ObjectInputStream in)
            throws ClassNotFoundException, IOException {
        // always perform the default de-serialization first
        in.defaultReadObject();
        // initialize cache to avoid NPE
        cache = new HashMap<String, Access>();
    }

    public String[] listUsernamesForPermission(String perm) {
        List<String> usernames = new ArrayList<String>();
        ACL merged = getMergedACLs("merged");
        for (ACE ace : merged.getACEs()) {
            if (ace.getPermission().equals(perm) && ace.isGranted()) {
                String username = ace.getUsername();
                if (!usernames.contains(username)) {
                    usernames.add(ace.getUsername());
                }
            }
        }
        return usernames.toArray(new String[usernames.size()]);
    }

    /*
     * NXP-1822 Rux: method for validating in one shot the users allowed to
     * perform an oration. It gets the list of individual permissions which
     * supposedly all grant.
     */
    public String[] listUsernamesForAnyPermission(Set<String> perms) {
        List<String> usernames = new ArrayList<String>();
        ACL merged = getMergedACLs("merged");
        for (ACE ace : merged.getACEs()) {
            if (perms.contains(ace.getPermission()) && ace.isGranted()) {
                String username = ace.getUsername();
                if (!usernames.contains(username)) {
                    usernames.add(username);
                }
            }
        }
        return usernames.toArray(new String[usernames.size()]);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object clone() {
        ACPImpl copy = new ACPImpl();
        for (ACL acl : acls) {
            copy.acls.add((ACL) acl.clone());
        }
        copy.owners.addAll((Collection<String>) owners.clone());
        return copy;
    }

}
