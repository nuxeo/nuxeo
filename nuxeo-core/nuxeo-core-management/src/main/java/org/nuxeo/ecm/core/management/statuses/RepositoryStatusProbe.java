/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Mariana Cedica
 */
package org.nuxeo.ecm.core.management.statuses;

import java.util.List;

import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.management.api.Probe;
import org.nuxeo.ecm.core.management.api.ProbeStatus;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Probe that checks that the repository is available by fetching the root doc
 * 
 * @since 9.3
 */
public class RepositoryStatusProbe implements Probe {

    @Override
    public ProbeStatus run() {
        ProbeStatus status;
        status = TransactionHelper.runInTransaction(() -> {
            List<String> repositories = getRepositoryName();
            boolean success = !repositories.isEmpty();
            for (String repository : repositories) {
                success = success && CoreInstance.doPrivileged(repository, (CoreSession session) -> {
                    return session.exists(session.getRootDocument().getRef());
                });
            }
            if (success) {
                return ProbeStatus.newSuccess("Repository started");
            } else {
                return ProbeStatus.newFailure("Repository not started corectly");
            }
        });
        return status;

    }

    protected List<String> getRepositoryName() {
        return Framework.getService(RepositoryService.class).getRepositoryNames();
    }
}
