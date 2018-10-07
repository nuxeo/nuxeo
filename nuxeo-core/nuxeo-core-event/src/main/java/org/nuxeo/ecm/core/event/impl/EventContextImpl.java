/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.core.event.impl;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

/**
 * Default implementation
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class EventContextImpl extends AbstractEventContext {

    private static final long serialVersionUID = 1L;

    protected transient CoreSession session;

    protected NuxeoPrincipal principal;

    /**
     * Constructor to be used by derived classes
     */
    protected EventContextImpl() {
    }

    public EventContextImpl(Object... args) {
        this(null, null, args);
    }

    public EventContextImpl(CoreSession session, NuxeoPrincipal principal, Object... args) {
        super(args);
        this.session = session;
        this.principal = principal;
        updateRepositoryName();
    }

    public EventContextImpl(CoreSession session, NuxeoPrincipal principal) {
        this.session = session;
        this.principal = principal;
        args = EMPTY;
        updateRepositoryName();
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }

    @Override
    public CoreSession getCoreSession() {
        return session;
    }

    @Override
    public NuxeoPrincipal getPrincipal() {
        return principal;
    }

    @Override
    public void setCoreSession(CoreSession session) {
        this.session = session;
        updateRepositoryName();
    }

    protected void updateRepositoryName() {
        if (session != null) {
            repositoryName = session.getRepositoryName();
        }
    }

    @Override
    public void setPrincipal(NuxeoPrincipal principal) {
        this.principal = principal;
    }

}
