/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     mcedica
 */
package org.nuxeo.ecm.platform.routing.core.impl;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.routing.api.LockableDocumentRoute;

/**
 * @author <a href="mailto:mcedica@nuxeo.com">Mariana Cedica</a>
 */
public class LockableDocumentRouteImpl implements LockableDocumentRoute {
    protected final DocumentModel doc;

    public LockableDocumentRouteImpl(DocumentModel document) {
        this.doc = document;
    }

    private static final long serialVersionUID = 1L;

    @Override
    public boolean isLocked(CoreSession session) {
        return session.getLockInfo(doc.getRef()) != null;
    }

    @Override
    public boolean isLockedByCurrentUser(CoreSession session) {
        Lock lockInfo = session.getLockInfo(doc.getRef());
        if (lockInfo == null) {
            return false;
        }
        String lockOwner = lockInfo.getOwner();
        NuxeoPrincipal userName = session.getPrincipal();
        return userName.getName().equals(lockOwner);
    }

    @Override
    public void lockDocument(CoreSession session) {
        session.setLock(doc.getRef());
    }

    @Override
    public void unlockDocument(CoreSession session) {
        DocumentRef ref = doc.getRef();
        session.removeLock(ref);
    }

    @Override
    public String getLockOwner(CoreSession session) {
        return session.getPrincipal().getName();
    }

}
