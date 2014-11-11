/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: DocumentLocationImpl.java 25074 2007-09-18 14:23:08Z atchertchian $
 */

package org.nuxeo.ecm.core.api.impl;

import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;

public class DocumentLocationImpl implements DocumentLocation {

    private static final long serialVersionUID = -1109935626596128985L;

    private final String serverName;

    private final DocumentRef docRef;

    private final IdRef docIdRef;

    private final PathRef docPathRef;

    public DocumentLocationImpl(DocumentModel doc) {
        serverName = doc.getRepositoryName();
        docRef = doc.getRef();
        String id = doc.getId();
        if (id != null) {
            docIdRef = new IdRef(id);
        } else {
            docIdRef = null;
        }
        String path = doc.getPathAsString();
        if (path != null) {
            docPathRef = new PathRef(path);
        } else {
            docPathRef = null;
        }
    }

    public DocumentLocationImpl(final String serverName, final IdRef idRef,
            final PathRef pathRef) {
        this.serverName = serverName;
        docRef = idRef;
        docIdRef = idRef;
        docPathRef = pathRef;
    }

    public DocumentLocationImpl(final String serverName,
            final DocumentRef docRef) {
        this.serverName = serverName;
        this.docRef = docRef;
        if (docRef instanceof IdRef) {
            docIdRef = (IdRef) docRef;
            docPathRef = null;
        } else if (docRef instanceof PathRef) {
            docIdRef = null;
            docPathRef = (PathRef) docRef;
        } else {
            docIdRef = null;
            docPathRef = null;
        }
    }

    @Override
    public DocumentRef getDocRef() {
        return docRef;
    }

    @Override
    public String getServerName() {
        return serverName;
    }

    @Override
    public IdRef getIdRef() {
        return docIdRef;
    }

    @Override
    public PathRef getPathRef() {
        return docPathRef;
    }

    @Override
    public String toString() {
        return String.format(
                "DocumentLocationImpl [docIdRef=%s, docPathRef=%s, docRef=%s, serverName=%s]",
                docIdRef, docPathRef, docRef, serverName);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     *
     * Overrides the default to use the docRef and serverName fields for hash
     * value tests.
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((docRef == null) ? 0 : docRef.hashCode());
        result = prime * result
                + ((serverName == null) ? 0 : serverName.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     *
     * Overrides the default to use the docRef and serverName fields for
     * equality tests.
     */
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
        DocumentLocation other = (DocumentLocation) obj;
        if (docRef == null) {
            if (other.getDocRef() != null) {
                return false;
            }
        } else if (!docRef.equals(other.getDocRef())) {
            return false;
        }
        if (serverName == null) {
            if (other.getServerName() != null) {
                return false;
            }
        } else if (!serverName.equals(other.getServerName())) {
            return false;
        }
        return true;
    }

}
