/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thomas Roger
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.video.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.Work.State;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.core.work.api.WorkManager.Scheduling;
import org.nuxeo.ecm.platform.video.TranscodedVideo;
import org.nuxeo.ecm.platform.video.Video;
import org.nuxeo.ecm.platform.video.VideoConversionStatus;
import org.nuxeo.ecm.platform.video.VideoHelper;
import org.nuxeo.ecm.platform.video.VideoInfo;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Default implementation of {@link VideoService}.
 *
 * @since 5.5
 */
public class VideoServiceImpl extends DefaultComponent implements VideoService {

    protected static final Log log = LogFactory.getLog(VideoServiceImpl.class);

    public static final String VIDEO_CONVERSIONS_EP = "videoConversions";

    public static final String DEFAULT_VIDEO_CONVERSIONS_EP = "automaticVideoConversions";

    protected VideoConversionContributionHandler videoConversions;

    protected AutomaticVideoConversionContributionHandler automaticVideoConversions;

    @Override
    public void activate(ComponentContext context) throws Exception {
        videoConversions = new VideoConversionContributionHandler();
        automaticVideoConversions = new AutomaticVideoConversionContributionHandler();
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        WorkManager workManager = Framework.getLocalService(WorkManager.class);
        if (workManager != null && workManager.isStarted()) {
            workManager.shutdownQueue(
                    workManager.getCategoryQueueId(VideoConversionWork.CATEGORY_VIDEO_CONVERSION),
                    10, TimeUnit.SECONDS);
        }
        videoConversions = null;
        automaticVideoConversions = null;
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (VIDEO_CONVERSIONS_EP.equals(extensionPoint)) {
            videoConversions.addContribution((VideoConversion) contribution);
        } else if (DEFAULT_VIDEO_CONVERSIONS_EP.equals(extensionPoint)) {
            automaticVideoConversions.addContribution((AutomaticVideoConversion) contribution);
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (VIDEO_CONVERSIONS_EP.equals(extensionPoint)) {
            videoConversions.removeContribution((VideoConversion) contribution);
        } else if (DEFAULT_VIDEO_CONVERSIONS_EP.equals(extensionPoint)) {
            automaticVideoConversions.removeContribution((AutomaticVideoConversion) contribution);
        }
    }

    @Override
    public Collection<VideoConversion> getAvailableVideoConversions() {
        return videoConversions.registry.values();
    }

    @Override
    public void launchConversion(DocumentModel doc, String conversionName) {
        WorkManager workManager = Framework.getLocalService(WorkManager.class);
        if (workManager == null) {
            throw new RuntimeException("No WorkManager available");
        }
        VideoConversionWork work = new VideoConversionWork(
                doc.getRepositoryName(), doc.getId(), conversionName);
        workManager.schedule(work, Scheduling.IF_NOT_RUNNING_OR_SCHEDULED);
    }

    @Override
    public void launchAutomaticConversions(DocumentModel doc) {
        List<AutomaticVideoConversion> conversions = new ArrayList<>(
                automaticVideoConversions.registry.values());
        Collections.sort(conversions);
        for (AutomaticVideoConversion conversion : conversions) {
            launchConversion(doc, conversion.getName());
        }
    }

    @Override
    @Deprecated
    public TranscodedVideo convert(VideoConversionId id, Video originalVideo,
            String conversionName) {
        return convert(originalVideo, conversionName);
    }

    @Override
    public TranscodedVideo convert(Video originalVideo, String conversionName) {
        try {
            if (!videoConversions.registry.containsKey(conversionName)) {
                throw new ClientRuntimeException(String.format(
                        "'%s' is not a registered video conversion.",
                        conversionName));
            }
            BlobHolder blobHolder = new SimpleBlobHolder(
                    originalVideo.getBlob());
            VideoConversion conversion = videoConversions.registry.get(conversionName);
            Map<String, Serializable> parameters = new HashMap<>();
            parameters.put("height", conversion.getHeight());
            parameters.put("videoInfo", originalVideo.getVideoInfo());
            ConversionService conversionService = Framework.getLocalService(ConversionService.class);
            BlobHolder result = conversionService.convert(
                    conversion.getConverter(), blobHolder, parameters);
            VideoInfo videoInfo = VideoHelper.getVideoInfo(result.getBlob());
            return TranscodedVideo.fromBlobAndInfo(conversionName,
                    result.getBlob(), videoInfo);
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    @Override
    @Deprecated
    public VideoConversionStatus getProgressStatus(VideoConversionId id) {
        DocumentLocation loc = id.getDocumentLocation();
        return getProgressStatus(loc.getServerName(), loc.getIdRef().value,
                id.getConversionName());
    }

    @Override
    public VideoConversionStatus getProgressStatus(String repositoryName,
            String docId, String conversionName) {
        WorkManager workManager = Framework.getLocalService(WorkManager.class);
        Work work = new VideoConversionWork(repositoryName, docId,
                conversionName);
        State state = workManager.getWorkState(work.getId());
        if (state == null || state == State.COMPLETED) {
            return null;
        } else if (state == State.SCHEDULED) {
            String queueId = workManager.getCategoryQueueId(VideoConversionWork.CATEGORY_VIDEO_CONVERSION);
            int queueSize = workManager.getQueueSize(queueId, State.SCHEDULED);
            return new VideoConversionStatus(
                    VideoConversionStatus.STATUS_CONVERSION_QUEUED, 0,
                    queueSize);
        } else { // RUNNING
            return new VideoConversionStatus(
                    VideoConversionStatus.STATUS_CONVERSION_PENDING, 0, 0);
        }
    }

}
