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
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.platform.threed.*;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.nuxeo.ecm.platform.threed.ThreeDDocumentConstants.RENDER_VIEWS_PROPERTY;
import static org.nuxeo.ecm.platform.threed.ThreeDDocumentConstants.TRANSMISSIONS_PROPERTY;
import static org.nuxeo.ecm.platform.threed.ThreeDRenderView.TITLE;
import static org.nuxeo.ecm.platform.threed.TransmissionThreeD.NAME;

/**
 * Default implementation of {@link ThreeDDocument}.
 *
 * @since 8.4
 */
public class ThreeDDocumentAdapter implements ThreeDDocument {

    final DocumentModel docModel;

    public ThreeDDocumentAdapter(DocumentModel threed) {
        docModel = threed;

    }

    @Override
    public ThreeD getThreeD() {
        BlobHolder bh = docModel.getAdapter(BlobHolder.class);
        List<Blob> resources = ((List<Map<String, Object>>) docModel.getPropertyValue(
                "files:files")).stream().map(file -> (Blob) file.get("file")).collect(Collectors.toList());
        Map<String, Serializable> infoMap = (Map<String, Serializable>) docModel.getPropertyValue("threed:info");
        ThreeDInfo info = (infoMap != null) ? new ThreeDInfo(infoMap) : null;
        return new ThreeD(bh.getBlob(), resources, info);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<TransmissionThreeD> getTransmissionThreeDs() {
        List<Map<String, Serializable>> list = (List<Map<String, Serializable>>) docModel.getPropertyValue(
                TRANSMISSIONS_PROPERTY);
        return list.stream().map(TransmissionThreeD::new).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    @Override
    public TransmissionThreeD getTransmissionThreeD(String name) {
        List<Map<String, Serializable>> list = (List<Map<String, Serializable>>) docModel.getPropertyValue(
                TRANSMISSIONS_PROPERTY);
        return list.stream()
                   .filter(item -> ((String) item.get(NAME)) != null && name.equals(item.get(NAME)))
                   .map(TransmissionThreeD::new)
                   .findFirst()
                   .orElse(null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<ThreeDRenderView> getRenderViews() {
        List<Map<String, Serializable>> list = (List<Map<String, Serializable>>) docModel.getPropertyValue(
                RENDER_VIEWS_PROPERTY);
        return list.stream().map(ThreeDRenderView::new).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    @Override
    public ThreeDRenderView getRenderView(String title) {
        List<Map<String, Serializable>> list = (List<Map<String, Serializable>>) docModel.getPropertyValue(
                RENDER_VIEWS_PROPERTY);
        return list.stream()
                   .filter(item -> title.equals(item.get(TITLE)))
                   .map(ThreeDRenderView::new)
                   .findFirst()
                   .orElse(null);
    }
}
