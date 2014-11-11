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
 *     anguenot
 *
 * $Id: WSRemotingSessionImpl.java 19195 2007-05-23 06:43:21Z sfermigier $
 */

package org.nuxeo.ecm.platform.api.ws.session.impl;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.api.ws.session.WSRemotingSession;
import org.nuxeo.ecm.platform.usermanager.UserManager;

/**
 * Web service remoting session implementation.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class WSRemotingSessionImpl implements WSRemotingSession {

    private final CoreSession docMgr;

    private final UserManager userMgr;

    private final String repository;

    private final String username;

    private final String password;

    public WSRemotingSessionImpl(CoreSession session, UserManager um,
            String repository, String username, String password) {
        docMgr = session;
        userMgr = um;
        this.repository = repository;
        this.username = username;
        this.password = password;
    }

    public CoreSession getDocumentManager() {
        return docMgr;
    }

    public String getPassword() {
        return password;
    }

    public String getRepository() {
        return repository;
    }

    public UserManager getUserManager() {
        return userMgr;
    }

    public String getUsername() {
        return username;
    }

}
