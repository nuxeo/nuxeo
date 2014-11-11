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
 */
package org.nuxeo.ecm.platform.preview.adapter.base;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.preview.adapter.BlobPostProcessor;
import org.nuxeo.ecm.platform.preview.adapter.PreviewAdapterManager;
import org.nuxeo.ecm.platform.preview.api.HtmlPreviewAdapter;
import org.nuxeo.ecm.platform.preview.api.PreviewException;
import org.nuxeo.ecm.platform.preview.helper.PreviewHelper;
import org.nuxeo.runtime.api.Framework;

/**
 * Abstract base class for PreviewAdapters
 *
 * @author tiry
 */
public abstract class AbstractHtmlPreviewAdapter implements HtmlPreviewAdapter {

    // private static final String TITLE_REGEXP = "<title\b[^>]*>(.*?)</title>";
    private static final String TITLE_REGEXP = "<title>(.*?)</title>";

    // private static final String TITLE_REGEXP = "<title[^>]*>[^<]*</title>";
    private static final Pattern TITLE_PATTERN = Pattern.compile(TITLE_REGEXP,
            Pattern.CASE_INSENSITIVE);

    protected DocumentModel adaptedDoc;

    protected static PreviewAdapterManager previewManager;

    protected PreviewAdapterManager getPreviewManager() throws PreviewException {
        if (previewManager == null) {
            try {
                previewManager = Framework.getService(PreviewAdapterManager.class);
            } catch (Exception e) {
                throw new PreviewException(e);
            }
        }
        return previewManager;
    }

    public void setAdaptedDocument(DocumentModel doc) {
        this.adaptedDoc = doc;
    }

    public String getFilePreviewURL() {
        return PreviewHelper.getPreviewURL(adaptedDoc);
    }

    public String getFilePreviewURL(String xpath) {
        return PreviewHelper.getPreviewURL(adaptedDoc, xpath);
    }

    protected String updateTitleInHtml(String htmlContent)
            throws ClientException {
        Matcher m = TITLE_PATTERN.matcher(htmlContent);
        // if (m.matches())
        // return m.replaceFirst("<title>" + getPreviewTitle() + "</title>");
        if (m.find()) {
            String found = m.group();
            htmlContent = htmlContent.replaceFirst(found, "<title>"
                    + getPreviewTitle() + "</title>");
        }

        return htmlContent;
    }

    protected void updateTitleInHtml(File file) throws IOException,
            ClientException {
        String htmlContent = FileUtils.readFile(file);
        htmlContent = updateTitleInHtml(htmlContent);
        FileUtils.writeFile(file, htmlContent);
    }

    protected String getPreviewTitle() throws ClientException {
        StringBuilder sb = new StringBuilder();

        sb.append(adaptedDoc.getTitle());
        sb.append(" ");
        String vl = adaptedDoc.getVersionLabel();
        if (vl != null) {
            sb.append(vl);
        }
        sb.append(" (preview)");

        return sb.toString();
    }

    public List<Blob> getFilePreviewBlobs() throws PreviewException {
        return getFilePreviewBlobs(false);
    }

    public List<Blob> getFilePreviewBlobs(String xpath) throws PreviewException {
        return getFilePreviewBlobs(xpath, false);
    }

    public List<Blob> getFilePreviewBlobs(boolean postProcess)
            throws PreviewException {
        List<Blob> blobs = getPreviewBlobs();
        if (postProcess) {
            blobs = postProcessBlobs(blobs);
        }
        return blobs;
    }

    protected abstract List<Blob> getPreviewBlobs() throws PreviewException;

    public List<Blob> getFilePreviewBlobs(String xpath, boolean postProcess)
            throws PreviewException {
        List<Blob> blobs = getPreviewBlobs(xpath);
        if (postProcess) {
            blobs = postProcessBlobs(blobs);
        }
        return blobs;
    }

    protected abstract List<Blob> getPreviewBlobs(String xpath)
            throws PreviewException;

    protected List<Blob> postProcessBlobs(List<Blob> blobs)
            throws PreviewException {
        List<Blob> processedBlobs = new ArrayList<Blob>();
        for (Blob blob : blobs) {
            for (BlobPostProcessor processor : getPreviewManager().getBlobPostProcessors()) {
                blob = processor.process(blob);
            }
            processedBlobs.add(blob);
        }
        return processedBlobs;
    }

}
