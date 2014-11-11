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
 * $Id: WSRemotingSession.java 19483 2007-05-27 10:52:56Z sfermigier $
 */

package org.nuxeo.ecm.platform.api.ws.session;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.usermanager.UserManager;

/**
 * Web service remoting session.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
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
     *
     * <p>
     * Note the password is clear.
     * </p>
     *
     * @return the user password as a string.
     */
    String getPassword();

}
