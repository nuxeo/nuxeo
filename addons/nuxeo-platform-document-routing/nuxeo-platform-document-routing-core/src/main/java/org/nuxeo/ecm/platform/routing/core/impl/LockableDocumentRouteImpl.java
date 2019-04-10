/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     mcedica
 */
package org.nuxeo.ecm.platform.routing.core.impl;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.routing.api.LockableDocumentRoute;

/**
 *
 * @author <a href="mailto:mcedica@nuxeo.com">Mariana Cedica</a>
 *
 */
public class LockableDocumentRouteImpl implements LockableDocumentRoute {
    protected final DocumentModel doc;

    public LockableDocumentRouteImpl(DocumentModel document) {
        this.doc = document;
    }

    private static final long serialVersionUID = 1L;

    @Override
    public boolean isLocked(CoreSession session) throws ClientException {
        return session.getLockInfo(doc.getRef()) != null;
    }

    @Override
    public boolean isLockedByCurrentUser(CoreSession session)
            throws ClientException {
        Lock lockInfo = session.getLockInfo(doc.getRef());
        if (lockInfo == null) {
            return false;
        }
        String lockOwner = lockInfo.getOwner();
        NuxeoPrincipal userName = (NuxeoPrincipal) session.getPrincipal();
        return userName.getName().equals(lockOwner);
    }

    @Override
    public void lockDocument(CoreSession session) throws ClientException {
        session.setLock(doc.getRef());
    }

    @Override
    public void unlockDocument(CoreSession session) throws ClientException {
        DocumentRef ref = doc.getRef();
        session.removeLock(ref);
    }

    @Override
    public String getLockOwner(CoreSession session) {
        return session.getPrincipal().getName();
    }

}
