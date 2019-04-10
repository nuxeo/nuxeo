/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.platform.importer.queue.consumer;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.importer.log.ImporterLogger;
import org.nuxeo.ecm.platform.importer.queue.manager.QueuesManager;
import org.nuxeo.ecm.platform.importer.source.SourceNode;

/**
 * @since 8.3
 */
public class SourceNodeConsumerFactory implements ConsumerFactory<SourceNode> {

    @Override
    public Consumer<SourceNode> createConsumer(ImporterLogger log, DocumentModel root, int batchSize,
                                   QueuesManager<SourceNode> queuesManager, int queue) {
        return new SourceNodeConsumer(log, root, batchSize, queuesManager, queue);
    }

}
