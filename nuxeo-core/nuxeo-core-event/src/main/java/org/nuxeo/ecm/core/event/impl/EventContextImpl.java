/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.core.event.impl;

import java.security.Principal;

import org.nuxeo.ecm.core.api.CoreSession;

/**
 * Default implementation
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class EventContextImpl extends AbstractEventContext {

    private static final long serialVersionUID = 1L;

    protected CoreSession session;
    protected Principal principal;

    /**
     * Constructor to be used by derived classes
     */
    protected EventContextImpl() {
    }

    public EventContextImpl(Object... args) {
        this(null, null, args);
    }

    public EventContextImpl(CoreSession session, Principal principal, Object... args) {
        super(args);
        this.session = session;
        this.principal = principal;
        updateRepositoryName();
    }

    public EventContextImpl(CoreSession session, Principal principal) {
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
    public Principal getPrincipal() {
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
    public void setPrincipal(Principal principal) {
        this.principal = principal;
    }

}
