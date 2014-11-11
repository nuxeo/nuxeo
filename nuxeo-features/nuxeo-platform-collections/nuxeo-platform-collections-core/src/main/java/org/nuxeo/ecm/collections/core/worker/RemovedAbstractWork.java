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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package org.nuxeo.ecm.collections.core.worker;

import java.lang.reflect.Constructor;
import java.util.List;

import org.nuxeo.ecm.collections.core.listener.CollectionAsynchrnonousQuery;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.query.nxql.NXQLQueryBuilder;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 5.9.3
 */
public abstract class RemovedAbstractWork extends AbstractWork {

    private static final long serialVersionUID = 5240954386944920642L;

    protected abstract String getQuery();

    protected long offset = 0;

    public RemovedAbstractWork() {
        this(0);
    }

    protected RemovedAbstractWork(final long offset) {
        super();
        this.offset = offset;
    }

    @Override
    public String getId() {
        return repositoryName + ":" + docId + ":" + offset;
    }

    @Override
    public void work() throws Exception {
        setStatus("Updating");
        if (docId != null) {
            initSession();
            final List<DocumentModel> results = getNextResults();
            final int nbResult = results.size();
            setProgress(new Progress(0, results.size()));
            for (int i = 0; i < nbResult; i++) {
                updateDocument(results.get(i));
                setProgress(new Progress(0, nbResult));
            }

            if (nbResult == CollectionAsynchrnonousQuery.MAX_RESULT) {
                setStatus("Rescheduling next work");
                Constructor<? extends RemovedAbstractWork> c = this.getClass().getDeclaredConstructor(long.class);
                RemovedAbstractWork nextWork = c.newInstance(offset + CollectionAsynchrnonousQuery.MAX_RESULT);
                nextWork.setDocument(repositoryName, docId);
                WorkManager workManager = Framework.getLocalService(WorkManager.class);
                workManager.schedule(nextWork, WorkManager.Scheduling.IF_NOT_SCHEDULED,
                        true);
                setStatus("Rescheduling Done");
            }
        }
        setStatus("Updating Done");
    }

    protected abstract void updateDocument(final DocumentModel d)
            throws ClientException;

    private List<DocumentModel> getNextResults() throws ClientException {
        List<DocumentModel> results;
        Object[] parameters = new Object[1];
        parameters[0] = docId;

        String query = NXQLQueryBuilder.getQuery(getQuery(), parameters, true,
                false, null);

        results = session.query(query, null,
                CollectionAsynchrnonousQuery.MAX_RESULT, 0,
                CollectionAsynchrnonousQuery.MAX_RESULT);
        return results;
    }

}
