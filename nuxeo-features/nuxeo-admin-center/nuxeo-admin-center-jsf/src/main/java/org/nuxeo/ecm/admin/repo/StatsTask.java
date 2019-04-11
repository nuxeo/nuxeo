/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.admin.repo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Runnable Class that is executed in the ThreadPool of {@link RepoStat} to gather statistics.
 *
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */
public class StatsTask implements Runnable {

    protected static final Log log = LogFactory.getLog(StatsTask.class);

    private final DocumentRef rootDocRef;

    protected final boolean includeBlob;

    protected final RepoStat cmdInstance;

    protected final String repositoryName;

    public StatsTask(String repoName, DocumentRef rootDocRef, boolean includeBlob, RepoStat instance) {
        this.repositoryName = repoName;
        this.rootDocRef = rootDocRef;
        this.includeBlob = includeBlob;
        this.cmdInstance = instance;
    }

    @Override
    public synchronized void run() {
        StatsTaskRunner runner = new StatsTaskRunner(repositoryName, includeBlob, rootDocRef, this);

        try {
            TransactionHelper.startTransaction();
            runner.runUnrestricted();
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
        }
    }

    public void exec(StatsTask task) {
        cmdInstance.exec(task);
    }

    protected StatsTask getNextTask(DocumentModel root) {
        if (cmdInstance.isPoolFull()) {
            return null;
        }
        return new StatsTask(repositoryName, root.getRef(), includeBlob, cmdInstance);
    }

    protected RepoStatInfo getInfo() {
        return cmdInstance.getInfo();
    }

}
