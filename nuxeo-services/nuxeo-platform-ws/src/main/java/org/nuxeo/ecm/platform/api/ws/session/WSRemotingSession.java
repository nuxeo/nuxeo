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
 * $Id: WSRemotingSession.java 19483 2007-05-27 10:52:56Z sfermigier $
 */

package org.nuxeo.ecm.platform.api.ws.session;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.usermanager.UserManager;

/**
 * Web service remoting session.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public interface WSRemotingSession {

    /**
     * Returns the document manager bound to the session.
     *
     * @return a <code>CoreSession</code> instance.
     */
    CoreSession getDocumentManager();

    /**
     * Returns the user manager bound to the session.
     */
    UserManager getUserManager();

    /**
     * Returns the repository bound to the session.
     *
     * @return the repository name
     */
    String getRepository();

    /**
     * Returns the user name bound to the session.
     *
     * @return the user name as a string.
     */
    String getUsername();

    /**
     * Returns the user password bound to the session
     * <p>
     * Note the password is clear.
     * </p>
     *
     * @return the user password as a string.
     */
    String getPassword();

}
