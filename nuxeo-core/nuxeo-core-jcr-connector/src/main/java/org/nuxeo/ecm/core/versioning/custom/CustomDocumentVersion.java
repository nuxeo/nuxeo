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

package org.nuxeo.ecm.core.versioning.custom;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.repository.jcr.JCRDocument;
import org.nuxeo.ecm.core.repository.jcr.JCRSession;
import org.nuxeo.ecm.core.repository.jcr.ModelAdapter;
import org.nuxeo.ecm.core.repository.jcr.NodeConstants;
import org.nuxeo.ecm.core.repository.jcr.versioning.Versioning;
import org.nuxeo.ecm.core.versioning.DocumentVersion;

/**
 * A Document implementation which is tied to a version of a document.
 * Instances of this class are created by custom versioning system.
 * <p>
 * This corresponds to ecm:version mixin which is a child of ecm:versionHistory.
 *
 * @author DM
 *
 */
public class CustomDocumentVersion extends JCRDocument implements
        DocumentVersion {

    private static final Log log = LogFactory.getLog(CustomDocumentVersion.class);

    private final Node versionNode;

    /**
     * Protected constructor called by CustomVersioningService class.
     * Rux NXP-2617: actually made it public to allow service extension. Otherwise, the
     * contribution is not considered because the implementation of service framework
     * (see {@link TypeImporter.createDocTypeDefinition})
     *
     * @param session
     * @param versionNode the node should be the immediate child of a versionHistory
     *   node. This node contains version metadata.
     * @throws RepositoryException
     */
    public CustomDocumentVersion(JCRSession session, Node versionNode)
            throws RepositoryException {

        if (versionNode == null) {
            throw new IllegalArgumentException("null versionNode");
        }
        this.versionNode = versionNode;

        // cannot call super(session, versionNode.getNodes().nextNode())
        this.session = session;

        if (versionNode.hasNodes()) {
            node = versionNode.getNodes().nextNode();
            type = session.getDocumentType(node); // TODO lazy load
            if (type == null) {
                throw new RepositoryException();
            }
        } else {
            // accept it as it is the root version node
            //throw new RepositoryException("no children for versionNode: "
            //        + versionNode.getPath());
        }

        // String localName =
        // node.getProperty("jcr:frozenPrimaryType").getString();
        // int i = localName.lastIndexOf(":") + 1;
        // localName = localName.substring(i);
        // this.type =
        // session.getRepository().getTypeManager().getDocumentType(localName);
    }

    public Calendar getCreated() throws DocumentException {
        try {
            if (versionNode
                    .hasProperty(NodeConstants.ECM_VERSION_CREATEDATE.rawname)) {
                return versionNode.getProperty(
                        NodeConstants.ECM_VERSION_CREATEDATE.rawname).getDate();
            }
        } catch (RepositoryException e) {
            throw new DocumentException("cannot get version label", e);
        }
        return null;
    }

    public String getDescription() throws DocumentException {
        try {
            if (versionNode
                    .hasProperty(NodeConstants.ECM_VERSION_DESCRIPTION.rawname)) {
                return versionNode.getProperty(
                        NodeConstants.ECM_VERSION_DESCRIPTION.rawname)
                        .getString();
            }
        } catch (RepositoryException e) {
            // e.printStackTrace();
            throw new DocumentException("cannot get version label", e);
        }
        return null;
    }

    public String getLabel() throws DocumentException {
        try {
            if (versionNode
                    .hasProperty(NodeConstants.ECM_VERSION_LABEL.rawname)) {
                return versionNode.getProperty(
                        NodeConstants.ECM_VERSION_LABEL.rawname).getString();
            }
        } catch (RepositoryException e) {
            throw new DocumentException("cannot get version label", e);
        }
        return null;
    }

    public DocumentVersion[] getPredecessors() throws DocumentException {

        try {
            List<Node> predecessors = VerServUtils.getPredecessors(versionNode);
            List<DocumentVersion> docs = new ArrayList<DocumentVersion>();
            for (Node predecessor : predecessors) {
                docs.add(new CustomDocumentVersion(session, predecessor));
            }

            DocumentVersion[] docsArray = new DocumentVersion[docs.size()];
            return docs.toArray(docsArray);
        } catch (RepositoryException e) {
            throw new DocumentException("cannot get predecessors", e);
        }
    }

    public DocumentVersion[] getSuccessors() throws DocumentException {

        try {
            List<Node> successors = VerServUtils.getSuccessors(versionNode);
            List<DocumentVersion> docs = new ArrayList<DocumentVersion>();
            for (Node successor : successors) {
                docs.add(new CustomDocumentVersion(session, successor));
            }

            DocumentVersion[] docsArray = new DocumentVersion[docs.size()];
            return docs.toArray(docsArray);
        } catch (RepositoryException e) {
            throw new DocumentException("cannot get successors", e);
        }
    }

    protected Node getSourceDocumentNode() throws RepositoryException {
        javax.jcr.Property prop = node.getProperty(NodeConstants.ECM_FROZEN_NODE_UUID.rawname);
        return session.jcrSession().getNodeByUUID(prop.getString());
    }

    /**
     * @return the parent of the source node (i.e. the parent of the node for
     *         which this document is a frozen version)
     */
    @Override
    public Document getParent() throws DocumentException {
        Node cnode;
        try {
            cnode = getSourceDocumentNode();
        } catch (ItemNotFoundException e) {
            log.warn("Source document node for version does not exist: "
                    + getUUID());
            return null;
        } catch (RepositoryException e) {
            throw new DocumentException(
                    "Failed to get document source for version " + getUUID(), e);
        }

        try {
            Node parentNode = ModelAdapter.getParentNode(cnode);
            if (parentNode == null) {
                return null;
            }
            return session.newDocument(parentNode);
        } catch (RepositoryException e) {
            String path = null;
            try {
                path = getPath();
            } catch (DocumentException e2) {
                // just log this, to not mask the original error
                log.error("cannot get doc path ", e2);
            }
            throw new DocumentException("Failed to get parent for document "
                    + (path != null ? ("path=" + path) : "uuid=" + getUUID()),
                    e);
        }
    }

    @Override
    public String getPath() throws DocumentException {
        try {
            final Node sourceDocNode = getSourceDocumentNode();
            return ModelAdapter.getPath(null, sourceDocNode);
        } catch (ItemNotFoundException e) {
            // source document does not exist:
            // TODO review the policy of docs deletion
            throw new DocumentException("source document does not exist", e);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
            throw new DocumentException("cannot get path", e);
        }
    }

    @Override
    public boolean isVersion() {
        return true;
    }

    @Override
    public Document getSourceDocument() throws DocumentException {
        try {
            Node headNode = getSourceDocumentNode();
            return session.newDocument(headNode);
        } catch (RepositoryException e) {
            throw new DocumentException("Failed to get parent for document "
                    + getPath(), e);
        }
    }

    @Override
    public final void remove() throws DocumentException {
        try {
            JCRDocument sourceDoc = (JCRDocument) getSourceDocument();
            String label = getLabel();
            Versioning.getService().removeDocumentVersion(sourceDoc, label);
        } catch (RepositoryException e) {
            throw new DocumentException(e);
        }
    }
}
