/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */
package org.nuxeo.web.ui;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.service.AbstractDocumentViewCodec;

/**
 * @since 9.10
 */
public class WebUIRedirectCodec extends AbstractDocumentViewCodec {

    @Override
    public DocumentView getDocumentViewFromUrl(String s) {
        // no need
        return null;
    }

    @Override
    public String getUrlFromDocumentView(DocumentView docView) {
        DocumentLocation docLoc = docView.getDocumentLocation();
        if (docLoc == null) {
            return null;
        }
        IdRef idRef = docLoc.getIdRef();
        PathRef pathRef = docLoc.getPathRef();
        if (idRef == null && pathRef == null) {
            return null;
        }

        List<String> fragments = new ArrayList<>();
        fragments.add(getPrefix());
        if (idRef != null) {
            fragments.add("doc");
            fragments.add(docLoc.getServerName());
            fragments.add(idRef.toString());
        } else {
            fragments.add("browse" + pathRef.toString());
        }
        return String.join("/", fragments);
    }
}
