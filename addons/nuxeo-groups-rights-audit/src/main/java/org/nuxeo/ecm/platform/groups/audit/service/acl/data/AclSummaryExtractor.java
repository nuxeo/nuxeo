/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Martin Pernollet
 */

package org.nuxeo.ecm.platform.groups.audit.service.acl.data;

import java.util.HashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.groups.audit.service.acl.Pair;
import org.nuxeo.ecm.platform.groups.audit.service.acl.filter.IContentFilter;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class AclSummaryExtractor {
    private static final Log log = LogFactory.getLog(AclSummaryExtractor.class);

    protected IContentFilter filter;

    public AclSummaryExtractor(IContentFilter filter) {
        this.filter = filter;
    }

    /**
     * Return a compact version of a document ACLs, e.g.:
     * <ul>
     * <li>user1 -> [(READ,true), (WRITE,false), (ADD_CHILDREN,false), ...]
     * <li>user2 -> [(READ,true), (WRITE,true), (ADD_CHILDREN,true), ...]
     * <li>
     * </ul>
     *
     * Remark: content might be ignored according to the policy implemented by
     * {@link IContentFilter}.
     *
     * @param doc
     * @return
     * @throws ClientException
     */
    public Multimap<String, Pair<String, Boolean>> getAllAclByUser(
            DocumentModel doc) throws ClientException {
        ACP acp = doc.getACP();
        ACL[] acls = acp.getACLs();
        return getAclByUser(acls);
    }

    public Multimap<String, Pair<String, Boolean>> getAclLocalByUser(
            DocumentModel doc) throws ClientException {
        ACP acp = doc.getACP();
        ACL acl = acp.getACL(ACL.LOCAL_ACL);
        return getAclByUser(acl);
    }

    public Multimap<String, Pair<String, Boolean>> getAclInheritedByUser(
            DocumentModel doc) throws ClientException {
        ACP acp = doc.getACP();
        ACL acl = acp.getACL(ACL.INHERITED_ACL);
        return getAclByUser(acl);
    }

    public Multimap<String, Pair<String, Boolean>> getAclByUser(ACL[] acls)
            throws ClientException {
        Multimap<String, Pair<String, Boolean>> aclByUser = HashMultimap.create();

        for (ACL acl : acls) {
            fillAceByUser(aclByUser, acl);
        }
        return aclByUser;
    }

    public Multimap<String, Pair<String, Boolean>> getAclByUser(ACL acl)
            throws ClientException {
        Multimap<String, Pair<String, Boolean>> aclByUser = HashMultimap.create();
        fillAceByUser(aclByUser, acl);
        return aclByUser;
    }

    protected void fillAceByUser(
            Multimap<String, Pair<String, Boolean>> aclByUser, ACL acl) {
        if(acl==null)
            return;
        for (ACE ace : acl.getACEs()) {
            if (filter.acceptsUserOrGroup(ace.getUsername())) {
                String userOrGroup = ace.getUsername();
                String permission = ace.getPermission();
                boolean allow = ace.isGranted();
                Pair<String, Boolean> pair = Pair.of(permission, allow);
                aclByUser.put(userOrGroup, pair);

                if (ace.isGranted() && ace.isDenied())
                    log.warn("stupid state: ace granted and denied at the same time. Considered granted");
            }
        }
    }

    /**
     * Returns true if this document owns an ACE locking inheritance
     *
     * Remark: content might be ignored according to the policy implemented by
     * {@link IContentFilter}.
     *
     * @see isLockInheritance(ACE)
     *
     * @param doc
     * @return
     * @throws ClientException
     */
    public boolean hasLockInheritanceACE(DocumentModel doc)
            throws ClientException {
        ACP acp = doc.getACP();
        ACL[] acls = acp.getACLs();

        for (ACL acl : acls) {
            for (ACE ace : acl.getACEs()) {
                if (filter.acceptsUserOrGroup(ace.getUsername())) {
                    if (isLockInheritance(ace))
                        return true;
                }
            }
        }
        return false;
    }

    public boolean hasLockInheritanceACE(
            Multimap<String, Pair<String, Boolean>> acls)
            throws ClientException {
        for (String user : acls.keySet()) {
            for (Pair<String, Boolean> ace : acls.get(user)) {
                if (SecurityConstants.EVERYONE.equals(user)) {
                    if (filter.acceptsUserOrGroup(user)) {
                        if (SecurityConstants.EVERYTHING.equals(ace.a)
                                && !ace.b)
                            return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Return true if this ACE locks inheritance, in other word:
     * <ul>
     * <li>EVERYONE
     * <li>EVERYTHING
     * <li>deny
     * </ul>
     */
    public boolean isLockInheritance(ACE ace) {
        return (SecurityConstants.EVERYONE.equals(ace.getUsername())
                && SecurityConstants.EVERYTHING.equals(ace.getPermission()) && ace.isDenied());
    }

    public boolean isLockInheritance(String user, Pair<String, Boolean> ace) {
        return (SecurityConstants.EVERYONE.equals(user)
                && SecurityConstants.EVERYTHING.equals(ace.a) && !ace.b);
    }

    /**
     * Return the set of users and permissions mentionned in this document's
     * ACLs.
     *
     * Remark: content might be ignored according to the policy implemented by
     * {@link IContentFilter}.
     *
     * @param doc
     * @return
     * @throws ClientException
     */
    public Pair<HashSet<String>, HashSet<String>> getAclSummary(
            DocumentModel doc) throws ClientException {
        Pair<HashSet<String>, HashSet<String>> summary = newSummary();
        ACP acp = doc.getACP();
        ACL[] acls = acp.getACLs();

        for (ACL acl : acls) {
            for (ACE ace : acl.getACEs()) {
                String userOrGroup = ace.getUsername();
                if (filter.acceptsUserOrGroup(userOrGroup)) {
                    String permission = ace.getPermission();
                    summary.a.add(userOrGroup);
                    summary.b.add(permission);
                }
            }
        }
        return summary;
    }

    protected Pair<HashSet<String>, HashSet<String>> newSummary() {
        return Pair.of(new HashSet<String>(), new HashSet<String>());
    }

    public void printAce(DocumentModel doc) throws ClientException {
        ACP acp = doc.getACP();
        ACL[] acls = acp.getACLs();

        for (ACL acl : acls) {
            for (ACE ace : acl.getACEs()) {
                System.out.println(ace);
            }
        }
    }
}
