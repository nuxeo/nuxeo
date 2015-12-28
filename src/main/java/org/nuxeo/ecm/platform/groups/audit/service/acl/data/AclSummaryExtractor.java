/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Martin Pernollet
 */

package org.nuxeo.ecm.platform.groups.audit.service.acl.data;

import java.util.HashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.groups.audit.service.acl.Pair;
import org.nuxeo.ecm.platform.groups.audit.service.acl.filter.IContentFilter;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import static org.nuxeo.ecm.core.api.security.ACL.LOCAL_ACL;

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
     * Remark: content might be ignored according to the policy implemented by {@link IContentFilter}.
     *
     * @param doc
     * @return
     */
    public Multimap<String, Pair<String, Boolean>> getAllAclByUser(DocumentModel doc) {
        ACP acp = doc.getACP();
        ACL[] acls = acp.getACLs();
        return getAclByUser(acls);
    }

    public Multimap<String, Pair<String, Boolean>> getAclLocalByUser(DocumentModel doc) {
        ACP acp = doc.getACP();
        ACL acl = acp.getACL(LOCAL_ACL);
        return getAclByUser(acl);
    }

    public Multimap<String, Pair<String, Boolean>> getAclInheritedByUser(DocumentModel doc) {
        ACP acp = doc.getACP();
        ACL acl = acp.getACL(ACL.INHERITED_ACL);
        return getAclByUser(acl);
    }

    public Multimap<String, Pair<String, Boolean>> getAclByUser(ACL[] acls) {
        Multimap<String, Pair<String, Boolean>> aclByUser = HashMultimap.create();

        for (ACL acl : acls) {
            fillAceByUser(aclByUser, acl);
        }
        return aclByUser;
    }

    public Multimap<String, Pair<String, Boolean>> getAclByUser(ACL acl) {
        Multimap<String, Pair<String, Boolean>> aclByUser = HashMultimap.create();
        fillAceByUser(aclByUser, acl);
        return aclByUser;
    }

    protected void fillAceByUser(Multimap<String, Pair<String, Boolean>> aclByUser, ACL acl) {
        if (acl == null)
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
     * Returns true if this document owns an ACE locking inheritance Remark: content might be ignored according to the
     * policy implemented by {@link IContentFilter}.
     *
     * @see isLockInheritance(ACE)
     * @param doc
     * @return
     */
    public boolean hasLockInheritanceACE(DocumentModel doc) {
        // Fetch only local ACL to prevent from having blocking inheritance on
        // all child.
        ACL acl = doc.getACP().getOrCreateACL(LOCAL_ACL);

        for (ACE ace : acl.getACEs()) {
            if (filter.acceptsUserOrGroup(ace.getUsername())) {
                if (isLockInheritance(ace))
                    return true;
            }
        }
        return false;
    }

    public boolean hasLockInheritanceACE(Multimap<String, Pair<String, Boolean>> acls) {
        for (String user : acls.keySet()) {
            for (Pair<String, Boolean> ace : acls.get(user)) {
                if (SecurityConstants.EVERYONE.equals(user)) {
                    if (filter.acceptsUserOrGroup(user)) {
                        if (SecurityConstants.EVERYTHING.equals(ace.a) && !ace.b)
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
        return (SecurityConstants.EVERYONE.equals(user) && SecurityConstants.EVERYTHING.equals(ace.a) && !ace.b);
    }

    /**
     * Return the set of users and permissions mentionned in this document's ACLs. Remark: content might be ignored
     * according to the policy implemented by {@link IContentFilter}.
     *
     * @param doc
     * @return
     */
    public Pair<HashSet<String>, HashSet<String>> getAclSummary(DocumentModel doc) {
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

    public void printAce(DocumentModel doc) {
        ACP acp = doc.getACP();
        ACL[] acls = acp.getACLs();

        for (ACL acl : acls) {
            for (ACE ace : acl.getACEs()) {
                System.out.println(ace);
            }
        }
    }
}
