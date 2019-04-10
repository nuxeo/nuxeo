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
package org.nuxeo.ecm.platform.threed.rendition;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeEntry;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.ecm.platform.rendition.extension.RenditionProvider;
import org.nuxeo.ecm.platform.rendition.service.RenditionDefinition;
import org.nuxeo.ecm.platform.rendition.service.RenditionDefinitionProvider;
import org.nuxeo.ecm.platform.threed.ThreeDDocument;
import org.nuxeo.ecm.platform.threed.service.AutomaticLOD;
import org.nuxeo.ecm.platform.threed.service.RenderView;
import org.nuxeo.ecm.platform.threed.service.ThreeDService;
import org.nuxeo.runtime.api.Framework;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Provides rendition definitions based on the existing render views and transmission formats.
 *
 * @since 8.4
 */
public class ThreeDRenditionDefinitionProvider implements RenditionDefinitionProvider {

    public static final String THREED_RENDER_VIEW_RENDITION_KIND = "nuxeo:threedRenderView:conversion";

    public static final String THREED_TRANSMISSION_RENDITION_KIND = "nuxeo:threedTransmission:conversion";

    public static final String THREED_RENDER_VIEW_RENDITION_TYPE = "Render";

    public static final String THREED_TRANSMISSION_RENDITION_TYPE = "LoD";

    protected RenditionDefinition buildRenditionDefinition(String title, String name, Blob blob, String kind,
            RenditionProvider provider, boolean visible, String iconPath) {
        RenditionDefinition renditionDefinition = new RenditionDefinition();
        renditionDefinition.setEnabled(true);
        renditionDefinition.setName(name);
        renditionDefinition.setKind(kind);
        renditionDefinition.setProvider(provider);
        renditionDefinition.setVisible(visible);
        renditionDefinition.setLabel(title);
        renditionDefinition.setIcon("/icons/" + iconPath);
        return renditionDefinition;
    }

    @Override
    public List<RenditionDefinition> getRenditionDefinitions(DocumentModel doc) {
        ThreeDDocument threeD = doc.getAdapter(ThreeDDocument.class);
        if (threeD == null) {
            return Collections.emptyList();
        }

        ThreeDService threeDService = Framework.getService(ThreeDService.class);
        MimetypeRegistry mimetypeRegistry = Framework.getService(MimetypeRegistry.class);
        List<RenditionDefinition> renditionDefinitions = new ArrayList<>();

        // Render View Renditions
        renditionDefinitions.addAll(threeD.getRenderViews().stream().filter(threeDRenderView -> {

            RenderView renderView = threeDService.getRenderView(threeDRenderView.getAzimuth(),
                    threeDRenderView.getZenith());
            return renderView != null && renderView.isEnabled() && renderView.isRendition()
                    && threeDRenderView.getContent() != null;

        }).map(threeDRenderView -> {

            RenderView renderView = threeDService.getRenderView(threeDRenderView.getAzimuth(),
                    threeDRenderView.getZenith());
            MimetypeEntry mimeType = mimetypeRegistry.getMimetypeEntryByMimeType(
                    threeDRenderView.getContent().getMimeType());
            return buildRenditionDefinition(THREED_RENDER_VIEW_RENDITION_TYPE + threeDRenderView.getTitle(),
                    threeDRenderView.getTitle(), threeDRenderView.getContent(), THREED_RENDER_VIEW_RENDITION_KIND,
                    new RenderViewRenditionProvider(), renderView.isRenditionVisible(), mimeType.getIconPath());
        }).collect(Collectors.toList()));

        // Transmission Renditions
        renditionDefinitions.addAll(threeD.getTransmissionThreeDs().stream().filter(transmissionThreeD -> {

            AutomaticLOD automaticLOD = threeDService.getAutomaticLOD(transmissionThreeD.getId());
            return automaticLOD != null && automaticLOD.isEnabled() && automaticLOD.isRendition()
                    && transmissionThreeD.getBlob() != null;

        }).map(transmissionThreeD -> {

            AutomaticLOD automaticLOD = threeDService.getAutomaticLOD(transmissionThreeD.getId());
            MimetypeEntry mimeType = mimetypeRegistry.getMimetypeEntryByMimeType(
                    transmissionThreeD.getBlob().getMimeType());
            return buildRenditionDefinition(THREED_TRANSMISSION_RENDITION_TYPE + transmissionThreeD.getTitle(),
                    transmissionThreeD.getTitle(), transmissionThreeD.getBlob(), THREED_TRANSMISSION_RENDITION_KIND,
                    new TransmissionThreeDRenditionProvider(), automaticLOD.isRenditionVisible(),
                    mimeType.getIconPath());

        }).collect(Collectors.toList()));
        return renditionDefinitions;
    }

}