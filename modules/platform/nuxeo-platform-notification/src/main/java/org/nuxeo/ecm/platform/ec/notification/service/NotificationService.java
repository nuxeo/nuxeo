/*
 * (C) Copyright 2007-2018 Nuxeo (http://nuxeo.com/) and others.
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

import static java.lang.Boolean.TRUE;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.mail.MessagingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.event.DocumentEventCategories;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.core.api.versioning.VersioningService;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.query.sql.NXQL;
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

/**
 * @author <a href="mailto:npaslaru@nuxeo.com">Narcis Paslaru</a>
 */
public class NotificationService extends DefaultComponent implements NotificationManager {

    private static final Log log = LogFactory.getLog(NotificationService.class);

    public static final ComponentName NAME = new ComponentName(
            "org.nuxeo.ecm.platform.ec.notification.service.NotificationService");

    protected static final String NOTIFICATIONS_EP = "notifications";

    protected static final String TEMPLATES_EP = "templates";

    protected static final String GENERAL_SETTINGS_EP = "generalSettings";

    protected static final GeneralSettingsDescriptor DEFAULT_SETTINGS = new GeneralSettingsDescriptor(
            "http://localhost:8080/nuxeo/", "[Nuxeo]", "java:/Mail");

    protected static final String NOTIFICATION_HOOK_EP = "notificationListenerHook";

    protected static final String NOTIFICATION_VETO_EP = "notificationListenerVeto";

    protected EmailHelper emailHelper = new EmailHelper();

    protected Map<String, NotificationListenerHook> hookListeners;

    protected List<NotificationListenerVeto> vetos;

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(NotificationManager.class)) {
            return (T) this;
        }
        return null;
    }

    @Override
    public void start(ComponentContext context) {
        hookListeners = new HashMap<>();
        List<NotificationListenerHookDescriptor> hooks = getRegistryContributions(NOTIFICATION_HOOK_EP);
        for (NotificationListenerHookDescriptor desc : hooks) {
            try {
                hookListeners.put(desc.name, desc.hookListener.getDeclaredConstructor().newInstance());
            } catch (ReflectiveOperationException e) {
                log.error(e, e);
            }
        }
        vetos = new ArrayList<>();
        List<NotificationListenerVetoDescriptor> vetoDescs = getRegistryContributions(NOTIFICATION_VETO_EP);
        for (NotificationListenerVetoDescriptor desc : vetoDescs) {
            try {
                this.vetos.add(desc.getNotificationVeto().getDeclaredConstructor().newInstance());
            } catch (ReflectiveOperationException e) {
                log.error(e, e);
            }
        }
    }

    @Override
    public void stop(ComponentContext context) throws InterruptedException {
        hookListeners = null;
        vetos = null;
    }

    @Override
    public List<String> getSubscribers(String notification, DocumentModel doc) {
        return doc.getAdapter(SubscriptionAdapter.class).getNotificationSubscribers(notification);
    }

    @Override
    public List<String> getSubscriptionsForUserOnDocument(String username, DocumentModel doc) {
        return doc.getAdapter(SubscriptionAdapter.class).getUserSubscriptions(username);
    }

    protected void disableEvents(DocumentModel doc) {
        doc.putContextData(DublinCoreListener.DISABLE_DUBLINCORE_LISTENER, TRUE);
        doc.putContextData(NotificationConstants.DISABLE_NOTIFICATION_SERVICE, TRUE);
        doc.putContextData(NXAuditEventsService.DISABLE_AUDIT_LOGGER, TRUE);
        doc.putContextData(VersioningService.DISABLE_AUTO_CHECKOUT, TRUE);
    }

    protected void restoreEvents(DocumentModel doc) {
        doc.putContextData(DublinCoreListener.DISABLE_DUBLINCORE_LISTENER, null);
        doc.putContextData(NotificationConstants.DISABLE_NOTIFICATION_SERVICE, null);
        doc.putContextData(NXAuditEventsService.DISABLE_AUDIT_LOGGER, null);
        doc.putContextData(VersioningService.DISABLE_AUTO_CHECKOUT, null);
    }

    @Override
    public void addSubscription(String username, String notification, DocumentModel doc, Boolean sendConfirmationEmail,
            NuxeoPrincipal principal, String notificationName) {

        CoreInstance.doPrivileged(doc.getRepositoryName(), (CoreSession session) -> {
            doc.getAdapter(SubscriptionAdapter.class).addSubscription(username, notification);
            disableEvents(doc);
            session.saveDocument(doc);
            restoreEvents(doc);
        });

        // send event for email if necessary
        if (Boolean.TRUE.equals(sendConfirmationEmail)) {
            raiseConfirmationEvent(principal, doc, username, notificationName);
        }
    }

    @Override
    public void addSubscriptions(String username, DocumentModel doc, Boolean sendConfirmationEmail,
            NuxeoPrincipal principal) {
        CoreInstance.doPrivileged(doc.getRepositoryName(), (CoreSession session) -> {
            doc.getAdapter(SubscriptionAdapter.class).addSubscriptionsToAll(username);
            disableEvents(doc);
            session.saveDocument(doc);
            restoreEvents(doc);
        });

        // send event for email if necessary
        if (Boolean.TRUE.equals(sendConfirmationEmail)) {
            raiseConfirmationEvent(principal, doc, username, "All Notifications");
        }
    }

    @Override
    public void removeSubscriptions(String username, List<String> notifications, DocumentModel doc) {
        CoreInstance.doPrivileged(doc.getRepositoryName(), (CoreSession session) -> {
            SubscriptionAdapter sa = doc.getAdapter(SubscriptionAdapter.class);
            for (String notification : notifications) {
                sa.removeUserNotificationSubscription(username, notification);
            }
            disableEvents(doc);
            session.saveDocument(doc);
            restoreEvents(doc);
        });
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
    public void removeSubscription(String username, String notification, DocumentModel doc) {
        removeSubscriptions(username, singletonList(notification), doc);
    }

    public URL getTemplateURL(String name) {
        return this.<TemplateDescriptor> getRegistryContribution(TEMPLATES_EP, name)
                   .map(t -> t.getSrc().toURL())
                   .orElse(null);
    }

    protected GeneralSettingsDescriptor getGeneralSettings() {
        return this.<GeneralSettingsDescriptor> getRegistryContribution(GENERAL_SETTINGS_EP).orElse(DEFAULT_SETTINGS);
    }

    public String getServerUrlPrefix() {
        return getGeneralSettings().getServerPrefix();
    }

    public String getEMailSubjectPrefix() {
        return getGeneralSettings().getEMailSubjectPrefix();
    }

    public String getMailSessionJndiName() {
        return getGeneralSettings().getMailSessionJndiName();
    }

    @Override
    public Notification getNotificationByName(String selectedNotification) {
        return this.<Notification> getRegistryContribution(NOTIFICATIONS_EP, selectedNotification).orElse(null);
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
        infoMap.put("docUrl", getDocLocator().getUrlFromDocumentView(docView, true,
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
        return Framework.getService(DocumentViewCodecManager.class);
    }

    @Override
    public List<Notification> getNotificationsForSubscriptions(String parentType) {
        return this.<NotificationRegistry> getExtensionPointRegistry(NOTIFICATIONS_EP)
                   .getNotificationsForSubscriptions(parentType);
    }

    @Override
    public List<Notification> getNotificationsForEvents(String eventId) {
        return this.<NotificationRegistry> getExtensionPointRegistry(NOTIFICATIONS_EP)
                   .getNotificationsForEvent(eventId);
    }

    public EmailHelper getEmailHelper() {
        return emailHelper;
    }

    public void setEmailHelper(EmailHelper emailHelper) {
        this.emailHelper = emailHelper;
    }

    @Override
    public Set<String> getNotificationEventNames() {
        return this.<NotificationRegistry> getExtensionPointRegistry(NOTIFICATIONS_EP).getNotificationEventNames();
    }

    public Collection<NotificationListenerHook> getListenerHooks() {
        return hookListeners.values();
    }

    public Collection<NotificationListenerVeto> getNotificationVetos() {
        return Collections.unmodifiableCollection(vetos);
    }

    @Override
    public List<String> getUsersSubscribedToNotificationOnDocument(String notification, DocumentModel doc) {
        return getSubscribers(notification, doc);
    }

    @Override
    public List<DocumentModel> getSubscribedDocuments(String prefixedPrincipalName, String repositoryName) {
        String nxql = "SELECT * FROM Document WHERE ecm:mixinType = '" + SubscriptionAdapter.NOTIFIABLE_FACET + "' "
                + "AND ecm:isVersion = 0 " + "AND notif:notifications/*/subscribers/* = "
                + NXQL.escapeString(prefixedPrincipalName);

        return CoreInstance.doPrivileged(repositoryName,
                (CoreSession s) -> s.query(nxql)
                                    .stream()
                                    .map(NotificationService::detachDocumentModel)
                                    .collect(toList()));
    }

    protected static DocumentModel detachDocumentModel(DocumentModel doc) {
        doc.detach(true);
        return doc;
    }

}
