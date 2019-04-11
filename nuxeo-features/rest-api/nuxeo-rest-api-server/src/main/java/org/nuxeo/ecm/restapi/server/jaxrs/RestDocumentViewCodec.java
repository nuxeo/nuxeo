/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.ecm.restapi.server.jaxrs;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.service.AbstractDocumentViewCodec;

import com.google.common.base.Joiner;

/**
 * Codec that resolve the Rest url for a document
 *
 * @since 5.7.2
 */
public class RestDocumentViewCodec extends AbstractDocumentViewCodec {

    public static final String PREFIX = "site/api/v1/id";

    public RestDocumentViewCodec() {
    }

    public RestDocumentViewCodec(String prefix) {
    }

    @Override
    public String getPrefix() {
        if (prefix != null) {
            return prefix;
        }
        return PREFIX;
    }

    @Override
    public String getUrlFromDocumentView(DocumentView docView) {
        DocumentLocation docLoc = docView.getDocumentLocation();
        if (docLoc != null) {

            List<String> items = new ArrayList<>();
            items.add(getPrefix());
            IdRef docRef = docLoc.getIdRef();
            if (docRef == null) {
                return null;
            }
            items.add(docRef.toString());
            String uri = Joiner.on("/").join(items);
            return URIUtils.addParametersToURIQuery(uri, docView.getParameters());
        }
        return null;
    }

    /**
     * There is no document veiw for Rest codec
     */
    @Override
    public DocumentView getDocumentViewFromUrl(String url) {
        return null;
    }

}
