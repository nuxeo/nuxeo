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
import javax.mail.SendFailedException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mvel2.PropertyAccessException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.PostCommitFilteringEventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.event.impl.ShallowDocumentModel;
import org.nuxeo.ecm.core.io.download.DownloadService;
import org.nuxeo.ecm.platform.ec.notification.email.EmailHelper;
import org.nuxeo.ecm.platform.ec.notification.service.NotificationService;
import org.nuxeo.ecm.platform.ec.notification.service.NotificationServiceHelper;
import org.nuxeo.ecm.platform.notification.api.Notification;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentViewCodecManager;
import org.nuxeo.ecm.platform.url.codec.api.DocumentViewCodec;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

public class NotificationEventListener implements PostCommitFilteringEventListener {

    private static final Log log = LogFactory.getLog(NotificationEventListener.class);

    private static final String CHECK_READ_PERMISSION_PROPERTY = "notification.check.read.permission";

    public static final String NOTIFICATION_DOCUMENT_ID_CODEC_NAME = "notificationDocId";

    public static final String JSF_NOTIFICATION_DOCUMENT_ID_CODEC_PREFIX = "nxdoc";

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
    public void handleEvent(EventBundle events) {

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
        if (!processEvents) {
            return;
        }
        for (Event event : events) {
            Boolean block = (Boolean) event.getContext()
                                           .getProperty(NotificationConstants.DISABLE_NOTIFICATION_SERVICE);
            if (block != null && block) {
                // ignore the event - we are blocked by the caller
                continue;
            }
            List<Notification> notifs = notificationService.getNotificationsForEvents(event.getName());
            if (notifs != null && !notifs.isEmpty()) {
                handleNotifications(event, notifs);
            }
        }

    }

    protected void handleNotifications(Event event, List<Notification> notifs) {

        EventContext ctx = event.getContext();
        DocumentEventContext docCtx;
        if (ctx instanceof DocumentEventContext) {
            docCtx = (DocumentEventContext) ctx;
        } else {
            log.warn("Can not handle notification on a event that is not bound to a DocumentEventContext");
            return;
        }

        if (docCtx.getSourceDocument() instanceof ShallowDocumentModel) {
            log.trace("Can not handle notification on a event that is bound to a ShallowDocument");
            return;
        }

        CoreSession coreSession = event.getContext().getCoreSession();
        Map<String, Serializable> properties = event.getContext().getProperties();
        Map<Notification, List<String>> targetUsers = new HashMap<>();

        for (NotificationListenerVeto veto : notificationService.getNotificationVetos()) {
            if (!veto.accept(event)) {
                return;
            }
        }

        for (NotificationListenerHook hookListener : notificationService.getListenerHooks()) {
            hookListener.handleNotifications(event);
        }

        gatherConcernedUsersForDocument(coreSession, docCtx.getSourceDocument(), notifs, targetUsers);

        for (Notification notif : targetUsers.keySet()) {
            if (!notif.getAutoSubscribed()) {
                for (String user : targetUsers.get(notif)) {
                    sendNotificationSignalForUser(notif, user, event, docCtx);
                }
            } else {
                Object recipientProperty = properties.get(NotificationConstants.RECIPIENTS_KEY);
                String[] recipients = null;
                if (recipientProperty != null) {
                    if (recipientProperty instanceof String[]) {
                        recipients = (String[]) properties.get(NotificationConstants.RECIPIENTS_KEY);
                    } else if (recipientProperty instanceof String) {
                        recipients = new String[1];
                        recipients[0] = (String) recipientProperty;
                    }
                }
                if (recipients == null) {
                    continue;
                }
                Set<String> users = new HashSet<>();
                for (String recipient : recipients) {
                    if (recipient == null) {
                        continue;
                    }
                    if (recipient.contains(NuxeoPrincipal.PREFIX)) {
                        users.add(recipient.replace(NuxeoPrincipal.PREFIX, ""));
                    } else if (recipient.contains(NuxeoGroup.PREFIX)) {
                        List<String> groupMembers = getGroupMembers(recipient.replace(NuxeoGroup.PREFIX, ""));
                        for (String member : groupMembers) {
                            users.add(member);
                        }
                    } else {
                        // test if the unprefixed recipient corresponds to a
                        // group, to fetch its members
                        if (NotificationServiceHelper.getUsersService().getGroup(recipient) != null) {
                            users.addAll(getGroupMembers(recipient));
                        } else {
                            users.add(recipient);
                        }
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
            userManager = Framework.getService(UserManager.class);
        }
        return userManager;
    }

    protected List<String> getGroupMembers(String groupId) {
        return getUserManager().getUsersInGroupAndSubGroups(groupId);
    }

    protected void sendNotificationSignalForUser(Notification notification, String subscriptor, Event event,
            DocumentEventContext ctx) {

        if (SecurityConstants.SYSTEM_USERNAME.equals(subscriptor)) {
            // it doesn't make sense to notify the system user
            return;
        }
        NuxeoPrincipal principal = getUserManager().getPrincipal(subscriptor);
        if (principal == null) {
            log.error("No Nuxeo principal found for '" + subscriptor
                    + "'. No notification will be sent to this user");
            return;
        }

        if (Boolean.parseBoolean(Framework.getProperty(CHECK_READ_PERMISSION_PROPERTY))) {
            if (!ctx.getCoreSession().hasPermission(principal, ctx.getSourceDocument().getRef(),
                    SecurityConstants.READ)) {
                log.debug("Notification will not be sent: + '" + subscriptor
                        + "' do not have Read permission on document " + ctx.getSourceDocument().getId());
                return;
            }
        }

        log.debug("Producing notification message.");

        Map<String, Serializable> eventInfo = ctx.getProperties();
        DocumentModel doc = ctx.getSourceDocument();
        String author = ctx.getPrincipal().getName();
        Calendar created = (Calendar) ctx.getSourceDocument().getPropertyValue("dc:created");

        // Get notification document codec
        DocumentViewCodecManager codecService = Framework.getService(DocumentViewCodecManager.class);
        DocumentViewCodec codec = codecService.getCodec(NOTIFICATION_DOCUMENT_ID_CODEC_NAME);
        boolean isNotificationCodec = codec != null;
        boolean isJSFUI = isNotificationCodec && JSF_NOTIFICATION_DOCUMENT_ID_CODEC_PREFIX.equals(codec.getPrefix());

        eventInfo.put(NotificationConstants.IS_JSF_UI, isJSFUI);
        eventInfo.put(NotificationConstants.DESTINATION_KEY, subscriptor);
        eventInfo.put(NotificationConstants.NOTIFICATION_KEY, notification);
        eventInfo.put(NotificationConstants.DOCUMENT_ID_KEY, doc.getId());
        eventInfo.put(NotificationConstants.DATE_TIME_KEY, new Date(event.getTime()));
        eventInfo.put(NotificationConstants.AUTHOR_KEY, author);
        eventInfo.put(NotificationConstants.DOCUMENT_VERSION, doc.getVersionLabel());
        eventInfo.put(NotificationConstants.DOCUMENT_STATE, doc.getCurrentLifeCycleState());
        eventInfo.put(NotificationConstants.DOCUMENT_CREATED, created.getTime());
        if (isNotificationCodec) {
            StringBuilder userUrl = new StringBuilder();
            userUrl.append(notificationService.getServerUrlPrefix());
            if (!isJSFUI) {
                userUrl.append("ui/");
                userUrl.append("#!/");
            }
            userUrl.append("user/").append(ctx.getPrincipal().getName());
            eventInfo.put(NotificationConstants.USER_URL_KEY, userUrl.toString());
        }
        eventInfo.put(NotificationConstants.DOCUMENT_LOCATION, doc.getPathAsString());
        // Main file link for downloading
        BlobHolder bh = doc.getAdapter(BlobHolder.class);
        if (bh != null && bh.getBlob() != null) {
            DownloadService downloadService = Framework.getService(DownloadService.class);
            String filename = bh.getBlob().getFilename();
            String docMainFile = notificationService.getServerUrlPrefix()
                    + downloadService.getDownloadUrl(doc, DownloadService.BLOBHOLDER_0, filename);
            eventInfo.put(NotificationConstants.DOCUMENT_MAIN_FILE, docMainFile);
        }

        if (!isDeleteEvent(event.getName())) {
            if (isNotificationCodec) {
                eventInfo.put(NotificationConstants.DOCUMENT_URL_KEY,
                        codecService.getUrlFromDocumentView(NOTIFICATION_DOCUMENT_ID_CODEC_NAME,
                                new DocumentViewImpl(doc), true, notificationService.getServerUrlPrefix()));
            }
            eventInfo.put(NotificationConstants.DOCUMENT_TITLE_KEY, doc.getTitle());
        }

        if (isInterestedInNotification(notification)) {
            sendNotification(event, ctx);
            if (log.isDebugEnabled()) {
                log.debug("notification " + notification.getName() + " sent to " + notification.getSubject());
            }
        }
    }

    public void sendNotification(Event event, DocumentEventContext ctx) {

        String eventId = event.getName();
        log.debug("Received a message for notification sender with eventId : " + eventId);

        Map<String, Serializable> eventInfo = ctx.getProperties();
        String userDest = (String) eventInfo.get(NotificationConstants.DESTINATION_KEY);
        NotificationImpl notif = (NotificationImpl) eventInfo.get(NotificationConstants.NOTIFICATION_KEY);

        // send email
        NuxeoPrincipal recepient = NotificationServiceHelper.getUsersService().getPrincipal(userDest);
        if (recepient == null) {
            log.error("Couldn't find user: " + userDest + " to send her a mail.");
            return;
        }
        String email = recepient.getEmail();
        if (email == null || "".equals(email)) {
            log.error("No email found for user: " + userDest);
            return;
        }

        String subjectTemplate = notif.getSubjectTemplate();

        String mailTemplate = null;
        // mail template can be dynamically computed from a MVEL expression
        if (notif.getTemplateExpr() != null) {
            try {
                mailTemplate = emailHelper.evaluateMvelExpresssion(notif.getTemplateExpr(), eventInfo);
            } catch (PropertyAccessException pae) {
                if (log.isDebugEnabled()) {
                    log.debug("Cannot evaluate mail template expression '" + notif.getTemplateExpr()
                            + "' in that context " + eventInfo, pae);
                }
            }
        }
        // if there is no mailTemplate evaluated, use the defined one
        if (StringUtils.isEmpty(mailTemplate)) {
            mailTemplate = notif.getTemplate();
        }

        log.debug("email: " + email);
        log.debug("mail template: " + mailTemplate);
        log.debug("subject template: " + subjectTemplate);

        Map<String, Object> mail = new HashMap<>();
        mail.put("mail.to", email);

        String authorUsername = (String) eventInfo.get(NotificationConstants.AUTHOR_KEY);

        if (authorUsername != null) {
            NuxeoPrincipal author = NotificationServiceHelper.getUsersService().getPrincipal(authorUsername);
            mail.put(NotificationConstants.PRINCIPAL_AUTHOR_KEY, author);
        }

        mail.put(NotificationConstants.DOCUMENT_KEY, ctx.getSourceDocument());
        String subject = notif.getSubject() == null ? NotificationConstants.NOTIFICATION_KEY : notif.getSubject();
        subject = notificationService.getEMailSubjectPrefix() + subject;
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
            String cause = "";
            if ((e instanceof SendFailedException) && (e.getCause() instanceof SendFailedException)) {
                cause = " - Cause: " + e.getCause().getMessage();
            }
            log.warn("Failed to send notification email to '" + email + "': " + e.getClass().getName() + ": "
                    + e.getMessage() + cause);
        }
    }

    /**
     * Adds the concerned users to the list of targeted users for these notifications.
     */
    private void gatherConcernedUsersForDocument(CoreSession coreSession, DocumentModel doc, List<Notification> notifs,
            Map<Notification, List<String>> targetUsers) {
        if (doc.getPath().segmentCount() > 1) {
            log.debug("Searching document: " + doc.getName());
            getInterstedUsers(doc, notifs, targetUsers);
            if (doc.getParentRef() != null && coreSession.exists(doc.getParentRef())) {
                DocumentModel parent = getDocumentParent(coreSession, doc);
                gatherConcernedUsersForDocument(coreSession, parent, notifs, targetUsers);
            }
        }
    }

    private DocumentModel getDocumentParent(CoreSession coreSession, DocumentModel doc) {
        if (doc == null) {
            return null;
        }
        return coreSession.getDocument(doc.getParentRef());
    }

    private void getInterstedUsers(DocumentModel doc, List<Notification> notifs,
            Map<Notification, List<String>> targetUsers) {
        for (Notification notification : notifs) {
            if (!notification.getAutoSubscribed()) {
                List<String> userGroup = notificationService.getSubscribers(notification.getName(), doc);
                for (String subscriptor : userGroup) {
                    if (subscriptor != null) {
                        if (isUser(subscriptor)) {
                            storeUserForNotification(notification, subscriptor.substring(5), targetUsers);
                        } else {
                            // it is a group - get all users and send
                            // notifications to them
                            List<String> usersOfGroup = getGroupMembers(subscriptor.substring(6));
                            if (usersOfGroup != null && !usersOfGroup.isEmpty()) {
                                for (String usr : usersOfGroup) {
                                    storeUserForNotification(notification, usr, targetUsers);
                                }
                            }
                        }
                    }
                }
            } else {
                // An automatic notification happens
                // should be sent to interested users
                targetUsers.put(notification, new ArrayList<>());
            }
        }
    }

    private static void storeUserForNotification(Notification notification, String user,
            Map<Notification, List<String>> targetUsers) {
        List<String> subscribedUsers = targetUsers.computeIfAbsent(notification, k -> new ArrayList<>());
        if (!subscribedUsers.contains(user)) {
            subscribedUsers.add(user);
        }
    }

    private boolean isDeleteEvent(String eventId) {
        List<String> deletionEvents = new ArrayList<>();
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

    public EmailHelper getEmailHelper() {
        return emailHelper;
    }

    public void setEmailHelper(EmailHelper emailHelper) {
        this.emailHelper = emailHelper;
    }

}
