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

package org.nuxeo.ecm.core.repository.jcr.versioning;

import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.repository.jcr.JCRDocument;
import org.nuxeo.ecm.core.repository.jcr.JCRSession;
import org.nuxeo.ecm.core.repository.jcr.ModelAdapter;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.versioning.DocumentVersion;

public class JCRDocumentVersion extends JCRDocument implements DocumentVersion {

    protected static final String DESC_PREFIX = "desc\n";

    // public JCRDocumentVersion(JCRSession session, Version version) throws
    // RepositoryException {
    // this.session = session;
    // this.version = version;
    // this.node = this.version.getNode("jcr:frozenNode");
    // String localName = node.getProperty("jcr:frozenPrimaryType").getString();
    // int i = localName.lastIndexOf(":") + 1;
    // localName = localName.substring(i);
    // this.type =
    // session.repository.getTypeManager().getDocumentType(localName);
    // }
    //
    public JCRDocumentVersion(JCRSession session, Node frozenNode)
            throws RepositoryException {
        this.session = session;
        node = frozenNode;
        String localName = node.getProperty("jcr:frozenPrimaryType")
                .getString();
        int i = localName.lastIndexOf(':') + 1;
        localName = localName.substring(i);
        type = session.getRepository().getTypeManager().getDocumentType(
                localName);
    }

    protected DocumentType getSourceDocumentType() throws RepositoryException {
        String localName = node.getProperty("jcr:frozenPrimaryType")
                .getString();
        int i = localName.lastIndexOf(':') + 1;
        localName = localName.substring(i);
        return session.getRepository().getTypeManager().getDocumentType(
                localName);
    }

    protected Node getSourceDocumentNode() throws RepositoryException {
        javax.jcr.Property prop = node.getProperty("jcr:frozenUuid");
        return session.jcrSession().getNodeByUUID(prop.getString());
    }

    @Override
    public String getPath() throws DocumentException {
        try {
            return ModelAdapter.getPath(null, getSourceDocumentNode());
        } catch (Exception e) {
            throw new DocumentException("Failed to compute path for " + this, e);
        }
    }

    @Override
    public Document getParent() throws DocumentException {
        try {
            Node cnode = getSourceDocumentNode();
            Node parentNode = ModelAdapter.getParentNode(cnode);
            if (parentNode == null) {
                return null;
            }
            return session.newDocument(parentNode);
        } catch (RepositoryException e) {
            throw new DocumentException("Failed to get parent for document "
                    + getPath(), e);
        }
    }

    @Override
    public DocumentVersion getLastVersion() throws DocumentException {
        throw new UnsupportedOperationException(
                "cannot call getLastVersions on version docs");
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

    public Calendar getCreated() throws DocumentException {
        try {
            return ((Version) node.getParent()).getCreated();
        } catch (RepositoryException e) {
            throw new DocumentException(e);
        }
    }

    public String getLabel() throws DocumentException {
        try {
            Version version = (Version) node.getParent();
            String[] labels = version.getContainingHistory().getVersionLabels(
                    version);

            // TODO check that labels size is 2
            for (String label : labels) {
                if (!label.startsWith(DESC_PREFIX)) {
                    return label;
                }
            }

        } catch (VersionException e) {
            throw new DocumentException(e);
        } catch (RepositoryException e) {
            throw new DocumentException(e);
        }
        return null;
    }

    public String getDescription() throws DocumentException {
        try {
            Version version = (Version) node.getParent();
            String[] labels = version.getContainingHistory().getVersionLabels(
                    version);

            // TODO check that s size is 2
            for (String label : labels) {
                if (label.startsWith(DESC_PREFIX)) {
                    return label.substring(DESC_PREFIX.length());
                }
            }

        } catch (VersionException e) {
            throw new DocumentException(e);
        } catch (RepositoryException e) {
            throw new DocumentException(e);
        }
        return null;
    }

    public DocumentVersion[] getPredecessors() {
        throw new UnsupportedOperationException(
                "getPredecessors not yet Implemented");
        // TODO Auto-generated method stub
        // return null;
    }

    public DocumentVersion[] getSuccessors() {
        throw new UnsupportedOperationException(
                "getSuccessors not yet Implemented");
        // TODO Auto-generated method stub
        // return null;
    }

    @Override
    public boolean isVersion() {
        return true;
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
