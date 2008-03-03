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

import javax.jcr.RepositoryException;
import javax.jcr.version.VersionIterator;

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.repository.jcr.JCRSession;
import org.nuxeo.ecm.core.versioning.DocumentVersion;
import org.nuxeo.ecm.core.versioning.DocumentVersionIterator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class JCRDocumentVersionIterator implements DocumentVersionIterator {

    private static final Log log = LogFactory.getLog(JCRDocumentVersionIterator.class);

    final VersionIterator delegate;
    final JCRSession session;

    public JCRDocumentVersionIterator(JCRSession session,  VersionIterator iterator) {
        delegate = iterator;
        this.session = session;
    }

    public DocumentVersion nextDocumentVersion() throws DocumentException {
        try {
            return (DocumentVersion) Versioning.getService()
                .newDocumentVersion(session, delegate.nextVersion().getNode("jcr:frozenNode"));
        } catch (RepositoryException e) {
            throw new DocumentException("Failed to create a document version instance", e);
        }
    }

    public boolean hasNext() {
        return delegate.hasNext();
    }

    public DocumentVersion next() {
        try {
            return (DocumentVersion) Versioning.getService()
                .newDocumentVersion(session, delegate.nextVersion().getNode("jcr:frozenNode"));
        } catch (RepositoryException e) {
            log.error("BUG: failed to instantiate document version object!");
            return next();
        }
    }

    public void remove() {
        delegate.remove();
    }

}
