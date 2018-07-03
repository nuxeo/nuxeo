/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.picture.api.ImageInfo;
import org.nuxeo.ecm.platform.picture.api.ImagingService;
import org.nuxeo.ecm.platform.pictures.tiles.gwt.client.TilingPreviewConstant;
import org.nuxeo.ecm.platform.preview.adapter.AbstractPreviewer;
import org.nuxeo.ecm.platform.preview.adapter.ImagePreviewer;
import org.nuxeo.ecm.platform.preview.adapter.MimeTypePreviewer;
import org.nuxeo.ecm.platform.preview.adapter.PlainImagePreviewer;
import org.nuxeo.ecm.platform.preview.adapter.base.ConverterBasedHtmlPreviewAdapter;
import org.nuxeo.ecm.platform.preview.api.PreviewException;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;

/**
 * @author Alexandre Russel
 */
public class TiledImagePreviewer extends AbstractPreviewer implements MimeTypePreviewer {

    private static final Log log = LogFactory.getLog(TiledImagePreviewer.class);

    protected static final String ORIGINAL_JPEG_VIEW_NAME = "OriginalJpeg";

    /**
     * @deprecated since 7.2. The Original view does not exist anymore. See NXP-16070.
     */
    @Deprecated
    protected static final String ORIGINAL_VIEW_NAME = "Original";

    public List<Blob> getPreview(Blob blob, DocumentModel dm) throws PreviewException {
        if (useTiling(blob)) {
            List<Blob> blobResults = new ArrayList<Blob>();
            String htmlFile = getString().replace("$repoId$", dm.getRepositoryName());
            htmlFile = htmlFile.replace("$docId$", dm.getId());
            htmlFile = htmlFile.replace("$tileWidth$", "" + 200);
            htmlFile = htmlFile.replace("$tileHeight$", "" + 200);
            htmlFile = htmlFile.replace("$maxTiles$", "" + 2);
            Blob mainBlob = Blobs.createBlob(htmlFile, "text/html", null, "index.html");
            blob.setFilename("image");

            blobResults.add(mainBlob);
            blobResults.add(blob);

            return blobResults;
        }

        ConfigurationService service = Framework.getService(ConfigurationService.class);
        return service.isBooleanPropertyTrue(ConverterBasedHtmlPreviewAdapter.OLD_PREVIEW_PROPERTY)
                ? new PlainImagePreviewer().getPreview(blob, dm)
                : new ImagePreviewer().getPreview(blob, dm);
    }

    protected boolean useTiling(Blob blob) {
        ImagingService imagingService = Framework.getService(ImagingService.class);
        if (imagingService != null) {
            ImageInfo info = imagingService.getImageInfo(blob);
            if (info != null) {
                int width = info.getWidth();
                int height = info.getHeight();
                Integer widthThreshold = Integer.valueOf(PictureTilingComponent.getEnvValue("WidthThreshold", "1200"));
                Integer heightThreshold = Integer.valueOf(PictureTilingComponent.getEnvValue("HeightThreshold", "1200"));
                return width > widthThreshold || height > heightThreshold;
            }
        }
        return false;
    }

    /**
     * @deprecated since 5.9.2. Use {@link #useTiling(org.nuxeo.ecm.core.api.Blob)}.
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
        writer.write("contextPath : '" + VirtualHostHelper.getContextPathProperty() + "'");
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
                + TilingPreviewConstant.ORG_NUXEO_ECM_PLATFORM_PICTURES_TILES_GWT_CLIENT_XPOINTER_FILTER + "\", ");
        sb.append("pointerAdapter: \""
                + TilingPreviewConstant.ORG_NUXEO_ECM_PLATFORM_PICTURES_TILES_GWT_CLIENT_POINTER_ADAPTER + "\", ");
        sb.append("annotationDecoratorFunction: \""
                + TilingPreviewConstant.ORG_NUXEO_ECM_PLATFORM_PICTURES_TILES_GWT_CLIENT_UPDATE_ANNOTATED_DOCUMENT
                + "\"");
        sb.append("}");
        sb.append("</script>");
    }

}
