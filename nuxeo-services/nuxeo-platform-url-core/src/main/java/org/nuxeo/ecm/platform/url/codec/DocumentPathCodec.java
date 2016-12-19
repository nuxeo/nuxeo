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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.url.codec;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.service.AbstractDocumentViewCodec;

/**
 * Codec handling a document repository, path, view and additional request parameters.
 *
 * @author Anahide Tchertchian
 */
public class DocumentPathCodec extends AbstractDocumentViewCodec {

    // The maximum length of an url for Internet Explorer.
    public static final int URL_MAX_LENGTH = 2000;

    public static final String PREFIX = "nxpath";

    // nxpath/server/path/to/doc@view_id?requestParams
    public static final String URL_PATTERN = "/" // slash
            + "([\\w\\.]+)" // server name (group 1)
            + "(?:/(.*))?" // path (group 2) (optional)
            + "@([\\w\\-\\.;=]+)" // view id (group 3)
            + "/?" // final slash (optional)
            + "(?:\\?(.*)?)?"; // query (group 4) (optional)

    private static final Log log = LogFactory.getLog(DocumentPathCodec.class);

    public DocumentPathCodec() {
    }

    public DocumentPathCodec(String prefix) {
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
        // Use DocumentIdCodec if the document is a version
        if ("true".equals(docView.getParameter("version"))) {
            if (docView.getDocumentLocation().getIdRef() != null) {
                DocumentIdCodec idCodec = new DocumentIdCodec();
                return idCodec.getUrlFromDocumentView(docView);
            }
        }

        DocumentLocation docLoc = docView.getDocumentLocation();
        if (docLoc != null) {
            List<String> items = new ArrayList<>();
            items.add(getPrefix());
            items.add(docLoc.getServerName());
            PathRef docRef = docLoc.getPathRef();

            if (docRef != null) {
                // this is a path, get rid of leading slash
                String path = docRef.toString();
                if (path.startsWith("/")) {
                    path = path.substring(1);
                }
                if (path.length() > 0) {
                    items.add(URIUtils.quoteURIPathComponent(path, false));
                }
            }

            String uri = String.join("/", items);
            String viewId = docView.getViewId();
            if (viewId != null) {
                uri += "@" + viewId;
            }

            String uriWithParam = URIUtils.addParametersToURIQuery(uri, docView.getParameters());

            // If the URL with the Path codec is to long, it use the URL with
            // the Id Codec.
            if (uriWithParam.length() > URL_MAX_LENGTH) {

                // If the DocumentLocation did not contains the document Id, it
                // use the Path Codec even if the Url is too long for IE.
                if (null == docView.getDocumentLocation().getIdRef()) {
                    log.error("The DocumentLocation did not contains the RefId.");
                    return uriWithParam;
                }

                DocumentIdCodec idCodec = new DocumentIdCodec();
                return idCodec.getUrlFromDocumentView(docView);

            } else {
                return uriWithParam;
            }
        }
        return null;
    }

    /**
     * Extracts document location from a Zope-like URL, eg: server/path_or_docId/view_id/tab_id .
     */
    @Override
    public DocumentView getDocumentViewFromUrl(String url) {
        final Pattern pattern = Pattern.compile(getPrefix() + URL_PATTERN);
        Matcher m = pattern.matcher(url);
        if (m.matches()) {

            final String server = m.group(1);
            String path = m.group(2);
            if (path != null) {
                // add leading slash to make it absolute if it's not the root
                path = "/" + URIUtils.unquoteURIPathComponent(path);
            }
            final DocumentRef docRef = path != null ? new PathRef(path) : null;
            String viewId = m.group(3);
            int jsessionidIndex = viewId.indexOf(";jsessionid");
            if (jsessionidIndex != -1) {
                viewId = viewId.substring(0, jsessionidIndex);
            }

            // get other parameters
            String query = m.group(4);
            Map<String, String> params = URIUtils.getRequestParameters(query);

            final DocumentLocation docLoc = new DocumentLocationImpl(server, docRef);

            return new DocumentViewImpl(docLoc, viewId, params);
        }

        return null;
    }

}
