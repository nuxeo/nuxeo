/*
 * (C) Copyright 2006-2012 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 *
 */

package org.nuxeo.ecm.platform.ec.notification;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.mail.MessagingException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mvel2.PropertyAccessException;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.PostCommitFilteringEventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.ec.notification.email.EmailHelper;
import org.nuxeo.ecm.platform.ec.notification.service.NotificationService;
import org.nuxeo.ecm.platform.ec.notification.service.NotificationServiceHelper;
import org.nuxeo.ecm.platform.notification.api.Notification;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.api.DocumentViewCodecManager;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

public class NotificationEventListener implements
        PostCommitFilteringEventListener {

    private static final Log log = LogFactory.getLog(NotificationEventListener.class);

    private DocumentViewCodecManager docLocator;

    private UserManager userManager;

    private EmailHelper emailHelper = new EmailHelper();

    private NotificationService notificationService = NotificationServiceHelper.getNotificationService();

    @Override
    public boolean acceptEvent(Event event) {
        if (notificationService == null) {
            return false;
        }
        return notificationService.getNotificationEventNames().contains(event.getName());
    }

    @Override
    public void handleEvent(EventBundle events) throws ClientException {

        if (notificationService == null) {
            log.error("Unable to get NotificationService, exiting");
            return;
        }
        boolean processEvents = false;
        for (String name : notificationService.getNotificationEventNames()) {
            if (events.containsEventName(name)) {
                processEvents = true;
                break;
            }
        }
        if (! processEvents) {
            return;
        }
        for (Event event : events) {
            Boolean block = (Boolean)event.getContext().getProperty(NotificationConstants.DISABLE_NOTIFICATION_SERVICE);
            if (block != null && block) {
                // ignore the event - we are blocked by the caller
                continue;
            }
            List<Notification> notifs = notificationService.getNotificationsForEvents(event.getName());
            if (notifs != null && !notifs.isEmpty()) {
                try {
                    handleNotifications(event, notifs);
                } catch (Exception e) {
                    log.error("Error during Notification processing for event "
                            + event.getName(), e);
                }
            }
        }

    }

    protected void handleNotifications(Event event, List<Notification> notifs)
            throws Exception {

        EventContext ctx = event.getContext();
        DocumentEventContext docCtx = null;
        if (ctx instanceof DocumentEventContext) {
            docCtx = (DocumentEventContext) ctx;
        } else {
            log.warn("Can not handle notification on a event that is not bound to a DocumentEventContext");
            return;
        }

        CoreSession coreSession = event.getContext().getCoreSession();
        Map<String, Serializable> properties = event.getContext().getProperties();
        Map<Notification, List<String>> targetUsers = new HashMap<Notification, List<String>>();

        for (NotificationListenerVeto veto : notificationService.getNotificationVetos()) {
            if (!veto.accept(event)) {
                return;
            }
        }

        for(NotificationListenerHook hookListener:notificationService.getListenerHooks()) {
            hookListener.handleNotifications(event);
        }

        gatherConcernedUsersForDocument(coreSession,
                docCtx.getSourceDocument(), notifs, targetUsers);

        for (Notification notif : targetUsers.keySet()) {
            if (!notif.getAutoSubscribed()) {
                for (String user : targetUsers.get(notif)) {
                    sendNotificationSignalForUser(notif, user, event, docCtx);
                }
            } else {
                Object recipientProperty =  properties.get(NotificationConstants.RECIPIENTS_KEY);
                String[] recipients = null;
                if (recipientProperty != null) {
                    if (recipientProperty instanceof String[]) {
                        recipients = (String[]) properties.get(NotificationConstants.RECIPIENTS_KEY);
                    } else if (recipientProperty instanceof String ) {
                        recipients = new String[1];
                        recipients[0] = (String) recipientProperty;
                    }
                }
                if (recipients == null) {
                    continue;
                }
                Set<String> users = new HashSet<String>();
                for (String recipient : recipients) {
                    if (recipient == null) {
                        continue;
                    }
                    if (recipient.contains(NuxeoPrincipal.PREFIX)) {
                        users.add(recipient.replace(NuxeoPrincipal.PREFIX, ""));
                    } else if (recipient.contains(NuxeoGroup.PREFIX)) {
                        List<String> groupMembers = getGroupMembers(recipient.replace(
                                NuxeoGroup.PREFIX, ""));
                        for (String member : groupMembers) {
                            users.add(member);
                        }
                    } else {
                        users.add(recipient);
                    }

                }
                for (String user : users) {
                    sendNotificationSignalForUser(notif, user, event, docCtx);
                }

            }
        }

    }

    protected UserManager getUserManager() {
        if (userManager == null) {
            try {
                userManager = Framework.getService(UserManager.class);
            } catch (Exception e) {
                throw new IllegalStateException(
                        "UserManager service not deployed.", e);
            }
        }
        return userManager;
    }

    protected List<String> getGroupMembers(String groupId)
            throws ClientException {
        return getUserManager().getUsersInGroupAndSubGroups(groupId);
    }

    private void sendNotificationSignalForUser(Notification notification,
            String subscriptor, Event event, DocumentEventContext ctx)
            throws ClientException {

        log.debug("Producing notification message.");

        Map<String, Serializable> eventInfo = ctx.getProperties();
        DocumentModel doc = ctx.getSourceDocument();
        String author = ctx.getPrincipal().getName();
        Calendar created = (Calendar) ctx.getSourceDocument().getPropertyValue(
                "dc:created");

        eventInfo.put(NotificationConstants.DESTINATION_KEY, subscriptor);
        eventInfo.put(NotificationConstants.NOTIFICATION_KEY, notification);
        eventInfo.put(NotificationConstants.DOCUMENT_ID_KEY, doc.getId());
        eventInfo.put(NotificationConstants.DATE_TIME_KEY,
                new Date(event.getTime()));
        eventInfo.put(NotificationConstants.AUTHOR_KEY, author);
        eventInfo.put(NotificationConstants.DOCUMENT_VERSION,
                doc.getVersionLabel());
        eventInfo.put(NotificationConstants.DOCUMENT_STATE,
                doc.getCurrentLifeCycleState());
        eventInfo.put(NotificationConstants.DOCUMENT_CREATED, created.getTime());
        StringBuilder userUrl = new StringBuilder();
        userUrl.append(
                notificationService.getServerUrlPrefix()).append(
                "user/").append(ctx.getPrincipal().getName());
        eventInfo.put(NotificationConstants.USER_URL_KEY, userUrl.toString());
        eventInfo.put(NotificationConstants.DOCUMENT_LOCATION,
                doc.getPathAsString());
        // Main file link for downloading
        BlobHolder bh = doc.getAdapter(BlobHolder.class);
        if (bh != null && bh.getBlob() != null) {
            StringBuilder docMainFile = new StringBuilder();
            docMainFile.append(
                    notificationService.getServerUrlPrefix()).append(
                    "nxfile/default/").append(doc.getId()).append(
                    "/blobholder:0/").append(bh.getBlob().getFilename());
            eventInfo.put(NotificationConstants.DOCUMENT_MAIN_FILE,
                    docMainFile.toString());
        }

        if (!isDeleteEvent(event.getName())) {
            DocumentView docView = new DocumentViewImpl(doc);
            DocumentViewCodecManager docLocator = getDocLocator();
            if (docLocator != null) {
                eventInfo.put(
                        NotificationConstants.DOCUMENT_URL_KEY,
                        getDocLocator().getUrlFromDocumentView(
                                docView,
                                true,
                                notificationService.getServerUrlPrefix()));
            } else {
                eventInfo.put(NotificationConstants.DOCUMENT_URL_KEY, "");
            }
            eventInfo.put(NotificationConstants.DOCUMENT_TITLE_KEY,
                    doc.getTitle());
        }

        if (isInterestedInNotification(notification)) {
            try {
                sendNotification(event, ctx);
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

    public void sendNotification(Event event, DocumentEventContext ctx)
            throws ClientException {

        String eventId = event.getName();
        log.debug("Received a message for notification sender with eventId : "
                + eventId);

        Map<String, Serializable> eventInfo = ctx.getProperties();
        String userDest = (String) eventInfo.get(NotificationConstants.DESTINATION_KEY);
        NotificationImpl notif = (NotificationImpl) eventInfo.get(NotificationConstants.NOTIFICATION_KEY);

        // send email
        NuxeoPrincipal recepient = NotificationServiceHelper.getUsersService().getPrincipal(
                userDest);
        if (recepient == null) {
            log.error("Couldn't find user: " + userDest
                    + " to send her a mail.");
            return;
        }
        // XXX hack, principals have only one model
        DataModel model = recepient.getModel().getDataModels().values().iterator().next();
        String email = (String) model.getData("email");
        if (email == null || "".equals(email)) {
            log.error("No email found for user: " + userDest);
            return;
        }

        String subjectTemplate = notif.getSubjectTemplate();

        String mailTemplate = null;
        // mail template can be dynamically computed from a MVEL expression
        if (notif.getTemplateExpr() != null) {
            try {
                mailTemplate = emailHelper.evaluateMvelExpresssion(
                        notif.getTemplateExpr(), eventInfo);
            } catch (PropertyAccessException pae) {
                if (log.isDebugEnabled()) {
                    log.debug("Cannot evaluate mail template expression '"
                            + notif.getTemplateExpr() + "' in that context "
                            + eventInfo, pae);
                }
            } catch (Exception e) {
                log.error(e);
            }
        }
        // if there is no mailTemplate evaluated, use the defined one
        if (StringUtils.isEmpty(mailTemplate)) {
            mailTemplate = notif.getTemplate();
        }

        log.debug("email: " + email);
        log.debug("mail template: " + mailTemplate);
        log.debug("subject template: " + subjectTemplate);

        Map<String, Object> mail = new HashMap<String, Object>();
        mail.put("mail.to", email);

        String authorUsername = (String) eventInfo.get(NotificationConstants.AUTHOR_KEY);

        if (authorUsername != null) {
            NuxeoPrincipal author = NotificationServiceHelper.getUsersService().getPrincipal(
                    authorUsername);
            mail.put(NotificationConstants.PRINCIPAL_AUTHOR_KEY, author);
        }

        mail.put(NotificationConstants.DOCUMENT_KEY, ctx.getSourceDocument());
        String subject = notif.getSubject() == null ? NotificationConstants.NOTIFICATION_KEY
                : notif.getSubject();
        subject = notificationService.getEMailSubjectPrefix()
                + subject;
        mail.put("subject", subject);
        mail.put("template", mailTemplate);
        mail.put("subjectTemplate", subjectTemplate);

        // Transferring all data from event to email
        for (String key : eventInfo.keySet()) {
            mail.put(key, eventInfo.get(key) == null ? "" : eventInfo.get(key));
            log.debug("Mail prop: " + key);
        }

        mail.put(NotificationConstants.EVENT_ID_KEY, eventId);

        try {
            emailHelper.sendmail(mail);
        } catch (MessagingException e) {
            log.warn("Failed to send notification email to '" + email + "': "
                    + e.getClass().getName() + ": " + e.getMessage());
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                StringBuilder sb = new StringBuilder(
                        "Failed to send email with these properties:\n");
                for (String key : mail.keySet()) {
                    sb.append("\t " + key + ": " + mail.get(key) + "\n");
                }
                log.debug(sb.toString());
            }
            throw new ClientException("Failed to send notification email ", e);
        }
    }

    /**
     * Adds the concerned users to the list of targeted users for these
     * notifications.
     */
    private void gatherConcernedUsersForDocument(CoreSession coreSession,
            DocumentModel doc, List<Notification> notifs,
            Map<Notification, List<String>> targetUsers) throws Exception {
        if (doc.getPath().segmentCount() > 1) {
            log.debug("Searching document: " + doc.getName());
            getInterstedUsers(doc, notifs, targetUsers);
            if (doc.getParentRef() != null && coreSession.exists(doc.getParentRef())) {
                DocumentModel parent = getDocumentParent(coreSession, doc);
                gatherConcernedUsersForDocument(coreSession, parent, notifs,
                        targetUsers);
            }
        }
    }

    private DocumentModel getDocumentParent(CoreSession coreSession,
            DocumentModel doc) throws ClientException {
        if (doc == null) {
            return null;
        }
        return coreSession.getDocument(doc.getParentRef());
    }

    private void getInterstedUsers(DocumentModel doc,
            List<Notification> notifs,
            Map<Notification, List<String>> targetUsers) throws Exception {
        for (Notification notification : notifs) {
            if (!notification.getAutoSubscribed()) {
                List<String> userGroup = notificationService.getSubscribers(
                        notification.getName(), doc.getId());
                for (String subscriptor : userGroup) {
                    if (subscriptor != null) {
                        if (isUser(subscriptor)) {
                            storeUserForNotification(notification,
                                    subscriptor.substring(5), targetUsers);
                        } else {
                            // it is a group - get all users and send
                            // notifications to them
                            List<String> usersOfGroup = getGroupMembers(subscriptor.substring(6));
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
                // should be sent to interested users
                targetUsers.put(notification, new ArrayList<String>());
            }
        }
    }

    private static void storeUserForNotification(Notification notification,
            String user, Map<Notification, List<String>> targetUsers) {
        List<String> subscribedUsers = targetUsers.get(notification);
        if (subscribedUsers == null) {
            targetUsers.put(notification, new ArrayList<String>());
        }
        if (!targetUsers.get(notification).contains(user)) {
            targetUsers.get(notification).add(user);
        }
    }

    private boolean isDeleteEvent(String eventId) {
        List<String> deletionEvents = new ArrayList<String>();
        deletionEvents.add("aboutToRemove");
        deletionEvents.add("documentRemoved");
        return deletionEvents.contains(eventId);
    }

    private boolean isUser(String subscriptor) {
        return subscriptor != null && subscriptor.startsWith("user:");
    }

    public boolean isInterestedInNotification(Notification notif) {
        return notif != null && "email".equals(notif.getChannel());
    }

    public DocumentViewCodecManager getDocLocator() {
        if (docLocator == null) {
            try {
                docLocator = Framework.getService(DocumentViewCodecManager.class);
                if (docLocator == null) {
                    log.warn("Could not get service for document view manager");
                }
            } catch (Exception e) {
                log.info("Could not get service for document view manager");
            }
        }
        return docLocator;
    }

    public EmailHelper getEmailHelper() {
        return emailHelper;
    }

    public void setEmailHelper(EmailHelper emailHelper) {
        this.emailHelper = emailHelper;
    }

}
