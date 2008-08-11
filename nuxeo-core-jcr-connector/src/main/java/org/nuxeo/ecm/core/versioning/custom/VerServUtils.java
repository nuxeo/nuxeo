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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.core.versioning.custom;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.OnParentVersionAction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.core.PropertyImpl;
import org.apache.jackrabbit.name.QName;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.repository.jcr.InternalSessionOperationsProxy;
import org.nuxeo.ecm.core.repository.jcr.JCRDocument;
import org.nuxeo.ecm.core.repository.jcr.JCRHelper;
import org.nuxeo.ecm.core.repository.jcr.JCRSession;
import org.nuxeo.ecm.core.repository.jcr.NodeConstants;
import org.nuxeo.ecm.core.versioning.DocumentVersion;

/**
 * Utility class used by <code>CustomVersioningService</code>. This contains methods to
 * easy construction of a Document version by embedding several storage peculiarities
 * of a version node.
 *
 * @author DM
 */
final class VerServUtils {

    public static final String START_VERSION_NODE = "root";

    public static final String VERSION_NODE_NAME_PREFIX = "version";

    /**
     * Logger instance.
     */
    private static final Log log = LogFactory.getLog(VerServUtils.class);

    // This is a utility class.
    private VerServUtils() { }

    /**
     * Checks if the versionStorage root node exists, and if not creates it.
     * The node location is: //versionStorage/
     *
     * @param session
     * @throws RepositoryException
     */
    private static Node getOrCreateVersionHistoryStorage(Session session)
            throws RepositoryException {
        final String logPrefix = "<getVersionHistoryStorage> ";

        final String vhStorageNodeName = NodeConstants.ECM_VERSION_STORAGE.name;

        log.debug(logPrefix + "Creating ecm:versionStorage node. ["
                + vhStorageNodeName + ']');

        final Node root = session.getRootNode();

        return getOrCreateNode(root, vhStorageNodeName, NodeConstants.ECM_VERSION_STORAGE.rawname);
    }

    /**
     * Checks if the VersionHistory exists, and if not create it, and sets
     * ecm:versionHistory.
     *
     * @param forNode
     * @throws RepositoryException
     */
    private static Node getOrCreateVersionHistoryNode(Node forNode)
            throws RepositoryException {
        final String logPrefix = "<getVersionHistoryNode> ";

        final Node storageNode = getOrCreateVersionHistoryStorage(forNode.getSession());

        final String uuid = forNode.getUUID();
        final Node vhParentNode = getOrCreateVHParent(storageNode, uuid);

        // final String versionHistoryNodeName =
        // NodeConstants.ECM_VERSION_HISTORY.name;

        log.debug(logPrefix + "Creating ecm:versionHistory node. " + '[' + uuid
                + ']');

        return getOrCreateNode(vhParentNode, uuid,
                NodeConstants.ECM_VERSION_HISTORY.rawname);
    }

    private static Node getOrCreateVHParent(Node storageNode, String uuid)
            throws RepositoryException {

        //final String logPrefix = "<getVHParent> ";

        final String level_1_parentPath = uuid.substring(0, 2);
        final String level_2_parentPath = uuid.substring(2, 4);
        final String level_3_parentPath = uuid.substring(4, 6);

        Node level_1 = getOrCreateNode(storageNode, level_1_parentPath,
                NodeConstants.ECM_VERSION_STORAGE.rawname);
        Node level_2 = getOrCreateNode(level_1, level_2_parentPath,
                NodeConstants.ECM_VERSION_STORAGE.rawname);
        Node level_3 = getOrCreateNode(level_2, level_3_parentPath,
                NodeConstants.ECM_VERSION_STORAGE.rawname);

        return level_3;
    }

    /**
     * Creates a JCRDocument frozen copy within nodes defining a version with the
     * given label and description.
     *
     * @param jdoc
     * @param description
     * @param label
     * @return
     * @throws DocumentException
     * @throws RepositoryException
     */
    public static DocumentVersion createVersion(JCRDocument jdoc, String label, String description)
            throws RepositoryException, DocumentException {

        final String logPrefix = "<createVersion> ";

        final Node docNode = jdoc.getNode();

        // check node mixin
        checkVersionable(docNode);

        // log node props:
        //printProperties(docNode);

        final Node predecessorNode;

        // check if we have a versionHistory node already
        // if not we create it
        final Node versionHistoryNode;
        if (docNode.hasProperty(NodeConstants.ECM_VERSION_HISTORY.rawname)) {
            versionHistoryNode = docNode.getProperty(
                    NodeConstants.ECM_VERSION_HISTORY.rawname).getNode();

            predecessorNode = getLastVersionNode(versionHistoryNode);
        } else {
            versionHistoryNode = getOrCreateVersionHistoryNode(docNode);
            // add first version = root version
            final Node rootVersionNode = getOrCreateNode(versionHistoryNode,
                    START_VERSION_NODE, NodeConstants.ECM_VERSION.rawname);

            predecessorNode = rootVersionNode;

            log.debug(logPrefix + "init versionHistory: " + NodeConstants.ECM_VERSION_HISTORY.rawname);
            docNode.setProperty(NodeConstants.ECM_VERSION_HISTORY.rawname,
                    versionHistoryNode);
        }

        // generate version node name
        final String versionNodeName = generateVersionName(versionHistoryNode);

        //
        // create version node
        //
        final Node versionNode = getOrCreateNode(versionHistoryNode,
                versionNodeName, NodeConstants.ECM_VERSION.rawname);

        // set label and description
        versionNode.setProperty(NodeConstants.ECM_VERSION_CREATEDATE.rawname, new GregorianCalendar());
        versionNode.setProperty(NodeConstants.ECM_VERSION_LABEL.rawname, label);
        versionNode.setProperty(NodeConstants.ECM_VERSION_DESCRIPTION.rawname,
                description);

        // add element to list
        //                               startVNode --- successor --->.....previousVNode ----->lastVNode
        //                               startVNode <--- predecessor ---.....previousVNode <-----lastVNode
        // lastVNode <--- successor ---- startVNode
        versionNode.setProperty(NodeConstants.ECM_VERSION_PREDECESSOR.rawname, predecessorNode);
        predecessorNode.setProperty(NodeConstants.ECM_VERSION_SUCCESSOR.rawname, versionNode);

        Node startNode = versionHistoryNode.getNode(START_VERSION_NODE);
        startNode.setProperty(NodeConstants.ECM_VERSION_PREDECESSOR.rawname, versionNode);

        // --------- using JCR copy --------
        // be sure all node from the version path are saved (and avoid to save the entire session for optimization)
        JCRHelper.saveNode(versionNode);
        // make a copy of the current node
        final Node copyNode = copyNode((JCRSession) jdoc.getSession(), jdoc.getNode(), versionNode);
        copyNode.setProperty(NodeConstants.ECM_FROZEN_NODE_UUID.rawname, docNode.getUUID());
        // NXP-754
        copyNode.save();
        // ----------------------------------------
        // --------- using custom copy -------
//        final Node copyNode = JCRHelper.copy(jdoc.getNode(), versionNode, jdoc.getName()+"_"+System.currentTimeMillis());
//        copyNode.setProperty(NodeConstants.ECM_FROZEN_NODE_UUID.rawname, docNode.getUUID());
//        // NXP-754
//        JCRHelper.saveNode(versionNode);
        // -----------------------------------------

        log.debug(logPrefix + "copy created: " + copyNode.getPath());
        //log.debug(logPrefix + "node props: " + printProperties(copyNode));

        // create a CustomDocumentVersion
        final CustomDocumentVersion docVersion = new CustomDocumentVersion(
                (JCRSession) jdoc.getSession(), versionNode);

        return docVersion;
    }


    /**
     *
     * @param docNode
     * @throws DocumentException if the given node doesn't have ecm:versionable mixin
     */
    private static void checkVersionable(Node docNode) throws DocumentException {
        final boolean isVersionable;
        try {
            isVersionable = docNode.isNodeType(NodeConstants.ECM_VERSIONABLE_MIXIN.rawname);
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new DocumentException("error checking doc type", e);
        }

        if (!isVersionable) {
            String path = null;
            try {
                path = docNode.getPath();
            } catch (RepositoryException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            throw new DocumentException("not versionable, node path: " + path);
        }
    }

    /**
     * Removes frozen nodes, version nodes and version history node for the given
     * Document.
     *
     * @param jdoc
     * @throws RepositoryException
     */
    static void removeVersionHistory(JCRDocument jdoc) throws RepositoryException {
        final String logPrefix = "<removeVersionHistory> ";
        final Node docNode = jdoc.getNode();
        final Node versionHistoryNode;
        if (docNode.hasProperty(NodeConstants.ECM_VERSION_HISTORY.rawname)) {
            versionHistoryNode = docNode.getProperty(
                    NodeConstants.ECM_VERSION_HISTORY.rawname).getNode();
        } else {
            // given document has no version history
            if (log.isDebugEnabled()) {
                log.debug(logPrefix + "Document " + docNode.getUUID()
                        + " has no version history.");
            }
            return;
        }

        versionHistoryNode.remove();
    }

    /**
     * Removes the version (snapshot) having the specified label of the given
     * doc.
     *
     * @param jdoc the source document that is supposed to have the specified
     *            version
     * @param label the label of the snapshot
     * @throws RepositoryException
     */
    static void removeVersion(JCRDocument jdoc, String label)
            throws RepositoryException {

        Node docNode = jdoc.getNode();

        log.debug("remove version with label " + label + " for doc "
                + docNode.getName());

        final Node versionHistoryNode;
        if (docNode.hasProperty(NodeConstants.ECM_VERSION_HISTORY.rawname)) {
            versionHistoryNode = docNode.getProperty(
                    NodeConstants.ECM_VERSION_HISTORY.rawname).getNode();
        } else {
            throw new RepositoryException("no version history");
        }
        // final Node versionHistoryNode =
        // getOrCreateVersionHistoryNode(docNode);

        Node startNode = versionHistoryNode.getNode(START_VERSION_NODE);
        Node versionNode = getVersionNodeWithLabel(versionHistoryNode, label);

        Node predecessor = getVersNodePredecessor(versionNode);
        Node successor = getVersNodeSuccessor(versionNode);
        if (predecessor != null) {
            if (successor != null) {
                predecessor.setProperty(
                        NodeConstants.ECM_VERSION_SUCCESSOR.rawname, successor);
                successor.setProperty(
                        NodeConstants.ECM_VERSION_PREDECESSOR.rawname,
                        predecessor);
            } else {
                // predecessor becomes the first node after startNode
                predecessor.setProperty(
                        NodeConstants.ECM_VERSION_SUCCESSOR.rawname, startNode);
                startNode.setProperty(
                        NodeConstants.ECM_VERSION_PREDECESSOR.rawname,
                        predecessor);
            }
        } else {
            if (successor != null) {
                // this was the first version (last in the list)
                successor.setProperty(
                        NodeConstants.ECM_VERSION_PREDECESSOR.rawname,
                        startNode);
                startNode.setProperty(
                        NodeConstants.ECM_VERSION_SUCCESSOR.rawname,
                        successor);
            } else {
                // no version remaining
                log.debug("no version remaining");
                startNode.setProperty(
                        NodeConstants.ECM_VERSION_PREDECESSOR.rawname,
                        startNode);
                startNode.setProperty(
                        NodeConstants.ECM_VERSION_SUCCESSOR.rawname,
                        startNode);
            }
        }

        log.debug("removing version node: " + versionNode.getName());

        versionNode.remove();

        log.debug("version removed");
    }

    /**
     * Retrieves last added version node.
     *
     * @param versionHistoryNode
     * @return
     * @throws RepositoryException
     */
    static Node getLastVersionNode(Node versionHistoryNode)
            throws RepositoryException {
        Node rootVersionNode = versionHistoryNode.getNode(START_VERSION_NODE);

        // previous element for root is the last element in list???
        Node lastVersionNode = rootVersionNode.getProperty(
                NodeConstants.ECM_VERSION_PREDECESSOR.rawname).getNode();

        return lastVersionNode;
    }

    private static Node getVersionNodeWithLabel(Node versionHistoryNode,
            String label) throws RepositoryException {
        final NodeIterator nit = versionHistoryNode.getNodes();
        while (nit.hasNext()) {
            final Node child = nit.nextNode();
            //final String childLabel = child.getProperty(
            //        NodeConstants.ECM_VERSION_LABEL.rawname).getString();
            String childLabel = null;
            try {
                if (child
                        .hasProperty(NodeConstants.ECM_VERSION_LABEL.rawname)) {
                    childLabel = child.getProperty(
                            NodeConstants.ECM_VERSION_LABEL.rawname).getString();
                }
            } catch (RepositoryException e) {
                //throw new DocumentException("cannot get version label", e);
                // maybe it's the root.. go to the next
                continue;
            }
            if (label.equals(childLabel)) {
                return child;
            }
        }
        return null;
    }

    private static synchronized String generateVersionName(Node versionHistoryNode)
            throws RepositoryException {
        final String propName = NodeConstants.ECM_VERSION_ID.rawname;

        final long id;
        if (versionHistoryNode.hasProperty(propName)) {
            long prevId = versionHistoryNode.getProperty(propName).getLong();
            id = prevId + 1;
        } else {
            // starting at 1...
            id = 1;
        }
        versionHistoryNode.setProperty(propName, id);

        final String versionNodeName = VERSION_NODE_NAME_PREFIX + id;

        return versionNodeName;
    }

    public static Node copyNode(JCRSession session, Node scrNode, Node dstParent)
            throws DocumentException {
        final String logPrefix = "<copyNode> ";

        // be sure all node from the version path are saved (and avoid to save the entire session for optimization)
        // otherwise jcr copy will fail
        JCRHelper.saveNode(dstParent);

        try {
            String srcName = scrNode.getUUID();
            String srcPath = scrNode.getPath();
            String dstPath = dstParent.getPath() + "/" + srcName;

            if (srcPath.equals(dstPath)) {
                throw new DocumentException("BAD PATH");
            }

            //log.debug("nodes: " + printNodes(dstParent.getNodes()));

            InternalSessionOperationsProxy.copy(session, srcPath, dstPath);

            final Node copyNode = dstParent.getNode(srcName);

            return copyNode;
        } catch (RepositoryException e) {
            log.error(logPrefix + "err: " + e.getMessage());
            throw new DocumentException("Could not copy the document", e);
        }
    }

    /**
     * Checks the node mixin property ecm:isCheckedOut.
     *
     * @param docNode
     * @return
     * @throws RepositoryException
     */
    static boolean isCheckedOut(Node docNode) throws RepositoryException {
        final String propname = NodeConstants.ECM_VER_ISCHECKEDOUT.rawname;
        if (docNode.hasProperty(propname)) {
            return docNode.getProperty(propname).getBoolean();
        }

        return docNode.setProperty(propname, true).getBoolean();
    }

    static void setCheckedOut(Node docNode, boolean checkedout)
            throws RepositoryException {
        final String propname = NodeConstants.ECM_VER_ISCHECKEDOUT.rawname;
        docNode.setProperty(propname, checkedout);
    }

    // ---------------------------------------------------------------------
    // utility methods
    //
    private static Node getOrCreateNode(Node parent, String path, String type)
            throws RepositoryException {
        final String logPrefix = "<getOrCreateNode> ";
        Node child;
        try {
            child = parent.getNode(path);
        } catch (PathNotFoundException e1) {
            // TODO Auto-generated catch block
            // e1.printStackTrace();
            log.debug(logPrefix + "create child: " + path + " for: "
                    + parent.getPath() + "; type: " + type);
            child = parent.addNode(path, type);
        }
        return child;
    }

    /**
     * Utility method to retrieve nodes names.
     *
     * @param nodes
     * @return
     * @throws RepositoryException
     */
    static String printNodes(NodeIterator nodes)
            throws RepositoryException {
        final StringBuilder buf = new StringBuilder();
        while (nodes.hasNext()) {
            Node node = (Node) nodes.next();
            buf.append(node.getName());
            buf.append(", ");
        }

        return buf.toString();
    }

    static void printProperties(Node node) {
        try {
            log.debug("node props: " + getPropsAsString(node));
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Iterates through node properties and construct a readable string with
     * prop-value pairs.
     *
     * @param node
     * @return
     * @throws RepositoryException
     */
    private static String getPropsAsString(Node node) throws RepositoryException {
        final StringBuilder buf = new StringBuilder();
        final PropertyIterator pi = node.getProperties();

        while (pi.hasNext()) {
            Property p = (Property) pi.next();
            buf.append(p.getName());
            try {
                buf.append('=').append(p.getValue().getString());
            } catch (ValueFormatException e) {
                buf.append("={");
                final Value[] values = p.getValues();
                for (Value value : values) {
                    buf.append(value.getString());
                    buf.append(',');
                }
                buf.append('}');
            }
            buf.append(", ");
        }

        return buf.toString();
    }

    /**
     * Restores the liveNode with informations from versionNode.
     *
     * @param liveNode
     * @param versionNode
     * @throws RepositoryException
     */
    public static void restore(Node liveNode, Node versionNode) throws RepositoryException {
        final String logPrefix = "<restore(N,N)> ";
        // copy all relevant properties from the Version

        if (!versionNode.hasNodes()) {
            log.error(logPrefix + "version node is empty: " + versionNode.getPath());
        }

        final Node frozenNode = versionNode.getNodes().nextNode();
        log.debug(logPrefix + "frozenNode: " + frozenNode.getPath());

        restoreNodeProps(frozenNode, liveNode, true);
    }

    static void restoreNodeProps(Node srcNode, Node destNode, boolean isMaster)
            throws RepositoryException {
        // copy frozen properties
        final Set<String> restoredProps = copyProps(srcNode, destNode);

        if (isMaster) {
            // reset our special fields
            destNode.setProperty(NodeConstants.ECM_FROZEN_NODE_UUID.rawname, (Value) null);
        }

        // remove properties that do not exist in the frozen representation
        removeProps(restoredProps, destNode);

        // add 'auto-create' properties that do not exist yet
        // XXX TODO

        // first delete all non frozen version histories (i.e. all OPV=Copy)
        // todo

        // restore the frozen nodes
        restoreChildNodes(srcNode, destNode);
    }

    private static Set<String> copyProps(Node srcNode, Node destNode)
            throws RepositoryException {
        final String logPrefix = "<copyProps> ";
        final PropertyIterator pit = srcNode.getProperties();

        Set<String> propNames = new HashSet<String>();
        while (pit.hasNext()) {
            final Property prop = pit.nextProperty();

            String name = prop.getName();
            propNames.add(name);
            if (name.equals("jcr:uuid") || name.equals("jcr:primaryType")) {
                // protected, cannot be written
                continue;
            }

            try {
                destNode.setProperty(name, prop.getValue());
            } catch (ConstraintViolationException e) {
                log.error(logPrefix + "err: " + e.getMessage());
            } catch (ValueFormatException e) {
                // could be prop.isMultiValued() : internal jackrabbit
                // won't log this - only err3 will matter
                // log.error(logPrefix + "err2: " + e.getMessage());

                try {
                    destNode.setProperty(name, prop.getValues());
                } catch (ConstraintViolationException e3) {
                    log.error(logPrefix + "err3: " + e3.getMessage());
                }
            }
        }

        return propNames;
    }

    /**
     * Removes properties from the given node which do not exist in the given set.
     *
     * @param keepPropNames
     * @param destNode
     * @throws RepositoryException
     */
    private static void removeProps(Set<String> keepPropNames, Node destNode)
            throws RepositoryException {
        PropertyIterator piter = destNode.getProperties();
        while (piter.hasNext()) {
            PropertyImpl prop = (PropertyImpl) piter.nextProperty();
            // ignore some props that are not well guarded by the OPV
            if (prop.getQName().equals(QName.JCR_VERSIONHISTORY)) {
                continue;
            } else if (prop.getQName().equals(QName.JCR_PREDECESSORS)) {
                continue;
            }
            if (prop.getDefinition().getOnParentVersion() == OnParentVersionAction.COPY
                    || prop.getDefinition().getOnParentVersion() == OnParentVersionAction.VERSION) {
                if (!keepPropNames.contains(prop.getName())) {
                    //removeChildProperty
                    destNode.setProperty(prop.getName(), (Value) null);
                }
            }
        }
    }

    private static void restoreChildNodes(Node srcNode, Node destNode)
            throws RepositoryException {

        // remove all child nodes in destnode
        final NodeIterator destNit = destNode.getNodes();
        while (destNit.hasNext()) {
            destNit.nextNode().remove();
        }

        final NodeIterator nit = srcNode.getNodes();
        while (nit.hasNext()) {
            final Node n = nit.nextNode();

            final String nodeTypeName = n.getPrimaryNodeType().getName();

            final Node newChildNode;
            try {
                newChildNode = destNode.addNode(n.getName(), nodeTypeName);
            } catch (RepositoryException e) {
                log.error("Cannot add child '" + n.getName() + "' of type "
                        + nodeTypeName + " for node: " + destNode.getPath());
                throw e;
            }

            try {
                restoreNodeProps(n, newChildNode, false);
            } catch (RepositoryException e) {
                log.error("Unable to restore properties (" + n.getName()
                        + ") at node: " + newChildNode.getPath()
                        + "; source node: " + n.getPath());
                throw e;
            }
        }
    }

    static List<Node> getPredecessors(Node versionNode)
            throws RepositoryException {
        // TODO check it is a version node
        final List<Node> predecessors = new ArrayList<Node>();

        while (versionNode != null) {
            final Property prop = versionNode
                    .getProperty(NodeConstants.ECM_VERSION_PREDECESSOR.rawname);
            if (prop != null) {
                final Node predecessor = prop.getNode();
                if (START_VERSION_NODE.equals(predecessor.getName())) {
                    break;
                } else {
                    predecessors.add(predecessor);
                    versionNode = predecessor;
                }
            } else {
                break;
            }
        }

        return predecessors;
    }

    static List<Node> getSuccessors(Node versionNode) throws RepositoryException {
        // TODO check it is a version node
        final List<Node> successors = new ArrayList<Node>();

        while (versionNode != null) {
            final Property prop;
            try {
                prop = versionNode
                        .getProperty(NodeConstants.ECM_VERSION_SUCCESSOR.rawname);
            } catch (PathNotFoundException e) {
                // no successor : end of line
                break;
            }
            if (prop != null) {
                final Node successor = prop.getNode();
                if (START_VERSION_NODE.equals(successor.getName())) {
                    break;
                } else {
                    successors.add(successor);
                    versionNode = successor;
                }
            } else {
                break;
            }
        }

        return successors;
    }

    static Node getVersNodeSuccessor(Node versionNode) throws RepositoryException {
        final Property prop;
        try {
            prop = versionNode
                    .getProperty(NodeConstants.ECM_VERSION_SUCCESSOR.rawname);
        } catch (PathNotFoundException e) {
            // no successor : end of line
            return null;
        }
        if (prop != null) {
            final Node successor = prop.getNode();
            if (START_VERSION_NODE.equals(successor.getName())) {
                return null;
            } else {
                return successor;
            }
        } else {
            return null;
        }
    }

    static Node getVersNodePredecessor(Node versionNode)
            throws RepositoryException {
        final Property prop = versionNode.getProperty(
                NodeConstants.ECM_VERSION_PREDECESSOR.rawname);
        if (prop != null) {
            final Node predecessor = prop.getNode();
            if (START_VERSION_NODE.equals(predecessor.getName())) {
                return null;
            } else {
                return predecessor;
            }
        } else {
            return null;
        }
    }

}
