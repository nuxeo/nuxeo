/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.mail.listener;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.mail.AuthenticationFailedException;
import javax.mail.MessagingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.platform.mail.utils.MailCoreHelper;

/**
 * Listener that listens for MailReceivedEvent.
 * <p>
 * The email connection corresponding to every MailFolder document found in the repository is checked for new incoming
 * email.
 *
 * @author Catalin Baican
 */
public class MailEventListener implements EventListener {

    public static final String EVENT_NAME = "MailReceivedEvent";

    public static final String PIPE_NAME = "nxmail";

    public static final String INBOX = "INBOX";

    private static final Log log = LogFactory.getLog(MailEventListener.class);

    protected Lock lock = new ReentrantLock();

    @Override
    public void handleEvent(Event event) {
        String eventId = event.getName();

        if (!EVENT_NAME.equals(eventId)) {
            return;
        }

        if (lock.tryLock()) {
            try {
                doHandleEvent(event);
            } finally {
                lock.unlock();
            }
        } else {
            log.info("Skip email check since it is already running");
        }

    }

    public void doHandleEvent(Event event) {

        try {
            CoreSession coreSession = CoreInstance.getCoreSession(null);
            StringBuilder query = new StringBuilder();
            query.append("SELECT * FROM MailFolder ");
            query.append(" WHERE ecm:isTrashed = 0 ");
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
        } catch (MessagingException e) {
            log.error("MailEventListener error...", e);
        }
    }

}
