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

package org.nuxeo.ecm.diff.restlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.international.LocaleSelector;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.diff.detaileddiff.DetailedDiffAdapter;
import org.nuxeo.ecm.diff.detaileddiff.DetailedDiffException;
import org.nuxeo.ecm.diff.detaileddiff.adapter.base.DetailedDiffConversionType;
import org.nuxeo.ecm.diff.web.DetailedDiffHelper;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.restAPI.BaseNuxeoRestlet;
import org.nuxeo.ecm.platform.util.RepositoryLocation;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.OutputRepresentation;

/**
 * Restlet to retrieve the detailed diff of a given property between two
 * documents.
 *
 * @author Antoine Taillefer
 * @since 5.6
 */
@Name("detailedDiffRestlet")
@Scope(ScopeType.EVENT)
public class DetailedDiffRestlet extends BaseNuxeoRestlet {

    private static final Log log = LogFactory.getLog(DetailedDiffRestlet.class);

    @In(create = true)
    protected NavigationContext navigationContext;

    @In(create = true)
    protected transient LocaleSelector localeSelector;

    protected CoreSession documentManager;

    protected DocumentModel leftDoc;

    protected DocumentModel rightDoc;

    protected static final List<String> detailedDiffInProcessing = Collections.synchronizedList(new ArrayList<String>());

    @Override
    public void handle(Request req, Response res) {

        // Forward locale from HttpRequest to Seam context
        localeSelector.setLocale(getHttpRequest(req).getLocale());

        String repo = (String) req.getAttributes().get("repo");
        String leftDocId = (String) req.getAttributes().get("leftDocId");
        String rightDocId = (String) req.getAttributes().get("rightDocId");
        String xpath = (String) req.getAttributes().get("fieldXPath");
        // TODO: remove?
        xpath = xpath.replace("-", "/");
        // TODO: need to manage subPath?
//        List<String> segments = req.getResourceRef().getSegments();
//        StringBuilder sb = new StringBuilder();
//        for (int i = 7; i < segments.size(); i++) {
//            sb.append(segments.get(i));
//            sb.append("/");
//        }
//        String subPath = sb.substring(0, sb.length() - 1);

        // Default conversion type is any2html
        DetailedDiffConversionType conversionType = DetailedDiffConversionType.any2html;
        // Check conversion type param
        String conversionTypeParam = (String) req.getAttributes().get(
                "conversionType");
        if (!StringUtils.isEmpty(conversionTypeParam)) {
            conversionType = DetailedDiffConversionType.valueOf(conversionTypeParam);
        }

        try {
            xpath = URLDecoder.decode(xpath, "UTF-8");
            //subPath = URLDecoder.decode(subPath, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.error(e);
        }

        String blobPostProcessingParameter = getQueryParamValue(req,
                "blobPostProcessing", "false");
        boolean blobPostProcessing = Boolean.parseBoolean(blobPostProcessingParameter);

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
            navigationContext.setCurrentServerLocation(new RepositoryLocation(
                    repo));
            documentManager = navigationContext.getOrCreateDocumentManager();
            leftDoc = documentManager.getDocument(new IdRef(leftDocId));
            rightDoc = documentManager.getDocument(new IdRef(rightDocId));
        } catch (ClientException e) {
            handleError(res, e);
            return;
        }

        List<Blob> detailedDiffBlobs;
        try {
            detailedDiffBlobs = initCachedDetailedDiffBlobs(res, xpath,
                    conversionType, blobPostProcessing);
        } catch (Exception e) {
            handleError(res, "Unable to get detailed diff.");
            return;
        }
        if (CollectionUtils.isEmpty(detailedDiffBlobs)) {
            // Response was already handled by initCachedDetailedDiffBlobs
            return;
        }
        HttpServletResponse response = getHttpResponse(res);
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Pragma", "no-cache");

        try {
            //if (StringUtils.isEmpty(subPath)) {
                handleDetailedDiff(res, detailedDiffBlobs.get(0), "text/html");
                return;
            // } else {
            // for (Blob blob : detailedDiffBlobs) {
            // if (subPath.equals(blob.getFilename())) {
            // handleDetailedDiff(res, blob, blob.getMimeType());
            // return;
            // }
            //
            // }
            //           }
        } catch (IOException e) {
            handleError(res, e);
        }
    }

    private List<Blob> initCachedDetailedDiffBlobs(Response res, String xpath,
            DetailedDiffConversionType conversionType,
            boolean blobPostProcessing) throws ClientException {

        DetailedDiffAdapter detailedDiffAdapter = null;

        if (detailedDiffAdapter == null) {
            detailedDiffAdapter = leftDoc.getAdapter(DetailedDiffAdapter.class);
        }

        if (detailedDiffAdapter == null) {
            handleNoDetailedDiff(res, xpath, null);
            return null;
        }

        List<Blob> detailedDiffBlobs = null;
        try {
            if (xpath.equals(DetailedDiffHelper.DETAILED_DIFF_URL_DEFAULT_XPATH)) {
                detailedDiffBlobs = detailedDiffAdapter.getFileDetailedDiffBlobs(
                        rightDoc, conversionType);
            } else {
                detailedDiffBlobs = detailedDiffAdapter.getFileDetailedDiffBlobs(
                        rightDoc, xpath, conversionType);
            }
        } catch (DetailedDiffException e) {
            detailedDiffInProcessing.remove(leftDoc.getId());
            handleNoDetailedDiff(res, xpath, e);
            return null;
        }

        if (CollectionUtils.isEmpty(detailedDiffBlobs)) {
            handleNoDetailedDiff(res, xpath, null);
            return null;
        }
        return detailedDiffBlobs;
    }

    protected void handleNoDetailedDiff(Response res, String xpath, Exception e) {
        StringBuilder sb = new StringBuilder();

        sb.append("<html><body><center><h1>");
        if (e == null) {
            sb.append("No detailed diff is available for these documents</h1>");
        } else {
            sb.append("Detailed diff can not be generated for these documents</h1>");
            sb.append("<pre>Blob path: ");
            sb.append(xpath);
            sb.append("</pre>");
            sb.append("<pre>");
            sb.append(e.toString());
            sb.append("</pre>");
        }

        sb.append("</center></body></html>");
        log.error("Could not build detailed diff for missing blob at " + xpath,
                e);

        res.setEntity(sb.toString(), MediaType.TEXT_HTML);
        HttpServletResponse response = getHttpResponse(res);

        response.setHeader("Content-Disposition", "inline");
    }

    protected void handleDetailedDiff(Response res, Blob detailedDiffBlob,
            String mimeType) throws IOException {
        final File tempfile = File.createTempFile(
                "nuxeo-detailedDiffRestlet-tmp", "");
        tempfile.deleteOnExit();
        detailedDiffBlob.transferTo(tempfile);
        res.setEntity(new OutputRepresentation(null) {
            @Override
            public void write(OutputStream outputStream) throws IOException {
                // the write call happens after the seam conversation is
                // finished which will garbage collect the CoreSession
                // instance, hence we store the blob content in a temporary
                // file
                FileInputStream instream = new FileInputStream(tempfile);
                FileUtils.copy(instream, outputStream);
                instream.close();
                tempfile.delete();
            }
        });
        HttpServletResponse response = getHttpResponse(res);

        response.setHeader("Content-Disposition", "inline");
        response.setContentType(mimeType);
    }
}
