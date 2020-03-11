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
 *     Delprat Thierry
 *     Delbosc Benoit
 */

package org.nuxeo.elasticsearch.work;

import static org.nuxeo.elasticsearch.ElasticSearchConstants.REINDEX_USING_CHILDREN_TRAVERSAL_PROPERTY;

import java.util.List;

import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.elasticsearch.Timestamp;
import org.nuxeo.elasticsearch.api.ElasticSearchIndexing;
import org.nuxeo.elasticsearch.commands.IndexingCommand;
import org.nuxeo.elasticsearch.commands.IndexingCommand.Type;
import org.nuxeo.runtime.api.Framework;

/**
 * Simple Indexing Worker
 */
public class IndexingWorker extends AbstractIndexingWorker implements Work {

    private static final long serialVersionUID = -5141471452954319812L;

    public IndexingWorker(String repositoryName, List<IndexingCommand> cmds) {
        super(repositoryName, cmds);
    }

    @Override
    public String getTitle() {
        return " ElasticSearch indexing for docs: " + getCmdsDigest();
    }

    protected boolean needRecurse(IndexingCommand cmd) {
        if (cmd.isRecurse()) {
            switch (cmd.getType()) {
            case INSERT:
            case UPDATE:
            case UPDATE_SECURITY:
            case UPDATE_DIRECT_CHILDREN:
                return true;
            case DELETE:
                // recurse deletion is done atomically
                return false;
            }
        }
        return false;
    }

    @Override
    protected void doIndexingWork(ElasticSearchIndexing esi, List<IndexingCommand> cmds) {
        long now = Timestamp.currentTimeMicros();
        for (IndexingCommand cmd : cmds) {
            cmd.setOrder(now);
        }
        esi.indexNonRecursive(cmds);
        WorkManager wm = Framework.getService(WorkManager.class);
        for (IndexingCommand cmd : cmds) {
            if (needRecurse(cmd)) {
                wm.schedule(getWorker(cmd));
            }
        }
    }

    private Work getWorker(IndexingCommand cmd) {
        Work ret;
        if (cmd.getType() == Type.UPDATE_DIRECT_CHILDREN) {
            ret = new ScrollingIndexingWorker(cmd.getRepositoryName(), String.format(
                    "SELECT ecm:uuid FROM Document WHERE ecm:parentId = '%s'", cmd.getTargetDocumentId()));
        } else {
            boolean useChildrenWorker = Boolean.parseBoolean(
                    Framework.getProperty(REINDEX_USING_CHILDREN_TRAVERSAL_PROPERTY, "false"));
            if (useChildrenWorker) {
                ret = new ChildrenIndexingWorker(cmd);
            } else {
                ret = new ScrollingIndexingWorker(cmd.getRepositoryName(), String.format(
                        "SELECT ecm:uuid FROM Document WHERE ecm:ancestorId = '%s'", cmd.getTargetDocumentId()));
            }
        }
        return ret;
    }

    public String getCmdsDigest() {
        String ret = "";
        for (IndexingCommand cmd : cmds) {
            ret += " " + cmd.getId();
        }
        return ret;
    }
}
