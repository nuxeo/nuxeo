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

package org.nuxeo.ecm.core.repository.jcr;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.jackrabbit.util.ISO9075;
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


/**
 *
 * A security manager that works only on JCR repositories.
 * <p>
 * This manager is keeping the ACP corresponding to a document inside
 * the document itself as a JCR node as follow:
 * <p>
 * <code>
 * document
 *  ecm:acp
 *      owners: String[]
 *      acl_name1
 *          usernames: String[]
 *          permissions: String[]
 *          permission_types: int[]
 *      acl_name2
 *      ...
 *
 * </code>
 *
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class JCRSecurityManager implements SecurityManager {


    public void invalidateCache(Session session) {
        // TODO: no cache is used for now
    }

    public boolean checkPermission(Document doc, String username,
            String permission) throws SecurityException {
        Access access = getAccess(doc, username, permission);
        // the default is DENY
        return access.toBoolean();
        //return true;
    }


    public Access getAccess(Document doc, String username,
            String permission) throws SecurityException {
        ACP acp = getMergedACP(doc);
        if (acp != null) {
            return acp.getAccess(username, permission);
        }
        return Access.UNKNOWN;
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


    public ACP getACP(Document doc) throws SecurityException {
        Node docNode = ((JCRDocument) doc).getNode();
        Node acpNode = getACPNode(docNode, false);
        if (acpNode == null) {
            return null;
        }
        ACP acp = new ACPImpl();
        collectOwners(acp, acpNode);
        collectACLs(acp, acpNode);
        return acp;
    }

    public void setACP(Document doc, ACP acp, boolean overwrite) throws SecurityException {
        // TODO: use JCR locks?
        if (overwrite) {
            replaceACP(doc, acp);
        } else {
            // TODO: udpate ACP was not completely tested - may not work correctly
            updateACP(doc, acp);
        }
    }

    public void replaceACP(Document doc, ACP acp) throws SecurityException {
        if (acp == null) {
            removeACP(doc);
        } else {
            try {
                // remove old node
                Node docNode = ((JCRDocument) doc).getNode();
                Node acpNode = getACPNode(docNode, false);
                if (acpNode != null) {
                    acpNode.remove();
                }
                // create an empty ACP node
                acpNode = docNode.addNode(NodeConstants.ECM_ACP.rawname,
                        NodeConstants.ECM_NT_ACP.rawname);
                // fill  the new node with data from the user acp
                writeACP(acpNode, acp);
            } catch (RepositoryException e) {
                throw new SecurityException("Failed to write ACP", e);
            }
        }
    }

    public void updateACP(Document doc, ACP acp) throws SecurityException {
        if (acp == null) {
            return;
        }
        try {
            // get old node
            Node docNode = ((JCRDocument) doc).getNode();
            Node acpNode = getACPNode(docNode, false);
            if (acpNode == null) { // create a new acp
                // create an empty ACP node
                acpNode = docNode.addNode(NodeConstants.ECM_ACP.rawname,
                        NodeConstants.ECM_NT_ACP.rawname);
                // fill  the new node with data from the user acp
                writeACP(acpNode, acp);
            } else { // merge with existing values
                String[] owners = acp.getOwners();
                if (owners != null) {
                    // replace existing owners
                    //TODO: should not replace but update
                    writeOwners(acpNode, owners);
                }
                for (ACL acl : acp.getACLs()) {
                    // overwrite each defined acl
                    Node aclNode = getACLNode(acpNode, acl.getName(), true);
                    updateACL(aclNode, acl);
                }
            }
        } catch (RepositoryException e) {
            throw new SecurityException("Failed to write ACP", e);
        } catch (DocumentException e) {
            throw new SecurityException("Failed to write ACP", e);
        }
    }

    private static void collectOwners(ACP acp, Node acpNode) throws SecurityException {
        try {
            Value[] values = acpNode
                .getProperty(NodeConstants.ECM_OWNERS.rawname).getValues();
            for (Value value : values) {
                acp.addOwner(value.getString());
            }
        } catch (PathNotFoundException e) {
            // ignore unset owners
        } catch (RepositoryException e) {
            throw new SecurityException("Failed to collect ACP owners", e);
        }
    }

    private void collectACLs(ACP acp, Node acpNode) throws SecurityException {
        try {
            NodeIterator it = acpNode.getNodes();
            while (it.hasNext()) {
                Node aclNode = it.nextNode();
                String name = aclNode.getName();
                ACL acl = new ACLImpl(name);
                collectACEs(acl, aclNode);
                acp.addACL(acl);
            }
        } catch (RepositoryException e) {
            throw new SecurityException("Failed to collect ACEs", e);
        }
    }

    private static void collectACEs(ACL acl, Node aclNode) throws SecurityException {
        try {
            NodeIterator it = aclNode.getNodes();
            while (it.hasNext()) {
                Node aceNode = it.nextNode();
                ACE ace = getACE(aceNode);
                acl.add(ace);
            }
        } catch (RepositoryException e) {
            throw new SecurityException("cannot get ACEs", e);
        }
    }

    private static ACE getACE(Node aceNode) throws SecurityException {
        try {
            boolean type = aceNode.getProperty(NodeConstants.ECM_TYPE.rawname).getBoolean();
            String principal = aceNode.getProperty(NodeConstants.ECM_PRINCIPAL.rawname).getString();
            String permission = aceNode.getProperty(NodeConstants.ECM_PERMISSION.rawname).getString();
            return new ACE(principal, permission, type);
        } catch (RepositoryException e) {
            throw new SecurityException("Failed to get ACE type", e);
        }
    }

    private static void writeACP(Node acpNode, ACP acp) throws SecurityException {
        String[] owners = acp.getOwners();
        writeOwners(acpNode, owners);
        ACL[] acls = acp.getACLs();
        for (ACL acl : acls) {
            //avoid to write down the inherited ACL
            if (!ACL.INHERITED_ACL.equals(acl.getName())) {
                writeACL(acpNode, acl);
            }
        }
    }

    private static void writeOwners(Node acpNode, String[] owners) throws SecurityException {
        try {
            acpNode.setProperty(NodeConstants.ECM_OWNERS.rawname, owners);
        } catch (RepositoryException e) {
            throw new SecurityException("Failed to write ACP", e);
        }
    }

    private static void writeACL(Node acpNode, ACL acl) throws SecurityException {
        try {
            Node aclNode = acpNode.addNode(acl.getName(),
                    NodeConstants.ECM_NT_ACL.rawname);
            // write ACEs
            for (ACE ace : acl) {
                String username = ace.getUsername();
                String permission = ace.getPermission();
                String name = ISO9075.encode(username) + '@' + permission;
                // if a username, permission pair already exists
                // then ignore this entry since it will be masked by the existing one
                try {
                    Node aceNode = aclNode.addNode(name,
                        NodeConstants.ECM_NT_ACE.rawname);
                    aceNode.setProperty(NodeConstants.ECM_PRINCIPAL.rawname, ace.getUsername());
                    aceNode.setProperty(NodeConstants.ECM_PERMISSION.rawname, ace.getPermission());
                    aceNode.setProperty(NodeConstants.ECM_TYPE.rawname, ace.isGranted());
                } catch (ItemExistsException e) {
                    // ignore masked entries
                }
            }
        } catch (RepositoryException e) {
            throw new SecurityException("Failed to write ACL", e);
        }
    }

    private static void updateACL(Node aclNode, ACL acl) throws SecurityException {
        try {
            // write ACEs
            for (ACE ace : acl) {
                String username = ace.getUsername();
                String permission = ace.getPermission();
                String name = ISO9075.encode(username) + '@' + permission;
                if (aclNode.hasNode(name)) {
                    aclNode.getNode(name).remove();
                }
                Node aceNode = aclNode.addNode(name,
                        NodeConstants.ECM_NT_ACE.rawname);
                aceNode.setProperty(NodeConstants.ECM_PRINCIPAL.rawname, ace.getUsername());
                aceNode.setProperty(NodeConstants.ECM_PERMISSION.rawname, ace.getPermission());
                aceNode.setProperty(NodeConstants.ECM_TYPE.rawname, ace.isGranted());
            }
        } catch (RepositoryException e) {
            throw new SecurityException("Failed to write ACL", e);
        }
    }

    private static Node getACPNode(Node docNode, boolean create)
            throws SecurityException {
        try {
            String name = NodeConstants.ECM_ACP.rawname;
            if (docNode.hasNode(name)) {
                return docNode.getNode(NodeConstants.ECM_ACP.rawname);
            } else if (create) {
                return docNode.addNode(NodeConstants.ECM_ACP.rawname,
                        NodeConstants.ECM_NT_ACP.rawname);
            } else {
                return null;
            }
        } catch (RepositoryException e) {
            throw new SecurityException("cannot get ACP", e);
        }
    }

    public static void setACL(Document doc, ACL acl, String beforeAcl)
            throws DocumentException, RepositoryException {
        if (acl == null) {
            return;
        }
        Node docNode = ((JCRDocument) doc).getNode();
        Node acpNode = getACPNode(docNode, true);
        Node aclNode = getACLNode(acpNode, acl.getName(), false);
        if (aclNode != null) {
            aclNode.remove();
        }
        writeACL(acpNode, acl);
        if (beforeAcl != null) {
            acpNode.orderBefore(acl.getName(), beforeAcl);
        }
    }

    public static ACL getACL(Document doc, String name) throws DocumentException {
        Node docNode = ((JCRDocument) doc).getNode();
        Node acpNode = getACPNode(docNode, false);
        if (acpNode == null) {
            return null;
        }
        Node aclNode = getACLNode(acpNode, name, false);
        ACL acl = new ACLImpl(name);
        collectACEs(acl, aclNode);
        return acl;
    }

    private static Node getACLNode(Node acpNode, String acl, boolean create)
            throws DocumentException {
        try {
            if (acpNode.hasNode(acl)) {
                return acpNode.getNode(acl);
            } else if (create) {
                return acpNode.addNode(acl,
                        NodeConstants.ECM_NT_ACL.rawname);
            } else {
                return null;
            }
        } catch (RepositoryException e) {
            throw new DocumentException("cannot get ACP", e);
        }
    }

    public static boolean hasACL(Document doc, String acl) throws DocumentException {
        Node docNode = ((JCRDocument) doc).getNode();
        Node acpNode = getACPNode(docNode, false);
        if (acpNode == null) {
            return false;
        }
        try {
            return acpNode.hasNode(acl);
        } catch (RepositoryException e) {
            throw new DocumentException("Failed to check acl node", e);
        }
    }

    public static void removeACP(Document doc) throws SecurityException {
        Node docNode = ((JCRDocument) doc).getNode();
        Node node = getACPNode(docNode, false);
        if (node != null) {
            try {
                node.remove();
            } catch (RepositoryException e) {
                throw new SecurityException("Failed to remove acp node", e);
            }
        }
    }

    public static void removeACL(Document doc, String name) throws DocumentException {
        Node docNode = ((JCRDocument) doc).getNode();
        Node acpNode = getACPNode(docNode, false);
        if (acpNode == null) {
            return;
        }
        Node aclNode = getACLNode(acpNode, name, false);
        if (aclNode != null) {
            try {
                aclNode.remove();
            } catch (RepositoryException e) {
                throw new DocumentException("Failed to remove acl node", e);
            }
        }
    }

    public ACL getInheritedACLs(Document doc) throws DocumentException {
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

    // ------------------------- check

//    public boolean checkPermission(String permission, String principal)
//            throws DocumentException {
//        // TODO Need to implement
//        // The basic security operation is to check if a given principal U is
//        // allowed to do a certain operation protected by P on a document. Note
//        // that here, P is never a group of permissions but always an basic one.
//        //
//        // The following algorithm is used:
//        //
//        // 1. collect the (ordered) list of ACPs that apply to the document:
//        //
//        // a. start with the ACP on the document itself, if there is one,
//        //
//        // b. continue upwards by finding the closest parent having an ACP, and
//        // addi the ACP after those already collected, this up until the root
//        // of the hierarchy,
//        //
//        // 2. set the `Owner` pseudo-principal to the owners of the first ACP
//        // (there is no inheritance of owners),
//        //
//        // 3. get all the ordered ACLs from the ACPs. If an ACL is a reference,
//        // use
//        // the pointed ACL instead,
//        //
//        // 4. get all the ordered ACEs from the ACLs,
//        //
//        // 5. check each ACE in order:
//        //
//        // a. if the ACE is a DENY for a principal implying U and a permission
//        // implying P, then deny the operation,
//        //
//        // b. if the ACE is a GRANT for a principal implying U and a permission
//        // implying P, then allow the operation,
//        //
//        // c. if all ACEs of all ACLs of all ACPs have been checked
//        // inconclusively, then deny the operation.
//
//        ArrayList<ACP> ACPs = new ArrayList<ACP>();
//        boolean granted = false;
//        ArrayList<String> owners = new ArrayList<String>();
//        List<ACL> ACLs;
//        ArrayList<ACE> ACEs = new ArrayList<ACE>();
//
//        // 1. a. - 1. b.
//        // throws DocumentException if problems occurs during ACP retrieval
//        // (may be document was removed or corrupted)
//        ACPs.addAll(retrieveApplicableACPs());
//
//        if (0 != ACPs.size()) {
//            // 2.
//            owners.addAll(ACPs.get(0).getOwners());
//            // 3.
//            ACLs = retrieveACLs(ACPs);
//            // 4.
//            ACEs.addAll(retrieveACEs(ACLs));
//            // 5.
//            granted = checkACEs(ACEs, permission, principal);
//        } else {
//            granted = true;
//        }
//
//        return granted;
//    }
//
//    /**
//     * Returns all the ACPs associated with the current document or its parents
//     * until root is met.
//     *
//     * @return the list of ACPs that hold the permissions associated with the
//     *         current core object and its parents.
//     */
//    public List<ACP> retrieveApplicableACPs() throws DocumentException {
//        ArrayList<ACP> acps = new ArrayList<ACP>();
//
//        // 1. a.
//        if (null != getACP()) {
//            acps.add(getACP());
//        }
//
//        // 1. b.
//
//        // get the ACPs from all the parent until the root node is met
//        try {
//            Document node = this.getParent();
//            ACP acp;
//
//            while (null != node) {
//                acp = node.getACP();
//
//                if (null != acp) {
//                    acps.add(acp);
//                }
//
//                node = node.getParent();
//            }
//        } catch (DocumentException e) {
//            //log.error("Exception appeared when traversing the object tree. Stopping the navigation.");
//        }
//
//        //log.debug("retrieveApplicableACPs() returned size = " + acps.size());
//
//        return acps;
//    }
//
//    /**
//     * Retrieves the ordered list of ACLs from the specified ACPs.
//     *
//     * @param acps
//     *            the list of ACPs
//     * @return the list of ACLs
//     */
//    public List<ACL> retrieveACLs(List<ACP> acps) {
//        ArrayList<ACL> acls = new ArrayList<ACL>();
//
//        // 3.
//        for (ACP acp : acps) {
//            acls.addAll(acp.getACLs());
//        }
//
//        return acls;
//    }
//
//    /**
//     * Returns the list of ACEs given a list of ACLs.
//     *
//     * @param acls
//     *            the list of ACLs
//     * @return the list of ACEs
//     */
//    public List<ACE> retrieveACEs(List<ACL> acls) {
//        ArrayList<ACE> aces = new ArrayList<ACE>();
//
//        // 4.
//        for (ACL acl : acls) {
//            aces.addAll(acl.getACEs());
//        }
//
//        return aces;
//    }
//
//    /**
//     * Checks whether the specified permission is GRANTED or DENIED for the
//     * specified user given the list of ACEs. Throws a DocumentAccessException
//     * if an ACE specifically DENIES the user the specified permission. Returns
//     * false if no ACE specifically DENIES or GRANTS the current user the
//     * current permission. Returns true if and only if there is an ACE that
//     * specifically GRANTS the passed user the passed permission.
//     *
//     * <p>
//     * The ACEs are evaluated in an ordered fashion. If a DENY is met, then an
//     * exception is thrown.
//     *
//     * @param aces
//     *            the list of ACEs that hold the permissions associated with the
//     *            current core object
//     * @param permission
//     *            the permission specified on te business method
//     * @param principal
//     *            the passed username that needs to be checked
//     * @return false if no ACEs specifically grant or deny the specified user
//     *         the specified permission
//     * @throws DocumentAccessException
//     *             it is thrown if one of the associated ACEs specifically DENY
//     *             the passed with the specified permission
//     */
//    protected boolean checkACEs(List<ACE> aces, String permission,
//            String principal) throws DocumentAccessException {
//        boolean allowed = false;
//
//        // 5.
//        for (ACE ace : aces) {
//            if (checkACE(ace, permission, principal)) {
//                allowed = true;
//
//                break;
//            }
//        }
//
//        return allowed;
//    }
//
//    /**
//     * Returns true if this ACE defines a GRANT on this permission with this
//     * user.
//     *
//     * @param ace
//     *            Holds the permission on the core object
//     * @param permission
//     *            the permission that must be checked
//     * @param principal
//     *            the username that accesses the core object
//     * @return false if the ACE defines no grant or deny for the specified user
//     *         and the specified permission
//     * @throws DocumentAccessException
//     *             when the ACE specifically DENIES the acces right to the user
//     *             for the specified permission
//     */
//    protected boolean checkACE(ACE ace, String permission, String principal)
//            throws DocumentAccessException {
//        boolean allowed = false;
//
//        if (ace.getType().equals(ACEType.GRANT)
//                && ace.containsPermission(permission)
//                && ace.containsPrincipal(principal)) {
//            allowed = true;
//        } else {
//            if (ace.getType().equals(ACEType.DENY)
//                    && ace.containsPermission(permission)
//                    && ace.containsPrincipal(principal)) {
//                throw new DocumentAccessException(principal, permission);
//            }
//        }
//
//        return allowed;
//    }


}
