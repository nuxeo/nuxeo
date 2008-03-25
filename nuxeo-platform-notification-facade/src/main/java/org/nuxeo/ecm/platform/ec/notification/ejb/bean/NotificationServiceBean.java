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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.ejb.Local;
import javax.ejb.PostActivate;
import javax.ejb.Remote;
import javax.ejb.Stateless;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.ec.notification.ejb.facade.NotificationServiceLocal;
import org.nuxeo.ecm.platform.ec.notification.email.EmailHelper;
import org.nuxeo.ecm.platform.ec.notification.service.NotificationService;
import org.nuxeo.ecm.platform.ec.notification.service.NotificationServiceHelper;
import org.nuxeo.ecm.platform.notification.api.Notification;
import org.nuxeo.ecm.platform.notification.api.NotificationManager;
import org.nuxeo.ecm.platform.notification.api.NotificationRegistry;
import org.nuxeo.ecm.platform.url.DocumentLocationImpl;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentLocation;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.api.DocumentViewCodecManager;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:npaslaru@nuxeo.com">Narcis Paslaru</a>
 *
 */
@Stateless
@Local(NotificationServiceLocal.class)
@Remote(NotificationManager.class)
public class NotificationServiceBean implements NotificationManager {

    // @PersistenceContext(unitName="nxplacefulservice")
    // EntityManager em;
    private static final Log log = LogFactory.getLog(NotificationServiceBean.class);

    protected transient NotificationService service;

    private DocumentViewCodecManager docLocator;

    @PostActivate
    @PostConstruct
    public void initialize() {
        service = (NotificationService) Framework.getRuntime().getComponent(
                NotificationService.NAME);
    }

    public void addSubscription(String username, String notification,
            DocumentModel doc, Boolean sendConfirmationEmail,
            NuxeoPrincipal principal, String notificationName) throws ClientException {
        service.addSubscription(username, notification, doc, sendConfirmationEmail, principal, notificationName);
    }

    public List<String> getSubscribers(String notification, String docId) throws ClientException {
        return service.getSubscribers(notification, docId);
    }

    public List<String> getSubscriptionsForUserOnDocument(String username, String docId)
            throws ClassNotFoundException, ClientException {
        return service.getSubscriptionsForUserOnDocument(username, docId);
    }

    public List<String> getUsersSubscribedToNotificationOnDocument(String notification,
            String docId) throws ClientException {
        return service.getUsersSubscribedToNotificationOnDocument(notification, docId);
    }

    public void removeSubscription(String username, String notification, String docId)
            throws ClientException {
        service.removeSubscription(username, notification, docId);
    }

    public NotificationRegistry getNotificationRegistry() {
        return service.getNotificationRegistry();
    }

    public Notification getNotificationByName(String selectedNotification) {
        return service.getNotificationByName(selectedNotification);
    }

    public void sendNotification(String notificationName, Map<String, Object> infoMap, String userPrincipal) throws ClientException {

        Notification notif = getNotificationByName(notificationName);

        NuxeoPrincipal recepient = NotificationServiceHelper.getUsersService().getPrincipal(userPrincipal);
        // XXX hack, principals have only one model
        DataModel model = recepient.getModel().getDataModels().values().iterator().next();
        String email = (String) model.getData("email");
        String mailTemplate = notif.getTemplate();

        infoMap.put("mail.to", email);

        String authorUsername = (String) infoMap.get("author");

        if (authorUsername != null) {
            NuxeoPrincipal author = NotificationServiceHelper.getUsersService().getPrincipal(authorUsername);
            infoMap.put("principalAuthor", author);
        }

//        mail.put("doc", docMessage); - should be already there

        String subject = notif.getSubject() == null ? "Notification"
                : notif.getSubject();
        subject = NotificationServiceHelper.getNotificationService().getEMailSubjectPrefix()
                + subject;

        infoMap.put("subject", subject);
        infoMap.put("template", mailTemplate);

        try {
            EmailHelper.sendmail(infoMap);
        } catch (Exception e) {
            throw new ClientException("Failed to send notification email ", e);
        }
    }

    public void sendDocumentByMail(DocumentModel doc,
            String freemarkerTemplateName, String subject, String comment,
            NuxeoPrincipal sender, List<String> sendTo) {
        Map<String, Object> infoMap = new HashMap<String, Object>();
        infoMap.put("document", doc);
        infoMap.put("subject", subject);
        infoMap.put("comment", comment);
        infoMap.put("sender", sender);

        DocumentLocation docLoc = new DocumentLocationImpl(
                doc.getRepositoryName(), doc.getRef());
        DocumentView docView = new DocumentViewImpl(docLoc);
        docView.setViewId("view_documents");
        infoMap.put(
                "docUrl",
                getDocLocator().getUrlFromDocumentView(
                        docView,
                        true,
                        NotificationServiceHelper.getNotificationService().getServerUrlPrefix()));


        if (freemarkerTemplateName == null){
            freemarkerTemplateName = "defaultNotifTemplate";
        }
        infoMap.put("template", freemarkerTemplateName);

        for (String to : sendTo) {
            infoMap.put("mail.to", to);
            try {
                EmailHelper.sendmail(infoMap);
            } catch (Exception e) {
                log.debug("Failed to send notification email "+e);
            }
        }
    }

    private DocumentViewCodecManager getDocLocator() {
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
