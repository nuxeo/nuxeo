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
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.platform.video.TranscodedVideo;
import org.nuxeo.ecm.platform.video.Video;
import org.nuxeo.ecm.platform.video.VideoDocument;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Task running a defined video conversion.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
public class VideoConversionTask implements Runnable {

    private static final Log log = LogFactory.getLog(VideoConversionTask.class);

    private final VideoConversionId id;

    private final DocumentRef docRef;

    private final String repositoryName;

    private final String conversionName;

    private final VideoServiceImpl service;

    public VideoConversionTask(DocumentModel doc, String conversionName,
            VideoServiceImpl service) {
        docRef = doc.getRef();
        repositoryName = doc.getRepositoryName();
        this.conversionName = conversionName;
        this.service = service;
        id = new VideoConversionId(new DocumentLocationImpl(repositoryName,
                docRef), conversionName);
    }

    protected boolean isStopped() {
        if (Thread.currentThread().isInterrupted()) {
            return true;
        }
        if (service.conversionExecutor.executor.isTerminating()) {
            return true;
        }
        if (service.conversionExecutor.executor.isShutdown()) {
            return true;
        }
        return false;
    }

    @Override
    public void run() {
        try {
            if (isStopped()) {
                return;
            }
            Video originalVideo = getVideoToConvert();
            if (originalVideo != null) {
                if (isStopped()) {
                    return;
                }
                TranscodedVideo transcodedVideo = service.convert(id,
                        originalVideo, conversionName);
                if (isStopped()) {
                    return;
                }
                saveNewTranscodedVideo(transcodedVideo);
            }
        } finally {
            service.clearProgressStatus(id);
        }
    }

    private Video getVideoToConvert() {
        final List<Video> videos = new ArrayList<Video>();
        TransactionHelper.startTransaction();
        try {
            new UnrestrictedSessionRunner(repositoryName) {
                @Override
                public void run() throws ClientException {
                    DocumentModel doc = session.getDocument(docRef);
                    VideoDocument videoDocument = doc.getAdapter(VideoDocument.class);
                    Video video = videoDocument.getVideo();
                    if (video != null) {
                        videos.add(video);
                    } else {
                        log.warn("No original video to transcode for: " + doc);
                    }
                }
            }.runUnrestricted();
        } catch (ClientException e) {
            TransactionHelper.setTransactionRollbackOnly();
            log.error(e, e);
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
        }
        return videos.isEmpty() ? null : videos.get(0);
    }

    private void saveNewTranscodedVideo(final TranscodedVideo transcodedVideo) {
        TransactionHelper.startTransaction();
        try {
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
        } catch (ClientException e) {
            TransactionHelper.setTransactionRollbackOnly();
            throw new ClientRuntimeException(e);
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
        }
    }

    public VideoConversionId getId() {
        return id;
    }

}
