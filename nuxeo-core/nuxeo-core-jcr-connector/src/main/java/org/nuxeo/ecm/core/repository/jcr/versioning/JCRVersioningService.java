/*
 * (C) Copyright 2007-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.core.repository.jcr.versioning;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.repository.jcr.JCRDocument;
import org.nuxeo.ecm.core.repository.jcr.JCRSession;
import org.nuxeo.ecm.core.versioning.DocumentVersion;
import org.nuxeo.ecm.core.versioning.DocumentVersionIterator;

/**
 * @author Bogdan Stefanescu
 * @author Florent Guillaume
 */
public class JCRVersioningService implements VersioningService {

    private static final Log log = LogFactory.getLog(JCRVersioningService.class);

    public void checkin(Document doc, String label) throws DocumentException {
        JCRDocument jdoc = (JCRDocument) doc;
        Node node = jdoc.getNode();
        try {
            Version version = node.checkin();
            if (label != null) {
                node.getVersionHistory().addVersionLabel(version.getName(),
                        label, false);
            }
        } catch (RepositoryException e) {
            String errMsg = "Failed to checkin document " + doc.getName()
                    + " : " + label;
            log.error(errMsg, e);
            throw new DocumentException(errMsg);
        }
    }

    // TODO: very weird method -> don't know what it should do -> copied as is
    // from JCRDocument
    public void checkin(Document doc, String label, String description)
            throws DocumentException {
        JCRDocument jdoc = (JCRDocument) doc;
        try {
            Node node = jdoc.getNode();
            Version version = node.checkin();
            if (label != null) {
                node.getVersionHistory().addVersionLabel(version.getName(),
                        label, false);
            }
            if (description != null) {
                node.getVersionHistory().addVersionLabel(version.getName(),
                        "desc\n" + description, false);
            }
        } catch (RepositoryException e) {
            throw new DocumentException("Failed to checkin document "
                    + doc.getName() + " : " + label);
        }
    }

    public void checkout(Document doc) throws DocumentException {
        JCRDocument jdoc = (JCRDocument) doc;
        try {
            jdoc.getNode().checkout();
        } catch (RepositoryException e) {
            throw new DocumentException(e);
        }
    }

    public DocumentVersion getLastVersion(Document doc)
            throws DocumentException {
        JCRDocument jdoc = (JCRDocument) doc;
        try {
            Version node = getBaseVersionNode(jdoc);
            return (DocumentVersion) newDocumentVersion(jdoc.jcrSession(), node
                    .getNode("jcr:frozenNode"));
        } catch (RepositoryException e) {
            throw new DocumentException(
                    "Failed to get last version for document " + doc.getName(),
                    e);
        }
    }

    public List<String> getVersionsIds(Document doc) throws DocumentException {
        final String QNAME_VERSION_HISTORY = "jcr:versionHistory";
        JCRDocument jdoc = (JCRDocument) doc;
        Node node = jdoc.getNode();
        try {
            if (!node.hasProperty(QNAME_VERSION_HISTORY)) {
                return Collections.emptyList();
            }
            VersionHistory versionHistory = node.getVersionHistory();
            VersionIterator it = versionHistory.getAllVersions();
            List<String> ids = new ArrayList<String>((int) it.getSize() - 1);
            // FIX ME versions are not guaranteed to be returned in order
            it.nextVersion(); // skip placeholder root version
            while (it.hasNext()) {
                Version version = it.nextVersion();
                if (versionHistory.getVersionLabels(version).length == 0) {
                    continue;
                }
                Node frozenNode = version.getNodes().nextNode();
                ids.add(frozenNode.getUUID());
            }
            return ids;
        } catch (RepositoryException e) {
            throw new DocumentException(e);
        }
    }

    public DocumentVersionIterator getVersions(Document doc)
            throws DocumentException {
        final String QNAME_VERSION_HISTORY = "jcr:versionHistory";

        JCRDocument jdoc = (JCRDocument) doc;
        try {
            // we might have not version history node, in this case Jackrabbit
            // throws exception
            // TODO : remove when fixed in jackrabbit
            // === patch start ===
            Node docNode = jdoc.getNode();
            if (!docNode.hasProperty(QNAME_VERSION_HISTORY)) {
                return new EmptyDocumentVersionIterator();
            }
            Property p = docNode.getProperty(QNAME_VERSION_HISTORY); // QName.JCR_VERSIONHISTORY
            if (p == null || p.getNode() == null) {
                return new EmptyDocumentVersionIterator();
            }
            // === patch end ===

            return new JCRDocumentVersionIterator(jdoc.jcrSession(),
                    docNode.getVersionHistory().getAllVersions());
        } catch (UnsupportedRepositoryOperationException e) {
            throw new DocumentException(e);
        } catch (RepositoryException e) {
            throw new DocumentException(e);
        }
    }

    private static class EmptyDocumentVersionIterator implements DocumentVersionIterator {

        public DocumentVersion nextDocumentVersion() throws DocumentException {
            return null;
        }

        public boolean hasNext() {
            return false;
        }

        public DocumentVersion next() {
            return null;
        }

        public void remove() {
        }

    }

    public boolean isCheckedOut(Document doc) throws DocumentException {
        JCRDocument jdoc = (JCRDocument) doc;
        try {
            return jdoc.getNode().isCheckedOut();
        } catch (RepositoryException e) {
            throw new DocumentException(e);
        }
    }

    public void restore(Document doc, String label) throws DocumentException {
        JCRDocument jdoc = (JCRDocument) doc;
        try {
            Version version = getVersionNode(jdoc, label);
            jdoc.getNode().restore(version, true);
        } catch (RepositoryException e) {
            throw new DocumentException(e);
        }
    }

    public Document getVersion(Document doc, String label)
            throws DocumentException {
        JCRDocument jdoc = (JCRDocument) doc;
        try {
            Version version = getVersionNode(jdoc, label);
            return newDocumentVersion(jdoc.jcrSession(), version
                    .getNode("jcr:frozenNode"));
        } catch (RepositoryException e) {
            throw new DocumentException(e);
        }
    }

    public String getLabel(DocumentVersion version) throws DocumentException {
        // TODO Auto-generated method stub
        return null;
    }

    public String getDescription(DocumentVersion version)
            throws DocumentException {
        // TODO Auto-generated method stub
        return null;
    }

    public Calendar getCreated(DocumentVersion version) throws DocumentException {
        // TODO Auto-generated method stub
        return null;
    }

    public JCRDocument newDocumentVersion(JCRSession session, Node node)
            throws RepositoryException {
        return new JCRDocumentVersion(session, node);
    }

    public boolean isVersionNode(Node node) throws RepositoryException {
        return "nt:frozenNode".equals(node.getPrimaryNodeType().getName());
    }

    protected Version getBaseVersionNode(JCRDocument doc)
            throws RepositoryException {
        return doc.getNode().getBaseVersion();
    }

    protected Version getVersionNode(JCRDocument doc, String label)
            throws RepositoryException {
        return doc.getNode().getVersionHistory().getVersionByLabel(label);
    }

    public void removeVersionHistory(JCRDocument doc) {
        // TODO Auto-generated method stub
        // maybe nothing to do as jackrabbit should take care of that when removing node
        // Jackrabbit is still buggy, see
        // http://issues.apache.org/jira/browse/JCR-134
    }

    public void removeDocumentVersion(JCRDocument doc, String label)
            throws RepositoryException {
        if (log.isDebugEnabled()) {
            try {
                log.info("remove doc (" + doc.getPath()
                        + ") version with label: " + label);
            } catch (DocumentException e) {
                log.error(e);
            }
        }
        VersionHistory vhist = doc.getNode().getVersionHistory();
        if (log.isDebugEnabled()) {
            _printVersions(vhist);
        }
        vhist.removeVersionLabel(label);
    }

    /**
     * Debugging method.
     *
     * @throws RepositoryException
     */
    private void _printVersions(VersionHistory vhist) throws RepositoryException {
        VersionIterator it = vhist.getAllVersions();

        while (it.hasNext()) {
            Version version = it.nextVersion();
            log.debug("version: " + version.getName());
        }
    }

    public void fixupAfterCopy(JCRDocument doc) {
        // nothing to do
    }

}
