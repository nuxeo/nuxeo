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
 *     troger
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.preview.codec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.nuxeo.common.utils.StringUtils;
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

    public DocumentView getDocumentViewFromUrl(String url) {
        final Pattern pattern = Pattern.compile(getPrefix() + URLPattern);
        Matcher m = pattern.matcher(url);
        if (m.matches()) {
            final String server = m.group(1);
            String uuid = m.group(2);
            final DocumentRef docRef = new IdRef(uuid);

            Map<String, String> params = new HashMap<String, String>();
            final String property = m.group(3);
            params.put(PROPERTY_PATH_KEY, property);

            final DocumentLocation docLoc = new DocumentLocationImpl(server,
                    docRef);
            final DocumentView docView = new DocumentViewImpl(docLoc, null,
                    params);

            return docView;
        }
        return null;
    }

    public String getUrlFromDocumentView(DocumentView docView) {
        DocumentLocation docLoc = docView.getDocumentLocation();
        String property = docView.getParameter(PROPERTY_PATH_KEY);
        if (docLoc != null) {
            List<String> items = new ArrayList<String>();
            items.add(getPrefix());
            items.add(docLoc.getServerName());
            items.add(docLoc.getDocRef().toString());
            items.add(property);
            String uri = StringUtils.join(items, "/");
            uri += '/';

            Map<String, String> requestParams = new HashMap<String, String>(
                    docView.getParameters());
            requestParams.remove(PROPERTY_PATH_KEY);
            return URIUtils.addParametersToURIQuery(uri, requestParams);
        }
        return null;
    }

}
