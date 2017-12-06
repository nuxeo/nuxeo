/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     mcedica
 */
package org.nuxeo.ecm.platform.management.statuses;

import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.management.api.ProbeStatus;
import org.nuxeo.runtime.api.Framework;

public class QueryRepositoryProbe implements org.nuxeo.ecm.core.management.api.Probe {

    protected static final String queryString = "SELECT * FROM Document";

    public static class Runner extends UnrestrictedSessionRunner {

        public Runner(String repositoryName) {
            super(repositoryName);
        }

        protected String info;

        @Override
        public void run() {
            DocumentModelList list = session.query(queryString, null, 1, 0, false);
            info = " selected " + list.size() + " documents";
        }

    }

    public ProbeStatus run() {
        RepositoryManager mgr = Framework.getService(RepositoryManager.class);
        Runner runner = new Runner(mgr.getDefaultRepositoryName());
        runner.runUnrestricted();
        return ProbeStatus.newSuccess(runner.info);
    }

}
