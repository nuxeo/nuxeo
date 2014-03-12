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
import org.nuxeo.ecm.core.api.DocumentModelIterator;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.elasticsearch.ElasticSearchService;
import org.nuxeo.runtime.api.Framework;

/**
 * Worker to index children recursively
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 *
 */
public class ChildrenIndexingWorker extends AbstractIndexingWorker implements
        Work {

    public ChildrenIndexingWorker(DocumentModel doc) {
        super(doc);
    }

    @Override
    public String getTitle() {
        String title = " Elasticsearch indexing children for doc " + docRef
                + " in repository " + repositoryName;
        if (path != null) {
            title = title + " (" + path + ")";
        }
        return title;
    }

    @Override
    protected void doIndexingWork(CoreSession session,
            ElasticSearchService ess, DocumentModel doc) throws Exception {
        DocumentModelIterator iter = session.getChildrenIterator(doc.getRef());
        while (iter.hasNext()) {
            DocumentModel child = iter.next();
            if (!isAlreadyScheduledForIndexing(child)) {
                ess.indexNow(child);
            }
            if (child.isFolder()) {
                ChildrenIndexingWorker subWorker = new ChildrenIndexingWorker(
                        child);
                WorkManager wm = Framework.getLocalService(WorkManager.class);
                wm.schedule(subWorker);
            }
        }

    }

    /*
     * @Override protected Work clone(DocumentModel doc) { return new
     * ChildrenIndexingWorker(doc); }
     */

}
