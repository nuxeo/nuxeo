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
 *     pierre
 *     Antoine Taillefer<ataillefer@nuxeo.com>
 */
package org.nuxeo.ecm.platform.picture.recompute;

import static org.nuxeo.ecm.platform.picture.PictureViewsHelper.NOTHING_TO_PROCESS_MESSAGE;
import static org.nuxeo.lib.stream.computation.AbstractComputation.INPUT_1;
import static org.nuxeo.lib.stream.computation.AbstractComputation.OUTPUT_1;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.DownloadBlobGuard;
import org.nuxeo.ecm.core.bulk.BulkServiceImpl;
import org.nuxeo.ecm.core.bulk.action.computation.AbstractBulkComputation;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.picture.PictureViewsHelper;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.stream.StreamProcessorTopology;

/**
 * BAF Computation that fills picture views for the blob property described by the given xpath.
 *
 * @since 11.1
 */
public class RecomputeViewsAction implements StreamProcessorTopology {

    public static final String ACTION_NAME = "recomputeViews";

    public static final String PARAM_XPATH = "xpath";

    @Override
    public Topology getTopology(Map<String, String> options) {
        return Topology.builder()
                .addComputation(RecomputeViewsComputation::new, //
                        Arrays.asList(INPUT_1 + ":" + ACTION_NAME, OUTPUT_1 + ":" + BulkServiceImpl.STATUS_STREAM))
                .build();
    }

    public static class RecomputeViewsComputation extends AbstractBulkComputation {

        public static final String PICTURE_VIEWS_GENERATION_DONE_EVENT = "pictureViewsGenerationDone";

        /**
         * @since 11.5
         */
        protected PictureViewsHelper pictureViewsHelper = new PictureViewsHelper();

        protected String xpath;

        protected String lastPictureViewsStatus;

        public RecomputeViewsComputation() {
            super(ACTION_NAME);
        }

        @Override
        public void startBucket(String bucketKey) {
            BulkCommand command = getCurrentCommand();
            xpath = command.getParam(PARAM_XPATH);
        }

        @Override
        protected void compute(CoreSession session, List<String> ids, Map<String, Serializable> properties) {

            for (String docId : ids) {
                pictureViewsHelper.newTransaction();
                pictureViewsHelper.computePictureViews(session, docId, xpath, this::setLastPictureViewsStatus);
                if (!NOTHING_TO_PROCESS_MESSAGE.equals(getLastPictureViewsStatus())) {
                    fireEvent(session, session.getDocument(new IdRef(docId)), PICTURE_VIEWS_GENERATION_DONE_EVENT);
                }
                // Avoid triggering fulltext extractor on the generated views
                DownloadBlobGuard.enable();
            }
        }

        protected void setLastPictureViewsStatus(String status) {
            lastPictureViewsStatus = status;
        }

        protected String getLastPictureViewsStatus() {
            return lastPictureViewsStatus;
        }

        /**
         * @since 11.5
         */
        protected void fireEvent(CoreSession session, DocumentModel document, String eventName) {
            DocumentEventContext ctx = new DocumentEventContext(session, session.getPrincipal(), document);
            Event event = ctx.newEvent(eventName);
            Framework.getService(EventService.class).fireEvent(event);
        }

    }

}
