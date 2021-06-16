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
 *     bdelbosc
 */

package org.nuxeo.ecm.platform.thumbnail.action;

import static org.nuxeo.ecm.core.bulk.BulkServiceImpl.STATUS_STREAM;
import static org.nuxeo.lib.stream.computation.AbstractComputation.INPUT_1;
import static org.nuxeo.lib.stream.computation.AbstractComputation.OUTPUT_1;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.bulk.action.computation.AbstractBulkComputation;
import org.nuxeo.ecm.platform.thumbnail.listener.ThumbnailHelper;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.stream.StreamProcessorTopology;

/**
 * Bulk Action processor that generates thumbnails from documents.
 *
 * @since 11.1
 */
public class RecomputeThumbnailsAction implements StreamProcessorTopology {

    public static final String ACTION_NAME = "recomputeThumbnails";

    @Override
    public Topology getTopology(Map<String, String> options) {
        return Topology.builder()
                       .addComputation(RecomputeThumbnailsComputation::new, //
                               Arrays.asList(INPUT_1 + ":" + ACTION_NAME, OUTPUT_1 + ":" + STATUS_STREAM))
                       .build();
    }

    public static class RecomputeThumbnailsComputation extends AbstractBulkComputation {

        protected ThumbnailHelper thumbnailHelper = new ThumbnailHelper();

        public RecomputeThumbnailsComputation() {
            super(ACTION_NAME);
        }

        @Override
        protected void compute(CoreSession session, List<String> ids, Map<String, Serializable> properties) {
            DocumentModelList docs = loadDocuments(session, ids);
            for (DocumentModel doc : docs) {
                thumbnailHelper.newTransaction();
                processDoc(session, doc);
            }
        }

        protected void processDoc(CoreSession session, DocumentModel doc) {
            thumbnailHelper.createThumbnailIfNeeded(session, doc);
        }
    }
}
