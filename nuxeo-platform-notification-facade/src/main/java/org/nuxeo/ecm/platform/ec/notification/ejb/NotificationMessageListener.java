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
 * $Id:NXAuditMessageListener.java 1583 2006-08-04 10:26:40Z janguenot $
 */

package org.nuxeo.ecm.platform.ec.notification.ejb;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJBException;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.platform.ec.notification.NotificationImpl;
import org.nuxeo.ecm.platform.ec.notification.email.EmailHelper;
import org.nuxeo.ecm.platform.ec.notification.service.NotificationService;
import org.nuxeo.ecm.platform.ec.notification.service.NotificationServiceHelper;
import org.nuxeo.ecm.platform.events.api.DocumentMessage;
import org.nuxeo.ecm.platform.notification.api.Notification;
import org.nuxeo.ecm.platform.url.DocumentLocationImpl;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentLocation;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.api.DocumentViewCodecManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Message Driven Bean listening for events, responsible for notifications.
 * <p>
 * It does:
 * <ul>
 * <li>(1) Get the message from the topic/NXCoreMessages</li>
 * <li>(2) Gets the notifications that contain the event</li>
 * <li>(3) Navigates upwards in document tree and gets the users that
 * subscribed to the subscriptions</li>
 * <li>(4) Removes the duplicates so that a user would not receive the same
 * notification twice</li>
 * <li>(5) Send notifications according to the channel (email, jabber, etc)</li>
 * </ul>
 *
 * @author <a mailto="npaslaru@nuxeo.com">Narcis Paslaru</a>
 */

@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "topic/NXPMessages"),
        @ActivationConfigProperty(propertyName = "providerAdapterJNDI", propertyValue = "java:/NXCoreEventsProvider"),
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge") })
@TransactionManagement(TransactionManagementType.CONTAINER)
public class NotificationMessageListener implements MessageListener {

    private static final Log log = LogFactory.getLog(NotificationMessageListener.class);

    private DocumentViewCodecManager docLocator;

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void onMessage(Message message) {
        log.debug("onMessage");
        try {

            final Serializable obj = ((ObjectMessage) message).getObject();
            if (!(obj instanceof DocumentMessage)) {
                log.debug("Not a DocumentMessage instance embedded ignoring.");
                return;
            }

            DocumentMessage doc = (DocumentMessage) obj;

            String eventId = doc.getEventId();
            log.debug("Recieved a message for notification with eventId : "
                    + eventId);

            NotificationService service = NotificationServiceHelper.getNotificationService();

            // (2)
            List<Notification> notifs = service.getNotificationRegistry().getNotificationsForEvent(
                    eventId);
            if (notifs == null || notifs.isEmpty()) {
                return;
            }

            // (3)
            Map<Notification, List<String>> targetUsers = new HashMap<Notification, List<String>>();

            LoginContext lc = Framework.login();
            gatherConcernedUsersForDocument(doc, notifs, targetUsers);
            lc.logout();

            for (Notification notif : targetUsers.keySet()) {
                if (!notif.getAutoSubscribed()) {
                    for (String user : targetUsers.get(notif)) {
                        sendNotificationSignalForUser(notif, user, doc);
                    }
                } else {
                    Map<String, Serializable> info = doc.getEventInfo();
                    String recipient = (String) info.get("recipients");
                    if (recipient == null || recipient.trim().length() <= 0) {
                        // nobody to send notification to
                        log.warn("An autosubscribed notification ["+eventId+"] was requested but found no target adress");
                        continue;
                    }
                    List<String> users = getUsersForMultiRecipients(recipient);
                    for (String user : users) {
                        sendNotificationSignalForUser(notif, user, doc);
                    }

                }
            }

        } catch (Exception e) {
            throw new EJBException(e);
        }
    }

    private List<String> getUsersForMultiRecipients(String recipient)
            throws ClientException {
        String[] recipients = recipient.split("\\|");
        List<String> users = new ArrayList<String>();

        for (String user : recipients) {
            users.addAll(getUsersForRecipient(user));
        }
        return users;
    }

    private List<String> getUsersForRecipient(String recipient)
            throws ClientException {
        List<String> users = new ArrayList<String>();
        if (isUser(recipient)) {
            users.add(recipient.substring(5));
        } else {
            // it is a group - get all users and send
            // notifications to them
            List<String> usersOfGroup = NotificationServiceHelper.getUsersService().getUsersInGroup(
                    recipient.substring(6));
            if (usersOfGroup != null && !usersOfGroup.isEmpty()) {
                users.addAll(usersOfGroup);
            }
        }
        return users;
    }

    /**
     * Adds the concerned users to the list of targeted users for these
     * notifications.
     *
     * @param doc
     * @param notifs
     * @param targetUsers
     * @throws Exception
     */
    private void gatherConcernedUsersForDocument(DocumentModel doc,
            List<Notification> notifs,
            Map<Notification, List<String>> targetUsers) throws Exception {
        if (doc.getPath().segmentCount() > 1) {
            log.debug("Searching document.... : " + doc.getName());
            getInterstedUsers(doc, notifs, targetUsers);
            DocumentModel parent = getDocumentParent(doc);
            gatherConcernedUsersForDocument(parent, notifs, targetUsers);
        }
    }

    private DocumentModel getDocumentParent(DocumentModel doc)
            throws ClientException {

        DocumentModel parentDoc = null;

        if (doc == null) {
            return parentDoc;
        }

        // Create a new system session.
        try {
            LoginContext lc = Framework.login();
            RepositoryManager mgr = Framework.getService(RepositoryManager.class);
            String repoName = doc.getRepositoryName();
            if (repoName != null) {
                Repository repo = mgr.getRepository(repoName);
                if (repo != null) {
                    CoreSession coreSession = repo.open();
                    parentDoc = coreSession.getDocument(doc.getParentRef());
                    CoreInstance.getInstance().close(coreSession);
                } else {
                    throw new ClientException(
                            "Cannot find repository instance : + " + repoName);
                }
            } else {
                throw new ClientException("No associated repository...");
            }
            lc.logout();
        } catch (Exception e) {
            throw new ClientException(e);
        }

        return parentDoc;
    }

    private void getInterstedUsers(DocumentModel doc,
            List<Notification> notifs,
            Map<Notification, List<String>> targetUsers) throws Exception {
        for (Notification notification : notifs) {
            if (!notification.getAutoSubscribed()) {
                List<String> userGroup = NotificationServiceHelper.getNotificationService().getSubscribers(
                        notification.getName(), doc.getId());
                for (String subscriptor : userGroup) {
                    if (subscriptor != null) {
                        if (isUser(subscriptor)) {
                            storeUserForNotification(notification,
                                    subscriptor.substring(5), targetUsers);
                        } else {
                            // it is a group - get all users and send
                            // notifications to them
                            List<String> usersOfGroup = NotificationServiceHelper.getUsersService().getUsersInGroup(
                                    subscriptor.substring(6));
                            if (usersOfGroup != null && !usersOfGroup.isEmpty()) {
                                for (String usr : usersOfGroup) {
                                    storeUserForNotification(notification, usr,
                                            targetUsers);
                                }
                            }
                        }
                    }
                }
            } else {
                // An automatic notification happens
                // should be sent to intersted users
                targetUsers.put(notification, new ArrayList<String>());
            }
        }
    }

    private void storeUserForNotification(Notification notification,
            String user, Map<Notification, List<String>> targetUsers) {
        List<String> subscribedUsers = targetUsers.get(notification);
        if (subscribedUsers == null) {
            targetUsers.put(notification, new ArrayList<String>());
        }
        if (!targetUsers.get(notification).contains(user)) {
            targetUsers.get(notification).add(user);
        }
    }

    private void sendNotificationSignalForUser(Notification notification,
            String subscriptor, DocumentMessage message) throws ClientException {

        log.debug("Producing notification message...........");

        Map<String, Serializable> eventInfo = message.getEventInfo();

        eventInfo.put("destination", subscriptor);
        eventInfo.put("notification", notification);
        eventInfo.put("docId", message.getId());
        eventInfo.put("dateTime", message.getEventDate());
        eventInfo.put("author", message.getPrincipalName());

        if (! documentHasBeenDeleted(message)){
            DocumentLocation docLoc = new DocumentLocationImpl(
                    message.getRepositoryName(), message.getRef());
            DocumentView docView = new DocumentViewImpl(docLoc);
            docView.setViewId("view_documents");
            eventInfo.put(
                    "docUrl",
                    getDocLocator().getUrlFromDocumentView(
                            docView,
                            true,
                            NotificationServiceHelper.getNotificationService().getServerUrlPrefix()));

            try {
                LoginContext lc = Framework.login();
                eventInfo.put("docTitle", message.getTitle());
//                eventInfo.put("document", message);
                lc.logout();
            } catch (LoginException le) {
                log.debug("Exception at reconnect !!! - no document data will be available in the template");
            }
        }
        if (isInterestedInNotification(notification)) {

            try {
                sendNotification(message);
                if (log.isDebugEnabled()) {
                    log.debug("notification " + notification.getName()
                            + " sent to " + notification.getSubject());
                }
            } catch (ClientException e) {
                log.error(
                        "An error occurred while trying to send user notification",
                        e);
            }

        }
    }

    private boolean documentHasBeenDeleted(DocumentMessage message) {
        List<String> deletionEvents = new ArrayList<String>();
        deletionEvents.add("aboutToRemove");
        deletionEvents.add("documentRemoved");
        return deletionEvents.contains(message.getEventId());
    }

    private boolean isUser(String subscriptor) {
        return subscriptor != null && subscriptor.startsWith("user:");
    }

    public boolean isInterestedInNotification(Notification notif) {
        return notif != null && "email".equals(notif.getChannel());
    }

    public void sendNotification(DocumentMessage docMessage)
            throws ClientException {

        String eventId = docMessage.getEventId();
        log.debug("Recieved a message for notification sender with eventId : "
                + eventId);

        Map<String, Serializable> eventInfo = docMessage.getEventInfo();
        String userDest = (String) eventInfo.get("destination");
        NotificationImpl notif = (NotificationImpl) eventInfo.get("notification");

        // send email
        NuxeoPrincipal recepient = NotificationServiceHelper.getUsersService().getPrincipal(
                userDest);
        // XXX hack, principals have only one model
        DataModel model = recepient.getModel().getDataModels().values().iterator().next();
        String email = (String) model.getData("email");
        String mailTemplate = notif.getTemplate();

        log.debug("email: " + email);
        log.debug("mail template: " + mailTemplate);

        Map<String, Object> mail = new HashMap<String, Object>();
        mail.put("mail.to", email);

        String authorUsername = (String) eventInfo.get("author");

        if (authorUsername != null) {
            NuxeoPrincipal author = NotificationServiceHelper.getUsersService().getPrincipal(
                    authorUsername);
            mail.put("principalAuthor", author);
        }

        mail.put("document", (DocumentModel)docMessage);
        String subject = notif.getSubject() == null ? "Notification"
                : notif.getSubject();
        subject = NotificationServiceHelper.getNotificationService().getEMailSubjectPrefix()
                + subject;
        mail.put("subject", subject);
        mail.put("template", mailTemplate);

        // Transferring all data from event to email
        for (String key : eventInfo.keySet()) {
            mail.put(key, eventInfo.get(key));
            log.debug("Mail prop: " + key);
        }

        mail.put("eventId", eventId);

        try {
            EmailHelper.sendmail(mail);
        } catch (Exception e) {
            throw new ClientException("Failed to send notification email ", e);
        }
    }

    public DocumentViewCodecManager getDocLocator() {
        if (docLocator == null) {
            try {
                docLocator = Framework.getService(DocumentViewCodecManager.class);
            } catch (Exception e) {
                log.info("Could not get service for document view manager");
            }
        }

        return docLocator;
    }

}
