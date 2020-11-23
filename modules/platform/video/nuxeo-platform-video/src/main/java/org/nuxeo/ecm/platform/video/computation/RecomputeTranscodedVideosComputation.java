/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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
 *     Charles Boidot
 */
package org.nuxeo.ecm.platform.video.computation;

import static org.nuxeo.ecm.core.api.CoreSession.ALLOW_VERSION_WRITE;
import static org.nuxeo.ecm.core.api.versioning.VersioningService.DISABLE_AUTO_CHECKOUT;
import static org.nuxeo.ecm.core.bulk.action.SetPropertiesAction.PARAM_DISABLE_AUDIT;
import static org.nuxeo.ecm.platform.video.VideoConstants.TRANSCODED_VIDEOS_PROPERTY;
import static org.nuxeo.ecm.platform.video.service.VideoConversionWork.VIDEO_CONVERSIONS_DONE_EVENT;

import java.io.Serializable;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.impl.primitives.BlobProperty;
import org.nuxeo.ecm.core.bulk.action.computation.AbstractBulkComputation;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.video.TranscodedVideo;
import org.nuxeo.ecm.platform.video.Video;
import org.nuxeo.ecm.platform.video.VideoDocument;
import org.nuxeo.ecm.platform.video.VideoHelper;
import org.nuxeo.ecm.platform.video.action.RecomputeVideoConversionsAction;
import org.nuxeo.ecm.platform.video.service.VideoService;
import org.nuxeo.runtime.api.Framework;

/**
 * Computation that fills the conversions in the
 * {@link org.nuxeo.ecm.platform.video.VideoConstants#TRANSCODED_VIDEOS_PROPERTY} videos property
 *
 * @since 11.5
 */
public class RecomputeTranscodedVideosComputation extends AbstractBulkComputation {

    private static final Logger log = LogManager.getLogger(RecomputeTranscodedVideosComputation.class);

    public static final String NAME = "bulk/recomputeTranscodedVideos";

    public static final String PARAM_XPATH = "xpath";

    public static final String PARAM_CONVERSION_NAMES = "conversionNames";

    protected String xpath;

    protected List<String> conversionNames;

    protected VideoService videoService = Framework.getService(VideoService.class);

    protected EventService eventService = Framework.getService(EventService.class);

    public RecomputeTranscodedVideosComputation() {
        super(NAME);
    }

    @Override
    protected Duration getBatchTransactionTimeout() {
        return Duration.ofSeconds(VideoHelper.getTransactionTimeout());
    }

    @Override
    public void startBucket(String bucketKey) {
        BulkCommand command = getCurrentCommand();
        xpath = command.getParam(PARAM_XPATH);
        conversionNames = command.getParam(PARAM_CONVERSION_NAMES);
        if (conversionNames.isEmpty()) {
            conversionNames = videoService.getAvailableVideoConversionsNames();
        }
    }

    @Override
    protected void compute(CoreSession session, List<String> ids, Map<String, Serializable> properties) {
        log.debug("Compute action: {} generating conversions: {} for doc ids: {}",
                RecomputeVideoConversionsAction.ACTION_NAME, conversionNames, ids);
        for (String docId : ids) {
            IdRef docRef = new IdRef(docId);
            if (!session.exists(docRef)) {
                log.debug("Doc id doesn't exist: {}", docId);
                continue;
            }
            DocumentModel workingDocument = session.getDocument(docRef);

            Property fileProp = workingDocument.getProperty(xpath);
            if (!(fileProp instanceof BlobProperty)) {
                log.warn("Property: {} of doc id: {} is not a blob.", xpath, docId);
                continue;
            }
            Blob blob = (Blob) fileProp.getValue();

            if (blob == null) {
                // do nothing
                log.debug("No blob for doc: {}", workingDocument.getId());
                continue;
            }
            try {
                VideoDocument videoDoc = workingDocument.getAdapter(VideoDocument.class);
                Video video = videoDoc.getVideo();
                for (String conversion : conversionNames) {
                    try {
                        var transcodedVideo = videoService.convert(video, conversion);
                        saveRendition(session, docRef, conversion, transcodedVideo);
                    } catch (ConversionException e) {
                        log.warn("Conversion: {} of doc id: {} has failed", conversion, docId);
                    }
                }
            } catch (DocumentNotFoundException e) {
                // a parent of the document may have been deleted.
                continue;
            }
            workingDocument.refresh();
            DocumentEventContext ctx = new DocumentEventContext(session, session.getPrincipal(), workingDocument);
            Event event = ctx.newEvent(VIDEO_CONVERSIONS_DONE_EVENT);
            eventService.fireEvent(event);
        }
    }

    protected void saveRendition(CoreSession session, IdRef docId, String conversionName,
            TranscodedVideo transcodedVideo) {
        DocumentModel doc = session.getDocument(docId);

        @SuppressWarnings("unchecked")
        var transcodedVideos = (List<Map<String, Serializable>>) doc.getPropertyValue(TRANSCODED_VIDEOS_PROPERTY);

        transcodedVideos.removeIf(tv -> conversionName.equals(tv.get("name")));
        if (transcodedVideo != null) {
            transcodedVideos.add(transcodedVideo.toMap());
        }
        doc.setPropertyValue(TRANSCODED_VIDEOS_PROPERTY, (Serializable) transcodedVideos);
        if (doc.isVersion()) {
            doc.putContextData(ALLOW_VERSION_WRITE, Boolean.TRUE);
        }
        doc.putContextData("disableNotificationService", Boolean.TRUE);
        doc.putContextData(PARAM_DISABLE_AUDIT, Boolean.TRUE);
        doc.putContextData(DISABLE_AUTO_CHECKOUT, Boolean.TRUE);
        session.saveDocument(doc);
    }
}
