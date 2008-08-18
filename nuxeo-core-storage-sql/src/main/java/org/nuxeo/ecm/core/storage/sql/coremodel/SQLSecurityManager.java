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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.Access;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Property;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.security.SecurityException;
import org.nuxeo.ecm.core.security.SecurityManager;
import org.nuxeo.ecm.core.storage.sql.ACLRow;

/**
 * @author Florent Guillaume
 */
public class SQLSecurityManager implements SecurityManager {

    /*
     * ----- org.nuxeo.ecm.core.security.SecurityManager -----
     */

    public ACP getACP(Document doc) throws SecurityException {
        try {
            Property property = ((SQLDocument) doc).getACLProperty();
            return aclRowsToACP((ACLRow[]) property.getValue());
        } catch (DocumentException e) {
            throw new SecurityException(e.getMessage(), e);
        }
    }

    public void setACP(Document doc, ACP acp, boolean overwrite)
            throws SecurityException {
        if (!overwrite && acp == null) {
            return;
        }
        try {
            Property property = ((SQLDocument) doc).getACLProperty();
            ACLRow[] aclrows;
            if (overwrite) {
                aclrows = acp == null ? null : acpToAclRows(acp);
            } else {
                aclrows = updateAclRows((ACLRow[]) property.getValue(), acp);
            }
            property.setValue(aclrows);
        } catch (DocumentException e) {
            throw new SecurityException(e.getMessage(), e);
        }
    }

    // TODO XXX
    public ACP getMergedACP(Document doc) throws SecurityException {
        try {
            ACP acp = getACP(doc);
            if (doc.getParent() == null) {
                return acp;
            }
            ACL acl = getInheritedACLs(doc);
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
        return getAccess(doc, username, permission).toBoolean();
    }

    public Access getAccess(Document doc, String username, String permission)
            throws SecurityException {
        ACP acp = getMergedACP(doc);
        return acp == null ? Access.UNKNOWN : acp.getAccess(username,
                permission);
    }

    public void invalidateCache(Session session) {
    }

    /*
     * ----- internal methods -----
     */

    // unit tested
    protected static ACP aclRowsToACP(ACLRow[] acls) {
        ACP acp = new ACPImpl();
        ACL acl = null;
        String name = null;
        for (ACLRow aclrow : acls) {
            if (!aclrow.name.equals(name)) {
                if (acl != null) {
                    acp.addACL(acl);
                }
                name = aclrow.name;
                acl = new ACLImpl(name);
            }
            // XXX should prefix user/group
            String user = aclrow.user;
            if (user == null) {
                user = aclrow.group;
            }
            acl.add(new ACE(user, aclrow.permission, aclrow.grant));
        }
        if (acl != null) {
            acp.addACL(acl);
        }
        return acp;
    }

    // unit tested
    protected static ACLRow[] acpToAclRows(ACP acp) {
        List<ACLRow> aclrows = new LinkedList<ACLRow>();
        int pos = 0;
        for (ACL acl : acp.getACLs()) {
            String name = acl.getName();
            if (name.equals(ACL.INHERITED_ACL)) {
                continue;
            }
            for (ACE ace : acl.getACEs()) {
                aclrows.add(makeACLRow(pos++, name, ace));
            }
        }
        ACLRow[] array = new ACLRow[aclrows.size()];
        return aclrows.toArray(array);
    }

    // unit tested
    protected static ACLRow[] updateAclRows(ACLRow[] aclrows, ACP acp) {
        List<ACLRow> newaclrows = new LinkedList<ACLRow>();
        Map<String, ACL> aclmap = new HashMap<String, ACL>();
        for (ACL acl : acp.getACLs()) {
            String name = acl.getName();
            if (ACL.INHERITED_ACL.equals(name)) {
                continue;
            }
            aclmap.put(name, acl);
        }
        List<ACE> aces = Collections.emptyList();
        Set<String> aceKeys = null;
        int pos = 0;
        String name = null;
        for (ACLRow aclrow : aclrows) {
            // new acl?
            if (!aclrow.name.equals(name)) {
                // finish remaining aces
                for (ACE ace : aces) {
                    newaclrows.add(makeACLRow(pos++, name, ace));
                }
                // start next round
                name = aclrow.name;
                ACL acl = aclmap.remove(name);
                aces = acl == null ? Collections.<ACE> emptyList()
                        : new LinkedList<ACE>(Arrays.asList(acl.getACEs()));
                aceKeys = new HashSet<String>();
                for (ACE ace : aces) {
                    aceKeys.add(getACEkey(ace));
                }
            }
            if (!aceKeys.contains(getACLrowKey(aclrow))) {
                // no match, keep the aclrow info instead of the ace
                newaclrows.add(new ACLRow(pos++, name, aclrow.grant,
                        aclrow.permission, aclrow.user, aclrow.group));
            }
        }
        // finish remaining aces for last acl done
        for (ACE ace : aces) {
            newaclrows.add(makeACLRow(pos++, name, ace));
        }
        // do non-done acls
        for (ACL acl : aclmap.values()) {
            name = acl.getName();
            for (ACE ace : acl.getACEs()) {
                newaclrows.add(makeACLRow(pos++, name, ace));
            }
        }
        ACLRow[] array = new ACLRow[newaclrows.size()];
        return newaclrows.toArray(array);
    }

    /** Key to distinguish ACEs */
    protected static String getACEkey(ACE ace) {
        // TODO separate user/group
        return ace.getUsername() + '|' + ace.getPermission();
    }

    /** Key to distinguish ACLRows */
    protected static String getACLrowKey(ACLRow aclrow) {
        // TODO separate user/group
        String user = aclrow.user;
        if (user == null) {
            user = aclrow.group;
        }
        return user + '|' + aclrow.permission;
    }

    protected static ACLRow makeACLRow(int pos, String name, ACE ace) {
        // XXX should prefix user/group
        String user = ace.getUsername();
        String group = null; // XXX all in user for now
        return new ACLRow(pos, name, ace.isGranted(), ace.getPermission(),
                user, group);
    }

    protected ACL getInheritedACLs(Document doc) throws DocumentException {
        ACL merged = null;
        doc = doc.getParent();
        while (doc != null) {
            ACP acp = getACP(doc);
            if (acp != null) {
                ACL acl = acp.getMergedACLs(ACL.INHERITED_ACL);
                if (merged == null) {
                    merged = acl;
                } else {
                    merged.addAll(acl);
                }
                if (acp.getAccess(SecurityConstants.EVERYONE,
                        SecurityConstants.EVERYTHING) == Access.DENY) {
                    break;
                }
            }
            doc = doc.getParent();
        }
        return merged;
    }

}
