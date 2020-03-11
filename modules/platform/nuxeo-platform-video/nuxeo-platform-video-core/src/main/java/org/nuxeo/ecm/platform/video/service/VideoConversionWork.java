/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 *     Florent Guillaume
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.platform.video.service;

import static org.nuxeo.ecm.core.api.CoreSession.ALLOW_VERSION_WRITE;
import static org.nuxeo.ecm.platform.video.VideoConstants.TRANSCODED_VIDEOS_PROPERTY;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.ecm.platform.video.TranscodedVideo;
import org.nuxeo.ecm.platform.video.Video;
import org.nuxeo.ecm.platform.video.VideoDocument;
import org.nuxeo.runtime.api.Framework;

/**
 * Work running a defined video conversion.
 *
 * @since 5.6
 */
public class VideoConversionWork extends AbstractWork {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(VideoConversionWork.class);

    public static final String CATEGORY_VIDEO_CONVERSION = "videoConversion";

    public static final String VIDEO_CONVERSIONS_DONE_EVENT = "videoConversionsDone";

    protected final String conversionName;

    protected static String computeIdPrefix(String repositoryName, String docId) {
        return repositoryName + ':' + docId + ":videoconv:";
    }

    public VideoConversionWork(String repositoryName, String docId, String conversionName) {
        super(computeIdPrefix(repositoryName, docId) + conversionName);
        setDocument(repositoryName, docId);
        this.conversionName = conversionName;
    }

    @Override
    public String getCategory() {
        return CATEGORY_VIDEO_CONVERSION;
    }

    @Override
    public String getTitle() {
        return "Video Conversion: " + conversionName;
    }

    @Override
    public void work() {
        setProgress(Progress.PROGRESS_INDETERMINATE);
        DocumentModel doc;
        Video originalVideo;
        try {
            openSystemSession();
            doc = session.getDocument(new IdRef(docId));
            originalVideo = getVideoToConvert(doc);
            Blob originalBlob;
            if (originalVideo == null || (originalBlob = originalVideo.getBlob()) == null
                    || originalBlob.getLength() == 0) {
                resetTranscodedVideos(doc);
                return;
            }
            commitOrRollbackTransaction();
        } finally {
            cleanUp(true, null);
        }

        // Perform the actual conversion
        log.debug(String.format("Processing %s conversion of Video document %s.", conversionName, doc));
        setStatus("Transcoding");
        VideoService service = Framework.getService(VideoService.class);
        TranscodedVideo transcodedVideo = service.convert(originalVideo, conversionName);

        // Saving it to the document
        startTransaction();
        setStatus("Saving");
        openSystemSession();
        doc = session.getDocument(new IdRef(docId));
        saveNewTranscodedVideo(doc, transcodedVideo);
        log.debug(String.format("End processing %s conversion of Video document %s.", conversionName, doc));
        setStatus("Done");
    }

    @Override
    public boolean isIdempotent() {
        // when the video is updated the work id is the same
        return false;
    }

    @Override
    public boolean isGroupJoin() {
        // This is a GroupJoin work with a trigger that can be used on the last work execution
        return true;
    }

    @Override
    public String getPartitionKey() {
        return computeIdPrefix(repositoryName, docId);
    }

    @Override
    public void onGroupJoinCompletion() {
        fireVideoConversionsDoneEvent();
    }

    @Override
    public boolean equals(Object other) {
        return super.equals(other) && Objects.equals(this, other);
    }

    @Override
    public int hashCode() {
        HashCodeBuilder builder = new HashCodeBuilder();
        builder.appendSuper(super.hashCode());
        builder.append(conversionName);
        return builder.toHashCode();
    }

    protected Video getVideoToConvert(DocumentModel doc) {
        VideoDocument videoDocument = doc.getAdapter(VideoDocument.class);
        return videoDocument.getVideo();
    }

    protected void resetTranscodedVideos(DocumentModel doc) {
        log.warn(String.format("No original video to transcode, resetting transcoded videos of document %s.", doc));
        setStatus("No video to process");
        doc.setPropertyValue(TRANSCODED_VIDEOS_PROPERTY, null);
        if (doc.isVersion()) {
            doc.putContextData(ALLOW_VERSION_WRITE, Boolean.TRUE);
        }
        session.saveDocument(doc);
    }

    protected void saveNewTranscodedVideo(DocumentModel doc, TranscodedVideo transcodedVideo) {
        @SuppressWarnings("unchecked")
        List<Map<String, Serializable>> transcodedVideos = (List<Map<String, Serializable>>) doc.getPropertyValue(
                TRANSCODED_VIDEOS_PROPERTY);
        if (transcodedVideos == null) {
            transcodedVideos = new ArrayList<>();
        } else {
            transcodedVideos = transcodedVideos.stream()
                                               .filter(map -> !transcodedVideo.getName().equals(map.get("name")))
                                               .collect(Collectors.toList());
        }
        transcodedVideos.add(transcodedVideo.toMap());
        doc.setPropertyValue(TRANSCODED_VIDEOS_PROPERTY, (Serializable) transcodedVideos);
        if (doc.isVersion()) {
            doc.putContextData(ALLOW_VERSION_WRITE, Boolean.TRUE);
        }
        session.saveDocument(doc);
    }

    /**
     * Fire a {@code VIDEO_CONVERSIONS_DONE_EVENT} event when no other VideoConversionWork is scheduled for this
     * document.
     *
     * @since 5.8
     */
    protected void fireVideoConversionsDoneEvent() {
        DocumentModel doc = session.getDocument(new IdRef(docId));
        DocumentEventContext ctx = new DocumentEventContext(session, session.getPrincipal(), doc);
        Event event = ctx.newEvent(VIDEO_CONVERSIONS_DONE_EVENT);
        Framework.getService(EventService.class).fireEvent(event);
    }

}
