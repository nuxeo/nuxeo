/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Stephane Lacoin
 */
package org.nuxeo.ecm.core.api;

import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.security.Principal;

import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Document repository reference including the principal owner of the session.
 *
 * @since 7.10
 * @author Stephane Lacoin at Nuxeo (aka matic)
 */
public class InstanceRef implements DocumentRef {

    private static final long serialVersionUID = 1L;

    final String repositoryName;

    final NuxeoPrincipal principal;

    final DocumentRef ref;

    transient DocumentModel referent;

    public InstanceRef(DocumentModel doc, NuxeoPrincipal principal) {
        if (doc.getRef() == null) {
            throw new NullPointerException("document as no reference yet");
        }
        referent = doc;
        repositoryName = doc.getRepositoryName();
        this.principal = principal;
        ref = doc.getRef();
    }

    @Override
    public int type() {
        return -1;
    }

    @Override
    public Object reference() {
        return referent;
    }

    private Object readResolve() throws ObjectStreamException {
        // we need a transaction for this
        boolean started = false;
        if (!TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            started = TransactionHelper.startTransaction();
        }
        try {
            try (CloseableCoreSession session = CoreInstance.openCoreSession(repositoryName, principal)) {
                referent = session.getDocument(ref);
                referent.detach(true);
                return referent;
            }
        } catch (RuntimeException cause) {
            InvalidObjectException error = new InvalidObjectException(
                    "Cannot refetch " + ref + " from " + repositoryName);
            error.initCause(cause);
            throw error;
        } finally {
            if (started) {
                TransactionHelper.commitOrRollbackTransaction();
            }
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((repositoryName == null) ? 0 : repositoryName.hashCode());
        result = prime * result + ((ref == null) ? 0 : ref.hashCode());
        result = prime * result + ((principal == null) ? 0 : principal.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        InstanceRef other = (InstanceRef) obj;
        if (repositoryName == null) {
            if (other.repositoryName != null) {
                return false;
            }
        } else if (!repositoryName.equals(other.repositoryName)) {
            return false;
        }
        if (ref == null) {
            if (other.ref != null) {
                return false;
            }
        } else if (!ref.equals(other.ref)) {
            return false;
        }
        if (principal == null) {
            if (other.principal != null) {
                return false;
            }
        } else if (!principal.equals(other.principal)) {
            return false;
        }
        return true;
    }
}
