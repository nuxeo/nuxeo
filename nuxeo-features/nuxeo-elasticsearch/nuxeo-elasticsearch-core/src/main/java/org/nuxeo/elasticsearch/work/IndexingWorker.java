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
 *     Nuxeo
 */

package org.nuxeo.elasticsearch.work;

import java.util.Arrays;
import java.util.List;

import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.elasticsearch.api.ElasticSearchIndexing;
import org.nuxeo.elasticsearch.commands.IndexingCommand;
import org.nuxeo.runtime.api.Framework;

/**
 * Simple Indexing Worker
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 *
 */
public class IndexingWorker extends AbstractIndexingWorker implements Work {

    private static final long serialVersionUID = 1L;

    public IndexingWorker(IndexingCommand cmd) {
        super(cmd);
    }

    @Override
    public String getTitle() {
        String title = " ElasticSearch indexing for doc "
                + cmd.getDocId() + " in repository "
                + cmd.getRepository();
        if (path != null) {
            title = title + " (" + path + ")";
        }
        return title;
    }

    protected final List<String> recursableCommands = Arrays.asList(
            IndexingCommand.UPDATE, IndexingCommand.INSERT,
            IndexingCommand.UPDATE_SECURITY);

    protected boolean needRecurse(IndexingCommand cmd) {
        return cmd.isRecurse() && recursableCommands.contains(cmd.getName());
    }

    @Override
    protected void doIndexingWork(ElasticSearchIndexing esi, IndexingCommand cmd)
            throws Exception {

        esi.indexNow(cmd);
        if (needRecurse(cmd)) {
            ChildrenIndexingWorker subWorker = new ChildrenIndexingWorker(cmd);
            WorkManager wm = Framework.getLocalService(WorkManager.class);
            wm.schedule(subWorker);
        }
    }

}
