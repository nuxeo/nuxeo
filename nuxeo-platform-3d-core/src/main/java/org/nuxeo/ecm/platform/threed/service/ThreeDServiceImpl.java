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
package org.nuxeo.ecm.platform.threed.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.threed.ThreeD;
import org.nuxeo.ecm.platform.threed.TransmissionThreeD;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.nuxeo.ecm.platform.threed.convert.Constants.BATCH_CONVERTER;
import static org.nuxeo.ecm.platform.threed.convert.Constants.COLLADA2GLTF_CONVERTER;
import static org.nuxeo.ecm.platform.threed.convert.Constants.COORDS_PARAMETER;
import static org.nuxeo.ecm.platform.threed.convert.Constants.DIMENSIONS_PARAMETER;
import static org.nuxeo.ecm.platform.threed.convert.Constants.LODS_PARAMETER;
import static org.nuxeo.ecm.platform.threed.convert.Constants.LOD_IDS_PARAMETER;
import static org.nuxeo.ecm.platform.threed.convert.Constants.MAX_POLYGONS_PARAMETER;
import static org.nuxeo.ecm.platform.threed.convert.Constants.OPERATORS_PARAMETER;
import static org.nuxeo.ecm.platform.threed.convert.Constants.RENDER_IDS_PARAMETER;

/**
 * Default implementation of {@link ThreeDService}
 *
 * @since 8.4
 */
public class ThreeDServiceImpl extends DefaultComponent implements ThreeDService {

    protected static final Log log = LogFactory.getLog(ThreeDServiceImpl.class);

    public static final String RENDER_VIEWS_EP = "renderViews";

    public static final String DEFAULT_RENDER_VIEWS_EP = "automaticRenderViews";

    public static final String DEFAULT_LODS_EP = "automaticLOD";

    protected AutomaticLODContributionHandler automaticLODs;

    protected AutomaticRenderViewContributionHandler automaticRenderViews;

    protected RenderViewContributionHandler renderViews;

    @Override
    public void activate(ComponentContext context) {
        automaticLODs = new AutomaticLODContributionHandler();
        automaticRenderViews = new AutomaticRenderViewContributionHandler();
        renderViews = new RenderViewContributionHandler();
    }

    @Override
    public void deactivate(ComponentContext context) {
        WorkManager workManager = Framework.getLocalService(WorkManager.class);
        if (workManager != null && workManager.isStarted()) {
            try {
                workManager.shutdownQueue(
                        workManager.getCategoryQueueId(ThreeDBatchUpdateWork.CATEGORY_THREED_CONVERSION), 10,
                        TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                // restore interrupted status
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
        automaticLODs = null;
        automaticRenderViews = null;
        renderViews = null;
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        switch (extensionPoint) {
        case RENDER_VIEWS_EP:
            renderViews.addContribution((RenderView) contribution);
            break;
        case DEFAULT_RENDER_VIEWS_EP:
            automaticRenderViews.addContribution((AutomaticRenderView) contribution);
            break;
        case DEFAULT_LODS_EP:
            automaticLODs.addContribution((AutomaticLOD) contribution);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        switch (extensionPoint) {
        case RENDER_VIEWS_EP:
            renderViews.removeContribution((RenderView) contribution);
            break;
        case DEFAULT_RENDER_VIEWS_EP:
            automaticRenderViews.removeContribution((AutomaticRenderView) contribution);
            break;
        case DEFAULT_LODS_EP:
            automaticLODs.removeContribution((AutomaticLOD) contribution);
        }
    }

    @Override
    public void launchBatchConversion(DocumentModel doc) {
        ThreeDBatchUpdateWork work = new ThreeDBatchUpdateWork(doc.getRepositoryName(), doc.getId());
        WorkManager workManager = Framework.getLocalService(WorkManager.class);
        workManager.schedule(work, WorkManager.Scheduling.IF_NOT_SCHEDULED, true);
    }

    @Override
    public Collection<Blob> batchConvert(ThreeD originalThreed) {
        ConversionService cs = Framework.getService(ConversionService.class);
        // get all the 3d content blobs
        List<Blob> in = new ArrayList<>();
        in.add(originalThreed.getBlob());
        if (originalThreed.getResources() != null) {
            in.addAll(originalThreed.getResources());
        }

        // gather 3D contribution default contributions
        List<RenderView> renderViews = automaticRenderViews.registry.values()
                                                                    .stream()
                                                                    .filter(AutomaticRenderView::isEnabled)
                                                                    .map(AutomaticRenderView::getId)
                                                                    .map(this::getRenderView)
                                                                    .filter(RenderView::isEnabled)
                                                                    .collect(Collectors.toList());

        List<AutomaticLOD> lods = (List<AutomaticLOD>) automaticLODs.registry.values()
                                                                             .stream()
                                                                             .filter(AutomaticLOD::isEnabled)
                                                                             .collect(Collectors.toList());

        // setup all work to be done in batch process (renders, lods)
        Map<String, Serializable> params = new HashMap<>();

        // operators
        String operators = "import";
        // add renders
        operators += new String(new char[renderViews.size()]).replace("\0", " render");
        // add conversion
        operators += " convert";
        // add lods
        operators += new String(new char[lods.size()]).replace("\0", " lod convert");
        params.put(OPERATORS_PARAMETER, operators);

        // render ids
        params.put(RENDER_IDS_PARAMETER, renderViews.stream().map(RenderView::getId).collect(Collectors.joining(" ")));

        // lod ids
        params.put(LOD_IDS_PARAMETER, lods.stream().map(AutomaticLOD::getId).collect(Collectors.joining(" ")));

        // lods
        params.put(LODS_PARAMETER,
                lods.stream().map(AutomaticLOD::getPercentage).map(String::valueOf).collect(Collectors.joining(" ")));

        // maxPolys
        params.put(MAX_POLYGONS_PARAMETER,
            lods.stream().map(AutomaticLOD::getMaxPoly).map(String::valueOf).collect(Collectors.joining(" ")));

        // spherical coordinates
        params.put(COORDS_PARAMETER, renderViews.stream()
            .map(renderView -> renderView.getAzimuth() + "," + renderView.getZenith())
            .collect(Collectors.joining(" ")));

        // dimensions
        params.put(DIMENSIONS_PARAMETER, renderViews.stream()
            .map(renderView -> renderView.getWidth() + "," + renderView.getHeight())
            .collect(Collectors.joining(" ")));

        BlobHolder result = cs.convert(BATCH_CONVERTER, new SimpleBlobHolder(in), params);

        return result.getBlobs();
    }

    @Override
    public Collection<RenderView> getAvailableRenderViews() {
        return renderViews.registry.values();
    }

    @Override
    public Collection<RenderView> getAutomaticRenderViews() {
        return automaticRenderViews.registry.values()
                                            .stream()
                                            .filter(AutomaticRenderView::isEnabled)
                                            .map(AutomaticRenderView::getId)
                                            .map(this::getRenderView)
                                            .collect(Collectors.toList());
    }

    @Override
    public Collection<AutomaticLOD> getAutomaticLODs() {
        return automaticLODs.registry.values();
    }

    @Override
    public AutomaticLOD getAutomaticLOD(String automaticLODId) {
        return automaticLODs.registry.get(automaticLODId);
    }

    @Override
    public RenderView getRenderView(String renderViewId) {
        return renderViews.registry.get(renderViewId);
    }

    @Override
    public RenderView getRenderView(Integer azimuth, Integer zenith) {
        return renderViews.registry.values()
                                   .stream()
                                   .filter(renderView -> renderView.getAzimuth().equals(azimuth)
                                           && renderView.getZenith().equals(zenith))
                                   .findFirst()
                                   .orElse(null);
    }

    @Override
    public TransmissionThreeD convertColladaToglTF(TransmissionThreeD colladaThreeD) {
        ConversionService cs = Framework.getService(ConversionService.class);
        Map<String, Serializable> parameters = new HashMap<>();

        BlobHolder result = cs.convert(COLLADA2GLTF_CONVERTER, new SimpleBlobHolder(colladaThreeD.getBlob()),
                parameters);
        List<Blob> blobs = result.getBlobs();
        return new TransmissionThreeD(blobs.get(0), colladaThreeD.getLod(), colladaThreeD.getMaxPoly(),
                colladaThreeD.getName());
    }
}
