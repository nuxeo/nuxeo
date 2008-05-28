/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.operation;

import java.io.Serializable;

import org.nuxeo.ecm.core.api.CoreSession;

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
    }


    public Object getDetails() {
        return details;
    }

    public String getId() {
        return id;
    }

    public ModificationSet getModifications() {
        return modifications;
    }

    public String getUserName() {
        return userName;
    }

    public String getRepositoryName() {
        return repository;
    }

    public String getSessionId() {
        return sessionId;
    }

}
