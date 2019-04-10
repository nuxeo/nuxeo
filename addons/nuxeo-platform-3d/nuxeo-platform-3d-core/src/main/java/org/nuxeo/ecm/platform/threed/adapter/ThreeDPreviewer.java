/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Tiago Cardoso <tcardoso@nuxeo.com>
 */
package org.nuxeo.ecm.platform.threed.adapter;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.preview.adapter.AbstractPreviewer;
import org.nuxeo.ecm.platform.preview.adapter.MimeTypePreviewer;
import org.nuxeo.ecm.platform.preview.api.PreviewException;
import org.nuxeo.ecm.platform.threed.ThreeDDocument;
import org.nuxeo.ecm.platform.threed.TransmissionThreeD;
import org.nuxeo.ecm.platform.threed.service.AutomaticLOD;
import org.nuxeo.ecm.platform.threed.service.ThreeDService;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.runtime.api.Framework;

import java.util.ArrayList;
import java.util.List;

/*
 * 3D content mimetype previewer.
 *
 * @since 8.4
 */
public class ThreeDPreviewer extends AbstractPreviewer implements MimeTypePreviewer {

    public List<Blob> getPreview(Blob blob, DocumentModel dm) throws PreviewException {
        ThreeDDocument threeDDocument = dm.getAdapter(ThreeDDocument.class);
        ThreeDService threeDService = Framework.getService(ThreeDService.class);
        if (threeDDocument.getTransmissionThreeDs().isEmpty()) {
            return new ArrayList<>();
        }
        AutomaticLOD previewLOD = threeDService.getAutomaticLODs().iterator().next();
        return buildPreview(threeDDocument.getTransmissionThreeD(previewLOD.getName()), dm);
    }

    protected List<Blob> buildPreview(TransmissionThreeD transmissionThreeD, DocumentModel dm) {
        List<Blob> blobResults = new ArrayList<>();
        String basePath = VirtualHostHelper.getContextPathProperty();
        StringBuffer html = new StringBuffer();
        html.append("<html><head>");
        html.append("<title>" + getPreviewTitle(dm) + "</title>");
        html.append(String.format(
                "<script src=\"%s/platform-3d/bower_components/webcomponentsjs/webcomponents-lite.min.js\"></script>",
                basePath));
        html.append(String.format("<link rel=\"import\" href=\"%s/platform-3d/nuxeo-3d-viewer.html\">", basePath));
        html.append(String.format("<nuxeo-3d-viewer src=\"%s\" loader=\"complete\">",
                transmissionThreeD.getBlob().getFilename()));
        blobResults.add(transmissionThreeD.getBlob());
        html.append("</nuxeo-3d-viewer>");
        html.append("</body>");
        Blob mainBlob = Blobs.createBlob(html.toString(), "text/html", null, "index.html");
        blobResults.add(0, mainBlob);
        return blobResults;
    }

}
