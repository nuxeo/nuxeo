/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer
 */

package org.nuxeo.ecm.diff.content;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.DocumentBlobHolder;
import org.nuxeo.ecm.core.api.blobholder.DocumentStringBlobHolder;
import org.nuxeo.ecm.diff.content.adapter.ContentDiffAdapterManager;
import org.nuxeo.ecm.diff.content.adapter.MimeTypeContentDiffer;
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

    public static final String DEFAULT_XPATH = "default";

    /**
     * Final class constructor.
     */
    private ContentDiffHelper() {
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
    public static String getContentDiffURL(DocumentModel leftDoc, DocumentModel rightDoc, String conversionType,
            String locale) {

        return getContentDiffURL(leftDoc.getRepositoryName(), leftDoc, rightDoc, DEFAULT_XPATH, conversionType, locale);
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
    public static String getContentDiffURL(DocumentModel leftDoc, DocumentModel rightDoc, String propertyXPath,
            String conversionType, String locale) {

        return getContentDiffURL(leftDoc.getRepositoryName(), leftDoc, rightDoc, propertyXPath, conversionType, locale);
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
    public static String getContentDiffURL(String repositoryName, DocumentModel leftDoc, DocumentModel rightDoc,
            String propertyXPath, String conversionType, String locale) {

        if (propertyXPath == null) {
            propertyXPath = DEFAULT_XPATH;
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
     * Checks if the HTML conversion content diff is relevant for the specified property, i.e. if its mime type is not
     * blacklisted.
     * <p>
     * For now, the list of blacklisted mime types for HTML conversion are:
     * <ul>
     * <li>PDF</li>
     * <li>Office spreadsheet mime types</li>
     * <li>Office presentation mime types</li>
     * </ul>
     * This is configurable with the {@code htmlConversionBlacklistedMimeTypes} extension point, see
     * <a href="https://jira.nuxeo.com/browse/NXP-25208">NXP-25208</a>.
     *
     * @see <a href="https://jira.nuxeo.com/browse/NXP-9421">NXP-9421</a>
     * @see <a href="https://jira.nuxeo.com/browse/NXP-9431">NXP-9431</a>
     */
    public static boolean isDisplayHtmlConversion(Serializable property) {

        // Always relevant except for the blacklisted mime types
        if (isContentProperty(property)) {
            Blob blob = (Blob) property;
            String mimeType = blob.getMimeType();
            if (Framework.getService(ContentDiffAdapterManager.class)
                         .getHtmlConversionBlacklistedMimeTypes()
                         .contains(mimeType)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the text conversion content diff is relevant for the specified property.
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

        ContentDiffAdapterManager contentDiffAdapterManager = Framework.getService(ContentDiffAdapterManager.class);
        MimeTypeContentDiffer mimeTypeContentDiffer = contentDiffAdapterManager.getContentDiffer(mimeType);

        if (mimeTypeContentDiffer != null) {
            return false;
        }
        return true;
    }

    /**
     * Checks if the specified property is a content property, ie. {@code instanceof Blob}.
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
     * @see <a href="https://jira.nuxeo.com/browse/NXP-9421">NXP-9421</a>
     * @see <a href="https://jira.nuxeo.com/browse/NXP-9431">NXP-9431</a>
     * @deprecated since 10.10
     */
    @Deprecated
    protected static List<String> getHtmlConversionBlackListedMimeTypes() {

        List<String> blackListedMimeTypes = new ArrayList<>();

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

    public static BlobHolder getBlobHolder(DocumentModel doc, String xPath) throws ContentDiffException {
        // TODO: manage other property types than Blob / String?
        Serializable prop = doc.getPropertyValue(xPath);
        if (prop instanceof Blob) {
            return new DocumentBlobHolder(doc, xPath);
        }
        if (prop instanceof String) {
            // Default mime type is text/plain. For a Note, use the
            // "note:mime_type" property, otherwise if the property value is
            // HTML use text/html.
            String mimeType = "text/plain";
            if ("note:note".equals(xPath)) {
                mimeType = (String) doc.getPropertyValue("note:mime_type");
            } else {
                if (HtmlGuesser.isHtml((String) prop)) {
                    mimeType = "text/html";
                }
            }
            return new DocumentStringBlobHolder(doc, xPath, mimeType);
        }
        throw new ContentDiffException(
                String.format("Cannot get BlobHolder for doc '%s' and xpath '%s'.", doc.getTitle(), xPath));
    }
}
