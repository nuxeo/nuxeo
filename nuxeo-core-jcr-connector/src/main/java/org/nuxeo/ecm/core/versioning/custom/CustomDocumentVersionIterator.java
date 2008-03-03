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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.repository.jcr.JCRSession;
import org.nuxeo.ecm.core.versioning.DocumentVersion;
import org.nuxeo.ecm.core.versioning.DocumentVersionIterator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * DocumentVersion iterator implementation. An iterator over CustomDocumentVersion
 * built from nodes handled by CustomVersioningService.
 *
 * @author DM
 *
 */
public class CustomDocumentVersionIterator implements DocumentVersionIterator {

    private static final Log log = LogFactory.getLog(CustomDocumentVersionIterator.class);

    private final Node versionHistory;

    private NodeIterator delegate;

    private final JCRSession session;

    /**
     * @param session
     * @param versionHistory
     */
    public CustomDocumentVersionIterator(JCRSession session,
            Node versionHistory) {
        this.versionHistory = versionHistory;
        this.session = session;
    }

    /**
     * This class acts as a delegate for node iterator.
     *
     * @return
     * @throws RepositoryException
     */
    private NodeIterator getIterDelegate() throws RepositoryException {
        if (delegate == null) {
            // will exclude version start node - as it is a dummy version node
            delegate = versionHistory.getNodes(); // VerServUtils.VERSION_NODE_NAME_PREFIX
                                                        // + "*");
        }
        return delegate;
    }

    public DocumentVersion nextDocumentVersion() throws DocumentException {
        try {
            Node versionNode = getIterDelegate().nextNode();
            //if (!versionNode.hasNodes()) {
            //    return nextDocumentVersion();
            //}
            return new CustomDocumentVersion(session, versionNode);
            //return (DocumentVersion) Versioning.getService()
            //        .newDocumentVersion(session,
            //                getIterDelegate().nextNode()); //Version().getNode("jcr:frozenNode")
        } catch (RepositoryException e) {
            throw new DocumentException(
                    "Failed to create a document version instance", e);
        }
    }

    public boolean hasNext() {
        try {
            return getIterDelegate().hasNext();
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
    }

    public DocumentVersion next() {
        try {
            Node versionNode = getIterDelegate().nextNode();
            //if (!versionNode.hasNodes()) {
            //    return next();
            //}
            return new CustomDocumentVersion(session, versionNode);
            //return (DocumentVersion) Versioning.getService()
            //        .newDocumentVersion(session,
            //                getIterDelegate().nextNode());//nextVersion().getNode("jcr:frozenNode"));
        } catch (RepositoryException e) {
            e.printStackTrace();
            log.error("BUG: failed to instantiate document version object!");
            return next();
        }
    }

    public void remove() {
        // TODO ...delegate.remove();
    }

}
