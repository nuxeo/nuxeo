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

package org.nuxeo.ecm.diff.content;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.diff.content.adapter.ContentDiffAdapterManager;
import org.nuxeo.ecm.diff.content.adapter.MimeTypeContentDiffer;
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

    public static final String CONTENT_DIFF_FANCYBOX_VIEW = "content_diff_fancybox";

    public static final String LABEL_URL_PARAM_NAME = "label";

    public static final String XPATH_URL_PARAM_NAME = "xPath";

    public static final String CONVERSION_TYPE_URL_PARAM_NAME = "conversionType";

    public static final String LOCALE_URL_PARAM_NAME = "locale";

    public static final String CONTENT_DIFF_URL_PREFIX = "restAPI/contentDiff/";

    public static final String CONTENT_DIFF_URL_DEFAULT_XPATH = "default";

    /**
     * Final class constructor.
     */
    private ContentDiffHelper() {
    }

    /**
     * Gets the content diff fancy box URL.
     *
     * @param currentDoc the current doc
     * @param propertyLabel the property label
     * @param propertyXPath the property xpath
     * @param conversionType the conversion type
     * @return the content diff fancy box URL
     * @throws ClientException if URL cannot be retrieved from current doc view
     */
    public static String getContentDiffFancyBoxURL(DocumentModel currentDoc,
            String propertyLabel, String propertyXPath, String conversionType)
            throws ClientException {

        DocumentLocation docLocation = new DocumentLocationImpl(
                currentDoc.getRepositoryName(), currentDoc.getRef());
        DocumentView docView = new DocumentViewImpl(docLocation,
                CONTENT_DIFF_FANCYBOX_VIEW);
        URLPolicyService urlPolicyService = Framework.getLocalService(URLPolicyService.class);
        String docUrl = urlPolicyService.getUrlFromDocumentView(docView, null);
        if (docUrl == null) {
            throw new ClientException(
                    "Cannot get URL from document view, probably because of a missing urlPattern contribution.");
        }
        StringBuilder urlSb = new StringBuilder(docUrl);
        urlSb.append("?");
        urlSb.append(LABEL_URL_PARAM_NAME);
        urlSb.append("=");
        urlSb.append(propertyLabel);
        urlSb.append("&");
        urlSb.append(XPATH_URL_PARAM_NAME);
        urlSb.append("=");
        urlSb.append(propertyXPath);
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
     * Gets the content diff URL.
     *
     * @param leftDoc the left doc
     * @param rightDoc the right doc
     * @param conversionType the conversion type
     * @param locale the locale
     * @return the content diff URL
     */
    public static String getContentDiffURL(DocumentModel leftDoc,
            DocumentModel rightDoc, String conversionType, String locale) {

        return getContentDiffURL(leftDoc.getRepositoryName(), leftDoc,
                rightDoc, CONTENT_DIFF_URL_DEFAULT_XPATH, conversionType,
                locale);
    }

    /**
     * Gets the content diff URL.
     *
     * @param leftDoc the left doc
     * @param rightDoc the right doc
     * @param propertyXPath the property xpath
     * @param conversionType the conversion type
     * @param locale the locale
     * @return the content diff URL
     */
    public static String getContentDiffURL(DocumentModel leftDoc,
            DocumentModel rightDoc, String propertyXPath,
            String conversionType, String locale) {

        return getContentDiffURL(leftDoc.getRepositoryName(), leftDoc,
                rightDoc, propertyXPath, conversionType, locale);
    }

    /**
     * Gets the content diff URL.
     *
     * @param repositoryName the repository name
     * @param leftDoc the left doc
     * @param rightDoc the right doc
     * @param propertyXPath the xpath
     * @param conversionType the conversion type
     * @param locale the locale
     * @return the content diff URL
     */
    public static String getContentDiffURL(String repositoryName,
            DocumentModel leftDoc, DocumentModel rightDoc,
            String propertyXPath, String conversionType, String locale) {

        if (propertyXPath == null) {
            propertyXPath = CONTENT_DIFF_URL_DEFAULT_XPATH;
        }

        StringBuilder sb = new StringBuilder();

        sb.append(CONTENT_DIFF_URL_PREFIX);
        sb.append(repositoryName);
        sb.append("/");
        sb.append(leftDoc.getId());
        sb.append("/");
        sb.append(rightDoc.getId());
        sb.append("/");
        sb.append(propertyXPath);
        sb.append("/");
        boolean isQueryParam = false;
        if (!StringUtils.isEmpty(conversionType)) {
            sb.append("?");
            sb.append(CONVERSION_TYPE_URL_PARAM_NAME);
            sb.append("=");
            sb.append(conversionType);
            isQueryParam = true;
        }
        if (!StringUtils.isEmpty(locale)) {
            sb.append(isQueryParam ? "&" : "?");
            sb.append(LOCALE_URL_PARAM_NAME);
            sb.append("=");
            sb.append(locale);
            isQueryParam = true;
        }

        return sb.toString();
    }

    /**
     * Checks if the HTML conversion content diff is relevant for the specified
     * property.
     */
    public static boolean isDisplayHtmlConversion(Serializable property) {

        // Always relevant except for the blacklisted mime types
        if (isContentProperty(property)) {
            Blob blob = (Blob) property;
            String mimeType = blob.getMimeType();
            if (getHtmlConversionBlackListedMimeTypes().contains(mimeType)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the text conversion content diff is relevant for the specified
     * property.
     */
    public static boolean isDisplayTextConversion(Serializable property) {

        // Must be a content property
        if (!isContentProperty(property)) {
            return false;
        }
        // Not relevant for the mime types associated to a content differ (see
        // the mimeTypeContentDiffer extension point)
        Blob blob = (Blob) property;
        String mimeType = blob.getMimeType();

        ContentDiffAdapterManager contentDiffAdapterManager = Framework.getLocalService(ContentDiffAdapterManager.class);
        MimeTypeContentDiffer mimeTypeContentDiffer = contentDiffAdapterManager.getContentDiffer(mimeType);

        if (mimeTypeContentDiffer != null) {
            return false;
        }
        return true;
    }

    /**
     * Checks if the specified property is a content property, ie.
     * {@code instanceof Blob}.
     */
    public static boolean isContentProperty(Serializable property) {
        return property instanceof Blob;
    }

    /**
     * Gets the list of blacklisted mime types for HTML conversion.
     * <p>
     * For now:
     * <ul>
     * <li>PDF</li>
     * <li>Office spreadsheet mime types</li>
     * <li>Office presentation mime types</li>
     * </ul>
     * </p>
     *
     * @see https://jira.nuxeo.com/browse/NXP-9421
     * @see https://jira.nuxeo.com/browse/NXP-9431
     */
    protected static List<String> getHtmlConversionBlackListedMimeTypes() {

        List<String> blackListedMimeTypes = new ArrayList<String>();

        // PDF
        blackListedMimeTypes.add("application/pdf");

        // Office spreadsheet
        blackListedMimeTypes.add("application/vnd.ms-excel");
        blackListedMimeTypes.add("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        blackListedMimeTypes.add("application/vnd.sun.xml.calc");
        blackListedMimeTypes.add("application/vnd.sun.xml.calc.template");
        blackListedMimeTypes.add("application/vnd.oasis.opendocument.spreadsheet");
        blackListedMimeTypes.add("application/vnd.oasis.opendocument.spreadsheet-template");

        // Office presentation
        blackListedMimeTypes.add("application/vnd.ms-powerpoint");
        blackListedMimeTypes.add("application/vnd.openxmlformats-officedocument.presentationml.presentation");
        blackListedMimeTypes.add("application/vnd.sun.xml.impress");
        blackListedMimeTypes.add("application/vnd.sun.xml.impress.template");
        blackListedMimeTypes.add("application/vnd.oasis.opendocument.presentation");
        blackListedMimeTypes.add("application/vnd.oasis.opendocument.presentation-template");

        return blackListedMimeTypes;
    }
}
