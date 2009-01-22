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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.rest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.url.DocumentLocationImpl;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentLocation;
import org.nuxeo.ecm.platform.url.api.DocumentView;

/**
 * TODO: document me.
 *
 * @author tiry
 */
@Deprecated
// see codec
public final class FancyURLMapper {

    // server/Path/view_id/tab_id/subURI
    private static final String URL_PATTERN = FancyURLConfig.FANCY_URL_PREFIX
            + "/(\\w*)/([a-zA-Z_0-9\\-]*)/([a-zA-Z_0-9\\-\\.]*)/([a-zA-Z_0-9\\-\\.]*)(/)?(.*)?";

    private static final Pattern PATTERN = Pattern.compile(URL_PATTERN);

    // Utility class.
    private FancyURLMapper() {
    }

    /**
     * Extracts document location from a Zope-style URL ie :
     * server/path_or_docId/view_id/tab_id .
     */
    public static DocumentView extractParametersFromURL(String url) {

        Matcher m = PATTERN.matcher(url);

        boolean b = m.matches();
        if (b) {
            if (m.groupCount() >= 4) {
                final String server = m.group(1);
                String uuid = m.group(2);
                final DocumentRef docRef = new IdRef(uuid);

                final String viewId = m.group(3);
                final String tab = m.group(4);

                String subURITmp;
                if (m.groupCount() == 6) {
                    subURITmp = m.group(6);
                } else {
                    subURITmp = null;
                }

                String subURI = subURITmp;
                if (subURI != null) {
                    subURI = subURI.trim();
                    if (subURI.length() == 0) {
                        subURI = null;
                    }
                }

                final DocumentLocation docLoc = new DocumentLocationImpl(
                        server, docRef);

                return new DocumentViewImpl(docLoc, viewId, tab, subURI);
            }
        }

        return null;
    }

    /**
     * Generates a Zope-like URL from a DocumentView.
     */
    public static String getFancyURL(DocumentView docView) {
        String url = FancyURLConfig.FANCY_URL_PREFIX + "/"
                + docView.getDocumentLocation().getServerLocationName() + "/"
                + docView.getDocumentLocation().getDocRef().toString() + "/"
                + docView.getViewId() + "/" + docView.getTabId() + "/";
        String subURI = docView.getSubURI();
        if (subURI != null && subURI.length() > 0) {
            if (subURI.endsWith("&")) {
                subURI = subURI.substring(0, subURI.length() - 1);
            }
            url = url + "?" + subURI;
        }
        return url;
    }

    public static String getFancyURL(DocumentLocation docLoc, String viewId,
            String tabId, String subURI) {
        DocumentView docView = new DocumentViewImpl(docLoc, viewId, tabId,
                subURI);
        return getFancyURL(docView);
    }

    /**
     * Converts a GET URL to a Zope-like URL.
     */
    public static String convertToFancyURL(String url) {
        String serverName = "";
        String docId = "";
        String viewId;
        String tabId = "";
        String subQS = "";
        if (!url.contains("?")) {
            return url;
        }
        String qs = url.split("\\?")[1];
        String[] params = qs.split("&");
        if (params.length == 0) {
            return url;
        }
        int nbParams = 0;
        for (String param : params) {
            String[] k = param.split("=");
            if (k.length != 2) {
                continue;
            }
            if (k[0].equals(FancyURLConfig.GET_URL_Server_Param)) {
                serverName = k[1];
                nbParams++;
                continue;
            } else if (k[0].equals(FancyURLConfig.GET_URL_Doc_Param)) {
                docId = k[1];
                nbParams++;
                continue;
            } else if (k[0].equals(FancyURLConfig.GET_URL_Tab_Param)) {
                tabId = k[1];
                nbParams++;
                continue;
            }
            subQS = subQS + param + "&";
        }

        if (nbParams < 3) {
            return url;
        }

        String[] viewPath = url.split("\\?")[0].split("/");
        viewId = viewPath[viewPath.length - 1];
        String baseURL = url.split(viewId)[0];
        baseURL = baseURL.substring(0, baseURL.length() - 1);
        if (viewId.contains(".")) {
            viewId = (viewId.split("\\."))[0];
        }

        final String server = serverName;
        final String idRef = docId;

        final DocumentLocation docLoc = new DocumentLocationImpl(server,
                new IdRef(docId));

        return baseURL + getFancyURL(docLoc, viewId, tabId, subQS);
    }

}
