/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.core.repository.jcr.versioning;

import java.util.Calendar;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.repository.jcr.JCRDocument;
import org.nuxeo.ecm.core.repository.jcr.JCRSession;
import org.nuxeo.ecm.core.versioning.DocumentVersion;
import org.nuxeo.ecm.core.versioning.DocumentVersionIterator;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface VersioningService {

    void checkout(Document doc) throws DocumentException;

    void checkin(Document doc, String label) throws DocumentException;

    void checkin(Document doc, String label, String description) throws DocumentException;

    void restore(Document doc, String label) throws DocumentException;

    Document getVersion(Document doc, String label) throws DocumentException;

    /**
     * Gets the list of version ids for a document.
     *
     * @param doc the document
     * @return the list of version ids
     * @throws DocumentException
     * @since 1.4.1
     */
    List<String> getVersionsIds(Document doc) throws DocumentException;

    /**
     * Returns an iterator over all the versions within doc version history
     * A version history will always have at least one version, the root version.
     *
     * @param doc
     * @return
     * @throws DocumentException
     */
    DocumentVersionIterator getVersions(Document doc) throws DocumentException;

    /**
     *
     * @param doc
     * @return last version of the given document as it is saved in versionHistory
     * @throws DocumentException
     */
    DocumentVersion getLastVersion(Document doc) throws DocumentException;

    boolean isCheckedOut(Document doc) throws DocumentException;

    String getLabel(DocumentVersion version) throws DocumentException;

    String getDescription(DocumentVersion version) throws DocumentException;

    Calendar getCreated(DocumentVersion version) throws DocumentException;


    JCRDocument newDocumentVersion(JCRSession session, Node node) throws RepositoryException;

    boolean isVersionNode(Node node) throws RepositoryException;

    void removeVersionHistory(JCRDocument doc) throws DocumentException;

    /**
     * Removes the version of the given document having the specified label.
     *
     * @param doc
     * @param versionLabel the label of the version to be removed
     * @throws RepositoryException
     */
    void removeDocumentVersion(JCRDocument doc, String versionLabel)
            throws RepositoryException;

    /**
     * Do versioning-related fixups after a copy.
     *
     * @param doc the new document just created by copy
     * @throws RepositoryException
     */
    void fixupAfterCopy(JCRDocument doc) throws RepositoryException;

}
