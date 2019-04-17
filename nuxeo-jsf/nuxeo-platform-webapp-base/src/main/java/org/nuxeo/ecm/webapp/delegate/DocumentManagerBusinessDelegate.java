/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
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
    protected final Map<RepositoryLocation, CloseableCoreSession> sessions = new HashMap<>();

    public void initialize() {
        log.debug("Seam component initialized...");
    }

    @Unwrap
    public CoreSession getDocumentManager() {
        /*
         * Explicit lookup, as this method is the only user of the Seam component. Also, in some cases (Seam remoting),
         * it seems that the injection is not done correctly.
         */
        RepositoryLocation currentServerLocation = (RepositoryLocation) Component.getInstance("currentServerLocation");
        return getDocumentManager(currentServerLocation);
    }

    @SuppressWarnings("resource") // session closed by remove()
    public CoreSession getDocumentManager(RepositoryLocation serverLocation) {

        if (serverLocation == null) {
            /*
             * currentServerLocation (factory in ServerContextBean) is set through navigationContext, which itself
             * injects documentManager, so it will be null the first time.
             */
            return null;
        }

        CloseableCoreSession session = sessions.get(serverLocation);
        if (session == null) {
            if (Lifecycle.isDestroying()) {
                /*
                 * During Seam component destroy phases, we don't want to recreate a core session just for injection.
                 * This happens during logout when the session context is destroyed; we don't want to cause EJB calls in
                 * this case as the authentication wouldn't work.
                 */
                return null;
            }
            String serverName = serverLocation.getName();
            session = CoreInstance.openCoreSession(serverName);
            log.debug("Opened session for repository " + serverName);
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
                if (Framework.getRuntime() != null) {
                    lc = Framework.login();
                }
            } catch (LoginException le) {
                log.error("Unable to login as System", le);
                log.warn("...try to feed CoreSession(s) without system login ...");
            }
            for (Entry<RepositoryLocation, CloseableCoreSession> entry : sessions.entrySet()) {
                String serverName = entry.getKey().getName();
                CloseableCoreSession session = entry.getValue();
                session.close();
                log.debug("Closed session for repository " + serverName);
            }
        } finally {
            if (lc != null) {
                try {
                    lc.logout();
                } catch (LoginException lo) {
                    log.error("Error when logout", lo);
                }
            }
            sessions.clear();
        }
    }
}
