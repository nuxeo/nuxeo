/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql.coremodel;

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.Access;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.security.SecurityException;
import org.nuxeo.ecm.core.security.SecurityManager;
import org.nuxeo.ecm.core.storage.sql.Node;

/**
 * @author Florent Guillaume
 */
public class SQLSecurityManager implements SecurityManager {

    /*
     * ----- org.nuxeo.ecm.core.security.SecurityManager -----
     */

    public void setACP(Document doc, ACP acp, boolean overwrite)
            throws SecurityException {
        if (overwrite) {
            replaceACP(doc, acp);
        } else {
            updateACP(doc, acp);
        }
    }

    public ACP getACP(Document doc) throws SecurityException {
        ACP acp = new ACPImpl();
        // collectOwners(acp, acpNode);
        collectACLs(acp, null);
        return acp;
    }

    public ACP getMergedACP(Document doc) throws SecurityException {
        // TODO cache ACPs
        try {
            ACL acl = getInheritedACLs(doc);
            ACP acp = getACP(doc);
            Document parent = doc.getParent();
            if (parent == null) {
                return acp;
            }
            if (acp == null) {
                if (acl == null) {
                    return null;
                }
                acp = new ACPImpl();
            }
            if (acl != null) {
                acp.addACL(acl);
            }
            return acp;
        } catch (DocumentException e) {
            throw new SecurityException("Failed to get merged acp", e);
        }
    }

    public boolean checkPermission(Document doc, String username,
            String permission) throws SecurityException {
        Access access = getAccess(doc, username, permission);
        // the default is DENY
        return access.toBoolean();
        // return true;
    }

    public Access getAccess(Document doc, String username, String permission)
            throws SecurityException {
        ACP acp = getMergedACP(doc);
        if (acp != null) {
            return acp.getAccess(username, permission);
        }
        return Access.UNKNOWN;
    }

    public void invalidateCache(Session session) {
        // TODO: no cache is used for now
    }

    /*
     * ----- internal methods -----
     */

    protected ACL getInheritedACLs(Document doc) throws DocumentException {
        ACL inheritedAcls = null;
        Document parent = doc.getParent();
        while (parent != null) {
            ACP acp = getACP(parent);
            if (acp != null) {
                ACL acl = acp.getMergedACLs(ACL.INHERITED_ACL);
                if (inheritedAcls == null) {
                    inheritedAcls = acl;
                } else {
                    inheritedAcls.addAll(acl);
                }
                if (acp.getAccess(SecurityConstants.EVERYONE,
                        SecurityConstants.EVERYTHING) == Access.DENY) {
                    break;
                }
            }
            parent = parent.getParent();
        }
        return inheritedAcls;
    }

    protected static void replaceACP(Document doc, ACP acp)
            throws SecurityException {
        if (acp == null) {
            removeACP(doc);
        } else {
            // remove old node

            // create an empty ACP node

            // fill the new node with data from the user acp
            // writeACP(acpNode, acp);
        }
    }

    protected static void updateACP(Document doc, ACP acp)
            throws SecurityException {
        if (acp == null) {
            return;
        }
        // TODO XZXX
    }

    protected static void collectOwners(ACP acp, Node acpNode) {
        // TODO XXX
        return;
    }

    protected static void collectACLs(ACP acp, Node acpNode)
            throws SecurityException {
        // XXX fake ACL for now
        ACL acl = new ACLImpl(ACL.LOCAL_ACL);
        collectACEs(acl, acpNode);
        acp.addACL(acl);
    }

    protected static void collectACEs(ACL acl, Node aclNode)
            throws SecurityException {
        // TODO XXX loop
        ACE ace = getACE(aclNode);
        acl.add(ace);
    }

    protected static ACE getACE(Node aceNode) throws SecurityException {
        // TODO XXX
        return new ACE("Administrator", "Everything", true);
    }

    protected static void writeACP(Node acpNode, ACP acp)
            throws SecurityException {
        String[] owners = acp.getOwners();
        writeOwners(acpNode, owners);
        ACL[] acls = acp.getACLs();
        for (ACL acl : acls) {
            // avoid to write down the inherited ACL
            if (!ACL.INHERITED_ACL.equals(acl.getName())) {
                writeACL(acpNode, acl);
            }
        }
    }

    protected static void writeOwners(Node acpNode, String[] owners)
            throws SecurityException {
        // TODO XXX
        return;
    }

    protected static void writeACL(Node acpNode, ACL acl)
            throws SecurityException {
        // write ACEs
        for (ACE ace : acl) {
            String username = ace.getUsername();
            String permission = ace.getPermission();
            // String name = ISO9075.encode(username) + '@' + permission;
            String name = username;
            // TODO XXX
        }
    }

    protected static void updateACL(Node aclNode, ACL acl)
            throws SecurityException {
        // write ACEs
        for (ACE ace : acl) {
            String username = ace.getUsername();
            String permission = ace.getPermission();
            // String name = ISO9075.encode(username) + '@' + permission;
            String name = username;
            // TODO XXX
        }
    }

    protected static void removeACP(Document doc) throws SecurityException {
        // TODO XXX
        return;
    }

}
