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
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.common.utils.i18n.Labeler;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.ec.notification.NotificationConstants;
import org.nuxeo.ecm.platform.notification.api.Notification;
import org.nuxeo.ecm.platform.notification.api.NotificationManager;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.ecm.webapp.base.InputController;

/**
 * Handles the subscriptions page.
 *
 * @author <a href="mailto:npaslaru@nuxeo.com">Narcis Paslaru</a>
 */
@Name("groupsSubscriptionsAction")
@Scope(ScopeType.PAGE)
public class GroupsSubscriptionsAction extends InputController implements Serializable {

    private static final long serialVersionUID = -2440187703248677446L;

    private static final Labeler labeler = new Labeler("label.subscriptions");

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(required = false)
    @Out(required = false)
    private List<String> selectedNotifications;

    @In(create = true)
    protected transient NotificationManager notificationManager;

    private String selectedGrant;

    private String selectedNotification;

    private SelectItem[] permissionActionItems;

    protected List<String> selectedEntries;

    /**
     * Gets all the notifications registered in the system.
     */
    public List<SelectItem> getNotificationList() {
        String parentType = documentManager.getSuperParentType(navigationContext.getCurrentDocument());
        List<Notification> notifs = notificationManager.getNotificationsForSubscriptions(parentType);
        List<SelectItem> notifsResult = new ArrayList<SelectItem>();
        for (Notification notification : notifs) {
            String notifName = notification.getName();
            String notifLabel = notification.getLabel();
            notifsResult.add(new SelectItem(notifName, resourcesAccessor.getMessages().get(notifLabel)));
        }
        return notifsResult;
    }

    /**
     * Registers the user's choices.
     */
    public void updateSubscriptions() {
        List<String> selectedNotifications = getSelectedNotifications();
        List<String> subscriptions = getSubscriptionsForCurrentUser();

        List<String> newSubscriptions = getDisjunctElements(selectedNotifications, subscriptions);
        List<String> removedSubscriptions = getDisjunctElements(subscriptions, selectedNotifications);

        DocumentModel currentDoc = navigationContext.getCurrentDocument();

        // removing the unselected subscriptions
        if (!removedSubscriptions.isEmpty()) {
            for (String subscription : removedSubscriptions) {
                notificationManager.removeSubscription("user:" + currentUser.getName(), subscription, currentDoc);
            }
        }

        // adding the newly selected subscriptions
        if (!newSubscriptions.isEmpty()) {
            for (String subscription : newSubscriptions) {
                notificationManager.addSubscription(NotificationConstants.USER_PREFIX + currentUser.getName(),
                        subscription, currentDoc, false, currentUser, "");
            }
        }

        facesMessages.add(StatusMessage.Severity.INFO,
                resourcesAccessor.getMessages().get("label.notifications.registered"));
    }

    private static List<String> getDisjunctElements(List<String> array1, List<String> array2) {
        List<String> result = new ArrayList<String>();
        for (String elem1 : array1) {

            if (!array2.contains(elem1)) {
                result.add(elem1);
            }
        }
        return result;
    }

    /**
     * @return the previously selected notifications.
     */
    public List<String> getSelectedNotifications() {
        if (selectedNotifications == null) {
            selectedNotifications = getSubscriptionsForCurrentUser();
        }
        return selectedNotifications;
    }

    /**
     * Returns the notifications that the user already subscribed for.
     */
    private List<String> getSubscriptionsForCurrentUser() {
        DocumentModel currentDoc = navigationContext.getCurrentDocument();
        List<String> subscriptions = notificationManager.getSubscriptionsForUserOnDocument(
                "user:" + currentUser.getName(), currentDoc);
        return subscriptions;
    }

    /**
     * Returns the users that subscribed to a notification.
     */
    public List<String> getSubscribedUsersForNotification(String notification) {
        DocumentModel currentDoc = navigationContext.getCurrentDocument();
        return notificationManager.getUsersSubscribedToNotificationOnDocument(notification, currentDoc);
    }

    /**
     * Returns a map that contains all users and groups subscribed to notifications(keys).
     */
    public Map<String, List<String>> getUsersByNotificationsForCurrentDocument() {
        Map<String, List<String>> result = new HashMap<String, List<String>>();

        String superParentType = documentManager.getSuperParentType(navigationContext.getCurrentDocument());
        List<Notification> notifications = notificationManager.getNotificationsForSubscriptions(superParentType);
        for (Notification notification : notifications) {
            result.put(notification.getLabel(), getSubscribedUsersForNotification(notification.getName()));
        }
        return result;
    }

    /**
     * @param selectedNotifications The selectedNotifications to set.
     */
    public void setSelectedNotifications(List<String> selectedNotifications) {
        this.selectedNotifications = selectedNotifications;
    }

    public SelectItem[] getNotificationActionItems() {
        List<String> permissionActions = new ArrayList<String>();
        List<SelectItem> jsfModelList = new ArrayList<SelectItem>();

        permissionActions.add("Subscribe");
        permissionActions.add("Unsubscribe");

        for (String permissionAction : permissionActions) {
            String label = labeler.makeLabel(permissionAction);
            SelectItem it = new SelectItem(permissionAction, resourcesAccessor.getMessages().get(label));
            jsfModelList.add(it);
        }

        permissionActionItems = jsfModelList.toArray(new SelectItem[0]);

        return permissionActionItems;
    }

    public String getSelectedGrant() {
        return selectedGrant;
    }

    public void setSelectedGrant(String selectedPermission) {
        selectedGrant = selectedPermission;
    }

    public String getSelectedNotification() {
        return selectedNotification;
    }

    public void setSelectedNotification(String selectedNotification) {
        this.selectedNotification = selectedNotification;
    }

    public boolean getCanAddSubscriptions() {
        return documentManager.hasPermission(currentDocument.getRef(), "WriteSecurity");
    }

    public String addSubscriptionsAndUpdate() {
        if (selectedEntries == null || selectedEntries.isEmpty()) {
            String message = ComponentUtils.translate(FacesContext.getCurrentInstance(),
                    "error.notifManager.noUserSelected");
            FacesMessages.instance().add(message);
            return null;
        }
        String notificationName = resourcesAccessor.getMessages().get(
                notificationManager.getNotificationByName(selectedNotification).getLabel());
        boolean subscribe = selectedGrant.equals("Subscribe");

        DocumentModel currentDoc = navigationContext.getCurrentDocument();

        List<String> registeredNotifications = null;
        if (subscribe) {
            registeredNotifications = getSubscribedUsersForNotification(selectedNotification);
        }

        for (String selectedEntry : selectedEntries) {
            if (subscribe) {
                if (registeredNotifications == null || !registeredNotifications.contains(selectedEntry)) {
                    notificationManager.addSubscription(selectedEntry, selectedNotification, currentDoc, true,
                            currentUser, notificationName);
                } else {
                    facesMessages.add(StatusMessage.Severity.WARN,
                            resourcesAccessor.getMessages().get("label.notifications.alreadyRegistered"), selectedEntry);
                }
            } else {
                notificationManager.removeSubscription(selectedEntry, selectedNotification, currentDoc);
            }
        }
        // reset
        selectedEntries = null;
        facesMessages.add(StatusMessage.Severity.INFO,
                resourcesAccessor.getMessages().get("label.notifications.registered"));
        return null;
    }

    public List<String> getSelectedEntries() {
        if (selectedEntries == null) {
            selectedEntries = new ArrayList<String>();
        }
        return selectedEntries;
    }

    public void setSelectedEntries(List<String> selectedEntries) {
        this.selectedEntries = selectedEntries;
    }

}
