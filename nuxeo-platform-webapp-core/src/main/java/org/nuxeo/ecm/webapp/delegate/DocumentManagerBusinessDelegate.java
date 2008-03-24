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

import javax.annotation.security.PermitAll;
import javax.ejb.EJBAccessException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.Component;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Unwrap;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.platform.ui.web.shield.NuxeoJavaBeanErrorHandler;
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
@NuxeoJavaBeanErrorHandler
public class DocumentManagerBusinessDelegate implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(DocumentManagerBusinessDelegate.class);

    protected CoreSession documentManager;

    protected RepositoryLocation documentManagerServerLocation;

    @In(create = true, required = false)
    protected transient RepositoryLocation currentServerLocation;

    public void initialize() {
        log.debug("Seam component initialized...");
    }

    @Unwrap
    public CoreSession getDocumentManager() throws ClientException {
        return getDocumentManager(currentServerLocation);
    }

    public CoreSession getDocumentManager(RepositoryLocation serverLocation)
            throws ClientException {

        // XXX TD : for some reasons the currentServerLocation is not always
        // injected by Seam
        // typical reproduction case includes Seam remoting call
        // ==> pull from factory by hand
        if (serverLocation == null) {
            serverLocation = (RepositoryLocation) Component.getInstance("currentServerLocation");
            if (serverLocation == null) {
                // serverLocation (factory in ServerContextBean) is set through
                // navigationContext, which itself injects documentManager, so
                // it will be null the first time
                return null;
            }
        }

        if (documentManager == null || documentManagerServerLocation == null ||
                !documentManagerServerLocation.equals(serverLocation)) {
            try {
                RepositoryManager repositoryManager = Framework.getService(RepositoryManager.class);
                Repository repository = repositoryManager.getRepository(serverLocation.getName());
                documentManager = repository.open();
                documentManagerServerLocation = serverLocation;
                log.trace("documentManager retrieved for server " +
                        serverLocation.getName());
            } catch (Exception e) {
                throw new ClientException("Error opening repository", e);
            }
        }
        return documentManager;
    }

    @Destroy
    @PermitAll
    public void remove() {
        if (documentManager == null) {
            return;
        }
        try {
            RepositoryManager repositoryManager = Framework.getService(RepositoryManager.class);
            Repository repository = repositoryManager.getRepository(documentManagerServerLocation.getName());
            repository.close(documentManager);
        } catch (EJBAccessException e) {
            // CoreInstance.close tries to call coreSession.getSessionId()
            // which makes another EJB call; don't log an error for this.
            // XXX but this means we don't close the session correctly
            log.debug("EJBAccessException while closing documentManager");
        } catch (Exception e) {
            log.error("Error closing documentManager", e);
        } finally {
            documentManager = null;
            documentManagerServerLocation = null;
        }
    }

}
