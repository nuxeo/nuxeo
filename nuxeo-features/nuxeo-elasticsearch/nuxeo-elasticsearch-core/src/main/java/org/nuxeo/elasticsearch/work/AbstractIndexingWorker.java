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

    protected final IndexingCommand cmd;

    public AbstractIndexingWorker(IndexingCommand cmd) {
        super();
        this.cmd = cmd;
        this.repositoryName = cmd.getRepository();
        this.docId = cmd.getDocId();
    }

    @Override
    public void doWork() {
        initSession();
        cmd.attach(session);
        ElasticSearchIndexing esi = Framework.getLocalService(ElasticSearchIndexing.class);
        doIndexingWork(esi, cmd);
    }

    protected abstract void doIndexingWork(ElasticSearchIndexing esi, IndexingCommand cmd);

}
