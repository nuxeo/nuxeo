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
        docIdRef = new IdRef(doc.getId());
        docPathRef = new PathRef(doc.getPathAsString());
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

    public DocumentRef getDocRef() {
        return docRef;
    }

    public String getServerName() {
        return serverName;
    }

    public IdRef getIdRef() {
        return docIdRef;
    }

    public PathRef getPathRef() {
        return docPathRef;
    }

}
