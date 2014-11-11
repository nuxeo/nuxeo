/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.pictures.tiles.service;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.platform.picture.api.ImageInfo;
import org.nuxeo.ecm.platform.picture.api.ImagingService;
import org.nuxeo.ecm.platform.pictures.tiles.gwt.client.TilingPreviewConstant;
import org.nuxeo.ecm.platform.preview.adapter.AbstractPreviewer;
import org.nuxeo.ecm.platform.preview.adapter.ImagePreviewer;
import org.nuxeo.ecm.platform.preview.adapter.MimeTypePreviewer;
import org.nuxeo.ecm.platform.preview.api.PreviewException;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Alexandre Russel
 *
 */
public class TiledImagePreviewer extends AbstractPreviewer implements
        MimeTypePreviewer {

    private static final Log log = LogFactory.getLog(TiledImagePreviewer.class);

    protected static final String ORIGINAL_JPEG_VIEW_NAME = "OriginalJpeg";

    protected static final String ORIGINAL_VIEW_NAME = "Original";

    public List<Blob> getPreview(Blob blob, DocumentModel dm)
            throws PreviewException {
        if (useTiling(blob)) {
            List<Blob> blobResults = new ArrayList<Blob>();
            String htmlFile = getString().replace("$repoId$",
                    dm.getRepositoryName());
            htmlFile = htmlFile.replace("$docId$", dm.getId());
            htmlFile = htmlFile.replace("$tileWidth$", "" + 200);
            htmlFile = htmlFile.replace("$tileHeight$", "" + 200);
            htmlFile = htmlFile.replace("$maxTiles$", "" + 2);
            Blob mainBlob = new StringBlob(htmlFile);
            mainBlob.setFilename("index.html");
            mainBlob.setMimeType("text/html");
            blob.setFilename("image");

            blobResults.add(mainBlob);
            blobResults.add(blob);

            return blobResults;
        }

        return new ImagePreviewer().getPreview(blob, dm);
    }

    protected boolean useTiling(Blob blob) {
        ImagingService imagingService = Framework.getLocalService(ImagingService.class);
        if (imagingService != null) {
            ImageInfo info = imagingService.getImageInfo(blob);
            if (info != null) {
                int width = info.getWidth();
                int height = info.getHeight();
                Integer widthThreshold = Integer.valueOf(PictureTilingComponent.getEnvValue(
                        "WidthThreshold", "1200"));
                Integer heightThreshold = Integer.valueOf(PictureTilingComponent.getEnvValue(
                        "HeightThreshold", "1200"));
                return width > widthThreshold || height > heightThreshold;
            }
        }
        return false;
    }

    /**
     * @deprecated since 5.9.2. Use
     *             {@link #useTiling(org.nuxeo.ecm.core.api.Blob)}.
     */
    @Deprecated
    protected boolean useTiling(Blob blob, DocumentModel dm) {
        return useTiling(blob);
    }

    private String getString() {
        StringWriter writer = new StringWriter();
        writer.write("<html><head></head><body>");
        writer.write("<script type=\"text/javascript\">");
        writer.write("var serverSetting = {");
        writer.write("repoId : '$repoId$' ,");
        writer.write("docId : '$docId$' ,");
        writer.write("contextPath : '"
                + VirtualHostHelper.getContextPathProperty() + "'");
        writer.write("};");
        writer.write("</script>");
        writer.write("<script type=\"text/javascript\"");
        writer.write("src=\""
                + VirtualHostHelper.getContextPathProperty()
                + "/org.nuxeo.ecm.platform.pictures.tiles.gwt.TilingPreview/org.nuxeo.ecm.platform.pictures.tiles.gwt.TilingPreview.nocache.js\">");
        writer.write("</script>");
        appendPreviewSettings(writer);
        writer.write("<div id=\"display\"></div>");
        writer.write("</body></html>");
        return writer.toString();
    }

    private static void appendPreviewSettings(StringWriter sb) {
        sb.append("<script type=\"text/javascript\">");
        sb.append("var previewSettings = { ");
        sb.append("imageOnly: \"true\", ");
        sb.append("multiImageAnnotation: \"true\", ");
        sb.append("xPointerFilterPath: \""
                + TilingPreviewConstant.ORG_NUXEO_ECM_PLATFORM_PICTURES_TILES_GWT_CLIENT_XPOINTER_FILTER
                + "\", ");
        sb.append("pointerAdapter: \""
                + TilingPreviewConstant.ORG_NUXEO_ECM_PLATFORM_PICTURES_TILES_GWT_CLIENT_POINTER_ADAPTER
                + "\", ");
        sb.append("annotationDecoratorFunction: \""
                + TilingPreviewConstant.ORG_NUXEO_ECM_PLATFORM_PICTURES_TILES_GWT_CLIENT_UPDATE_ANNOTATED_DOCUMENT
                + "\"");
        sb.append("}");
        sb.append("</script>");
    }

}
