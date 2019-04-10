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

import java.text.DateFormat;
import java.util.Date;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
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
        return session.getDocument(doc.getRef()).isLocked();
    }

    @Override
    public boolean isLockedByCurrentUser(CoreSession session)
            throws ClientException {
        if (!isLocked(session)) {
            return false;
        }
        String lockOwner = session.getLock(doc.getRef()).split(":")[0];
        NuxeoPrincipal userName = (NuxeoPrincipal) session.getPrincipal();
        return userName.getName().equals(lockOwner);
    }

    @Override
    public void lockDocument(CoreSession session) throws ClientException {
        StringBuilder lockKey = new StringBuilder();
        lockKey.append(getLockOwner(session)).append(':').append(
                DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date()));
        session.setLock(doc.getRef(), lockKey.toString());
        session.save();
    }

    @Override
    public void unlockDocument(CoreSession session) throws ClientException {
        DocumentRef ref = doc.getRef();
        session.unlock(ref);
        session.save();
    }

    @Override
    public String getLockOwner(CoreSession session) {
        return session.getPrincipal().getName();
    }

}
