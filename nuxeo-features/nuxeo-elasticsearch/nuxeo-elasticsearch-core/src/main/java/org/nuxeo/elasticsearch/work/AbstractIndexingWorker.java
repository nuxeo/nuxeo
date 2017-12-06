/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo
 */

package org.nuxeo.elasticsearch.work;

import java.util.Collections;
import java.util.List;

import org.nuxeo.elasticsearch.api.ElasticSearchIndexing;
import org.nuxeo.elasticsearch.commands.IndexingCommand;
import org.nuxeo.runtime.api.Framework;

/**
 * Abstract class for sharing code between ElasticSearch related workers
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public abstract class AbstractIndexingWorker extends BaseIndexingWorker {

    private static final long serialVersionUID = 1L;

    protected final List<IndexingCommand> cmds;

    public AbstractIndexingWorker(IndexingCommand cmd) {
        this.cmds = Collections.singletonList(cmd);
        this.repositoryName = cmd.getRepositoryName();
        this.docId = cmd.getTargetDocumentId();
    }

    public AbstractIndexingWorker(String repositoryName, List<IndexingCommand> cmds) {
        this.cmds = cmds;
        this.repositoryName = repositoryName;
        if (!cmds.isEmpty()) {
            this.docId = cmds.get(0).getTargetDocumentId();
        }
    }

    @Override
    public void doWork() {
        openSystemSession();
        for (IndexingCommand cmd : cmds) {
            cmd.attach(session);
        }
        ElasticSearchIndexing esi = Framework.getService(ElasticSearchIndexing.class);
        doIndexingWork(esi, cmds);
    }

    protected abstract void doIndexingWork(ElasticSearchIndexing esi, List<IndexingCommand> cmds);

}
