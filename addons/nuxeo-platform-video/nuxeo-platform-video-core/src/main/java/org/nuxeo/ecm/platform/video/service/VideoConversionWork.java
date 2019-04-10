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
 */
package org.nuxeo.ecm.platform.video.service;

import static org.nuxeo.ecm.core.api.CoreSession.ALLOW_VERSION_WRITE;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.ecm.core.work.api.WorkManager;
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
        return "Video Conversion " + conversionName;
    }

    @Override
    public void work() {
        setStatus("Extracting");
        setProgress(Progress.PROGRESS_INDETERMINATE);

        Video originalVideo = null;
        try {
            openSystemSession();
            originalVideo = getVideoToConvert();
            commitOrRollbackTransaction();
        } finally {
            cleanUp(true, null);
        }

        if (originalVideo == null) {
            setStatus("Nothing to process");
            return;
        }

        // Perform the actual conversion
        VideoService service = Framework.getService(VideoService.class);
        setStatus("Transcoding");
        TranscodedVideo transcodedVideo = service.convert(originalVideo, conversionName);

        // Saving it to the document
        startTransaction();
        setStatus("Saving");
        openSystemSession();
        DocumentModel doc = session.getDocument(new IdRef(docId));
        saveNewTranscodedVideo(doc, transcodedVideo);
        fireVideoConversionsDoneEvent(doc);
        setStatus("Done");
    }

    protected Video getVideoToConvert() {
        DocumentModel doc = session.getDocument(new IdRef(docId));
        VideoDocument videoDocument = doc.getAdapter(VideoDocument.class);
        Video video = videoDocument.getVideo();
        if (video == null) {
            log.warn("No original video to transcode for: " + doc);
        }
        return video;
    }

    protected void saveNewTranscodedVideo(DocumentModel doc, TranscodedVideo transcodedVideo) {
        @SuppressWarnings("unchecked")
        List<Map<String, Serializable>> transcodedVideos = (List<Map<String, Serializable>>) doc.getPropertyValue("vid:transcodedVideos");
        if (transcodedVideos == null) {
            transcodedVideos = new ArrayList<>();
        }
        transcodedVideos.add(transcodedVideo.toMap());
        doc.setPropertyValue("vid:transcodedVideos", (Serializable) transcodedVideos);
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
    protected void fireVideoConversionsDoneEvent(DocumentModel doc) {
        WorkManager workManager = Framework.getService(WorkManager.class);
        List<String> workIds = workManager.listWorkIds(CATEGORY_VIDEO_CONVERSION, null);
        String idPrefix = computeIdPrefix(repositoryName, docId);
        int worksCount = 0;
        for (String workId : workIds) {
            if (workId.startsWith(idPrefix)) {
                if (++worksCount > 1) {
                    // another work scheduled
                    return;
                }
            }
        }

        DocumentEventContext ctx = new DocumentEventContext(session, session.getPrincipal(), doc);
        Event event = ctx.newEvent(VIDEO_CONVERSIONS_DONE_EVENT);
        Framework.getService(EventService.class).fireEvent(event);
    }

}
