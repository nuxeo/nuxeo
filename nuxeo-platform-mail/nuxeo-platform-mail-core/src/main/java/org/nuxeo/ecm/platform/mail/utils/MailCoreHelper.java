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

package org.nuxeo.ecm.platform.mail.utils;

import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.CORE_SESSION_ID_KEY;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.EMAIL_PROPERTY_NAME;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.EMAILS_LIMIT_PROPERTY_NAME;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.HOST_PROPERTY_NAME;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.IMAP;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.MIMETYPE_SERVICE_KEY;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.PARENT_PATH_KEY;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.PASSWORD_PROPERTY_NAME;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.PORT_PROPERTY_NAME;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.PROTOCOL_TYPE_PROPERTY_NAME;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.SOCKET_FACTORY_FALLBACK_PROPERTY_NAME;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.SOCKET_FACTORY_PORT_PROPERTY_NAME;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.SSL_PROTOCOLS_PROPERTY_NAME;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.STARTTLS_ENABLE_PROPERTY_NAME;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.IMAPS;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.POP3S;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.PROTOCOL_TYPE_KEY;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.LEAVE_ON_SERVER_KEY;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.mail.FetchProfile;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Flags.Flag;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.mail.action.ExecutionContext;
import org.nuxeo.ecm.platform.mail.action.MessageActionPipe;
import org.nuxeo.ecm.platform.mail.action.Visitor;
import org.nuxeo.ecm.platform.mail.service.MailService;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.ecm.platform.mail.listener.MailEventListener;
import org.nuxeo.runtime.api.Framework;

/**
 * Helper for Mail Core.
 *
 * @author Catalin Baican
 */
public final class MailCoreHelper {

    private static final Log log = LogFactory.getLog(MailEventListener.class);

    public static final String PIPE_NAME = "nxmail";

    public static final String INBOX = "INBOX";

    public static final String DELETED_LIFECYCLE_STATE = "deleted";

    public static final long EMAILS_LIMIT_DEFAULT = 100;

    private static MailService mailService;

    private static MimetypeRegistry mimeService;

    private MailCoreHelper() {
    }

    private static MailService getMailService() {
        if (mailService == null) {
            try {
                mailService = Framework.getService(MailService.class);
            } catch (Exception e) {
                log.error("Exception in get mail service");
            }
        }

        return mailService;
    }

    private static MimetypeRegistry getMimeService() {
        if (mimeService == null) {
            try {
                mimeService = Framework.getService(MimetypeRegistry.class);
            } catch (Exception e) {
                log.error("Exception in get mime service");
            }
        }

        return mimeService;
    }

    /**
     * Creates MailMessage documents for every unread mail found in the INBOX.
     * The parameters needed to connect to the email INBOX are retrieved from
     * the MailFolder document passed as a parameter.
     */
    public static void checkMail(DocumentModel currentMailFolder,
            CoreSession coreSession) throws Exception {
        String email = (String) currentMailFolder.getPropertyValue(EMAIL_PROPERTY_NAME);
        String password = (String) currentMailFolder.getPropertyValue(PASSWORD_PROPERTY_NAME);
        if (!StringUtils.isEmpty(email) && !StringUtils.isEmpty(password)) {
            mailService = getMailService();

            MessageActionPipe pipe = mailService.getPipe(PIPE_NAME);

            Visitor visitor = new Visitor(pipe);
            Thread.currentThread().setContextClassLoader(
                    Framework.class.getClassLoader());

            // initialize context
            ExecutionContext initialExecutionContext = new ExecutionContext();

            initialExecutionContext.put(MIMETYPE_SERVICE_KEY, getMimeService());

            initialExecutionContext.put(PARENT_PATH_KEY,
                    currentMailFolder.getPathAsString());

            initialExecutionContext.put(CORE_SESSION_ID_KEY,
                    coreSession.getSessionId());

            initialExecutionContext.put(LEAVE_ON_SERVER_KEY, Boolean.TRUE); // TODO should be an attribute in 'protocol' schema

            Folder rootFolder = null;
            try {
                String protocolType = (String) currentMailFolder.getPropertyValue(PROTOCOL_TYPE_PROPERTY_NAME);
                initialExecutionContext.put(PROTOCOL_TYPE_KEY, protocolType);
//                log.debug(PROTOCOL_TYPE_KEY + ": " + (String) initialExecutionContext.get(PROTOCOL_TYPE_KEY));

                String host = (String) currentMailFolder.getPropertyValue(HOST_PROPERTY_NAME);
                String port = (String) currentMailFolder.getPropertyValue(PORT_PROPERTY_NAME);
                Boolean socketFactoryFallback = (Boolean) currentMailFolder.getPropertyValue(SOCKET_FACTORY_FALLBACK_PROPERTY_NAME);
                String socketFactoryPort = (String) currentMailFolder.getPropertyValue(SOCKET_FACTORY_PORT_PROPERTY_NAME);
                Boolean starttlsEnable = (Boolean) currentMailFolder.getPropertyValue(STARTTLS_ENABLE_PROPERTY_NAME);
                String sslProtocols = (String) currentMailFolder.getPropertyValue(SSL_PROTOCOLS_PROPERTY_NAME);
                Long emailsLimit = (Long) currentMailFolder.getPropertyValue(EMAILS_LIMIT_PROPERTY_NAME);
                long emailsLimitLongValue = emailsLimit == null ? EMAILS_LIMIT_DEFAULT
                        : emailsLimit.longValue();

                Properties properties = new Properties();
                properties.put("mail.store.protocol", protocolType);
//                properties.put("mail.host", host);
                // Is IMAP connection
                if (IMAP.equals(protocolType)) {
                    properties.put("mail.imap.host", host);
                    properties.put("mail.imap.port", port);
                    properties.put("mail.imap.starttls.enable",
                            starttlsEnable.toString());
                    properties.put("mail.imap.debug","true");
                } else if (IMAPS.equals(protocolType)) {
                    properties.put("mail.imaps.host", host);
                    properties.put("mail.imaps.port", port);
                    properties.put("mail.imaps.starttls.enable",
                            starttlsEnable.toString());
                    properties.put("mail.imaps.ssl.protocols", sslProtocols);
                    properties.put("mail.imaps.socketFactory.class",
                    "javax.net.ssl.SSLSocketFactory");
                    properties.put("mail.imaps.socketFactory.fallback",
                            socketFactoryFallback.toString());
                    properties.put("mail.imaps.socketFactory.port",
                            socketFactoryPort);
                } else if (POP3S.equals(protocolType)) {
                    properties.put("mail.pop3s.host", host);
                    properties.put("mail.pop3s.port", port);
                    properties.put("mail.pop3s.socketFactory.class",
                    "javax.net.ssl.SSLSocketFactory");
                    properties.put("mail.pop3s.socketFactory.fallback",
                            socketFactoryFallback.toString());
                    properties.put("mail.pop3s.socketFactory.port",
                            socketFactoryPort);
                    properties.put("mail.pop3s.ssl.protocols", sslProtocols);
                } else {
                    // Is POP3 connection
                    properties.put("mail.pop3.host", host);
                    properties.put("mail.pop3.port", port);
                }

                properties.put("user", email);
                properties.put("password", password);

                Session session = Session.getInstance(properties);

                Store store = session.getStore();
                store.connect(email, password);

                String folderName = INBOX; // TODO should be an attribute in 'protocol' schema
                rootFolder = store.getFolder(folderName);

                rootFolder.open(Folder.READ_WRITE);

                Message[] allMessages = rootFolder.getMessages();
                // VDU
                log.debug("nbr of messages in folder:" + allMessages.length);

                FetchProfile fetchProfile = new FetchProfile();
                fetchProfile.add(FetchProfile.Item.FLAGS);
                rootFolder.fetch(allMessages, fetchProfile);

                List<Message> unreadMessagesList = new ArrayList<Message>();
                for (Message message : allMessages) {
                    Flags flags = message.getFlags();
                    int unreadMessagesListSize = unreadMessagesList.size();
                    if (flags != null && !flags.contains(Flag.SEEN) && unreadMessagesListSize < emailsLimitLongValue) {
                        unreadMessagesList.add(message);
                        if (unreadMessagesListSize == emailsLimitLongValue - 1) {
                            break;
                        }
                    }
                }

                // perform email import
                visitor.visit(
                        unreadMessagesList.toArray(new Message[unreadMessagesList.size()]),
                        initialExecutionContext);
            } finally {
                if (rootFolder != null && rootFolder.isOpen()) {
                    rootFolder.close(true);
                }
            }
        }
    }

}
