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
package org.nuxeo.ecm.diff.detaileddiff.adapter.base;

import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.diff.detaileddiff.DetailedDiffException;
import org.nuxeo.ecm.diff.detaileddiff.HtmlDetailedDiffAdapter;
import org.nuxeo.ecm.diff.detaileddiff.adapter.DetailedDiffAdapterManager;
import org.nuxeo.ecm.diff.web.DetailedDiffHelper;
import org.nuxeo.runtime.api.Framework;

/**
 * Abstract base class for Detailed diff adapters.
 *
 * @author Antoine Taillefer
 * @since 5.6
 */
public abstract class AbstractHtmlDetailedDiffAdapter implements
        HtmlDetailedDiffAdapter {

    // private static final String TITLE_REGEXP = "<title>(.*?)</title>";
    //
    // private static final Pattern TITLE_PATTERN =
    // Pattern.compile(TITLE_REGEXP,
    // Pattern.CASE_INSENSITIVE);

    protected DocumentModel adaptedDoc;

    protected static DetailedDiffAdapterManager detailedDiffManager;

    protected DetailedDiffAdapterManager getDetailedDiffManager()
            throws DetailedDiffException {
        if (detailedDiffManager == null) {
            try {
                detailedDiffManager = Framework.getService(DetailedDiffAdapterManager.class);
            } catch (Exception e) {
                throw new DetailedDiffException(e);
            }
        }
        return detailedDiffManager;
    }

    public String getFileDetailedDiffURL(DocumentModel otherDoc) {
        return DetailedDiffHelper.getDetailedDiffURL(adaptedDoc, otherDoc);
    }

    public String getFileDetailedDiffURL(DocumentModel otherDoc, String xpath) {
        return DetailedDiffHelper.getDetailedDiffURL(adaptedDoc, otherDoc,
                xpath);
    }

    // protected String updateTitleInHtml(String htmlContent)
    // throws ClientException {
    // Matcher m = TITLE_PATTERN.matcher(htmlContent);
    // // if (m.matches())
    // // return m.replaceFirst("<title>" + getPreviewTitle() + "</title>");
    // if (m.find()) {
    // String found = m.group();
    // htmlContent = htmlContent.replaceFirst(found, "<title>"
    // + getDetailedDiffTitle() + "</title>");
    // }
    //
    // return htmlContent;
    // }

    // protected void updateTitleInHtml(File file) throws IOException,
    // ClientException {
    // String htmlContent = FileUtils.readFile(file);
    // htmlContent = updateTitleInHtml(htmlContent);
    // FileUtils.writeFile(file, htmlContent);
    // }

    // protected String getDetailedDiffTitle() throws ClientException {
    // StringBuilder sb = new StringBuilder();
    //
    // sb.append(adaptedDoc.getTitle());
    // sb.append(" ");
    // String vl = adaptedDoc.getVersionLabel();
    // if (vl != null) {
    // sb.append(vl);
    // }
    // sb.append(" (detailed diff)");
    //
    // return sb.toString();
    // }

    public List<Blob> getFileDetailedDiffBlobs(DocumentModel otherDoc)
            throws DetailedDiffException {
        // return getFileDetailedDiffBlobs(otherDoc, false);
        return getDetailedDiffBlobs(otherDoc);
    }

    public List<Blob> getFileDetailedDiffBlobs(DocumentModel otherDoc,
            String xpath) throws DetailedDiffException {
        // return getFileDetailedDiffBlobs(otherDoc, xpath, false);
        return getDetailedDiffBlobs(otherDoc, xpath);
    }

    // public List<Blob> getFileDetailedDiffBlobs(DocumentModel otherDoc,
    // boolean postProcess)
    // throws DetailedDiffException {
    // List<Blob> blobs = getDetailedDiffBlobs(otherDoc);
    // if (postProcess) {
    // blobs = postProcessBlobs(blobs);
    // }
    // return blobs;
    // }

    protected abstract List<Blob> getDetailedDiffBlobs(DocumentModel otherDoc)
            throws DetailedDiffException;

    // public List<Blob> getFileDetailedDiffBlobs(DocumentModel otherDoc, String
    // xpath, boolean postProcess)
    // throws DetailedDiffException {
    // List<Blob> blobs = getDetailedDiffBlobs(otherDoc, xpath);
    // if (postProcess) {
    // blobs = postProcessBlobs(blobs);
    // }
    // return blobs;
    // }

    protected abstract List<Blob> getDetailedDiffBlobs(DocumentModel otherDoc,
            String xpath) throws DetailedDiffException;

    // protected List<Blob> postProcessBlobs(List<Blob> blobs)
    // throws DetailedDiffException {
    // List<Blob> processedBlobs = new ArrayList<Blob>();
    // for (Blob blob : blobs) {
    // for (BlobPostProcessor processor :
    // getPreviewManager().getBlobPostProcessors()) {
    // blob = processor.process(blob);
    // }
    // processedBlobs.add(blob);
    // }
    // return processedBlobs;
    // }

    public void setAdaptedDocument(DocumentModel doc) {
        this.adaptedDoc = doc;
    }

}
