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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.4.3
 */
public class VideoConversionTask implements Runnable {

    private static final Log log = LogFactory.getLog(VideoConversionTask.class);

    private final DocumentRef docRef;

    private final String repositoryName;

    private final String conversionName;

    private final VideoService service;

    public VideoConversionTask(DocumentModel doc, String conversionName,
            VideoService service) {
        docRef = doc.getRef();
        repositoryName = doc.getRepositoryName();
        this.conversionName = conversionName;
        this.service = service;
    }

    @Override
    public void run() {
        try {
            Blob originalVideo = getBlobToConvert();
            if (originalVideo != null) {
                Blob transcodedVideo = service.convert(originalVideo,
                        conversionName);
                saveNewTranscodedVideo(transcodedVideo);
            }
        } catch (ClientException e) {
            log.error(e, e);
        }
    }

    private Blob getBlobToConvert() {
        final List<Blob> blobs = new ArrayList<Blob>();
        TransactionHelper.startTransaction();
        try {
            new UnrestrictedSessionRunner(repositoryName) {
                @Override
                public void run() throws ClientException {
                    DocumentModel doc = session.getDocument(docRef);
                    BlobHolder originalVideo = doc.getAdapter(BlobHolder.class);
                    if (originalVideo != null) {
                        blobs.add(originalVideo.getBlob());
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
        return blobs.isEmpty() ? null : blobs.get(0);
    }

    private void saveNewTranscodedVideo(final Blob blob) {
        TransactionHelper.startTransaction();
        try {
            new UnrestrictedSessionRunner(repositoryName) {
                @Override
                public void run() throws ClientException {
                    DocumentModel doc = session.getDocument(docRef);
                    List<Map<String, Serializable>> transcodedVideos = (List<Map<String, Serializable>>) doc.getPropertyValue("vid:transcodedVideos");
                    if (transcodedVideos == null) {
                        transcodedVideos = new ArrayList<Map<String, Serializable>>();
                    }
                    Map<String, Serializable> transcodedVideo = new HashMap<String, Serializable>();
                    Map<String, Serializable> metadata = new HashMap<String, Serializable>();
                    metadata.put("name", conversionName);
                    transcodedVideo.put("content", (Serializable) blob);
                    transcodedVideo.put("metadata", (Serializable) metadata);
                    transcodedVideos.add(transcodedVideo);
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

}
