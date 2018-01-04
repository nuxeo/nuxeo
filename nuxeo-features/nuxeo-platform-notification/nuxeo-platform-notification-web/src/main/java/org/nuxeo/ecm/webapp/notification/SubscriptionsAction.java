/*
 * (C) Copyright 2007 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.webapp.notification;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.datamodel.DataModel;
import org.jboss.seam.annotations.datamodel.DataModelSelection;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.ec.notification.NotificationConstants;
import org.nuxeo.ecm.platform.notification.api.Notification;
import org.nuxeo.ecm.platform.notification.api.NotificationManager;
import org.nuxeo.ecm.webapp.base.InputController;
import org.nuxeo.ecm.webapp.helpers.EventNames;

/**
 * Handles the subscriptions page.
 *
 * @author <a href="mailto:npaslaru@nuxeo.com">Narcis Paslaru</a>
 */
@Name("subscriptionAction")
@Scope(ScopeType.PAGE)
public class SubscriptionsAction extends InputController implements Serializable {

    private static final long serialVersionUID = -2440187703248677446L;

    private static final Log log = LogFactory.getLog(SubscriptionsAction.class);

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @DataModel("notificationList")
    protected List<SelectableSubscription> notificationList;

    @DataModel("inheritedNotifications")
    private List<Notification> inheritedNotifications;

    @DataModelSelection(value = "notificationList")
    private SelectableSubscription currentSubscription;

    @In(create = true)
    protected transient NotificationManager notificationManager;

    public static final String CONFIRM_FOLLOW = "label.subscriptions.follow.confirm";

    public static final String CONFIRM_UNFOLLOW = "label.subscriptions.unfollow.confirm";

    /**
     * Gets all the notifications the user may subscribe to.
     */
    @Factory("notificationList")
    public void getNotificationsList() {
        log.debug("Factory for notifications list");

        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        String superParentType = documentManager.getSuperParentType(currentDocument);

        List<Notification> notifs = notificationManager.getNotificationsForSubscriptions(superParentType);

        List<String> subscriptions = getSelectedNotifications();
        log.debug("Selected notifications : " + subscriptions);

        List<SelectableSubscription> notifsResult = new ArrayList<SelectableSubscription>();
        for (Notification notification : notifs) {
            String notifName = notification.getName();
            if (subscriptions.contains(notifName)) {
                notifsResult.add(new SelectableSubscription(true, notification));
            } else {
                notifsResult.add(new SelectableSubscription(false, notification));
            }
        }
        notificationList = notifsResult;
    }

    /**
     * Gets all the notifications the user may subscribe to.
     */
    @Factory("inheritedNotifications")
    public void loadInheritedNotifications() throws ClassNotFoundException {
        inheritedNotifications = new ArrayList<Notification>();
        DocumentModel currentDoc = navigationContext.getCurrentDocument();
        NuxeoPrincipal principal = (NuxeoPrincipal) FacesContext.getCurrentInstance().getExternalContext().getUserPrincipal();
        for (String group : principal.getAllGroups()) {
            List<String> notifs = notificationManager.getSubscriptionsForUserOnDocument("group:" + group,
                    currentDoc);
            for (String inheritedNotification : notifs) {
                Notification notif = notificationManager.getNotificationByName(inheritedNotification);
                inheritedNotifications.add(notif);
            }
        }
    }

    /**
     * Registers the user's choices.
     */
    public void updateSubscriptions() {

        NuxeoPrincipal principal = (NuxeoPrincipal) FacesContext.getCurrentInstance().getExternalContext().getUserPrincipal();
        DocumentModel currentDoc = navigationContext.getCurrentDocument();
        if (currentSubscription.isSelected()) {
            notificationManager.removeSubscription("user:" + principal.getName(),
                    currentSubscription.getNotification().getName(), currentDoc);
        } else {
            notificationManager.addSubscription(NotificationConstants.USER_PREFIX + principal.getName(),
                    currentSubscription.getNotification().getName(), currentDoc, false, principal, "");
        }
        getNotificationsList();
    }

    /**
     * Manage (un)subscription to all notifications
     */
    public void updateAllSubscriptions() {
        NuxeoPrincipal principal = (NuxeoPrincipal) FacesContext.getCurrentInstance().getExternalContext().getUserPrincipal();
        DocumentModel currentDoc = navigationContext.getCurrentDocument();
        List<String> userSubscriptions = notificationManager.getSubscriptionsForUserOnDocument(
                NotificationConstants.USER_PREFIX + principal.getName(), currentDoc);
        if (userSubscriptions.size() == 0) {
            notificationManager.addSubscriptions(NotificationConstants.USER_PREFIX + principal.getName(), currentDoc,
                    false, principal);
            facesMessages.add(StatusMessage.Severity.INFO, resourcesAccessor.getMessages().get(CONFIRM_FOLLOW));
        } else {
            notificationManager.removeSubscriptions(NotificationConstants.USER_PREFIX + principal.getName(),
                    userSubscriptions, currentDoc);
            facesMessages.add(StatusMessage.Severity.INFO, resourcesAccessor.getMessages().get(CONFIRM_UNFOLLOW));
        }
        getNotificationsList();
    }

    @Observer(value = EventNames.DOCUMENT_SELECTION_CHANGED, create = false)
    @BypassInterceptors
    public void invalidateNotificationsSelection() {
        log.debug("Invalidate archive records.................");
        notificationList = null;
        currentSubscription = null;
        inheritedNotifications = null;
    }

    /**
     * @return the previously selected notifications.
     */
    public List<String> getSelectedNotifications() {
        return getSubscriptionsForCurrentUser();
    }

    /**
     * Returns the notifications that the user already subscribed for.
     */
    private List<String> getSubscriptionsForCurrentUser() {

        DocumentModel currentDoc = navigationContext.getCurrentDocument();
        if (currentDoc == null) {
            return Collections.emptyList();
        }
        NuxeoPrincipal principal = (NuxeoPrincipal) FacesContext.getCurrentInstance().getExternalContext().getUserPrincipal();
        List<String> subscriptions = notificationManager.getSubscriptionsForUserOnDocument(
                "user:" + principal.getName(), currentDoc);
        return subscriptions;
    }

    public SelectableSubscription getCurrentSubscription() {
        return currentSubscription;
    }

    public void setCurrentSubscription(SelectableSubscription currentSubscription) {
        this.currentSubscription = currentSubscription;
    }

    public List<SelectableSubscription> getNotificationList() {
        return notificationList;
    }

    public void setNotificationList(List<SelectableSubscription> notificationList) {
        this.notificationList = notificationList;
    }

    public List<Notification> getInheritedNotifications() {
        return inheritedNotifications;
    }

    public void setInheritedNotifications(List<Notification> inheritedNotifications) {
        this.inheritedNotifications = inheritedNotifications;
    }

    /**
     * @since 9.2
     */
    public boolean canFollow() {
        return !navigationContext.getCurrentDocument().isProxy();
    }

}
