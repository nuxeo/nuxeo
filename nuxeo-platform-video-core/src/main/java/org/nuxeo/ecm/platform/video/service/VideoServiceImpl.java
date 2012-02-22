/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.platform.video.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.AsyncWaitHook;
import org.nuxeo.ecm.core.event.impl.EventServiceImpl;
import org.nuxeo.ecm.platform.video.TranscodedVideo;
import org.nuxeo.ecm.platform.video.Video;
import org.nuxeo.ecm.platform.video.VideoConversionStatus;
import org.nuxeo.ecm.platform.video.VideoHelper;
import org.nuxeo.ecm.platform.video.VideoInfo;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

import com.google.common.collect.MapMaker;

/**
 * Default implementation of {@link VideoService}.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
public class VideoServiceImpl extends DefaultComponent implements VideoService {

    protected static final Log log = LogFactory.getLog(VideoServiceImpl.class);

    public static final String VIDEO_CONVERSIONS_EP = "videoConversions";

    public static final String DEFAULT_VIDEO_CONVERSIONS_EP = "automaticVideoConversions";

    protected VideoConversionContributionHandler videoConversions;

    protected AutomaticVideoConversionContributionHandler automaticVideoConversions;

    protected VideoConversionExecutor conversionExecutor;

    protected AsyncWaitHook asyncWaitHook;

    protected final Map<VideoConversionId, String> states = new MapMaker().concurrencyLevel(
            10).expiration(1, TimeUnit.DAYS).makeMap();

    @Override
    public void activate(ComponentContext context) throws Exception {
        videoConversions = new VideoConversionContributionHandler();
        automaticVideoConversions = new AutomaticVideoConversionContributionHandler();
        conversionExecutor = new VideoConversionExecutor();
        asyncWaitHook = new AsyncWaitHook() {

            @Override
            public boolean shutdown() {
                try {
                    return conversionExecutor.shutdownNow();
                } finally {
                    conversionExecutor = null;
                }
            }

            @Override
            public boolean waitForAsyncCompletion() {
                try {
                    return conversionExecutor.shutdown();
                } finally {
                    conversionExecutor = new VideoConversionExecutor();
                }
            }
        };
        ((EventServiceImpl) Framework.getLocalService(EventService.class)).registerForAsyncWait(asyncWaitHook);
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        ((EventServiceImpl) Framework.getLocalService(EventService.class)).unregisterForAsyncWait(asyncWaitHook);
        conversionExecutor.shutdown();

        videoConversions = null;
        automaticVideoConversions = null;
        conversionExecutor = null;
        asyncWaitHook = null;
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
        DocumentLocation docLoc = new DocumentLocationImpl(
                doc.getRepositoryName(), doc.getRef());
        VideoConversionId id = new VideoConversionId(docLoc, conversionName);
        if (states.containsKey(id)) {
            return;
        }
        states.put(id, VideoConversionStatus.STATUS_CONVERSION_QUEUED);
        VideoConversionTask task = new VideoConversionTask(doc, conversionName,
                this);
        conversionExecutor.execute(task);
    }

    @Override
    public void launchAutomaticConversions(DocumentModel doc) {
        List<AutomaticVideoConversion> conversions = new ArrayList<AutomaticVideoConversion>(
                automaticVideoConversions.registry.values());
        Collections.sort(conversions);
        for (AutomaticVideoConversion conversion : conversions) {
            launchConversion(doc, conversion.getName());
        }
    }

    @Override
    public TranscodedVideo convert(Video originalVideo, String conversionName) {
        return convert(null, originalVideo, conversionName);
    }

    @Override
    public TranscodedVideo convert(VideoConversionId id, Video originalVideo,
            String conversionName) {
        try {
            if (!videoConversions.registry.containsKey(conversionName)) {
                throw new ClientRuntimeException(String.format(
                        "'%s' is not a registered video conversion.",
                        conversionName));
            }

            if (id != null) {
                states.put(id, VideoConversionStatus.STATUS_CONVERSION_PENDING);
            }

            BlobHolder blobHolder = new SimpleBlobHolder(
                    originalVideo.getBlob());
            VideoConversion conversion = videoConversions.registry.get(conversionName);
            Map<String, Serializable> parameters = new HashMap<String, Serializable>();
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
    public VideoConversionStatus getProgressStatus(VideoConversionId id) {
        String status = states.get(id);
        if (status == null) {
            // early return
            return null;
        }
        @SuppressWarnings("rawtypes")
        Queue q = null;
        if (VideoConversionStatus.STATUS_CONVERSION_QUEUED.equals(status)) {
            q = conversionExecutor.queue;
        }
        int posInQueue = 0;
        int queueSize = 0;
        if (q != null) {
            Object[] queuedItems = q.toArray();
            queueSize = queuedItems.length;
            for (int i = 0; i < queueSize; i++) {
                if (((VideoConversionTask) queuedItems[i]).getId().equals(id)) {
                    posInQueue = i + 1;
                    break;
                }
            }
        }
        return new VideoConversionStatus(status, posInQueue, queueSize);
    }

    @Override
    public void clearProgressStatus(VideoConversionId id) {
        states.remove(id);
    }
}
