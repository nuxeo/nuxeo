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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package org.nuxeo.ecm.collections.core.worker;

import java.lang.reflect.Constructor;
import java.util.List;

import org.nuxeo.ecm.collections.core.listener.CollectionAsynchrnonousQuery;
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
    public void work() {
        setStatus("Updating");
        if (docId != null) {
            openSystemSession();
            final List<DocumentModel> results = getNextResults();
            final int nbResult = results.size();
            setProgress(new Progress(0, results.size()));
            for (int i = 0; i < nbResult; i++) {
                updateDocument(results.get(i));
                setProgress(new Progress(0, nbResult));
            }

            if (nbResult == CollectionAsynchrnonousQuery.MAX_RESULT) {
                setStatus("Rescheduling next work");
                RemovedAbstractWork nextWork;
                try {
                    Constructor<? extends RemovedAbstractWork> c = this.getClass().getDeclaredConstructor(long.class);
                    nextWork = c.newInstance(offset + CollectionAsynchrnonousQuery.MAX_RESULT);
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException(e);
                }
                nextWork.setDocument(repositoryName, docId);
                WorkManager workManager = Framework.getService(WorkManager.class);
                workManager.schedule(nextWork, WorkManager.Scheduling.IF_NOT_SCHEDULED, true);
                setStatus("Rescheduling Done");
            }
        }
        setStatus("Updating Done");
    }

    protected abstract void updateDocument(final DocumentModel d);

    private List<DocumentModel> getNextResults() {
        List<DocumentModel> results;
        Object[] parameters = new Object[1];
        parameters[0] = docId;

        String query = NXQLQueryBuilder.getQuery(getQuery(), parameters, true, false, null);

        results = session.query(query, null, CollectionAsynchrnonousQuery.MAX_RESULT, 0,
                CollectionAsynchrnonousQuery.MAX_RESULT);
        return results;
    }

}
