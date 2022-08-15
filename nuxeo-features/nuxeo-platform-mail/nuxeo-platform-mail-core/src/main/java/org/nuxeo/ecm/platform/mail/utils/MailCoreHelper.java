/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.ecm.platform.mail.utils;

import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.CORE_SESSION_KEY;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.EMAILS_LIMIT_PROPERTY_NAME;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.EMAIL_PROPERTY_NAME;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.HOST_PROPERTY_NAME;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.IMAP;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.IMAPS;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.LEAVE_ON_SERVER_KEY;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.MIMETYPE_SERVICE_KEY;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.PARENT_PATH_KEY;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.PASSWORD_PROPERTY_NAME;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.POP3S;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.PORT_PROPERTY_NAME;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.PROTOCOL_TYPE_KEY;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.PROTOCOL_TYPE_PROPERTY_NAME;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.SOCKET_FACTORY_FALLBACK_PROPERTY_NAME;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.SOCKET_FACTORY_PORT_PROPERTY_NAME;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.SSL_PROTOCOLS_PROPERTY_NAME;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.STARTTLS_ENABLE_PROPERTY_NAME;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.mail.FetchProfile;
import javax.mail.Flags;
import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Store;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.trash.TrashService;
import org.nuxeo.ecm.platform.mail.action.ExecutionContext;
import org.nuxeo.ecm.platform.mail.action.MessageActionPipe;
import org.nuxeo.ecm.platform.mail.action.Visitor;
import org.nuxeo.ecm.platform.mail.listener.MailEventListener;
import org.nuxeo.ecm.platform.mail.service.MailService;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.mail.MailSessionBuilder;
import org.nuxeo.runtime.api.Framework;

import com.sun.mail.imap.IMAPFolder;

/**
 * Helper for Mail Core.
 *
 * @author Catalin Baican
 */
public final class MailCoreHelper {

    private static final Log log = LogFactory.getLog(MailEventListener.class);

    public static final String PIPE_NAME = "nxmail";

    public static final String INBOX = "INBOX";

    /**
     * @deprecated since 10.3, use {@link TrashService} instead
     */
    @Deprecated
    public static final String DELETED_LIFECYCLE_STATE = "deleted";

    public static final long EMAILS_LIMIT_DEFAULT = 100;

    private static MailService mailService;

    private static MimetypeRegistry mimeService;

    public static final String IMAP_DEBUG = "org.nuxeo.mail.imap.debug";

    protected static final CopyOnWriteArrayList<String> processingMailBoxes = new CopyOnWriteArrayList<>();

    // @since 2021.25
    protected static final String MESSAGE_LIMIT_OPTION = "org.nuxeo.mail.message.limit";

    // @since 2021.25
    protected static final int DEFAULT_MESSAGE_LIMIT = 2_000;

    private MailCoreHelper() {
    }

    private static MailService getMailService() {
        if (mailService == null) {
            mailService = Framework.getService(MailService.class);
        }
        return mailService;
    }

    private static MimetypeRegistry getMimeService() {
        if (mimeService == null) {
            mimeService = Framework.getService(MimetypeRegistry.class);
        }
        return mimeService;
    }

    /**
     * Creates MailMessage documents for every unread mail found in the INBOX. The parameters needed to connect to the
     * email INBOX are retrieved from the MailFolder document passed as a parameter.
     */
    public static void checkMail(DocumentModel currentMailFolder, CoreSession coreSession) throws MessagingException {

        if (processingMailBoxes.addIfAbsent(currentMailFolder.getId())) {
            try {
                doCheckMail(currentMailFolder, coreSession);
            } finally {
                processingMailBoxes.remove(currentMailFolder.getId());
            }
        } else {
            log.info("Mailbox " + currentMailFolder.getPathAsString() + " is already being processed");
        }
    }

    protected static void doCheckMail(DocumentModel currentMailFolder, CoreSession coreSession)
            throws MessagingException {
        String email = (String) currentMailFolder.getPropertyValue(EMAIL_PROPERTY_NAME);
        String password = (String) currentMailFolder.getPropertyValue(PASSWORD_PROPERTY_NAME);
        if (!StringUtils.isEmpty(email) && !StringUtils.isEmpty(password)) {
            mailService = getMailService();

            MessageActionPipe pipe = mailService.getPipe(PIPE_NAME);

            Visitor visitor = new Visitor(pipe);
            Thread.currentThread().setContextClassLoader(Framework.class.getClassLoader());

            // initialize context
            ExecutionContext initialExecutionContext = new ExecutionContext();

            initialExecutionContext.put(MIMETYPE_SERVICE_KEY, getMimeService());

            initialExecutionContext.put(PARENT_PATH_KEY, currentMailFolder.getPathAsString());

            initialExecutionContext.put(CORE_SESSION_KEY, coreSession);

            initialExecutionContext.put(LEAVE_ON_SERVER_KEY, Boolean.TRUE); // TODO should be an attribute in 'protocol'
                                                                            // schema

            Folder rootFolder = null;
            Store store = null;
            try {
                String protocolType = (String) currentMailFolder.getPropertyValue(PROTOCOL_TYPE_PROPERTY_NAME);
                initialExecutionContext.put(PROTOCOL_TYPE_KEY, protocolType);
                // log.debug(PROTOCOL_TYPE_KEY + ": " + (String) initialExecutionContext.get(PROTOCOL_TYPE_KEY));

                String host = (String) currentMailFolder.getPropertyValue(HOST_PROPERTY_NAME);
                String port = (String) currentMailFolder.getPropertyValue(PORT_PROPERTY_NAME);
                Boolean socketFactoryFallback = (Boolean) currentMailFolder.getPropertyValue(SOCKET_FACTORY_FALLBACK_PROPERTY_NAME);
                String socketFactoryPort = (String) currentMailFolder.getPropertyValue(SOCKET_FACTORY_PORT_PROPERTY_NAME);
                Boolean starttlsEnable = (Boolean) currentMailFolder.getPropertyValue(STARTTLS_ENABLE_PROPERTY_NAME);
                String sslProtocols = (String) currentMailFolder.getPropertyValue(SSL_PROTOCOLS_PROPERTY_NAME);
                Long emailsLimit = (Long) currentMailFolder.getPropertyValue(EMAILS_LIMIT_PROPERTY_NAME);
                long emailsLimitLongValue = emailsLimit == null ? EMAILS_LIMIT_DEFAULT : emailsLimit.longValue();

                String imapDebug = Framework.getProperty(IMAP_DEBUG, "false");

                Properties properties = new Properties();
                properties.put("mail.store.protocol", protocolType);
                // properties.put("mail.host", host);
                // Is IMAP connection
                if (IMAP.equals(protocolType)) {
                    properties.put("mail.imap.host", host);
                    properties.put("mail.imap.port", port);
                    properties.put("mail.imap.starttls.enable", starttlsEnable.toString());
                    properties.put("mail.imap.debug", imapDebug);
                    properties.put("mail.imap.partialfetch", "false");
                } else if (IMAPS.equals(protocolType)) {
                    properties.put("mail.imaps.host", host);
                    properties.put("mail.imaps.port", port);
                    properties.put("mail.imaps.starttls.enable", starttlsEnable.toString());
                    properties.put("mail.imaps.ssl.protocols", sslProtocols);
                    properties.put("mail.imaps.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                    properties.put("mail.imaps.socketFactory.fallback", socketFactoryFallback.toString());
                    properties.put("mail.imaps.socketFactory.port", socketFactoryPort);
                    properties.put("mail.imap.partialfetch", "false");
                    properties.put("mail.imaps.partialfetch", "false");
                } else if (POP3S.equals(protocolType)) {
                    properties.put("mail.pop3s.host", host);
                    properties.put("mail.pop3s.port", port);
                    properties.put("mail.pop3s.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                    properties.put("mail.pop3s.socketFactory.fallback", socketFactoryFallback.toString());
                    properties.put("mail.pop3s.socketFactory.port", socketFactoryPort);
                    properties.put("mail.pop3s.ssl.protocols", sslProtocols);
                } else {
                    // Is POP3 connection
                    properties.put("mail.pop3.host", host);
                    properties.put("mail.pop3.port", port);
                }

                properties.put("user", email);
                properties.put("password", password);

                store = MailSessionBuilder.fromProperties(properties).buildAndConnect();

                String folderName = INBOX; // TODO should be an attribute in 'protocol' schema
                rootFolder = store.getFolder(folderName);

                // need RW access to update message flags
                rootFolder.open(Folder.READ_WRITE);

                Message[] allMessages = rootFolder.getMessages(1, getMessageLimit(rootFolder));
                // VDU
                log.debug("nbr of messages in folder:" + allMessages.length);

                FetchProfile fetchProfile = new FetchProfile();
                fetchProfile.add(FetchProfile.Item.FLAGS);
                fetchProfile.add(FetchProfile.Item.ENVELOPE);
                fetchProfile.add(FetchProfile.Item.CONTENT_INFO);
                fetchProfile.add("Message-ID");
                fetchProfile.add("Content-Transfer-Encoding");

                rootFolder.fetch(allMessages, fetchProfile);

                if (rootFolder instanceof IMAPFolder) {
                    // ((IMAPFolder)rootFolder).doCommand(FetchProfile)
                }

                List<Message> unreadMessagesList = new ArrayList<>();
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

                Message[] unreadMessagesArray = unreadMessagesList.toArray(new Message[unreadMessagesList.size()]);

                // perform email import
                visitor.visit(unreadMessagesArray, initialExecutionContext);

                // perform flag update globally
                Flags flags = new Flags();
                flags.add(Flag.SEEN);

                boolean leaveOnServer = (Boolean) initialExecutionContext.get(LEAVE_ON_SERVER_KEY);
                if ((IMAP.equals(protocolType) || IMAPS.equals(protocolType)) && leaveOnServer) {
                    flags.add(Flag.SEEN);
                } else {
                    flags.add(Flag.DELETED);
                }
                rootFolder.setFlags(unreadMessagesArray, flags, true);

            } finally {
                if (rootFolder != null && rootFolder.isOpen()) {
                    rootFolder.close(true);
                }
                if (store != null) {
                    store.close();
                }
            }
        }
    }

    // @since 2021.25
    protected static int getMessageLimit(Folder mailbox) throws MessagingException {
        int messageCount = mailbox.getMessageCount();
        int limit = Integer.parseInt(
                Framework.getProperty(MESSAGE_LIMIT_OPTION, Integer.toString(DEFAULT_MESSAGE_LIMIT)));
        if (log.isDebugEnabled()) {
            log.debug("Mailbox: " + mailbox.getName() + " contains: " + messageCount + " messages, limit: " + limit);
        }
        if (messageCount > limit) {
            log.warn("Too many messages in mailbox: " + mailbox.getName() + " limit to " + limit + " out of "
                    + messageCount);
            return limit;
        }
        return messageCount;
    }

}
