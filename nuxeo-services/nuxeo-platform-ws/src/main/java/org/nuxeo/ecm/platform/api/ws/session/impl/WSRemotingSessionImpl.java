/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
public class WSRemotingSessionImpl implements WSRemotingSession {

    private final CoreSession docMgr;

    private final UserManager userMgr;

    private final String repository;

    private final String username;

    private final String password;

    public WSRemotingSessionImpl(CoreSession session, UserManager um, String repository, String username,
            String password) {
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
