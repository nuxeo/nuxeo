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
 *     Razvan Caraghin
 *     Olivier Grisel
 *     Thierry Delprat
 *     Florent Guillaume
 */

package org.nuxeo.ecm.webapp.delegate;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.security.PermitAll;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.Component;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Unwrap;
import org.jboss.seam.contexts.Lifecycle;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.platform.util.RepositoryLocation;
import org.nuxeo.runtime.api.Framework;

/**
 * Acquires a {@link CoreSession} connection.
 *
 * @author Razvan Caraghin
 * @author Olivier Grisel
 * @author Thierry Delprat
 * @author Florent Guillaume
 */
@Name("documentManager")
@Scope(CONVERSATION)
public class DocumentManagerBusinessDelegate implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(DocumentManagerBusinessDelegate.class);

    /**
     * Map holding the open session for each repository location.
     */
    protected final Map<RepositoryLocation, CoreSession> sessions = new HashMap<RepositoryLocation, CoreSession>();

    public void initialize() {
        log.debug("Seam component initialized...");
    }

    @Unwrap
    public CoreSession getDocumentManager() throws ClientException {
        /*
         * Explicit lookup, as this method is the only user of the Seam
         * component. Also, in some cases (Seam remoting), it seems that the
         * injection is not done correctly.
         */
        RepositoryLocation currentServerLocation = (RepositoryLocation) Component.getInstance("currentServerLocation");
        return getDocumentManager(currentServerLocation);
    }

    public CoreSession getDocumentManager(RepositoryLocation serverLocation)
            throws ClientException {

        if (serverLocation == null) {
            /*
             * currentServerLocation (factory in ServerContextBean) is set
             * through navigationContext, which itself injects documentManager,
             * so it will be null the first time.
             */
            return null;
        }

        CoreSession session = sessions.get(serverLocation);
        if (session == null) {
            if (Lifecycle.isDestroying()) {
                /*
                 * During Seam component destroy phases, we don't want to
                 * recreate a core session just for injection. This happens
                 * during logout when the session context is destroyed; we don't
                 * want to cause EJB calls in this case as the authentication
                 * wouldn't work.
                 */
                return null;
            }
            String serverName = serverLocation.getName();
            try {
                RepositoryManager repositoryManager = Framework.getService(RepositoryManager.class);
                Repository repository = repositoryManager.getRepository(serverName);
                session = repository.open();
                log.debug("Opened session for repository " + serverName);
            } catch (Exception e) {
                throw new ClientException(
                        "Error opening session for repository " + serverName, e);
            }
            sessions.put(serverLocation, session);
        }
        return session;
    }

    @Destroy
    @PermitAll
    public void remove() {
        LoginContext lc = null;
        try {
            try {
                lc = Framework.login();
            }
            catch (LoginException le) {
                 log.error("Unable to login as System", le);
                 log.warn("...try to feed CoreSession(s) without system login ...");
            }
            for (Entry<RepositoryLocation, CoreSession> entry : sessions.entrySet()) {
                String serverName = entry.getKey().getName();
                CoreSession session = entry.getValue();
                Repository.close(session);
                log.debug("Closed session for repository " + serverName);
            }
        }
        finally {
            if (lc != null) {
                try {
                    lc.logout();
                } catch (LoginException lo) {
                    log.error("Error when loggin out", lo);
                }
            }
            sessions.clear();
        }

    }
}
