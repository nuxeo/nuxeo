/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
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

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.elasticsearch.ElasticSearchService;
import org.nuxeo.runtime.api.Framework;

/**
 * Simple Indexing Worker
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 *
 */
public class IndexingWorker extends AbstractIndexingWorker implements Work {

    protected final boolean recurse;

    public IndexingWorker(DocumentModel doc, boolean recurse) {
        super(doc);
        this.recurse = recurse;
    }

    @Override
    public String getTitle() {
        String title = " Elasticsearch indexing for doc " + docRef
                + " in repository " + repositoryName;
        if (path != null) {
            title = title + " (" + path + ")";
        }
        return title;
    }

    @Override
    protected void doIndexingWork(CoreSession session,
            ElasticSearchService ess, DocumentModel doc) throws Exception {
        ess.indexNow(doc);
        if (recurse) {
            ChildrenIndexingWorker subWorker = new ChildrenIndexingWorker(doc);
            WorkManager wm = Framework.getLocalService(WorkManager.class);
            wm.schedule(subWorker);
        }
    }

    /*
     * @Override protected Work clone(DocumentModel doc) { return new
     * IndexingWorker(doc, recurse); }
     */

}
