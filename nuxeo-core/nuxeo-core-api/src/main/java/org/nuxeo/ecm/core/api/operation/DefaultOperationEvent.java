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
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.operation;

import java.io.Serializable;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.repository.cache.DirtyUpdateChecker;

/**
 * This class is not thread safe
 * TODO: should remove this class -> the command itself may be used as the event
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DefaultOperationEvent implements OperationEvent {

    private static final long serialVersionUID = 1L;

    public String id; // the command ID
    public String sessionId; // the sessionId that performed the modifs
    public String repository; // the repository name
    // create an identity object? (principal, key)
    public String userName;

    public ModificationSet modifications;

    public Object details; // should be serializable

    public Object dirtyUpdateTag;

    /**
     *
     */
    public DefaultOperationEvent() {
        // TODO Auto-generated constructor stub
    }

    public DefaultOperationEvent(CoreSession session, String id, ModificationSet modifs) {
        this(session, id, modifs, null);
    }

    public DefaultOperationEvent(CoreSession session, String id, ModificationSet modifs,
            Serializable details) {
        this(session.getSessionId(), session.getRepositoryName(), session.getPrincipal().getName(),
                id, modifs, details);
    }

    public DefaultOperationEvent(String sessionId, String repositoryName, String principal,
            String id, ModificationSet modifs, Object details) {
        this.sessionId = sessionId;
        repository = repositoryName;
        userName = principal;
        this.id = id;
        modifications = modifs;
        this.details = details;

        this.dirtyUpdateTag = DirtyUpdateChecker.computeTag(sessionId, modifs);
    }


    @Override
    public Object getDetails() {
        return details;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public ModificationSet getModifications() {
        return modifications;
    }

    @Override
    public String getUserName() {
        return userName;
    }

    @Override
    public String getRepositoryName() {
        return repository;
    }

    @Override
    public String getSessionId() {
        return sessionId;
    }

    @Override
    public Object getDirtyUpdateTag() {
        return dirtyUpdateTag;
    }

}
