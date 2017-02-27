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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.ecm.platform.web.common;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.api.DocumentViewCodecManager;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.runtime.api.Framework;

/**
 * Helper for the banner to open a document in the mobile application.
 *
 * @since 9.1
 */
public class MobileBannerHelper {

    public static final String PROTOCOL_PROPERTY = "nuxeo.mobile.application.protocol";

    public static final String ANDROID_PACKAGE_PROPERTY = "nuxeo.mobile.application.android.package";

    public static final String ITUNES_ID_PROPERTY = "nuxeo.mobile.application.iTunesId";

    public static final String ANDROID_PROTOCOL_SCHEME = "android-app";

    public static final String ITUNES_URL = "https://itunes.apple.com/app/";

    /**
     * Returns a full URL opening the Android mobile application.
     */
    public static String getURLForAndroidApplication(HttpServletRequest request) {
        return getURLForAndroidApplication(request, null);
    }

    /**
     * Returns a full URL opening the Android mobile application for the given document.
     */
    public static String getURLForAndroidApplication(HttpServletRequest request, DocumentModel doc) {
        return getURLForMobileApplication(getAndroidProtocol(), request, doc);
    }

    /**
     * Returns a full URL opening the iOS mobile application.
     */
    public static String getURLForIOSApplication(HttpServletRequest request) {
        return getURLForIOSApplication(request, null);
    }

    /**
     * Returns a full URL opening the iOS mobile application for the given document.
     */
    public static String getURLForIOSApplication(HttpServletRequest request, DocumentModel doc) {
        return getURLForMobileApplication(getIOSProtocol(), request, doc);
    }

    /**
     * Returns the URL of the iOS mobile application in the App Store.
     */
    public static String getAppStoreURL() {
        return ITUNES_URL + Framework.getProperty(ITUNES_ID_PROPERTY);
    }

    public static String getAndroidProtocol() {
        return String.format("%s://%s/%s/", ANDROID_PROTOCOL_SCHEME, Framework.getProperty(ANDROID_PACKAGE_PROPERTY),
                Framework.getProperty(PROTOCOL_PROPERTY));
    }

    public static String getIOSProtocol() {
        return String.format("%s://", Framework.getProperty(PROTOCOL_PROPERTY));
    }

    public static String getURLForMobileApplication(String protocol, HttpServletRequest request, DocumentModel doc) {
        String baseURL = VirtualHostHelper.getBaseURL(request);
        String requestedURL = request.getParameter("requestedUrl");
        return getURLForMobileApplication(protocol, baseURL, doc, requestedURL);
    }

    public static String getURLForMobileApplication(String protocol, String baseURL, DocumentModel doc,
            String requestedURL) {
        String url = protocol + getServerPart(baseURL);
        if (doc != null) {
            return url + getDocumentPart(doc);
        }
        if (StringUtils.isNotBlank(requestedURL)) {
            return url + getDocumentPart(requestedURL);
        }
        return url;
    }

    protected static String getServerPart(String baseURL) {
        if (!baseURL.endsWith("/")) {
            baseURL += "/";
        }
        return baseURL.replaceAll("://", "/");
    }

    protected static String getDocumentPart(DocumentModel doc) {
        return doc.getRepositoryName() + "/id/" + doc.getId();
    }

    protected static String getDocumentPart(String requestedURL) {
        String docPart = "";
        DocumentViewCodecManager documentViewCodecManager = Framework.getService(DocumentViewCodecManager.class);
        DocumentView docView = documentViewCodecManager.getDocumentViewFromUrl(requestedURL, false, null);
        if (docView != null) {
            DocumentLocation docLoc = docView.getDocumentLocation();
            if (docLoc != null) {
                String serverName = docLoc.getServerName();
                if (serverName != null) {
                    docPart += serverName;
                    IdRef idRef = docLoc.getIdRef();
                    PathRef pathRef = docLoc.getPathRef();
                    if (idRef != null) {
                        docPart += "/id/" + idRef;
                    } else if (pathRef != null) {
                        docPart += "/path" + pathRef;
                    }
                }
            }
        }
        return docPart;
    }

}
