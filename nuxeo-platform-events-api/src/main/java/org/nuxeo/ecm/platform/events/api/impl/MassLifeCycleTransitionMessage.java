/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.events.api.impl;

import java.io.Serializable;
import java.security.Principal;
import java.util.Date;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.platform.events.api.EventMessage;

public class MassLifeCycleTransitionMessage implements EventMessage {

    private static final long serialVersionUID = 1L;

    protected EventMessage eventMessage;

    public String user;

    public String repository;

    public String transition;

    public DocumentRef parentRef;

    public MassLifeCycleTransitionMessage(String currentUser, String transition,
            String repository, DocumentRef parentRef) {
        // need to initialize this thus getProperty won't fail
        user = currentUser;
        this.repository = repository;
        this.transition = transition;
        this.parentRef = parentRef;
    }

    public String getCategory() {
        return null;
    }

    public String getComment() {
        return null;
    }

    public Date getEventDate() {
        return null;
    }

    public String getEventId() {
        return "massLifeCycleTransition";
    }

    public Map<String, Serializable> getEventInfo() {
        return null;
    }

    public Principal getPrincipal() {
        return null;
    }

    public String getPrincipalName() {
        return null;
    }

    public void setEventInfo(Map<String, Serializable> eventInfo) {
        // TODO Auto-generated method stub
    }

    public void feed(CoreEvent coreEvent) {
        // TODO Auto-generated method stub
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public void setTransition(String transition) {
        this.transition = transition;
    }

    public void setParentRef(DocumentRef parentRef) {
        this.parentRef = parentRef;
    }

    public String getUser() {
        return user;
    }

    public String getRepository() {
        return repository;
    }

    public String getTransition() {
        return transition;
    }

    public DocumentRef getParentRef() {
        return parentRef;
    }

}
