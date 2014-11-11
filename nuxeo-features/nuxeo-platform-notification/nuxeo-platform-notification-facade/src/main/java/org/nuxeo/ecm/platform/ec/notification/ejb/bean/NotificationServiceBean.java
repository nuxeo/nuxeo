/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.ec.notification.ejb.bean;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.ejb.Local;
import javax.ejb.PostActivate;
import javax.ejb.Remote;
import javax.ejb.Stateless;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.ec.notification.ejb.facade.NotificationServiceLocal;
import org.nuxeo.ecm.platform.ec.notification.service.NotificationRegistryImpl;
import org.nuxeo.ecm.platform.notification.api.Notification;
import org.nuxeo.ecm.platform.notification.api.NotificationManager;
import org.nuxeo.ecm.platform.notification.api.NotificationRegistry;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:npaslaru@nuxeo.com">Narcis Paslaru</a>
 * @author <a href="mailto:tmartins@nuxeo.com">Thierry Martins</a>
 *
 */
@Stateless
@Local(NotificationServiceLocal.class)
@Remote(NotificationManager.class)
public class NotificationServiceBean implements NotificationServiceLocal {

    // @PersistenceContext(unitName="nxplacefulservice")
    // EntityManager em;
    private static final Log log = LogFactory.getLog(NotificationServiceBean.class);

    protected NotificationManager service;

    @PostActivate
    @PostConstruct
    public void initialize() {
        try {
            service = Framework.getLocalService(NotificationManager.class);
        } catch (Exception e) {
            log.error("Could not get relation service", e);
        }
    }

    public void addSubscription(String username, String notification,
            DocumentModel doc, Boolean sendConfirmationEmail,
            NuxeoPrincipal principal, String notificationName)
            throws ClientException {
        service.addSubscription(username, notification, doc,
                sendConfirmationEmail, principal, notificationName);
    }

    public List<String> getSubscribers(String notification, String docId)
            throws ClientException {
        return service.getSubscribers(notification, docId);
    }

    public List<String> getSubscriptionsForUserOnDocument(String username,
            String docId) throws ClassNotFoundException, ClientException {
        return service.getSubscriptionsForUserOnDocument(username, docId);
    }

    public List<String> getUsersSubscribedToNotificationOnDocument(
            String notification, String docId) throws ClientException {
        return service.getUsersSubscribedToNotificationOnDocument(notification,
                docId);
    }

    public void removeSubscription(String username, String notification,
            String docId) throws ClientException {
        service.removeSubscription(username, notification, docId);
    }

    /**
     * @deprecated should not have to use the registry
     */
    @Deprecated
    public NotificationRegistry getNotificationRegistry() {
        NotificationRegistryImpl registry = (NotificationRegistryImpl) service.getNotificationRegistry();
        return new SerializableNotificationRegistry(registry);
    }

    public Notification getNotificationByName(String selectedNotification) {
        return service.getNotificationByName(selectedNotification);
    }

    public List<Notification> getNotificationsForSubscriptions(String parentType) {
        return service.getNotificationsForSubscriptions(parentType);
    }

    public void sendDocumentByMail(DocumentModel doc,
            String freemarkerTemplateName, String subject, String comment,
            NuxeoPrincipal sender, List<String> sendTo) {
        service.sendDocumentByMail(doc, freemarkerTemplateName, subject,
                comment, sender, sendTo);
    }

    public void sendNotification(String notificationName,
            Map<String, Object> infoMap, String userPrincipal)
            throws ClientException {
        service.sendNotification(notificationName, infoMap, userPrincipal);
    }

    public List<Notification> getNotificationsForEvents(String eventId) {
        return service.getNotificationsForEvents(eventId);
    }

    @Override
    public Set<String> getNotificationEventNames() {
        return service.getNotificationEventNames();
    }
}
