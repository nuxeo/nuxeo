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

package org.nuxeo.ecm.diff.content.restlet;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.convert.api.ConverterNotRegistered;
import org.nuxeo.ecm.core.io.download.DownloadService;
import org.nuxeo.ecm.diff.content.ContentDiffAdapter;
import org.nuxeo.ecm.diff.content.ContentDiffHelper;
import org.nuxeo.ecm.diff.content.adapter.base.ContentDiffConversionType;
import org.nuxeo.ecm.platform.ui.web.restAPI.BaseNuxeoRestlet;
import org.nuxeo.runtime.api.Framework;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

/**
 * Restlet to retrieve the content diff of a given property between two documents.
 *
 * @author Antoine Taillefer
 * @since 5.6
 */
public class ContentDiffRestlet extends BaseNuxeoRestlet {

    private static final Log log = LogFactory.getLog(ContentDiffRestlet.class);

    protected Locale locale;

    protected DocumentModel leftDoc;

    protected DocumentModel rightDoc;

    @Override
    public void handle(Request req, Response res) {
        HttpServletResponse response = getHttpResponse(res);
        HttpServletRequest request = getHttpRequest(req);

        String repo = (String) req.getAttributes().get("repo");
        String leftDocId = (String) req.getAttributes().get("leftDocId");
        String rightDocId = (String) req.getAttributes().get("rightDocId");
        String xpath = (String) req.getAttributes().get("fieldXPath");
        xpath = xpath.replace("--", "/");

        // Get subPath for other content diff blobs, such as images
        List<String> segments = req.getResourceRef().getSegments();
        StringBuilder sb = new StringBuilder();
        int pos = segments.indexOf("restAPI") + 6;
        for (int i = pos; i < segments.size(); i++) {
            sb.append(segments.get(i));
            sb.append("/");
        }
        String subPath = sb.substring(0, sb.length() - 1);

        // Check conversion type param, default is html.
        String conversionTypeParam = getQueryParamValue(req, ContentDiffHelper.CONVERSION_TYPE_URL_PARAM_NAME,
                ContentDiffConversionType.html.name());
        ContentDiffConversionType conversionType = ContentDiffConversionType.valueOf(conversionTypeParam);

        // Check locale
        String localeParam = getQueryParamValue(req, ContentDiffHelper.LOCALE_URL_PARAM_NAME, null);
        locale = isBlank(localeParam) ? Locale.getDefault() : LocaleUtils.toLocale(localeParam);

        try {
            xpath = URLDecoder.decode(xpath, "UTF-8");
            subPath = URLDecoder.decode(subPath, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.error(e);
        }

        if (repo == null || repo.equals("*")) {
            handleError(res, "You must specify a repository.");
            return;
        }
        if (leftDocId == null || leftDocId.equals("*")) {
            handleError(res, "You must specify a left document id.");
            return;
        }
        if (rightDocId == null || rightDocId.equals("*")) {
            handleError(res, "You must specify a right document id.");
            return;
        }
        try (CloseableCoreSession documentManager = CoreInstance.openCoreSession(repo)) {
            leftDoc = documentManager.getDocument(new IdRef(leftDocId));
            rightDoc = documentManager.getDocument(new IdRef(rightDocId));

        List<Blob> contentDiffBlobs = initCachedContentDiffBlobs(res, xpath, conversionType);
        if (CollectionUtils.isEmpty(contentDiffBlobs)) {
            // Response was already handled by initCachedContentDiffBlobs
            return;
        }

        // find blob
        Blob blob = null;
        if (StringUtils.isEmpty(subPath)) {
            blob = contentDiffBlobs.get(0);
            blob.setMimeType("text/html");
        } else {
            for (Blob b : contentDiffBlobs) {
                if (subPath.equals(b.getFilename())) {
                    blob = b;
                    break;
                }
            }
        }
        if (blob == null) {
            res.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
            return;
        }

        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Pragma", "no-cache");

        String reason = "contentDiff";
        final Blob fblob = blob;
        Boolean inline = Boolean.TRUE;
        Map<String, Serializable> extendedInfos = new HashMap<>();
        extendedInfos.put("subPath", subPath);
        extendedInfos.put("leftDocId", leftDocId);
        extendedInfos.put("rightDocId", rightDocId);
        // check permission on right doc (downloadBlob will check on the left one)
        DownloadService downloadService = Framework.getService(DownloadService.class);
        if (!downloadService.checkPermission(rightDoc, xpath, blob, reason, extendedInfos)) {
            res.setStatus(Status.CLIENT_ERROR_FORBIDDEN);
            return;
        }

            downloadService.downloadBlob(request, response, leftDoc, xpath, blob, blob.getFilename(), reason,
                    extendedInfos, inline, byteRange -> setEntityToBlobOutput(fblob, byteRange, res));
        } catch (NuxeoException | IOException e) {
            handleError(res, e);
        }
    }

    private List<Blob> initCachedContentDiffBlobs(Response res, String xpath,
            ContentDiffConversionType conversionType) {

        ContentDiffAdapter contentDiffAdapter = leftDoc.getAdapter(ContentDiffAdapter.class);

        if (contentDiffAdapter == null) {
            handleNoContentDiff(res, xpath, null);
            return null;
        }

        List<Blob> contentDiffBlobs;
        try {
            if (xpath.equals(ContentDiffHelper.DEFAULT_XPATH)) {
                contentDiffBlobs = contentDiffAdapter.getFileContentDiffBlobs(rightDoc, conversionType, locale);
            } else {
                contentDiffBlobs = contentDiffAdapter.getFileContentDiffBlobs(rightDoc, xpath, conversionType, locale);
            }
        } catch (NuxeoException ce) {
            handleNoContentDiff(res, xpath, ce);
            return null;
        }

        if (CollectionUtils.isEmpty(contentDiffBlobs)) {
            handleNoContentDiff(res, xpath, null);
            return null;
        }
        return contentDiffBlobs;
    }

    protected void handleNoContentDiff(Response res, String xpath, NuxeoException e) {
        StringBuilder sb = new StringBuilder();

        sb.append("<html><body><center><h1>");
        if (e == null) {
            sb.append("No content diff is available for these documents</h1>");
        } else {
            sb.append("Content diff can not be generated for these documents</h1>");
            sb.append("<pre>Blob path: ");
            sb.append(xpath);
            sb.append("</pre>");
            sb.append("<pre>");
            if (e instanceof ConverterNotRegistered) {
                sb.append(e.getMessage());
            } else {
                sb.append(e.toString());
            }
            sb.append("</pre>");
        }

        sb.append("</center></body></html>");
        log.error("Could not build content diff for missing blob at " + xpath, e);

        res.setEntity(sb.toString(), MediaType.TEXT_HTML);
        HttpServletResponse response = getHttpResponse(res);

        response.setHeader("Content-Disposition", "inline");
    }

}
