/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Delprat Thierry
 *     Delbosc Benoit
 */

package org.nuxeo.elasticsearch.work;

import static org.nuxeo.elasticsearch.ElasticSearchConstants.REINDEX_USING_CHILDREN_TRAVERSAL_PROPERTY;

import java.util.List;

import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.elasticsearch.api.ElasticSearchIndexing;
import org.nuxeo.elasticsearch.commands.IndexingCommand;
import org.nuxeo.elasticsearch.core.IndexingMonitor;
import org.nuxeo.runtime.api.Framework;

/**
 * Simple Indexing Worker
 */
public class IndexingWorker extends AbstractIndexingWorker implements Work {

    private static final long serialVersionUID = 1L;

    public IndexingWorker(IndexingMonitor monitor, String repositoryName, List<IndexingCommand> cmds) {
        super(monitor, repositoryName, cmds);
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
        esi.indexNonRecursive(cmds);
        WorkManager wm = Framework.getLocalService(WorkManager.class);
        for (IndexingCommand cmd : cmds) {
            if (needRecurse(cmd)) {
                Work subWorker;
                boolean useChildrenWorker = Boolean.parseBoolean(Framework.getProperty(REINDEX_USING_CHILDREN_TRAVERSAL_PROPERTY,
                        "false"));
                if (useChildrenWorker) {
                    subWorker = new ChildrenIndexingWorker(monitor, cmd);
                } else {
                    subWorker = new ScrollingIndexingWorker(monitor, cmd.getRepositoryName(), String.format(
                            "SELECT ecm:uuid FROM Document WHERE ecm:ancestorId = '%s'", cmd.getTargetDocumentId()));
                }
                wm.schedule(subWorker);
            }
        }
    }

    public String getCmdsDigest() {
        String ret = "";
        for (IndexingCommand cmd : cmds) {
            ret += " " + cmd.getId();
        }
        return ret;
    }
}
