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
 * $Id:ContentHistoryBusinessDelegate.java 3895 2006-10-11 19:12:47Z janguenot $
 */

package org.nuxeo.ecm.platform.workflow.document.api.ejb.delegate;

import java.io.Serializable;
import java.util.Map;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Core document manager business delegate.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class CoreDocumentManagerBusinessDelegate implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory
            .getLog(CoreDocumentManagerBusinessDelegate.class);

    protected transient CoreSession documentManager;

    protected LoginContext login() throws LoginException {
        LoginContext ctx = Framework.login();
        return ctx;
    }

    protected void logout(LoginContext ctx) throws LoginException {
        if (ctx != null) {
            ctx.logout();
        }
    }

    public CoreSession getDocumentManager(String repositoryUri,
            Map<String, Object> sessionContext) throws ClientException {
        log.debug("getDocumentManager()");

        if (repositoryUri == null) {
            log.debug("No repository URI given as paramter.");
            return null;
        }

        // first destroy if needed
        if (documentManager != null) {
            log.trace("Removing the documentManager first.");
            documentManager = null;
        }

        // Open a system session. This session is unrestricted here.
        try {
            login();
        } catch (LoginException e) {
            throw new ClientException(e.getMessage());
        }

        try {
            RepositoryManager mgr = Framework.getService(RepositoryManager.class);
            documentManager = mgr.getRepository(repositoryUri).open();
        } catch (Exception e) {
            throw new ClientException(e.getMessage());
        }

        log.debug("DocumentManager bean found :"
                + documentManager.getClass().toString());
        return documentManager;
    }

}
