/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thomas Roger
 */

package org.nuxeo.dam.codec;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.codec.DocumentIdCodec;
import org.nuxeo.ecm.platform.url.codec.DocumentPathCodec;
import org.nuxeo.ecm.platform.url.service.AbstractDocumentViewCodec;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @since 5.7
 */
public class DamCodec extends DocumentPathCodec {

    private static final Log log = LogFactory.getLog(DamCodec.class);

    public static final String PREFIX = "nxdam";

    @Override
    public String getPrefix() {
        if (prefix != null) {
            return prefix;
        }
        return PREFIX;
    }

    @Override
    public DocumentView getDocumentViewFromUrl(String url) {
        DocumentView docView = super.getDocumentViewFromUrl(url);
        if (docView != null) {
            docView.setViewId("assets");
            return docView;
        }
        return null;
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
            List<String> items = new ArrayList<String>();
            items.add(getPrefix());
            items.add(docLoc.getServerName());
            PathRef docRef = docLoc.getPathRef();

            // TODO make it generic in DocumentPathCodec (or other codec) to create URLs even without a document
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
            String uri = StringUtils.join(items, "/");
            String viewId = docView.getViewId();
            if (viewId != null) {
                uri += "@" + viewId;
            }

            String uriWithParam = URIUtils.addParametersToURIQuery(uri,
                    docView.getParameters());

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
     * Never handle document views: this codec is useless on post requests
     */
    @Override
    public boolean handleDocumentView(DocumentView docView) {
        return false;
    }

}
