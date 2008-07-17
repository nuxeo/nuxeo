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

package org.nuxeo.ecm.webapp.notification;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.core.FacesMessages;
import org.nuxeo.common.utils.i18n.Labeler;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.notification.api.Notification;
import org.nuxeo.ecm.platform.notification.api.NotificationManager;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.ecm.webapp.base.InputController;
import org.nuxeo.ecm.webapp.security.PrincipalListManager;

/**
 * Handles the subscriptions page.
 *
 * @author <a href="mailto:npaslaru@nuxeo.com">Narcis Paslaru</a>
 *
 */
@Name("groupsSubscriptionsAction")
@Scope(ScopeType.PAGE)
public class GroupsSubscriptionsAction extends InputController implements
        Serializable {

    private static final long serialVersionUID = -2440187703248677446L;

    private static final Log log = LogFactory.getLog(GroupsSubscriptionsAction.class);

    private static final Labeler labeler = new Labeler("label.subscriptions");

    @In(create = true, required = false)
    private transient CoreSession documentManager;

    @In(required = false)
    @Out(required = false)
    private List<String> selectedNotifications;

    @In(create = true)
    private PrincipalListManager principalListManager;

    @In(create = true)
    private transient NotificationManager notificationManager;

    private String selectedGrant;

    private String selectedNotification;

    private SelectItem[] permissionActionItems;

    /**
     * Gets all the notifications registered in the system.
     *
     * @return
     * @throws ClientException
     */
    public List<SelectItem> getNotificationList() throws ClientException {
        //Using runtime
//        NotificationService service = (NotificationService) NXRuntime
//                .getRuntime().getComponent(NotificationService.NAME);

        //Using EJB3.0
//        JNDILookupHelper helper = new JNDILookupHelper(null);
//        NotificationServiceRemote service = (NotificationServiceRemote) helper.lookupEjbReference("NotificationService");

        String parentType = documentManager.getSuperParentType(
                navigationContext.getCurrentDocument());
        List<Notification> notifs = notificationManager.getNotificationsForSubscriptions(parentType);
        List<SelectItem> notifsResult = new ArrayList<SelectItem>();
        for (Notification notification : notifs) {
            String notifName = notification.getName();
            String notifLabel = notification.getLabel();
            notifsResult.add(
                    new SelectItem(notifName, resourcesAccessor.getMessages().get(notifLabel)));
        }
        return notifsResult;
    }

    /**
     * Registers the user's choices.
     *
     * @throws ClientException
     */
    public void updateSubscriptions() throws ClientException {
        log.info("You have chosen : " + selectedNotifications);
        List<String> selectedNotifications = getSelectedNotifications();
        List<String> subscriptions = getSubscriptionsForCurrentUser();

        List<String> newSubscriptions = getDisjunctElements(
                selectedNotifications, subscriptions);
        List<String> removedSubscriptions = getDisjunctElements(subscriptions,
                selectedNotifications);

        //Using runtime
//        NotificationService service = (NotificationService) NXRuntime
//                .getRuntime().getComponent(NotificationService.NAME);
        //Using EJB3.0
//        JNDILookupHelper helper = new JNDILookupHelper(null);
//        NotificationServiceRemote service = (NotificationServiceRemote) helper.lookupEjbReference("NotificationService");

        NuxeoPrincipal principal = (NuxeoPrincipal) FacesContext
                .getCurrentInstance().getExternalContext().getUserPrincipal();
        DocumentModel currentDoc = navigationContext.getCurrentDocument();
        // removing the unselected subscriptions
        if (!removedSubscriptions.isEmpty()) {
            for (String subscription : removedSubscriptions) {
                notificationManager.removeSubscription("user:" + principal.getName(),
                        subscription, currentDoc.getId());
            }
        }
        // ading the newly selected subsctiptions
        if (!newSubscriptions.isEmpty()) {
            for (String subscription : newSubscriptions) {
                notificationManager.addSubscription("user:" + principal.getName(),
                        subscription, currentDoc, false, principal, "");
            }
        }

        facesMessages.add(FacesMessage.SEVERITY_INFO,
                resourcesAccessor.getMessages().get("label.notifications.registered"));
        log.info("Updating subscriptions.... whatch out !");
    }

    private static List<String> getDisjunctElements(List<String> array1,
            List<String> array2) {
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
     * @throws ClientException
     */
    public List<String> getSelectedNotifications() throws ClientException {
        log.info("GetSelected notifications");
        if (selectedNotifications == null) {
            selectedNotifications = getSubscriptionsForCurrentUser();
            log.info("Current notification for user : "
                    + selectedNotifications.toString());
        }
        return selectedNotifications;
    }

    /**
     * Returns the notifications that the user already subscribed for.
     *
     * @return
     * @throws ClientException
     */
    private List<String> getSubscriptionsForCurrentUser()
            throws ClientException {
//        NotificationService service = (NotificationService) NXRuntime
//                .getRuntime().getComponent(NotificationService.NAME);
        DocumentModel currentDoc = navigationContext.getCurrentDocument();
        NuxeoPrincipal principal = (NuxeoPrincipal) FacesContext
                .getCurrentInstance().getExternalContext().getUserPrincipal();
        List<String> subscriptions;
        try {
            subscriptions = notificationManager.getSubscriptionsForUserOnDocument(
                    "user:" + principal.getName(), currentDoc.getId());
        } catch (ClassNotFoundException e) {
            throw new ClientException(e.getMessage());
        }
        return subscriptions;
    }

    /**
     * Returns the users that subscribed to a notification.
     *
     * @return
     * @throws ClientException
     */
    public List<String> getSubscribedUsersForNotification(String notification)
            throws ClientException {
//        NotificationService service = (NotificationService) NXRuntime
//                .getRuntime().getComponent(NotificationService.NAME);
        DocumentModel currentDoc = navigationContext.getCurrentDocument();
        return notificationManager.getUsersSubscribedToNotificationOnDocument(
                notification, currentDoc.getId());
    }

    /**
     * Returns a hashmap that contains all users and groups subscribed to notifications(keys).
     *
     * @return
     * @throws ClientException
     */
    public Map<String, List<String>> getUsersByNotificationsForCurrentDocument()
            throws ClientException {
        Map<String, List<String>> result = new HashMap<String, List<String>>();
//        NotificationService service = (NotificationService) NXRuntime
//                .getRuntime().getComponent(NotificationService.NAME);

        String superParentType = documentManager.getSuperParentType(
                navigationContext.getCurrentDocument());
        List<Notification> notifications = notificationManager.getNotificationsForSubscriptions(superParentType);
        for (Notification notification : notifications) {
            List<String> userGroups = getSubscribedUsersForNotification(notification.getName());
            List<String> principals = new ArrayList<String>();
            for (String usr : userGroups) {
                if (usr != null) {
                    principals.add(usr.substring(usr.indexOf(":") + 1));
                }
            }
            result.put(notification.getLabel(), principals);
        }
        return result;
    }

    /**
     * @param selectedNotifications The selectedNotifications to set.
     */
    public void setSelectedNotifications(List<String> selectedNotifications) {
        this.selectedNotifications = selectedNotifications;
    }

    public Map<String, String> getIconAltMap() {
        return principalListManager.iconAlt;
    }

    public Map<String, String> getIconPathMap() {
        return principalListManager.iconPath;
    }

    public SelectItem[] getNotificationActionItems() {
//        if (null == permissionActionItems) {
        log.debug("Factory method called...");

        List<String> permissionActions = new ArrayList<String>();
        List<SelectItem> jsfModelList = new ArrayList<SelectItem>();

        permissionActions.add("Subscribe");
        permissionActions.add("Unsubscribe");

        for (String permissionAction : permissionActions) {
            String label = labeler.makeLabel(permissionAction);
            SelectItem it = new SelectItem(permissionAction,
                    resourcesAccessor.getMessages().get(label));
            jsfModelList.add(it);
        }

        permissionActionItems = jsfModelList.toArray(new SelectItem[0]);
//        }

        return permissionActionItems;
    }

    public String getSelectedGrant() {
        return selectedGrant;
    }

    public void setSelectedGrant(String selectedPermission) {
        selectedGrant = selectedPermission;
    }

    /**
     * @return the selectedNotification.
     */
    public String getSelectedNotification() {
        return selectedNotification;
    }

    /**
     * @param selectedNotification The selectedNotification to set.
     */
    public void setSelectedNotification(String selectedNotification) {
        this.selectedNotification = selectedNotification;
    }

    public boolean getCanAddSubscriptions() throws ClientException {
        return documentManager.hasPermission(currentDocument.getRef(),
                "WriteSecurity");
    }

    public String addSubscriptionsAndUpdate() throws ClientException {
        if (principalListManager.getSelectedUserListEmpty()) {
            String message = ComponentUtils.translate(
                    FacesContext.getCurrentInstance(),
                    "error.notifManager.noUserSelected");
            FacesMessages.instance().add(message);
            return null;
        }
        List<String> principalsName = principalListManager.getSelectedUsers();
        String notificationName = resourcesAccessor.getMessages().get(
                notificationManager.getNotificationByName(selectedNotification).getLabel());
        boolean subscribe = selectedGrant.equals("Subscribe");

//        NotificationService service = (NotificationService) NXRuntime
//                .getRuntime().getComponent(NotificationService.NAME);
        DocumentModel currentDoc = navigationContext.getCurrentDocument();
        NuxeoPrincipal currentPrincipal = (NuxeoPrincipal) FacesContext
                .getCurrentInstance().getExternalContext().getUserPrincipal();


        for (String principal : principalsName) {
            String principalType = principalListManager
                    .getPrincipalType(principal);
            if (subscribe) {
                if ("GROUP_TYPE".equals(principalType)) {
                    notificationManager.addSubscription("group:" + principal,
                            selectedNotification, currentDoc, true, currentPrincipal, notificationName);
                } else {
                    notificationManager.addSubscription("user:" + principal,
                            selectedNotification, currentDoc, true, currentPrincipal, notificationName);
                }
            } else {
                if ("GROUP_TYPE".equals(principalType)) {
                    notificationManager.removeSubscription("group:" + principal,
                            selectedNotification, currentDoc.getId());
                } else {
                    notificationManager.removeSubscription("user:" + principal,
                            selectedNotification, currentDoc.getId());
                }
            }
        }
        facesMessages.add(FacesMessage.SEVERITY_INFO,
                resourcesAccessor.getMessages().get("label.notifications.registered"));
        return null;
    }

}
