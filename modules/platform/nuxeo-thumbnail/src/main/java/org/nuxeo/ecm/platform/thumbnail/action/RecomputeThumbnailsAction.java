/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nour Al Kotob
 */

package org.nuxeo.ecm.platform.thumbnail.action;

import static org.nuxeo.ecm.core.api.CoreSession.ALLOW_VERSION_WRITE;
import static org.nuxeo.ecm.core.bulk.BulkServiceImpl.STATUS_STREAM;
import static org.nuxeo.lib.stream.computation.AbstractComputation.INPUT_1;
import static org.nuxeo.lib.stream.computation.AbstractComputation.OUTPUT_1;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.thumbnail.ThumbnailAdapter;
import org.nuxeo.ecm.core.api.versioning.VersioningService;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.bulk.action.computation.AbstractBulkComputation;
import org.nuxeo.ecm.platform.dublincore.listener.DublinCoreListener;
import org.nuxeo.ecm.platform.ec.notification.NotificationConstants;
import org.nuxeo.ecm.platform.thumbnail.ThumbnailConstants;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.stream.StreamProcessorTopology;

/**
 * Bulk Action processor that generates thumbnails from documents.
 *
 * @since 11.1
 */
public class RecomputeThumbnailsAction implements StreamProcessorTopology {

    public static final String THUMBNAIL_UPDATED = "thumbnailUpdated";

    public static final String ACTION_NAME = "recomputeThumbnails";

    public static final String ACTION_FULL_NAME = "bulk/" + ACTION_NAME;

    @Override
    public Topology getTopology(Map<String, String> options) {
        return Topology.builder()
                       .addComputation(RecomputeThumbnailsComputation::new, //
                               Arrays.asList(INPUT_1 + ":" + ACTION_FULL_NAME, OUTPUT_1 + ":" + STATUS_STREAM))
                       .build();
    }

    public static class RecomputeThumbnailsComputation extends AbstractBulkComputation {

        public RecomputeThumbnailsComputation() {
            super(ACTION_FULL_NAME);
        }

        @Override
        protected void compute(CoreSession session, List<String> ids, Map<String, Serializable> properties) {
            DocumentModelList docs = loadDocuments(session, ids);

            for (DocumentModel doc : docs) {
                processDoc(session, doc);
            }
        }

        protected void processDoc(CoreSession session, DocumentModel doc) {
            Blob thumbnailBlob = getManagedThumbnail(doc);
            if (thumbnailBlob == null) {
                ThumbnailAdapter thumbnailAdapter = doc.getAdapter(ThumbnailAdapter.class);
                if (thumbnailAdapter == null) {
                    return;
                }
                thumbnailBlob = thumbnailAdapter.computeThumbnail(session);
            }
            if (thumbnailBlob != null) {
                if (!doc.hasFacet(ThumbnailConstants.THUMBNAIL_FACET)) {
                    doc.addFacet(ThumbnailConstants.THUMBNAIL_FACET);
                }
                doc.setPropertyValue(ThumbnailConstants.THUMBNAIL_PROPERTY_NAME, (Serializable) thumbnailBlob);
            } else {
                if (doc.hasFacet(ThumbnailConstants.THUMBNAIL_FACET)) {
                    doc.setPropertyValue(ThumbnailConstants.THUMBNAIL_PROPERTY_NAME, null);
                    doc.removeFacet(ThumbnailConstants.THUMBNAIL_FACET);
                }
            }
            if (doc.isDirty()) {
                doc.putContextData(VersioningService.VERSIONING_OPTION, VersioningOption.NONE);
                doc.putContextData(VersioningService.DISABLE_AUTO_CHECKOUT, Boolean.TRUE);
                doc.putContextData(DublinCoreListener.DISABLE_DUBLINCORE_LISTENER, Boolean.TRUE);
                doc.putContextData(NotificationConstants.DISABLE_NOTIFICATION_SERVICE, Boolean.TRUE);
                doc.putContextData("disableAuditLogger", Boolean.TRUE);
                if (doc.isVersion()) {
                    doc.putContextData(ALLOW_VERSION_WRITE, Boolean.TRUE);
                }
                doc.putContextData(THUMBNAIL_UPDATED, true);
                session.saveDocument(doc);
            }
        }

        protected Blob getManagedThumbnail(DocumentModel doc) {
            BlobHolder bh = doc.getAdapter(BlobHolder.class);
            if (bh == null) {
                return null;
            }
            Blob blob = bh.getBlob();
            if (blob == null) {
                return null;
            }
            BlobManager blobManager = Framework.getService(BlobManager.class);
            try (InputStream is = blobManager.getThumbnail(blob)) {
                if (is == null) {
                    return null;
                }
                return Blobs.createBlob(is);
            } catch (IOException e) {
                throw new NuxeoException("Failed to get managed blob thumbnail", e);
            }
        }
    }
}
