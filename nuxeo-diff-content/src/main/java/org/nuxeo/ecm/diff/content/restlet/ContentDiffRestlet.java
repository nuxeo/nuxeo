/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Antoine Taillefer
 */

package org.nuxeo.ecm.diff.content.restlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.international.LocaleSelector;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.convert.api.ConverterNotRegistered;
import org.nuxeo.ecm.diff.content.ContentDiffAdapter;
import org.nuxeo.ecm.diff.content.ContentDiffHelper;
import org.nuxeo.ecm.diff.content.adapter.base.ContentDiffConversionType;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.restAPI.BaseNuxeoRestlet;
import org.nuxeo.ecm.platform.util.RepositoryLocation;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.OutputRepresentation;

/**
 * Restlet to retrieve the content diff of a given property between two documents.
 *
 * @author Antoine Taillefer
 * @since 5.6
 */
@Name("contentDiffRestlet")
@Scope(ScopeType.EVENT)
public class ContentDiffRestlet extends BaseNuxeoRestlet {

    private static final Log log = LogFactory.getLog(ContentDiffRestlet.class);

    @In(create = true)
    protected NavigationContext navigationContext;

    @In(create = true)
    protected transient LocaleSelector localeSelector;

    protected CoreSession documentManager;

    protected DocumentModel leftDoc;

    protected DocumentModel rightDoc;

    @Override
    public void handle(Request req, Response res) {

        String repo = (String) req.getAttributes().get("repo");
        String leftDocId = (String) req.getAttributes().get("leftDocId");
        String rightDocId = (String) req.getAttributes().get("rightDocId");
        String xpath = (String) req.getAttributes().get("fieldXPath");
        xpath = xpath.replace("--", "/");

        // Get subPath for other content diff blobs, such as images
        List<String> segments = req.getResourceRef().getSegments();
        StringBuilder sb = new StringBuilder();
        for (int i = 7; i < segments.size(); i++) {
            sb.append(segments.get(i));
            sb.append("/");
        }
        String subPath = sb.substring(0, sb.length() - 1);

        // Check conversion type param, default is html.
        String conversionTypeParam = getQueryParamValue(req, ContentDiffHelper.CONVERSION_TYPE_URL_PARAM_NAME,
                ContentDiffConversionType.html.name());
        ContentDiffConversionType conversionType = ContentDiffConversionType.valueOf(conversionTypeParam);

        // Check locale
        String localeParam = getQueryParamValue(req, ContentDiffHelper.LOCALE_URL_PARAM_NAME,
                localeSelector.getLocaleString());
        localeSelector.setLocaleString(localeParam);

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
        try {
            navigationContext.setCurrentServerLocation(new RepositoryLocation(repo));
            documentManager = navigationContext.getOrCreateDocumentManager();
            leftDoc = documentManager.getDocument(new IdRef(leftDocId));
            rightDoc = documentManager.getDocument(new IdRef(rightDocId));
        } catch (ClientException e) {
            handleError(res, e);
            return;
        }

        List<Blob> contentDiffBlobs = initCachedContentDiffBlobs(res, xpath, conversionType);
        if (CollectionUtils.isEmpty(contentDiffBlobs)) {
            // Response was already handled by initCachedContentDiffBlobs
            return;
        }
        HttpServletResponse response = getHttpResponse(res);
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Pragma", "no-cache");

        try {
            if (StringUtils.isEmpty(subPath)) {
                handleContentDiff(res, contentDiffBlobs.get(0), "text/html");
                return;
            } else {
                for (Blob blob : contentDiffBlobs) {
                    if (subPath.equals(blob.getFilename())) {
                        handleContentDiff(res, blob, blob.getMimeType());
                        return;
                    }

                }
            }
        } catch (IOException ioe) {
            log.error(ioe.getMessage(), ioe);
            handleError(res, ioe);
        }
    }

    private List<Blob> initCachedContentDiffBlobs(Response res, String xpath, ContentDiffConversionType conversionType) {

        ContentDiffAdapter contentDiffAdapter = leftDoc.getAdapter(ContentDiffAdapter.class);

        if (contentDiffAdapter == null) {
            handleNoContentDiff(res, xpath, null);
            return null;
        }

        List<Blob> contentDiffBlobs = null;
        try {
            if (xpath.equals(ContentDiffHelper.CONTENT_DIFF_URL_DEFAULT_XPATH)) {
                contentDiffBlobs = contentDiffAdapter.getFileContentDiffBlobs(rightDoc, conversionType,
                        localeSelector.getLocale());
            } else {
                contentDiffBlobs = contentDiffAdapter.getFileContentDiffBlobs(rightDoc, xpath, conversionType,
                        localeSelector.getLocale());
            }
        } catch (ClientException ce) {
            handleNoContentDiff(res, xpath, ce);
            return null;
        }

        if (CollectionUtils.isEmpty(contentDiffBlobs)) {
            handleNoContentDiff(res, xpath, null);
            return null;
        }
        return contentDiffBlobs;
    }

    protected void handleNoContentDiff(Response res, String xpath, ClientException e) {
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

    protected void handleContentDiff(Response res, final Blob blob, String mimeType) throws IOException {
        // blobs are always persistent, and temporary blobs are GCed only when not referenced anymore
        res.setEntity(new OutputRepresentation(null) {
            @Override
            public void write(OutputStream outputStream) throws IOException {
                try (InputStream stream = blob.getStream()) {
                    IOUtils.copy(stream, outputStream);
                }
            }
        });
        HttpServletResponse response = getHttpResponse(res);

        response.setHeader("Content-Disposition", "inline");
        response.setContentType(mimeType);
    }
}
