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

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.diff.content.adapter.base.ContentDiffConversionType;
import org.nuxeo.ecm.platform.ui.web.rest.RestHelper;
import org.nuxeo.ecm.platform.ui.web.rest.api.URLPolicyService;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.runtime.api.Framework;

/**
 * Helper for content diff.
 */
public final class ContentDiffHelper {

    private static final String CONTENT_DIFF_FANCYBOX_VIEW = "content_diff_fancybox";

    private static final String SCHEMA_URL_PARAM_NAME = "schemaName";

    private static final String FIELD_URL_PARAM_NAME = "fieldName";

    private static final String CONVERSION_TYPE_URL_PARAM_NAME = "conversionType";

    private static final String CONTENT_DIFF_URL_PREFIX = "restAPI/contentDiff/";

    public static final String CONTENT_DIFF_URL_DEFAULT_XPATH = "default";

    /**
     * Final class constructor.
     */
    private ContentDiffHelper() {
    }

    /**
     * Gets the content diff fancy box url.
     *
     * @param currentDoc the current doc
     * @param schemaName the schema name
     * @param fieldName the field name
     * @param conversionType the conversion type
     * @return the content diff fancy box url
     */
    public static String getContentDiffFancyBoxURL(DocumentModel currentDoc,
            String schemaName, String fieldName, String conversionType) {

        DocumentLocation docLocation = new DocumentLocationImpl(
                currentDoc.getRepositoryName(), currentDoc.getRef());
        DocumentView docView = new DocumentViewImpl(docLocation,
                CONTENT_DIFF_FANCYBOX_VIEW);
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
        if (!StringUtils.isEmpty(conversionType)) {
            urlSb.append("&");
            urlSb.append(CONVERSION_TYPE_URL_PARAM_NAME);
            urlSb.append("=");
            urlSb.append(conversionType);
        }
        return VirtualHostHelper.getContextPathProperty() + "/"
                + RestHelper.addCurrentConversationParameters(urlSb.toString());
    }

    /**
     * Gets the content diff url.
     *
     * @param leftDoc the left doc
     * @param rightDoc the right doc
     * @return the content diff url
     */
    public static String getContentDiffURL(DocumentModel leftDoc,
            DocumentModel rightDoc, ContentDiffConversionType conversionType) {

        return getContentDiffURL(leftDoc.getRepositoryName(), leftDoc,
                rightDoc, CONTENT_DIFF_URL_DEFAULT_XPATH, conversionType);
    }

    /**
     * Gets the content diff url.
     *
     * @param leftDoc the left doc
     * @param rightDoc the right doc
     * @param xpath the xpath
     * @param conversionType the conversion type
     * @return the content diff url
     */
    public static String getContentDiffURL(DocumentModel leftDoc,
            DocumentModel rightDoc, String xpath,
            ContentDiffConversionType conversionType) {

        return getContentDiffURL(leftDoc.getRepositoryName(), leftDoc,
                rightDoc, xpath, conversionType);
    }

    /**
     * Gets the content diff url.
     *
     * @param repositoryName the repository name
     * @param leftDoc the left doc
     * @param rightDoc the right doc
     * @param xpath the xpath
     * @param conversionType the conversion type
     * @return the content diff url
     */
    public static String getContentDiffURL(String repositoryName,
            DocumentModel leftDoc, DocumentModel rightDoc, String xpath,
            ContentDiffConversionType conversionType) {

        if (xpath == null) {
            xpath = CONTENT_DIFF_URL_DEFAULT_XPATH;
        }

        StringBuilder sb = new StringBuilder();

        sb.append(CONTENT_DIFF_URL_PREFIX);
        sb.append(repositoryName);
        sb.append("/");
        sb.append(leftDoc.getId());
        sb.append("/");
        sb.append(rightDoc.getId());
        sb.append("/");
        sb.append(xpath);
        sb.append("/");
        if (conversionType != null) {
            sb.append(conversionType.name());
            sb.append("/");
        }

        return sb.toString();
    }
}
