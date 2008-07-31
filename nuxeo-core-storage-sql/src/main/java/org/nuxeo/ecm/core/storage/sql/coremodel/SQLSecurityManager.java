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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.ACLRow;
import org.nuxeo.ecm.core.storage.sql.CollectionProperty;

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
        String aclname = null;
        for (ACLRow aclrow : acls) {
            if (!aclrow.aclname.equals(aclname)) {
                if (acl != null) {
                    acp.addACL(acl);
                }
                aclname = aclrow.aclname;
                acl = new ACLImpl(aclname);
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
        int aclpos = -1;
        for (ACL acl : acp.getACLs()) {
            String aclname = acl.getName();
            if (aclname.equals(ACL.INHERITED_ACL)) {
                continue;
            }
            aclpos++;
            int pos = 0;
            for (ACE ace : acl.getACEs()) {
                aclrows.add(makeACLRow(aclpos, aclname, pos++, ace));
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
            String aclname = acl.getName();
            if (aclname.equals(ACL.INHERITED_ACL)) {
                continue;
            }
            aclmap.put(aclname, acl);
        }
        List<ACE> aces = Collections.emptyList();
        String aclname = null;
        int aclpos = -1;
        int pos = -1;
        for (ACLRow aclrow : aclrows) {
            // new acl?
            if (!aclrow.aclname.equals(aclname)) {
                // finish remaining aces
                for (ACE ace : aces) {
                    newaclrows.add(makeACLRow(aclpos, aclname, pos++, ace));
                }
                // start next round
                aclpos++;
                aclname = aclrow.aclname;
                pos = 0;
                ACL acl = aclmap.remove(aclname);
                aces = acl == null ? Collections.<ACE> emptyList()
                        : new LinkedList<ACE>(Arrays.asList(acl.getACEs()));
            }
            // check if any ace replaces current row
            for (Iterator<ACE> it = aces.iterator(); it.hasNext();) {
                ACE ace = it.next();
                String user = aclrow.user;
                if (user == null) {
                    user = aclrow.group;
                }
                if (user.equals(ace.getUsername()) &&
                        aclrow.permission.equals(ace.getPermission())) {
                    // match user + permission -> replace
                    if (aclrow.grant != ace.isGranted()) {
                        // NOTE slightly different semantics than in JCR (here
                        // we don't move updated rows to the end, this keeps the
                        // pos unchanged)
                        aclrow = new ACLRow(aclpos, aclname, pos,
                                ace.isGranted(), aclrow.permission,
                                aclrow.user, aclrow.group);
                    }
                    it.remove();
                    break;
                }
            }
            // add acl
            newaclrows.add(aclrow);
            pos++;
        }
        // finish remaining aces for last acl done
        for (ACE ace : aces) {
            newaclrows.add(makeACLRow(aclpos, aclname, pos++, ace));
        }
        // do non-done acls
        for (ACL acl : aclmap.values()) {
            aclpos++;
            aclname = acl.getName();
            pos = 0;
            for (ACE ace : acl.getACEs()) {
                newaclrows.add(makeACLRow(aclpos, aclname, pos++, ace));
            }
        }
        ACLRow[] array = new ACLRow[newaclrows.size()];
        return newaclrows.toArray(array);
    }

    protected static ACLRow makeACLRow(int aclpos, String aclname, int pos,
            ACE ace) {
        // XXX should prefix user/group
        String user = ace.getUsername();
        String group = null; // XXX all in user for now
        return new ACLRow(aclpos, aclname, pos, ace.isGranted(),
                ace.getPermission(), user, group);
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
