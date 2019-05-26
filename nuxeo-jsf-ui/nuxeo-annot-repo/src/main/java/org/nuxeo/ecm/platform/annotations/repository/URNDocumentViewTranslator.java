/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
public class URNDocumentViewTranslator {

    public URI getNuxeoUrn(String server, String id) {
        try {
            return new URI("urn:nuxeo:" + server + ":" + id);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public URI getUriFromDocumentView(String serverName, DocumentRef docRef) {
        try {
            return new URI("urn:nuxeo:" + serverName + ":" + docRef);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
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
