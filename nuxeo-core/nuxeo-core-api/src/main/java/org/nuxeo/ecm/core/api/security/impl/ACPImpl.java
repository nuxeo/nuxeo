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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.api.security.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.Access;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.UserEntry;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;

/**
 * The ACP implementation uses a cache used when calling getAccess().
 */
public class ACPImpl implements ACP {

    /**
     * ConfigurationService property to enable legacy behavior.
     *
     * @since 10.2
     */
    public static final String LEGACY_BEHAVIOR_PROPERTY = "nuxeo.security.acl.legacyBehavior";

    private static final long serialVersionUID = 1L;

    private final List<ACL> acls;

    private transient Map<String, Access> cache;

    private Boolean legacyBehavior;

    public ACPImpl() {
        acls = new ArrayList<>();
        cache = new HashMap<>();
    }

    /**
     * This method must append the ACL and not insert it since it is used to append the inherited ACL which is the less
     * significant ACL.
     */
    @Override
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
                String name = acl.getName();
                switch (name) {
                case ACL.INHERITED_ACL:
                    // add the inherited ACL always at the end
                    acls.add(acl);
                    break;
                case ACL.LOCAL_ACL:
                    // add the local ACL before the inherited if any
                    ACL inherited = getACL(ACL.INHERITED_ACL);
                    if (inherited != null) {
                        int i = acls.indexOf(inherited);
                        acls.add(i, acl);
                    } else {
                        acls.add(acl);
                    }
                    break;
                default:
                    ACL local = getACL(ACL.LOCAL_ACL);
                    if (local != null) {
                        int i = acls.indexOf(local);
                        if (useLegacyBehavior()) {
                            i++;
                        }
                        acls.add(i, acl);
                    } else {
                        inherited = getACL(ACL.INHERITED_ACL);
                        if (inherited != null) {
                            int i = acls.indexOf(inherited);
                            acls.add(i, acl);
                        } else {
                            acls.add(acl);
                        }
                    }
                }
            }
        }
        // if oldACL and ACL are the same instance, we just need to clear
        // the cache
        cache.clear();
    }

    @Override
    public void addACL(int pos, ACL acl) {
        ACL oldACL = getACL(acl.getName());
        if (oldACL != null) {
            acls.remove(oldACL);
        }
        acls.add(pos, acl);
        cache.clear();
    }

    @Override
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

    @Override
    public ACL getACL(String name) {
        String localName = name == null ? ACL.LOCAL_ACL : name;
        return acls.stream().filter(acl -> acl.getName().equals(localName)).findFirst().orElse(null);
    }

    @Override
    public ACL[] getACLs() {
        return acls.toArray(new ACL[acls.size()]);
    }

    @Override
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

    @Override
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

    @Override
    public Access getAccess(String principal, String permission) {
        // check first the cache
        String key = principal + ':' + permission;
        Access access = cache.get(key);
        if (access == null) {
            access = Access.UNKNOWN;
            FOUND_ACE: for (ACL acl : acls) {
                for (ACE ace : acl) {
                    if (permissionsMatch(ace, permission) && principalsMatch(ace, principal)) {
                        access = ace.isGranted() ? Access.GRANT : Access.DENY;
                        break FOUND_ACE;
                    }
                }
            }
            cache.put(key, access);
        }
        return access;
    }

    @Override
    public Access getAccess(String[] principals, String[] permissions) {
        for (ACL acl : acls) {
            for (ACE ace : acl) {
                // only check for effective ACEs
                if (ace.isEffective()) {
                    // fully check ACE in turn against username/permissions
                    // and usergroups/permgroups
                    Access access = getAccess(ace, principals, permissions);
                    if (access != Access.UNKNOWN) {
                        return access;
                    }
                }
            }
        }
        return Access.UNKNOWN;
    }

    public static Access getAccess(ACE ace, String[] principals, String[] permissions) {
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
        if (!SecurityConstants.RESTRICTED_READ.equals(permission)) {
            if (SecurityConstants.EVERYTHING.equals(acePerm)) {
                return true;
            }
        }
        return StringUtils.equals(acePerm, permission);
    }

    private static boolean permissionsMatch(String acePerm, String permission) {
        // RESTRICTED_READ needs special handling, is not implied by EVERYTHING.
        if (SecurityConstants.EVERYTHING.equals(acePerm)) {
            if (!SecurityConstants.RESTRICTED_READ.equals(permission)) {
                return true;
            }
        }
        return StringUtils.equals(acePerm, permission);
    }

    private static boolean principalsMatch(ACE ace, String principal) {
        String acePrincipal = ace.getUsername();
        return principalsMatch(acePrincipal, principal);
    }

    private static boolean principalsMatch(String acePrincipal, String principal) {
        return SecurityConstants.EVERYONE.equals(acePrincipal) || StringUtils.equals(acePrincipal, principal);
    }

    public void addAccessRule(String aclName, ACE ace) {
        ACL acl = getACL(aclName);
        if (acl == null) {
            acl = new ACLImpl(aclName);
            addACL(acl);
        }
        acl.add(ace);
    }

    @Override
    public ACL getOrCreateACL(String name) {
        ACL acl = getACL(name);
        if (acl == null) {
            acl = new ACLImpl(name);
            addACL(acl);
        }
        return acl;
    }

    @Override
    public ACL getOrCreateACL() {
        return getOrCreateACL(ACL.LOCAL_ACL);
    }

    // Rules.

    @Override
    public void setRules(String aclName, UserEntry[] userEntries) {
        setRules(aclName, userEntries, true);
    }

    @Override
    public void setRules(String aclName, UserEntry[] userEntries, boolean overwrite) {

        ACL acl = getACL(aclName);
        if (acl == null) { // create the loca ACL
            acl = new ACLImpl(aclName);
            addACL(acl);
        } else if (overwrite) {
            // :XXX: Should not overwrite entries not given as parameters here.
            acl.clear();
        }
        for (UserEntry entry : userEntries) {
            String username = entry.getUserName();
            for (String permission : entry.getGrantedPermissions()) {
                acl.add(new ACE(username, permission, true));
            }
            for (String permission : entry.getDeniedPermissions()) {
                acl.add(new ACE(username, permission, false));
            }
        }
        cache.clear();
    }

    @Override
    public void setRules(UserEntry[] userEntries) {
        setRules(ACL.LOCAL_ACL, userEntries);
    }

    @Override
    public void setRules(UserEntry[] userEntries, boolean overwrite) {
        setRules(ACL.LOCAL_ACL, userEntries, overwrite);
    }

    // Serialization.

    private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {
        // always perform the default de-serialization first
        in.defaultReadObject();
        // initialize cache to avoid NPE
        cache = new HashMap<>();
    }

    /*
     * NXP-1822 Rux: method for validating in one shot the users allowed to perform an oration. It gets the list of
     * individual permissions which supposedly all grant.
     */
    @Override
    public String[] listUsernamesForAnyPermission(Set<String> perms) {
        List<String> usernames = new ArrayList<>();
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

    @Override
    public ACPImpl clone() {
        ACPImpl copy = new ACPImpl();
        for (ACL acl : acls) {
            copy.acls.add((ACL) acl.clone());
        }
        return copy;
    }

    @Override
    public boolean blockInheritance(String aclName, String username) {
        if (aclName == null) {
            throw new NullPointerException("'aclName' cannot be null");
        }
        if (username == null) {
            throw new NullPointerException("'username' cannot be null");
        }

        ACL acl = getOrCreateACL(aclName);
        boolean aclChanged = acl.blockInheritance(username);
        if (aclChanged) {
            addACL(acl);
        }
        return aclChanged;
    }

    @Override
    public boolean unblockInheritance(String aclName) {
        if (aclName == null) {
            throw new NullPointerException("'aclName' cannot be null");
        }

        ACL acl = getOrCreateACL(aclName);
        boolean aclChanged = acl.unblockInheritance();
        if (aclChanged) {
            addACL(acl);
        }
        return aclChanged;
    }

    @Override
    public boolean addACE(String aclName, ACE ace) {
        if (aclName == null) {
            throw new NullPointerException("'aclName' cannot be null");
        }

        ACL acl = getOrCreateACL(aclName);
        boolean aclChanged = acl.add(ace);
        if (aclChanged) {
            addACL(acl);
        }
        return aclChanged;
    }

    @Override
    public boolean replaceACE(String aclName, ACE oldACE, ACE newACE) {
        if (aclName == null) {
            throw new NullPointerException("'aclName' cannot be null");
        }

        ACL acl = getOrCreateACL(aclName);
        boolean aclChanged = acl.replace(oldACE, newACE);
        if (aclChanged) {
            addACL(acl);
        }
        return aclChanged;
    }

    @Override
    public boolean removeACE(String aclName, ACE ace) {
        if (aclName == null) {
            throw new NullPointerException("'aclName' cannot be null");
        }

        ACL acl = getOrCreateACL(aclName);
        boolean aclChanged = acl.remove(ace);
        if (aclChanged) {
            addACL(acl);
        }
        return aclChanged;
    }

    @Override
    public boolean removeACEsByUsername(String aclName, String username) {
        if (aclName == null) {
            throw new NullPointerException("'aclName' cannot be null");
        }

        ACL acl = getOrCreateACL(aclName);
        boolean aclChanged = acl.removeByUsername(username);
        if (aclChanged) {
            addACL(acl);
        }
        return aclChanged;
    }

    @Override
    public boolean removeACEsByUsername(String username) {
        boolean changed = false;
        for (ACL acl : acls) {
            boolean aclChanged = acl.removeByUsername(username);
            if (aclChanged) {
                addACL(acl);
                changed = true;
            }
        }
        return changed;
    }

    @SuppressWarnings("AutoBoxing")
    protected boolean useLegacyBehavior() {
        if (legacyBehavior == null) {
            // check runtime is present - as ACP is a simple object, it could be used outside of a runtime context
            // otherwise don't use legacy behavior
            legacyBehavior = Framework.getRuntime() != null
                    && Framework.getService(ConfigurationService.class).isBooleanPropertyTrue(LEGACY_BEHAVIOR_PROPERTY);
        }
        return legacyBehavior.booleanValue();
    }
}
