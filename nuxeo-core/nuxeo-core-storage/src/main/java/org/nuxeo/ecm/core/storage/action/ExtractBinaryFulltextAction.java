/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */

package org.nuxeo.ecm.core.storage.action;

import static org.nuxeo.ecm.core.api.CoreSession.BINARY_FULLTEXT_MAIN_KEY;
import static org.nuxeo.ecm.core.bulk.BulkServiceImpl.STATUS_STREAM;
import static org.nuxeo.ecm.core.storage.FulltextExtractorWork.SYSPROP_FULLTEXT_BINARY;
import static org.nuxeo.lib.stream.computation.AbstractComputation.INPUT_1;
import static org.nuxeo.lib.stream.computation.AbstractComputation.OUTPUT_1;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.repository.FulltextConfiguration;
import org.nuxeo.ecm.core.bulk.action.computation.AbstractBulkComputation;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.ecm.core.storage.FulltextExtractorWork;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.stream.StreamProcessorTopology;

/**
 * Bulk Action to extract fulltext from blobs.
 * Updated documents and their proxies are indexed by batch (transaction).
 *
 * @since 2021.33
 */
public class ExtractBinaryFulltextAction implements StreamProcessorTopology {

    public static final String ACTION_NAME = "extractBinaryFulltext";

    @Override
    public Topology getTopology(Map<String, String> options) {
        return Topology.builder()
                       .addComputation(ExtractBinaryFulltextComputation::new,
                               Arrays.asList(INPUT_1 + ":" + ACTION_NAME, //
                                       OUTPUT_1 + ":" + STATUS_STREAM))
                       .build();
    }

    public static class ExtractBinaryFulltextComputation extends AbstractBulkComputation {

        private static final Logger log = LogManager.getLogger(ExtractBinaryFulltextAction.class);

        // Option to use after a configuration change to nullifying fulltext of non-indexable docs
        protected boolean force;

        public ExtractBinaryFulltextComputation() {
            super(ACTION_NAME);
        }

        @Override
        public void startBucket(String bucketKey) {
            BulkCommand command = getCurrentCommand();
            Serializable forceOption = command.getParam("force");
            force = forceOption != null && Boolean.parseBoolean(forceOption.toString());
        }

        @Override
        protected void compute(CoreSession session, List<String> ids, Map<String, Serializable> properties) {
            RepositoryService repositoryService = Framework.getService(RepositoryService.class);
            Repository repository = repositoryService.getRepository(session.getRepositoryName());
            FulltextConfiguration fulltextConfiguration = repository.getFulltextConfiguration();
            for (DocumentModel doc : loadDocuments(session, ids)) {
                if (doc.isProxy()) {
                    // proxy is not holding any binary fulltext
                    continue;
                }
                if (fulltextConfiguration.isFulltextIndexable(doc.getType())) {
                    FulltextExtractorWork work = new FulltextExtractorWork(doc.getRepositoryName(), doc.getId(), false,
                            true, false);
                    log.debug("Running fulltext extractor on doc: {}", doc::getRef);
                    work.extractBinaryFulltext(session, doc);
                } else if (force) {
                    Map<String, String> ft = session.getBinaryFulltext(doc.getRef());
                    if (ft == null || StringUtils.isBlank(ft.get(BINARY_FULLTEXT_MAIN_KEY))) {
                        // already empty
                        continue;
                    }
                    log.debug("Remove binary fulltext on doc: {}", doc::getRef);
                    session.setDocumentSystemProp(doc.getRef(), SYSPROP_FULLTEXT_BINARY, "");
                }
            }
        }
    }
}
