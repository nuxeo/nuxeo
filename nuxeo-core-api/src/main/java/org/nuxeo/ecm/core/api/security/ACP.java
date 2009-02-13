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

package org.nuxeo.ecm.core.api.security;

import java.io.Serializable;
import java.util.Set;

/**
 *
 * Access control policy (ACP) control the permissions access on a resource.
 * <p>
 * An ACP may contains several ACLs (access control list) identified by names.
 * <p>
 * The list of ACLs is ordered so that when checking permissions the ACL are
 * consulted in an ascending order. (The ACL on position 0 is consulted first).
 * <p>
 * Every ACP has at least one ACL having the reserved name "local". This is the
 * only user editable list (through the security UI).
 * <p>
 * Other ACLs are used internally and are editable only through the API.
 * <p>
 * Also an ACP may have a list named "inherited" that represents the ACLs
 * inherited from the resource parents if any. These ACLs are merged in a single
 * list that is always read only even through the API.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public interface ACP extends Serializable, Cloneable {

    /**
     * Check whether this ACP grant the given permission on the given user,
     * denies it or doesn't specify a rule.
     * <p>
     * This is checking only the ACLs on that ACP. Parents if any are not
     * checked.
     *
     * @param principal the principal to check
     * @param permission the permission to check
     * @return Access.GRANT if granted, Access.DENY if denied or Access.UNKNOWN
     *         if no rule for that permission exists. Never returns null.
     */
    Access getAccess(String principal, String permission);

    /**
     * Checks the access on the ACLs for each set of the given permissions and
     * principals.
     * <p>
     * This differs for an iterative check using getAccess(String principal,
     * String permission) in the order of checks - so that in this case each ACE
     * is fully checked against the given users and permissions before passing
     * to the next ACE.
     *
     * @param principals
     * @param permissions
     * @return
     */
    Access getAccess(String[] principals, String[] permissions);

    /**
     * Replaces the modifiable user entries (associated with the
     * currentDocument) related to the current ACP.
     * <p>
     * Considers that all the passed entries are modifiable and attempts to set
     * them as local entries related to the current document.
     *
     * @param userEntries
     */
    void setRules(UserEntry[] userEntries);

    /**
     * Replaces the modifiable user entries (associated with the
     * currentDocument) related to the current ACP.
     * <p>
     * Considers that all the passed entries are modifiable and attempts to set
     * them as local entries related to the current document.
     * <p>
     * The current behavior reset <strong>completely</strong> the current ACL.
     *
     * @param userEntries
     * @param overwrite if true, will overwrite the whole current ACL
     */
    void setRules(UserEntry[] userEntries, boolean overwrite);

    /**
     * Replaces the modifiable user entries (associated with the
     * currentDocument) related to the ACP.
     * <p>
     * Considers that all the passed entries are modifiable and attempts to set
     * them as entries related to the current document.
     *
     * @param aclName
     * @param userEntries
     */
    void setRules(String aclName, UserEntry[] userEntries);

    /**
     * Replaces the modifiable user entries (associated with the
     * currentDocument) related to the ACP.
     * <p>
     * Considers that all the passed entries are modifiable and attempts to set
     * them as entries related to the current document.
     *
     * @param aclName
     * @param userEntries
     * @param overwrite if true, will overwrite the whole ACL
     */
    void setRules(String aclName, UserEntry[] userEntries, boolean overwrite);

    String[] getOwners();

    void setOwners(String[] owners);

    void removeOwner(String owner);

    void addOwner(String owner);

    boolean isOwner(String username);

    void addACL(ACL acl);

    void addACL(int pos, ACL acl);

    void addACL(String afterMe, ACL acl);

    ACL removeACL(String name);

    ACL getACL(String name);

    ACL[] getACLs();

    ACL getMergedACLs(String name);

    ACL getOrCreateACL(String name);

    ACL getOrCreateACL();

    /**
     * Returns the usernames having a given permission.
     *
     * @param perm the permission name.
     * @return a list of usernames
     */
    String[] listUsernamesForPermission(String perm);

    /**
     * Returns the usernames granted to perform an operation based on a list of
     * permissions.
     *
     * @param perms the list of permissions.
     * @return a list of usernames
     */
    String[] listUsernamesForAnyPermission(Set<String> perms);

    /**
     * Return a recursive copy of the ACP sharing no mutable substructure with
     * the original
     *
     * @return a copy
     */
    Object clone();

}
