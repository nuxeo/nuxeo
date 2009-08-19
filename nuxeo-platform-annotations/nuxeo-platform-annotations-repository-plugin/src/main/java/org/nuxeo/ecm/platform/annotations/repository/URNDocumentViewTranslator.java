/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.repository;

import java.net.URI;
import java.net.URISyntaxException;

import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentView;

/**
 * @author Alexandre Russel
 *
 */
public class URNDocumentViewTranslator {
    public URI getNuxeoUrn(String server, String id) throws URISyntaxException {
        return new URI("urn:nuxeo:" + server + ":" + id);
    }

    public URI getUriFromDocumentView(String serverName, DocumentRef docRef)
            throws URISyntaxException {
        return new URI("urn:nuxeo:"
                + serverName + ":"
                + docRef);
    }

    public DocumentView getDocumentViewFromUri(URI uri) {
        if (!isNuxeoUrn(uri)) {
            return null;
        }
        String[] tokens = uri.toString().split(":");
        DocumentRef ref = new IdRef(tokens[3]);
        DocumentLocation dl = new DocumentLocationImpl(tokens[2], ref);
        DocumentView view = null;
        if (tokens.length < 5) {
            view = new DocumentViewImpl(dl);
        }
        return view;
    }

    public boolean isNuxeoUrn(URI uri) {
        return uri.toString().startsWith("urn:nuxeo");
    }
}
