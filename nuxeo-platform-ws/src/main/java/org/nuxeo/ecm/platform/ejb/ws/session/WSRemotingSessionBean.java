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
 * $Id: WSRemotingSessionBean.java 21703 2007-07-01 20:48:16Z sfermigier $
 */

package org.nuxeo.ecm.platform.ejb.ws.session;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.api.ws.session.WSRemotingSession;
import org.nuxeo.ecm.platform.api.ws.session.WSRemotingSessionManager;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.ws.session.WSRemotingSessionManagerImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * Web service remote session bean.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
@Stateless
@Local(WSRemotingSessionManagerLocal.class)
@Remote(WSRemotingSessionManager.class)
public class WSRemotingSessionBean implements WSRemotingSessionManagerLocal {

    private static final Log log = LogFactory.getLog(WSRemotingSessionBean.class);

    private WSRemotingSessionManager manager;

    private WSRemotingSessionManager getManager() {
        if (manager == null) {
            manager = (WSRemotingSessionManager) Framework.getRuntime().getComponent(
                    WSRemotingSessionManagerImpl.NAME);
        }
        return manager;
    }

    public void addSession(String sid, WSRemotingSession session) {
        WSRemotingSessionManager manager = getManager();
        if (manager != null) {
            manager.addSession(sid, session);
        } else {
            log.error("Cannot find Nuxeo runtime service... Canceling");
        }
    }

    public WSRemotingSession createSession(String username, String password,
            String repository, UserManager um, CoreSession session) {
        WSRemotingSessionManager manager = getManager();
        if (manager != null) {
            return manager.createSession(username, password, repository, um,
                    session);
        }
        log.error("Cannot find Nuxeo runtime service... Cancelling");
        return null;
    }

    public void delSession(String sid) throws ClientException {
        WSRemotingSessionManager manager = getManager();
        if (manager != null) {
            manager.delSession(sid);
        } else {
            throw new ClientException(
                    "Cannot find Nuxeo runtime service... Cancelling");
        }
    }

    public WSRemotingSession getSession(String sid) throws ClientException {
        WSRemotingSessionManager manager = getManager();
        if (manager != null) {
            return manager.getSession(sid);
        } else {
            throw new ClientException(
                    "Cannot find Nuxeo runtime service... Cancelling");
        }
    }

}
