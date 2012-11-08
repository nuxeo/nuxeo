/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.ecm.platform.video.TranscodedVideo;
import org.nuxeo.ecm.platform.video.Video;
import org.nuxeo.ecm.platform.video.VideoDocument;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Work running a defined video conversion.
 *
 * @since 5.6
 */
public class VideoConversionWork extends AbstractWork {

    private static final Log log = LogFactory.getLog(VideoConversionWork.class);

    public static final String CATEGORY_VIDEO_CONVERSION = "videoConversion";

    protected final VideoConversionId id;

    public VideoConversionWork(VideoConversionId id) {
        this.id = id;
    }

    @Override
    public String getCategory() {
        return CATEGORY_VIDEO_CONVERSION;
    }

    @Override
    public String getTitle() {
        return "Video Conversion " + id.getConversionName();
    }

    @Override
    public void work() throws ClientException {
        setStatus("Extracting");
        setProgress(Progress.PROGRESS_INDETERMINATE);
        Video originalVideo = getVideoToConvert();
        if (originalVideo != null) {
            // Release transaction resources while calling ffmpeg (which can
            // take a long time)
            if (isTransactional() && isTransactionStarted) {
                TransactionHelper.commitOrRollbackTransaction();
                isTransactionStarted = false;
            }

            // Perform the actual conversion
            VideoService service = Framework.getLocalService(VideoService.class);
            String conversionName = id.getConversionName();
            setStatus("Transcoding");
            TranscodedVideo transcodedVideo = service.convert(id,
                    originalVideo, conversionName);

            // Reopen a new transaction to save the results back to the
            // repository
            if (isTransactional()) {
                isTransactionStarted = TransactionHelper.startTransaction();
            }
            setStatus("Saving");
            saveNewTranscodedVideo(transcodedVideo);
        }
        setStatus(null);
    }

    protected Video getVideoToConvert() throws ClientException {
        final Video[] videos = new Video[1];
        final DocumentRef docRef = id.getDocumentLocation().getDocRef();
        String repositoryName = id.getDocumentLocation().getServerName();
        new UnrestrictedSessionRunner(repositoryName) {
            @Override
            public void run() throws ClientException {
                DocumentModel doc = session.getDocument(docRef);
                VideoDocument videoDocument = doc.getAdapter(VideoDocument.class);
                Video video = videoDocument.getVideo();
                videos[0] = video;
                if (video == null) {
                    log.warn("No original video to transcode for: " + doc);
                }
            }
        }.runUnrestricted();
        return videos[0];
    }

    protected void saveNewTranscodedVideo(final TranscodedVideo transcodedVideo)
            throws ClientException {
        final DocumentRef docRef = id.getDocumentLocation().getDocRef();
        String repositoryName = id.getDocumentLocation().getServerName();
        new UnrestrictedSessionRunner(repositoryName) {
            @Override
            public void run() throws ClientException {
                DocumentModel doc = session.getDocument(docRef);
                @SuppressWarnings("unchecked")
                List<Map<String, Serializable>> transcodedVideos = (List<Map<String, Serializable>>) doc.getPropertyValue("vid:transcodedVideos");
                if (transcodedVideos == null) {
                    transcodedVideos = new ArrayList<Map<String, Serializable>>();
                }
                transcodedVideos.add(transcodedVideo.toMap());
                doc.setPropertyValue("vid:transcodedVideos",
                        (Serializable) transcodedVideos);
                session.saveDocument(doc);
                session.save();
            }
        }.runUnrestricted();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof VideoConversionWork)) {
            return false;
        }
        return id.equals(((VideoConversionWork) other).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

}
