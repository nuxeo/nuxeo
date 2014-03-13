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
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchIndexing;
import org.nuxeo.runtime.api.Framework;

/**
 * Abstract class for sharing code between ElasticSearch related workers
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 *
 */
public abstract class AbstractIndexingWorker extends AbstractWork {

    protected final String repositoryName;

    protected final DocumentRef docRef;

    protected String path;

    public AbstractIndexingWorker(DocumentModel doc) {
        repositoryName = doc.getRepositoryName();
        docRef = doc.getRef();
        path = doc.getPathAsString();
    }

    public boolean isAlreadyScheduledForIndexing(DocumentModel doc) {
        return Framework.getLocalService(ElasticSearchAdmin.class).isAlreadyScheduledForIndexing(
                doc);
    }

    @Override
    public String getCategory() {
        return "elasticSearchIndexing";
    }

    @Override
    public void work() throws Exception {
        CoreSession session = initSession(repositoryName);
        ElasticSearchIndexing esi = Framework.getLocalService(ElasticSearchIndexing.class);
        DocumentModel doc = session.getDocument(docRef);
        doIndexingWork(session, esi, doc);
    }

    protected abstract void doIndexingWork(CoreSession session,
            ElasticSearchIndexing esi, DocumentModel doc) throws Exception;

}
