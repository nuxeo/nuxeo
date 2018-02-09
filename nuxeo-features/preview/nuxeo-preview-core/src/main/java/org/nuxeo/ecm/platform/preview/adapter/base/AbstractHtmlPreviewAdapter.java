/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.platform.preview.adapter.base;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
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
    private static final Pattern TITLE_PATTERN = Pattern.compile(TITLE_REGEXP, Pattern.CASE_INSENSITIVE);

    protected DocumentModel adaptedDoc;

    protected static PreviewAdapterManager previewManager;

    protected PreviewAdapterManager getPreviewManager() {
        if (previewManager == null) {
            previewManager = Framework.getService(PreviewAdapterManager.class);
        }
        return previewManager;
    }

    @Override
    public void setAdaptedDocument(DocumentModel doc) {
        adaptedDoc = doc;
    }

    @Override
    public String getFilePreviewURL() {
        return PreviewHelper.getPreviewURL(adaptedDoc);
    }

    @Override
    public String getFilePreviewURL(String xpath) {
        return PreviewHelper.getPreviewURL(adaptedDoc, xpath);
    }

    protected String updateTitleInHtml(String htmlContent) {
        Matcher m = TITLE_PATTERN.matcher(htmlContent);
        // if (m.matches())
        // return m.replaceFirst("<title>" + getPreviewTitle() + "</title>");
        if (m.find()) {
            String found = m.group();
            htmlContent = htmlContent.replaceFirst(found, "<title>" + getPreviewTitle() + "</title>");
        }

        return htmlContent;
    }

    protected void updateTitleInHtml(File file) throws IOException {
        String htmlContent = FileUtils.readFileToString(file, UTF_8);
        htmlContent = updateTitleInHtml(htmlContent);
        FileUtils.writeStringToFile(file, htmlContent, UTF_8);
    }

    protected String getPreviewTitle() {
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

    @Override
    public List<Blob> getFilePreviewBlobs() throws PreviewException {
        return getFilePreviewBlobs(false);
    }

    @Override
    public List<Blob> getFilePreviewBlobs(String xpath) throws PreviewException {
        return getFilePreviewBlobs(xpath, false);
    }

    @Override
    public List<Blob> getFilePreviewBlobs(boolean postProcess) throws PreviewException {
        List<Blob> blobs = getPreviewBlobs();
        if (postProcess) {
            blobs = postProcessBlobs(blobs);
        }
        return blobs;
    }

    protected abstract List<Blob> getPreviewBlobs() throws PreviewException;

    @Override
    public List<Blob> getFilePreviewBlobs(String xpath, boolean postProcess) throws PreviewException {
        List<Blob> blobs = getPreviewBlobs(xpath);
        if (postProcess) {
            blobs = postProcessBlobs(blobs);
        }
        return blobs;
    }

    protected abstract List<Blob> getPreviewBlobs(String xpath) throws PreviewException;

    protected List<Blob> postProcessBlobs(List<Blob> blobs) throws PreviewException {
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
