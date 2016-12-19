/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     troger
 */
package org.nuxeo.ecm.platform.preview.codec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.service.AbstractDocumentViewCodec;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class DocumentPreviewCodec extends AbstractDocumentViewCodec {

    public static final String PROPERTY_PATH_KEY = "PROPERTY_PATH_KEY";

    public static final String PREFIX = "restAPI/preview";

    // /server/docId/property_path/
    public static final String URLPattern = "/(\\w+)/([a-zA-Z_0-9\\-]+)/([a-zA-Z_0-9:\\-\\.\\]\\[]*)/";

    @Override
    public String getPrefix() {
        if (prefix != null) {
            return prefix;
        }
        return PREFIX;
    }

    @Override
    public DocumentView getDocumentViewFromUrl(String url) {
        final Pattern pattern = Pattern.compile(getPrefix() + URLPattern);
        Matcher m = pattern.matcher(url);
        if (m.matches()) {
            final String server = m.group(1);
            String uuid = m.group(2);
            final DocumentRef docRef = new IdRef(uuid);

            Map<String, String> params = new HashMap<>();
            final String property = m.group(3);
            params.put(PROPERTY_PATH_KEY, property);

            final DocumentLocation docLoc = new DocumentLocationImpl(server, docRef);
            final DocumentView docView = new DocumentViewImpl(docLoc, null, params);

            return docView;
        }
        return null;
    }

    @Override
    public String getUrlFromDocumentView(DocumentView docView) {
        DocumentLocation docLoc = docView.getDocumentLocation();
        String property = docView.getParameter(PROPERTY_PATH_KEY);

        // NXP-11215 Avoid NPE with not persisted documentModel
        if (docLoc != null && docLoc.getDocRef() != null) {
            List<String> items = new ArrayList<>();
            items.add(getPrefix());
            items.add(docLoc.getServerName());
            items.add(docLoc.getDocRef().toString());
            items.add(property);
            String uri = String.join("/", items) + '/';

            Map<String, String> requestParams = new HashMap<>(docView.getParameters());
            requestParams.remove(PROPERTY_PATH_KEY);
            return URIUtils.addParametersToURIQuery(uri, requestParams);
        }
        return null;
    }

}
