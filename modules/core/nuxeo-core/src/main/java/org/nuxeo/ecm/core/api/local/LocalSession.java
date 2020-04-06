/*
 * (C) Copyright 2006-2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.api.local;

import org.nuxeo.ecm.core.api.AbstractSession;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.runtime.api.Framework;

/**
 * Local Session: implementation of {@link CoreSession} beyond {@link AbstractSession}, dealing with low-level stuff.
 */
public class LocalSession extends AbstractSession implements CloseableCoreSession {

    private static final long serialVersionUID = 1L;

    protected String repositoryName;

    protected NuxeoPrincipal principal;

    public LocalSession(String repositoryName, NuxeoPrincipal principal) {
        this.repositoryName = repositoryName;
        this.principal = principal;
    }

    @Override
    public String getRepositoryName() {
        return repositoryName;
    }

    @Override
    public String getSessionId() {
        return toString();
    }

    @Override
    public String toString() {
        return repositoryName + "/" + principal;
    }

    @Override
    public Session<?> getSession() {
        RepositoryService repositoryService = Framework.getService(RepositoryService.class);
        return repositoryService.getSession(repositoryName);
    }

    @Override
    public void close() {
        // nothing (the session holds no resources to close)
    }

    @Override
    public void destroy() {
        // nothing (deprecated)
    }

    @Override
    public NuxeoPrincipal getPrincipal() {
        return principal;
    }

    @Override
    public boolean isStateSharedByAllThreadSessions() {
        // by design we always share state when in the same thread
        return true;
    }

}
