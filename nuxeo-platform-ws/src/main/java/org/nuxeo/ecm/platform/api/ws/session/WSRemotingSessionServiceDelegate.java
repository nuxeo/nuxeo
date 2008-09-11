/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: WSRemotingSessionServiceDelegate.java 20682 2007-06-17 16:19:40Z sfermigier $
 */

package org.nuxeo.ecm.platform.api.ws.session;

import org.nuxeo.ecm.platform.api.ws.WSException;
import org.nuxeo.runtime.api.Framework;

/**
 * Stateless Web service remoting session service delegate.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public final class WSRemotingSessionServiceDelegate {

    // Utility class.
    private WSRemotingSessionServiceDelegate() {}

    /**
     * Returns the WS remoting session service.
     *
     * @return the WS remoting session service
     * @throws WSException
     */
    public static WSRemotingSessionManager getRemoteWSRemotingSessionManager()
            throws WSException {
        try {
            return Framework.getService(WSRemotingSessionManager.class);
        } catch (Exception e) {
            throw new WSException(e);
        }
    }

}
