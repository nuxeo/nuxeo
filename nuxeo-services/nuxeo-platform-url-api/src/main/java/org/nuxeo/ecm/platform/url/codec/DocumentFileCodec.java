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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.core.utils.DocumentModelUtils;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.service.AbstractDocumentViewCodec;

public class DocumentFileCodec extends AbstractDocumentViewCodec {

    public static final String FILE_PROPERTY_PATH_KEY = "FILE_PROPERTY_PATH";

    public static final String FILENAME_KEY = "FILENAME";

    // nxdoc/server/docId/property_path/filename/?requestParams
    public static final String URLPattern = "/(\\w+)/([a-zA-Z_0-9\\-]+)(/([a-zA-Z_0-9/:\\-\\.\\]\\[]*))+(/([^\\?]*))+(\\?)?(.*)?";

    public DocumentFileCodec() {
    }

    public DocumentFileCodec(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public String getUrlFromDocumentView(DocumentView docView) {
        DocumentLocation docLoc = docView.getDocumentLocation();
        String filepath = docView.getParameter(FILE_PROPERTY_PATH_KEY);
        String filename = docView.getParameter(FILENAME_KEY);
        if (docLoc != null && filepath != null && filename != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(getPrefix());
            sb.append("/");
            sb.append(docLoc.getServerName());
            sb.append("/");
            sb.append(docLoc.getDocRef().toString());
            sb.append("/");
            sb.append(filepath);
            sb.append("/");
            sb.append(URIUtils.quoteURIPathToken(filename));
            String uri = sb.toString();
            Map<String, String> requestParams = new HashMap<>(docView.getParameters());
            requestParams.remove(FILE_PROPERTY_PATH_KEY);
            requestParams.remove(FILENAME_KEY);
            return URIUtils.addParametersToURIQuery(uri, requestParams);
        }
        return null;
    }

    /**
     * Extracts document location from a Zope-like URL ie : server/path_or_docId/view_id/tab_id .
     */
    @Override
    public DocumentView getDocumentViewFromUrl(String url) {
        final Pattern pattern = Pattern.compile(getPrefix() + URLPattern);
        Matcher m = pattern.matcher(url);
        if (m.matches()) {
            if (m.groupCount() >= 4) {

                // for debug
                // for (int i = 1; i < m.groupCount() + 1; i++) {
                // System.err.println(i + ": " + m.group(i));
                // }

                final String server = m.group(1);
                String uuid = m.group(2);
                final DocumentRef docRef = new IdRef(uuid);

                // get other parameters

                Map<String, String> params = new HashMap<>();
                if (m.groupCount() >= 4) {
                    String filePropertyPath = m.group(4);
                    params.put(FILE_PROPERTY_PATH_KEY, filePropertyPath);
                }

                if (m.groupCount() >= 6) {
                    String filename = m.group(6);
                    try {
                        filename = URLDecoder.decode(filename, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        filename = StringUtils.toAscii(filename);
                    }
                    int jsessionidIndex = filename.indexOf(";jsessionid");
                    if (jsessionidIndex != -1) {
                        filename = filename.substring(0, jsessionidIndex);
                    }
                    params.put(FILENAME_KEY, filename);
                }

                if (m.groupCount() >= 8) {
                    String query = m.group(8);
                    Map<String, String> requestParams = URIUtils.getRequestParameters(query);
                    if (requestParams != null) {
                        params.putAll(requestParams);
                    }
                }

                final DocumentLocation docLoc = new DocumentLocationImpl(server, docRef);

                return new DocumentViewImpl(docLoc, null, params);
            }
        }

        return null;
    }

    public static String getFilename(DocumentModel doc, DocumentView docView) {
        String filename = docView.getParameter(FILENAME_KEY);
        if (filename == null) {
            // try to get it from document
            String propertyPath = docView.getParameter(FILE_PROPERTY_PATH_KEY);
            String propertyName = DocumentModelUtils.decodePropertyName(propertyPath);
            if (propertyName != null) {
                filename = (String) DocumentModelUtils.getPropertyValue(doc, propertyName + "/filename");
            }
        }
        return filename;
    }

}
