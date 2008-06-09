/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.versioning.custom;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.version.VersionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.repository.jcr.versioning.VersioningService;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.repository.jcr.JCRDocument;
import org.nuxeo.ecm.core.repository.jcr.JCRName;
import org.nuxeo.ecm.core.repository.jcr.JCRQueryXPath;
import org.nuxeo.ecm.core.repository.jcr.JCRSession;
import org.nuxeo.ecm.core.repository.jcr.NodeConstants;
import org.nuxeo.ecm.core.versioning.DocumentVersion;
import org.nuxeo.ecm.core.versioning.DocumentVersionIterator;

/**
 * Implementation of the {@link org.nuxeo.ecm.core.versioning.VersioningService}
 * interface.
 * <p>
 * It delegates most of the calls to VerServUtils functions.
 *
 * @author Dragos Mihalache
 * @author Florent Guillaume
 */
public class CustomVersioningService implements VersioningService {

    private static final Log log = LogFactory.getLog(CustomVersioningService.class);

    /**
     * Creates a new Version under the VersionHistory. This Version is a copy
     * (using the JCR Workspace.copy API) of the original document (this is
     * simpler than the JCR schema where there is a small Version node that
     * itself holds a frozen node).
     *
     * @throws DocumentException if ecm:isCheckedOut is false
     */
    public void checkin(Document doc, String label) throws DocumentException {
        checkin(doc, label, null);
    }

    private boolean hasPendingChangesSafe(Node docNode)
            throws DocumentException {

        try {
            return hasPendingChanges(docNode);
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();

            throw new DocumentException("Failed to check document state ");
        }
    }

    private static boolean hasPendingChanges(Node docNode)
            throws RepositoryException {
        boolean dirty = docNode.isNew() || docNode.isModified();
        if (dirty) {
            return true;
        }
        final NodeIterator nodes = docNode.getNodes();
        while (nodes.hasNext()) {
            Node childNode = (Node) nodes.next();
            if (hasPendingChanges(childNode)) {
                return true;
            }
        }

        return false;
    }

    public void checkin(Document doc, String label, String description)
            throws DocumentException {
        final String logPrefix = "<checkin> ";

        JCRDocument jdoc = (JCRDocument) doc;

        // check if versionable
        try {
            checkVersionable(jdoc);
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();

            throw new DocumentException("Failed to checkin document " +
                    doc.getName() + " : " + label);
        }

        final Node docNode = jdoc.getNode();

        if (hasPendingChangesSafe(docNode)) {
            throw new DocumentException("Failed to checkin document " +
                    doc.getName() + " : " + label +
                    ". Document changes pending.");
        }

        // jdoc.getSession().getRepository().getTypeManager().get

        try {
            // step 1: fail if ecm:isCheckedOut is false
            if (!VerServUtils.isCheckedOut(docNode)) {
                throw new DocumentException("not checked out");
            }

            // step 2: create a new Version under the VersionHistory
            DocumentVersion version = VerServUtils.createVersion(jdoc, label,
                    description);

            // step 3: set ecm:isCheckedOut to false on the base document
            docNode.setProperty(NodeConstants.ECM_VER_ISCHECKEDOUT.rawname,
                    false);

            // step 4: set ecm:baseVersion to the new version
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();

            throw new DocumentException("Failed to checkin document " +
                    doc.getName() + " : " + label, e);
        }

        // VersionableDocument
        // getVersionHistoryStorage(jdoc.getSession());

        // Node node = jdoc.getNode();
        /*
         * try { Version version = node.checkin(); if (label != null) {
         * node.getVersionHistory().addVersionLabel(version.getName(), label,
         * false); } } catch (RepositoryException e) { throw new
         * DocumentException("Failed to checkin document "+doc.getName()+" :
         * "+label); }
         */

        // check
    }

    /**
     * The checkout operation will do the following:
     * <p>
     * <ul>
     * <li>fail if ecm:isCheckedOut is not false,
     * <li>set ecm:isCheckedOut to true.
     * </ul>
     */
    public void checkout(Document doc) throws DocumentException {
        final JCRDocument jdoc = (JCRDocument) doc;

        final Node docNode = jdoc.getNode();
        try {
            if (VerServUtils.isCheckedOut(docNode)) {
                throw new DocumentException("document " + doc.getName() +
                        " is not checked in");
            }
            docNode.setProperty(NodeConstants.ECM_VER_ISCHECKEDOUT.rawname,
                    true);
        } catch (RepositoryException e) {
            // e.printStackTrace();
            throw new DocumentException("Failed to checkout document " +
                    doc.getName());
        }
    }

    public List<String> getVersionsIds(Document doc) throws DocumentException {
        JCRDocument jdoc = (JCRDocument) doc;
        Node node = jdoc.getNode();
        try {
            if (!node.hasProperty(NodeConstants.ECM_VERSION_HISTORY.rawname)) {
                return Collections.emptyList();
            }
            Node versionHistory;
            versionHistory = node.getProperty(
                    NodeConstants.ECM_VERSION_HISTORY.rawname).getNode();
            NodeIterator it = versionHistory.getNodes();
            List<String> ids = new ArrayList<String>((int) it.getSize() - 1);
            it.nextNode(); // skip placeholder root version
            while (it.hasNext()) {
                Node versionNode = it.nextNode();
                NodeIterator vc = versionNode.getNodes();
                Node frozenNode = vc.nextNode();
                ids.add(frozenNode.getUUID());
            }
            return ids;
        } catch (RepositoryException e) {
            throw new DocumentException(e);
        }
    }

    public DocumentVersion getLastVersion(Document doc)
            throws DocumentException {
        JCRDocument jdoc = (JCRDocument) doc;
        Node docNode = jdoc.getNode();

        try {
            final Node versionHistoryNode;
            if (docNode.hasProperty(NodeConstants.ECM_VERSION_HISTORY.rawname)) {
                versionHistoryNode = docNode.getProperty(
                        NodeConstants.ECM_VERSION_HISTORY.rawname).getNode();
            } else {
                // XXX or throw an error?
                return null;
            }
            final Node lastVersionNode = VerServUtils.getLastVersionNode(versionHistoryNode);

            return new CustomDocumentVersion(jdoc.jcrSession(), lastVersionNode);
        } catch (RepositoryException e) {
            throw new DocumentException("cannot get last version", e);
        }
    }

    public Document getVersion(Document doc, String label)
            throws DocumentException {
        // TODO Auto-generated method stub
        // XXX
        // Version version = getVersionNode(jdoc, label);
        // Node versionHistoryNode =
        // VerServUtils.getOrCreateVersionHistoryNode(jdoc.getNode());
        final DocumentVersionIterator it = getVersions(doc);
        while (it.hasNext()) {
            DocumentVersion docVer = it.next();

            final String verlabel = docVer.getLabel();
            if (verlabel != null && verlabel.equals(label)) {
                return docVer;
            }
        }
        // Node versionNode = versionHistoryNode.getNode("a");
        // return newDocumentVersion(jdoc.jcrSession(), versionNode);// version
        // .getNode("jcr:frozenNode"));
        return null;
    }

    /**
     * If versionHistory is not initialized (i.e. no checkin has been performed)
     * the return will be an iterator over an empty list.
     *
     */
    public DocumentVersionIterator getVersions(Document doc)
            throws DocumentException {

        final String logPrefix = "<getVersions> ";

        JCRDocument jdoc = (JCRDocument) doc;
        Node docNode = jdoc.getNode();

        final Node versionHistoryNode;
        try {
            if (docNode.hasProperty(NodeConstants.ECM_VERSION_HISTORY.rawname)) {
                versionHistoryNode = docNode.getProperty(
                        NodeConstants.ECM_VERSION_HISTORY.rawname).getNode();
            } else {
                final String msg = "versionHistory not initialized";
                log.debug(logPrefix + msg + " for doc: " + doc.getUUID());

                // do not throw new DocumentException(msg);
                // will return an iterator over an empty list
                return new DocumentVersionIterator() {

                    public void remove() {
                    }

                    public DocumentVersion next() {
                        return null;
                    }

                    public boolean hasNext() {
                        return false;
                    }

                    public DocumentVersion nextDocumentVersion() {
                        return null;
                    }

                };
            }
            // verHistoryNode =
            // VerServUtils.getOrCreateVersionHistoryNode(docNode);
        } catch (RepositoryException e) {
            throw new DocumentException(e);
        }
        /*
         * try { return new CustomDocumentVersionIterator(jdoc.jcrSession(),
         * jdoc .getNode().getVersionHistory().getAllVersions()); } catch
         * (UnsupportedRepositoryOperationException e) { throw new
         * DocumentException(e); } catch (RepositoryException e) { throw new
         * DocumentException(e); }
         */
        return new CustomDocumentVersionIterator(jdoc.jcrSession(),
                versionHistoryNode);
    }

    public boolean isCheckedOut(Document doc) throws DocumentException {
        JCRDocument jdoc = (JCRDocument) doc;
        try {
            return VerServUtils.isCheckedOut(jdoc.getNode());
        } catch (RepositoryException e) {
            throw new DocumentException(e);
        }
    }

    public void restore(Document doc, String label) throws DocumentException {
        JCRDocument jdoc = (JCRDocument) doc;
        try {
            final DocumentVersion version = (DocumentVersion) getVersion(doc,
                    label);

            if (null == version) {
                throw new DocumentException("no version for document uuid: " +
                        doc.getUUID() + " with label: '" + label + "'");
            }

            final Node liveNode = jdoc.getNode();

            // restore identified version
            // VerServUtils.restore(jdoc.getNode(), ((JCRDocument) version)
            // .getNode());
            VerServUtils.restoreNodeProps(((JCRDocument) version).getNode(),
                    liveNode, true);

            // As in jcr impl
            // 3. N's jcr:isCheckedOut property is set to false.
            VerServUtils.setCheckedOut(liveNode, false);

        } catch (RepositoryException e) {
            throw new DocumentException(
                    "unable to restore version with label '" + label + "'", e);
        }
    }

    public Calendar getCreated(DocumentVersion version)
            throws DocumentException {
        return version.getCreated();
    }

    public String getDescription(DocumentVersion version)
            throws DocumentException {
        return version.getDescription();
    }

    public String getLabel(DocumentVersion version) throws DocumentException {
        return version.getLabel();
    }

    public boolean isVersionNode(Node node) throws RepositoryException {
        // TODO Auto-generated method stub
        // return node.getPrimaryNodeType().getName().equals("nt:frozenNode");
        if (!node.hasProperty(NodeConstants.ECM_FROZEN_NODE_UUID.rawname)) {
            // non versionable node
            return false;
        }

        final String frozenUuid = node.getProperty(
                NodeConstants.ECM_FROZEN_NODE_UUID.rawname).getString();

        return frozenUuid != null;
    }

    public JCRDocument newDocumentVersion(JCRSession session, Node node)
            throws RepositoryException {
        // FIXME the parent node should be of type ecm:version
        Node versionNode = node.getParent();
        return new CustomDocumentVersion(session, versionNode);
    }

    /**
     * Checks if the given jdoc is versionable, i.e. has
     * 'ECM_VERSIONABLE_MIXIN'.
     *
     * @param jdoc
     *
     * @throws DocumentException if this node is not versionable
     * @throws RepositoryException
     */
    private static void checkVersionable(JCRDocument jdoc)
            throws DocumentException, RepositoryException {
        if (!jdoc.getNode().isNodeType(
                NodeConstants.ECM_VERSIONABLE_MIXIN.rawname)) {
            String msg = "Unable to perform versioning operation on non versionable node: " +
                    jdoc.getPath();
            log.debug(msg);
            throw new DocumentException(msg);
        }
    }

    public void removeVersionHistory(JCRDocument jdoc) throws DocumentException {
        try {
            VerServUtils.removeVersionHistory(jdoc);
        } catch (RepositoryException e) {
            throw new DocumentException(
                    "unable to remove version history for doc '" +
                            jdoc.getPath() + "'", e);
        }
    }

    public void removeDocumentVersion(JCRDocument doc, String versionLabel)
            throws RepositoryException {
        if (log.isDebugEnabled()) {
            try {
                log.info("remove document (" + doc.getName() +
                        ") version with label " + versionLabel);
            } catch (DocumentException e) {
                log.error(e);
            }
        }

        VerServUtils.removeVersion(doc, versionLabel);
    }

    /*
     * Make sure that after a copy we create new version histories for all
     * versionable documents.
     */
    public void fixupAfterCopy(JCRDocument doc) throws RepositoryException {
        String queryString = String.format("/jcr:root%s//element(*, %s)[%s]",
                JCRQueryXPath.quotePath(doc.getNode().getPath()),
                NodeConstants.ECM_VERSIONABLE_MIXIN.rawname,
                NodeConstants.ECM_VERSION_HISTORY.rawname);
        QueryManager queryManager = doc.jcrSession().getSession().getWorkspace().getQueryManager();
        Query query = queryManager.createQuery(queryString, Query.XPATH);
        NodeIterator nodes = query.execute().getNodes();
        while (nodes.hasNext()) {
            clearHistoryReference(nodes.nextNode());
        }
        // then doc itself (not found by XPath descendant search)
        clearHistoryReference(doc.getNode());
    }

    protected static void clearHistoryReference(Node node)
            throws RepositoryException {
        if (node.hasProperty(NodeConstants.ECM_VERSION_HISTORY.rawname)) {
            node.getProperty(NodeConstants.ECM_VERSION_HISTORY.rawname).remove();
        }
    }

}
