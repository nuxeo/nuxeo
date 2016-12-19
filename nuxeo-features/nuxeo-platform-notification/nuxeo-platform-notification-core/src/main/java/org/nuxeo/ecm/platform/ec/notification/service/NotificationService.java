/*
 * (C) Copyright 2007-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.ec.notification.service;

import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.mail.MessagingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.event.DocumentEventCategories;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;
import org.nuxeo.ecm.platform.dublincore.listener.DublinCoreListener;
import org.nuxeo.ecm.platform.ec.notification.NotificationConstants;
import org.nuxeo.ecm.platform.ec.notification.NotificationListenerHook;
import org.nuxeo.ecm.platform.ec.notification.NotificationListenerVeto;
import org.nuxeo.ecm.platform.ec.notification.SubscriptionAdapter;
import org.nuxeo.ecm.platform.ec.notification.email.EmailHelper;
import org.nuxeo.ecm.platform.notification.api.Notification;
import org.nuxeo.ecm.platform.notification.api.NotificationManager;
import org.nuxeo.ecm.platform.notification.api.NotificationRegistry;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.api.DocumentViewCodecManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;

/**
 * @author <a href="mailto:npaslaru@nuxeo.com">Narcis Paslaru</a>
 */
public class NotificationService extends DefaultComponent implements NotificationManager {

    public static final ComponentName NAME = new ComponentName(
            "org.nuxeo.ecm.platform.ec.notification.service.NotificationService");

    private static final Log log = LogFactory.getLog(NotificationService.class);

    public static final String SUBSCRIPTION_NAME = "UserSubscription";

    protected static final String NOTIFICATIONS_EP = "notifications";

    protected static final String TEMPLATES_EP = "templates";

    protected static final String GENERAL_SETTINGS_EP = "generalSettings";

    protected static final String NOTIFICATION_HOOK_EP = "notificationListenerHook";

    protected static final String NOTIFICATION_VETO_EP = "notificationListenerVeto";

    // FIXME: performance issue when putting URLs in a Map.
    protected static final Map<String, URL> TEMPLATES_MAP = new HashMap<>();

    protected EmailHelper emailHelper = new EmailHelper();

    protected GeneralSettingsDescriptor generalSettings;

    protected NotificationRegistry notificationRegistry;

    protected DocumentViewCodecManager docLocator;

    protected final Map<String, NotificationListenerHook> hookListeners = new HashMap<>();

    protected NotificationListenerVetoRegistry notificationVetoRegistry;

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(NotificationManager.class)) {
            return (T) this;
        }
        return null;
    }

    @Override
    public void activate(ComponentContext context) {
        notificationRegistry = new NotificationRegistryImpl();
        notificationVetoRegistry = new NotificationListenerVetoRegistry();

        // init default settings
        generalSettings = new GeneralSettingsDescriptor();
        generalSettings.serverPrefix = "http://localhost:8080/nuxeo/";
        generalSettings.eMailSubjectPrefix = "[Nuxeo]";
        generalSettings.mailSessionJndiName = "java:/Mail";
    }

    @Override
    public void deactivate(ComponentContext context) {
        notificationRegistry.clear();
        notificationVetoRegistry.clear();
        notificationRegistry = null;
        notificationVetoRegistry = null;
    }

    @Override
    public void registerExtension(Extension extension) {
        log.info("Registering notification extension");
        String xp = extension.getExtensionPoint();
        if (NOTIFICATIONS_EP.equals(xp)) {
            Object[] contribs = extension.getContributions();
            for (Object contrib : contribs) {
                NotificationDescriptor notifDesc = (NotificationDescriptor) contrib;
                notificationRegistry.registerNotification(notifDesc, getNames(notifDesc.getEvents()));
            }
        } else if (TEMPLATES_EP.equals(xp)) {
            Object[] contribs = extension.getContributions();
            for (Object contrib : contribs) {
                TemplateDescriptor templateDescriptor = (TemplateDescriptor) contrib;
                templateDescriptor.setContext(extension.getContext());
                registerTemplate(templateDescriptor);
            }
        } else if (GENERAL_SETTINGS_EP.equals(xp)) {
            Object[] contribs = extension.getContributions();
            for (Object contrib : contribs) {
                registerGeneralSettings((GeneralSettingsDescriptor) contrib);
            }
        } else if (NOTIFICATION_HOOK_EP.equals(xp)) {
            Object[] contribs = extension.getContributions();
            for (Object contrib : contribs) {
                NotificationListenerHookDescriptor desc = (NotificationListenerHookDescriptor) contrib;
                Class<? extends NotificationListenerHook> clazz = desc.hookListener;
                try {
                    NotificationListenerHook hookListener = clazz.newInstance();
                    registerHookListener(desc.name, hookListener);
                } catch (ReflectiveOperationException e) {
                    log.error(e);
                }
            }
        } else if (NOTIFICATION_VETO_EP.equals(xp)) {
            Object[] contribs = extension.getContributions();
            for (Object contrib : contribs) {
                NotificationListenerVetoDescriptor desc = (NotificationListenerVetoDescriptor) contrib;
                notificationVetoRegistry.addContribution(desc);
            }
        }
    }

    private void registerHookListener(String name, NotificationListenerHook hookListener) {
        hookListeners.put(name, hookListener);
    }

    protected void registerGeneralSettings(GeneralSettingsDescriptor desc) {
        generalSettings = desc;
        String serverPrefix = Framework.expandVars(generalSettings.serverPrefix);
        if (serverPrefix != null) {
            generalSettings.serverPrefix = serverPrefix.endsWith("//") ? serverPrefix.substring(0,
                    serverPrefix.length() - 1) : serverPrefix;
        }
        generalSettings.eMailSubjectPrefix = Framework.expandVars(generalSettings.eMailSubjectPrefix);
        generalSettings.mailSessionJndiName = Framework.expandVars(generalSettings.mailSessionJndiName);
    }

    private static List<String> getNames(List<NotificationEventDescriptor> events) {
        List<String> eventNames = new ArrayList<>();
        for (NotificationEventDescriptor descriptor : events) {
            eventNames.add(descriptor.name);
        }
        return eventNames;
    }

    @Override
    public void unregisterExtension(Extension extension) {
        String xp = extension.getExtensionPoint();
        if (NOTIFICATIONS_EP.equals(xp)) {
            Object[] contribs = extension.getContributions();
            for (Object contrib : contribs) {
                NotificationDescriptor notifDesc = (NotificationDescriptor) contrib;
                notificationRegistry.unregisterNotification(notifDesc, getNames(notifDesc.getEvents()));
            }
        } else if (TEMPLATES_EP.equals(xp)) {
            Object[] contribs = extension.getContributions();
            for (Object contrib : contribs) {
                TemplateDescriptor templateDescriptor = (TemplateDescriptor) contrib;
                templateDescriptor.setContext(extension.getContext());
                unregisterTemplate(templateDescriptor);
            }
        } else if (NOTIFICATION_VETO_EP.equals(xp)) {
            Object[] contribs = extension.getContributions();
            for (Object contrib : contribs) {
                NotificationListenerVetoDescriptor vetoDescriptor = (NotificationListenerVetoDescriptor) contrib;
                notificationVetoRegistry.removeContribution(vetoDescriptor);
            }
        }
    }

    public NotificationListenerVetoRegistry getNotificationListenerVetoRegistry() {
        return notificationVetoRegistry;
    }

    /**
     * @deprecated since 7.3
     * @see NotificationService#getSubscribers(String, DocumentModel)
     */
    @Deprecated
    @Override
    public List<String> getSubscribers(String notification, String docId) {
        return getSubscribers(notification, UnrestrictedDocFetcher.fetch(docId));
    }

    @Override
    public List<String> getSubscribers(String notification, DocumentModel doc) {
        return doc.getAdapter(SubscriptionAdapter.class).getNotificationSubscribers(notification);
    }

    /**
     * @deprecated since 7.3
     * @see NotificationService#getSubscriptionsForUserOnDocument(String, DocumentModel)
     */

    @Deprecated
    @Override
    public List<String> getSubscriptionsForUserOnDocument(String username, String docId) {
        return getSubscriptionsForUserOnDocument(username, UnrestrictedDocFetcher.fetch(docId));
    }

    @Override
    public List<String> getSubscriptionsForUserOnDocument(String username, DocumentModel doc) {
        return doc.getAdapter(SubscriptionAdapter.class).getUserSubscriptions(username);
    }

    private void disableEvents(DocumentModel doc) {
        doc.putContextData(DublinCoreListener.DISABLE_DUBLINCORE_LISTENER, true);
        doc.putContextData(NotificationConstants.DISABLE_NOTIFICATION_SERVICE, true);
        doc.putContextData(NXAuditEventsService.DISABLE_AUDIT_LOGGER, true);
        doc.putContextData(VersioningService.DISABLE_AUTO_CHECKOUT, true);

    }

    @Override
    public void addSubscription(String username, String notification, DocumentModel doc, Boolean sendConfirmationEmail,
            NuxeoPrincipal principal, String notificationName) {

        UnrestrictedSessionRunner runner = new UnrestrictedSessionRunner(doc.getRepositoryName()) {

            @Override
            public void run() {
                doc.getAdapter(SubscriptionAdapter.class).addSubscription(username, notification);
                disableEvents(doc);
                session.saveDocument(doc);
            }

        };

        runner.runUnrestricted();

        // send event for email if necessary
        if (sendConfirmationEmail) {
            raiseConfirmationEvent(principal, doc, username, notificationName);
        }
    }

    @Override
    public void addSubscriptions(String username, DocumentModel doc, Boolean sendConfirmationEmail,
            NuxeoPrincipal principal) {
        UnrestrictedSessionRunner runner = new UnrestrictedSessionRunner(doc.getRepositoryName()) {

            @Override
            public void run() {
                doc.getAdapter(SubscriptionAdapter.class).addSubscriptionsToAll(username);
                disableEvents(doc);
                session.saveDocument(doc);
            }
        };
        runner.runUnrestricted();

        // send event for email if necessary
        if (sendConfirmationEmail) {
            raiseConfirmationEvent(principal, doc, username, "All Notifications");
        }
    }

    @Override
    public void removeSubscriptions(String username, List<String> notifications, String docId) {
        removeSubscriptions(username, notifications, UnrestrictedDocFetcher.fetch(docId));
    }

    @Override
    public void removeSubscriptions(String username, List<String> notifications, DocumentModel doc) {
        UnrestrictedSessionRunner runner = new UnrestrictedSessionRunner(doc.getRepositoryName()) {

            @Override
            public void run() {
                SubscriptionAdapter sa = doc.getAdapter(SubscriptionAdapter.class);
                for (String notification : notifications) {
                    sa.removeUserNotificationSubscription(username, notification);
                }
                disableEvents(doc);
                session.saveDocument(doc);
            }
        };
        runner.runUnrestricted();

    }

    protected EventProducer producer;

    protected void doFireEvent(Event event) {
        if (producer == null) {
            producer = Framework.getService(EventProducer.class);
        }
        producer.fireEvent(event);
    }

    private void raiseConfirmationEvent(NuxeoPrincipal principal, DocumentModel doc, String username,
            String notification) {

        Map<String, Serializable> options = new HashMap<>();

        // Name of the current repository
        options.put(CoreEventConstants.REPOSITORY_NAME, doc.getRepositoryName());

        // Add the session ID
        options.put(CoreEventConstants.SESSION_ID, doc.getSessionId());

        // options for confirmation email
        options.put("recipients", username);
        options.put("notifName", notification);

        CoreSession session = doc.getCoreSession();
        DocumentEventContext ctx = new DocumentEventContext(session, principal, doc);
        ctx.setCategory(DocumentEventCategories.EVENT_CLIENT_NOTIF_CATEGORY);
        ctx.setProperties(options);
        Event event = ctx.newEvent(DocumentEventTypes.SUBSCRIPTION_ASSIGNED);
        doFireEvent(event);
    }

    @Override
    public void removeSubscription(String username, String notification, String docId) {
        removeSubscription(username, notification, UnrestrictedDocFetcher.fetch(docId));
    }

    @Override
    public void removeSubscription(String username, String notification, DocumentModel doc) {
        removeSubscriptions(username, Arrays.asList(notification), doc);
    }

    /**
     * @deprecated since 7.3 use {@link #getUsersSubscribedToNotificationOnDocument(String, DocumentModel)}
     * @see NotificationService#getSubscribers(String, DocumentModel)
     */
    @Deprecated
    @Override
    public List<String> getUsersSubscribedToNotificationOnDocument(String notification, String docId) {
        return getSubscribers(notification, docId);
    }

    private static void registerTemplate(TemplateDescriptor td) {
        if (td.src != null && td.src.length() > 0) {
            URL url = td.getContext().getResource(td.src);
            TEMPLATES_MAP.put(td.name, url);
        }
    }

    private static void unregisterTemplate(TemplateDescriptor td) {
        if (td.name != null) {
            TEMPLATES_MAP.remove(td.name);
        }
    }

    public static URL getTemplateURL(String name) {
        return TEMPLATES_MAP.get(name);
    }

    public String getServerUrlPrefix() {
        return generalSettings.getServerPrefix();
    }

    public String getEMailSubjectPrefix() {
        return generalSettings.getEMailSubjectPrefix();
    }

    public String getMailSessionJndiName() {
        return generalSettings.getMailSessionJndiName();
    }

    @Override
    public Notification getNotificationByName(String selectedNotification) {
        List<Notification> listNotif = notificationRegistry.getNotifications();
        for (Notification notification : listNotif) {
            if (notification.getName().equals(selectedNotification)) {
                return notification;
            }
        }
        return null;
    }

    @Override
    public void sendNotification(String notificationName, Map<String, Object> infoMap, String userPrincipal) {

        Notification notif = getNotificationByName(notificationName);

        NuxeoPrincipal recipient = NotificationServiceHelper.getUsersService().getPrincipal(userPrincipal);
        String email = recipient.getEmail();
        String mailTemplate = notif.getTemplate();

        infoMap.put("mail.to", email);

        String authorUsername = (String) infoMap.get("author");

        if (authorUsername != null) {
            NuxeoPrincipal author = NotificationServiceHelper.getUsersService().getPrincipal(authorUsername);
            infoMap.put("principalAuthor", author);
        }

        // mail.put("doc", docMessage); - should be already there

        String subject = notif.getSubject() == null ? "Alert" : notif.getSubject();
        if (notif.getSubjectTemplate() != null) {
            subject = notif.getSubjectTemplate();
        }

        subject = NotificationServiceHelper.getNotificationService().getEMailSubjectPrefix() + " " + subject;

        infoMap.put("subject", subject);
        infoMap.put("template", mailTemplate);

        try {
            emailHelper.sendmail(infoMap);
        } catch (MessagingException e) {
            throw new NuxeoException("Failed to send notification email ", e);
        }
    }

    @Override
    public void sendDocumentByMail(DocumentModel doc, String freemarkerTemplateName, String subject, String comment,
            NuxeoPrincipal sender, List<String> sendTo) {
        Map<String, Object> infoMap = new HashMap<>();
        infoMap.put("document", doc);
        infoMap.put("subject", subject);
        infoMap.put("comment", comment);
        infoMap.put("sender", sender);

        DocumentLocation docLoc = new DocumentLocationImpl(doc);
        DocumentView docView = new DocumentViewImpl(docLoc);
        docView.setViewId("view_documents");
        infoMap.put(
                "docUrl",
                getDocLocator().getUrlFromDocumentView(docView, true,
                        NotificationServiceHelper.getNotificationService().getServerUrlPrefix()));

        if (freemarkerTemplateName == null) {
            freemarkerTemplateName = "defaultNotifTemplate";
        }
        infoMap.put("template", freemarkerTemplateName);

        for (String to : sendTo) {
            infoMap.put("mail.to", to);
            try {
                emailHelper.sendmail(infoMap);
            } catch (MessagingException e) {
                log.debug("Failed to send notification email " + e);
            }
        }
    }

    private DocumentViewCodecManager getDocLocator() {
        if (docLocator == null) {
            docLocator = Framework.getService(DocumentViewCodecManager.class);
        }
        return docLocator;
    }

    @Override
    public List<Notification> getNotificationsForSubscriptions(String parentType) {
        return notificationRegistry.getNotificationsForSubscriptions(parentType);
    }

    @Override
    public List<Notification> getNotificationsForEvents(String eventId) {
        return notificationRegistry.getNotificationsForEvent(eventId);
    }

    public EmailHelper getEmailHelper() {
        return emailHelper;
    }

    public void setEmailHelper(EmailHelper emailHelper) {
        this.emailHelper = emailHelper;
    }

    @Override
    public Set<String> getNotificationEventNames() {
        return notificationRegistry.getNotificationEventNames();
    }

    public Collection<NotificationListenerHook> getListenerHooks() {
        return hookListeners.values();
    }

    public Collection<NotificationListenerVeto> getNotificationVetos() {
        return notificationVetoRegistry.getVetos();
    }

    @Override
    public List<String> getUsersSubscribedToNotificationOnDocument(String notification, DocumentModel doc) {
        return getSubscribers(notification, doc);
    }

    @Override
    public List<DocumentModel> getSubscribedDocuments(String prefixedPrincipalName) {
        String nxql = "SELECT * FROM Document WHERE ecm:mixinType = '" + SubscriptionAdapter.NOTIFIABLE_FACET + "' "
                + "AND ecm:isCheckedInVersion = 0 " + "AND notif:notifications/*/subscribers/* = "
                + NXQL.escapeString(prefixedPrincipalName);

        return UnrestrictedDocFetcher.query(nxql);
    }

}
