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

import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelIterator;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.elasticsearch.api.ElasticSearchIndexing;
import org.nuxeo.elasticsearch.commands.IndexingCommand;
import org.nuxeo.runtime.api.Framework;

/**
 * Worker to index children recursively
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public class ChildrenIndexingWorker extends AbstractIndexingWorker implements Work {

    private static final long serialVersionUID = 724369727479693496L;

    public ChildrenIndexingWorker(IndexingCommand cmd) {
        super(cmd);
    }

    @Override
    public String getTitle() {
        return " ElasticSearch indexing children for cmd " + (cmds.isEmpty() ? "null" : cmds.get(0));
    }

    @Override
    protected void doIndexingWork(ElasticSearchIndexing esi, List<IndexingCommand> cmds) {
        if (cmds.isEmpty()) {
            return;
        }
        IndexingCommand cmd = cmds.get(0);
        DocumentModel doc = getDocument(cmd);
        if (doc == null) {
            return;
        }
        DocumentModelIterator iter = session.getChildrenIterator(doc.getRef());
        while (iter.hasNext()) {
            // Add a session save to process cache invalidation
            session.save();
            DocumentModel child = iter.next();

            IndexingCommand childCommand = cmd.clone(child);
            esi.indexNonRecursive(childCommand);
            if (child.isFolder()) {
                ChildrenIndexingWorker subWorker = new ChildrenIndexingWorker(childCommand);
                WorkManager wm = Framework.getService(WorkManager.class);
                wm.schedule(subWorker);
            }
        }

    }

    private DocumentModel getDocument(IndexingCommand cmd) {
        DocumentModel doc = cmd.getTargetDocument();
        if (doc == null) {
            // doc has been deleted
            return null;
        }
        return doc;
    }

}
