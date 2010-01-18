/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.ecm.platform.mail.listener;

import javax.mail.AuthenticationFailedException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.platform.mail.utils.MailCoreHelper;
import org.nuxeo.runtime.api.Framework;

/**
 * Listener that listens for MailReceivedEvent.
 * <p>
 * The email connection corresponding to every MailFolder document found in the
 * repository is checked for new incoming email.
 *
 * @author Catalin Baican
 */
public class MailEventListener implements EventListener {

    public static final String EVENT_NAME = "MailReceivedEvent";

    public static final String PIPE_NAME = "nxmail";

    public static final String INBOX = "INBOX";

    private static final Log log = LogFactory.getLog(MailEventListener.class);

    public void handleEvent(Event event) {
        String eventId = event.getName();

        if (!EVENT_NAME.equals(eventId)) {
            return;
        }

        LoginContext loginContext = null;
        CoreSession coreSession = null;

        try {
            // open a system session
            loginContext = Framework.login();
            RepositoryManager mgr = Framework.getService(RepositoryManager.class);
            Repository repository = mgr.getDefaultRepository();
            if (repository != null) {
                coreSession = repository.open();
            }
            if (coreSession == null) {
                return;
            }

            StringBuilder query = new StringBuilder();
            query.append("SELECT * FROM MailFolder ");
            query.append(String.format(
                    " WHERE ecm:currentLifeCycleState != '%s' ",
                    LifeCycleConstants.DELETED_STATE));
            query.append(" AND ecm:isProxy = 0 ");
            DocumentModelList mailFolderList = coreSession.query(query.toString());

            for (DocumentModel currentMailFolder : mailFolderList) {
                try {
                    MailCoreHelper.checkMail(currentMailFolder, coreSession);
                } catch (AuthenticationFailedException e) {
                    // If authentication to an account fails, continue with
                    // the next folder
                    log.warn("Error connecting to email account", e);
                    continue;
                }
            }
        } catch (Exception e) {
            log.error("MailEventListener error...", e);
        } finally {
            if (coreSession != null) {
                CoreInstance.getInstance().close(coreSession);
            }
            if (loginContext != null) {
                try {
                    loginContext.logout();
                } catch (LoginException e) {
                }
            }
        }
    }

}
