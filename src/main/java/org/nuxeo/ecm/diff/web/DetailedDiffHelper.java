/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Antoine Taillefer
 */

package org.nuxeo.ecm.diff.web;

import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.platform.ui.web.rest.RestHelper;
import org.nuxeo.ecm.platform.ui.web.rest.api.URLPolicyService;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.runtime.api.Framework;

/**
 * Helper for detailed diff.
 */
public final class DetailedDiffHelper {

    private static final String DETAILED_DIFF_FANCYBOX_VIEW = "detailed_diff_fancybox";

    private static final String SCHEMA_URL_PARAM_NAME = "schemaName";

    private static final String FIELD_URL_PARAM_NAME = "fieldName";

    private static final String DETAILED_DIFF_URL_PREFIX = "restAPI/detailedDiff/";

    public static final String DETAILED_DIFF_URL_DEFAULT_XPATH = "default";

    /**
     * Final class constructor.
     */
    private DetailedDiffHelper() {
    }

    /**
     * Gets the detailed diff fancy box url.
     *
     * @param currentDoc the current doc
     * @param schemaName the schema name
     * @param fieldName the field name
     * @return the detailed diff fancy box url
     */
    public static String getDetailedDiffFancyBoxURL(DocumentModel currentDoc,
            String schemaName, String fieldName) {

        DocumentLocation docLocation = new DocumentLocationImpl(
                currentDoc.getRepositoryName(), currentDoc.getRef());
        DocumentView docView = new DocumentViewImpl(docLocation,
                DETAILED_DIFF_FANCYBOX_VIEW);
        URLPolicyService urlPolicyService = Framework.getLocalService(URLPolicyService.class);
        StringBuilder urlSb = new StringBuilder(
                urlPolicyService.getUrlFromDocumentView(docView, null));
        urlSb.append("?");
        urlSb.append(SCHEMA_URL_PARAM_NAME);
        urlSb.append("=");
        urlSb.append(schemaName);
        urlSb.append("&");
        urlSb.append(FIELD_URL_PARAM_NAME);
        urlSb.append("=");
        urlSb.append(fieldName);
        return VirtualHostHelper.getContextPathProperty() + "/"
                + RestHelper.addCurrentConversationParameters(urlSb.toString());
    }

    /**
     * Gets the detailed diff url.
     *
     * @param leftDoc the left doc
     * @param rightDoc the right doc
     * @return the detailed diff url
     */
    public static String getDetailedDiffURL(DocumentModel leftDoc,
            DocumentModel rightDoc) {

        return getDetailedDiffURL(leftDoc.getRepositoryName(), leftDoc,
                rightDoc, DETAILED_DIFF_URL_DEFAULT_XPATH);
    }

    /**
     * Gets the detailed diff url.
     *
     * @param leftDoc the left doc
     * @param rightDoc the right doc
     * @param xpath the xpath
     * @return the detailed diff url
     */
    public static String getDetailedDiffURL(DocumentModel leftDoc,
            DocumentModel rightDoc, String xpath) {

        return getDetailedDiffURL(leftDoc.getRepositoryName(), leftDoc,
                rightDoc, xpath);
    }

    /**
     * Gets the detailed diff url.
     *
     * @param repositoryName the repository name
     * @param leftDoc the left doc
     * @param rightDoc the right doc
     * @param xpath the xpath
     * @return the detailed diff url
     */
    public static String getDetailedDiffURL(String repositoryName,
            DocumentModel leftDoc, DocumentModel rightDoc, String xpath) {

        if (xpath == null) {
            xpath = DETAILED_DIFF_URL_DEFAULT_XPATH;
        }

        StringBuilder sb = new StringBuilder();

        sb.append(DETAILED_DIFF_URL_PREFIX);
        sb.append(repositoryName);
        sb.append("/");
        sb.append(leftDoc.getId());
        sb.append("/");
        sb.append(rightDoc.getId());
        sb.append("/");
        sb.append(xpath);
        sb.append("/");

        return sb.toString();
    }
}
