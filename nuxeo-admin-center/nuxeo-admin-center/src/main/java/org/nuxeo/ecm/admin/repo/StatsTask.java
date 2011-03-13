/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.admin.repo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Runnable Class that is executed in the ThreadPool of {@link RepoStat} to
 * gather statistics.
 *
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */
public class StatsTask implements Runnable {

    protected static final Log log = LogFactory.getLog(StatsTask.class);

    private final DocumentRef rootDocRef;

    protected final boolean includeBlob;

    protected final RepoStat cmdInstance;

    protected final String repositoryName;

    public StatsTask(String repoName, DocumentRef rootDocRef,
            boolean includeBlob, RepoStat instance) throws Exception {
        this.repositoryName = repoName;
        this.rootDocRef = rootDocRef;
        this.includeBlob = includeBlob;
        this.cmdInstance = instance;
    }

    public synchronized void run() {
        StatsTaskRunner runner = new StatsTaskRunner(repositoryName,
                includeBlob, rootDocRef, this);

        try {
            TransactionHelper.startTransaction();
            runner.runUnrestricted();
        } catch (ClientException e) {
            log.error("Error while executing StatsTaskRunner", e);
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
        StatsTask newTask;
        try {
            newTask = new StatsTask(repositoryName, root.getRef(), includeBlob,
                    cmdInstance);
        } catch (Exception e) {
            log.error("Unable to start new task", e);
            return null;
        }
        return newTask;
    }

    protected RepoStatInfo getInfo() {
        return cmdInstance.getInfo();
    }

}
