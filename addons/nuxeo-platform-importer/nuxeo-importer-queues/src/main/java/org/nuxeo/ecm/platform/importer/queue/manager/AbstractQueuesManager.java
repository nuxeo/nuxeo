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
package org.nuxeo.ecm.platform.importer.queue.manager;

import java.util.Random;

import org.nuxeo.ecm.platform.importer.log.ImporterLogger;
import org.nuxeo.ecm.platform.importer.source.SourceNode;


/**
 * @since 8.3
 */
public abstract class AbstractQueuesManager implements QueuesManager {

    protected final int queuesNb;
    protected final ImporterLogger log;
    private final Random rand;

    public AbstractQueuesManager(ImporterLogger logger, int queuesNb) {
        this.queuesNb = queuesNb;
        log = logger;
        rand = new Random(System.currentTimeMillis());
    }

    @Deprecated
    protected int getTargetQueue(SourceNode bh, int nbQueues) {
        return rand.nextInt(nbQueues);
    }

    @Override
    public int count() {
        return queuesNb;
    }

}
