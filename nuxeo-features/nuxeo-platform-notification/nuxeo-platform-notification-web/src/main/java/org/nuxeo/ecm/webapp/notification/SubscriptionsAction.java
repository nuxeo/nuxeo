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
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.ec.notification.NotificationConstants;
import org.nuxeo.ecm.platform.notification.api.Notification;
import org.nuxeo.ecm.platform.notification.api.NotificationManager;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.base.InputController;
import org.nuxeo.ecm.webapp.helpers.EventNames;

/**
 * Handles the subscriptions page.
 *
 * @author <a href="mailto:npaslaru@nuxeo.com">Narcis Paslaru</a>
 */
@Name("subscriptionAction")
@Scope(ScopeType.PAGE)
public class SubscriptionsAction extends InputController implements
        Serializable {

    private static final long serialVersionUID = -2440187703248677446L;

    private static final Log log = LogFactory.getLog(SubscriptionsAction.class);

    @In
    protected transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @DataModel("notificationList")
    protected List<SelectableSubscription> notificationList;

    @DataModel("inheritedNotifications")
    private List<Notification> inheritedNotifications;

    @DataModelSelection(value="notificationList")
    private SelectableSubscription currentSubscription;

    @In(create = true)
    protected transient NotificationManager notificationManager;

    /**
     * Gets all the notifications the user may subscribe to.
     */
    @Factory("notificationList")
    public void getNotificationsList() throws ClientException {
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
    public void loadInheritedNotifications() throws ClientException, ClassNotFoundException {
        inheritedNotifications = new ArrayList<Notification>();
        DocumentModel currentDoc = navigationContext.getCurrentDocument();
        NuxeoPrincipal principal = (NuxeoPrincipal) FacesContext
                .getCurrentInstance().getExternalContext().getUserPrincipal();
        for(String group : principal.getAllGroups()){
            List<String> notifs = notificationManager.getSubscriptionsForUserOnDocument("group:"+group, currentDoc.getId());
            for (String inheritedNotification : notifs) {
                Notification notif = notificationManager.getNotificationByName(inheritedNotification);
                inheritedNotifications.add(notif);
            }
        }
    }
    /**
     * Registers the user's choices.
     */
    public void updateSubscriptions() throws ClientException {

        NuxeoPrincipal principal = (NuxeoPrincipal) FacesContext
                .getCurrentInstance().getExternalContext().getUserPrincipal();
        DocumentModel currentDoc = navigationContext.getCurrentDocument();
        if (currentSubscription.isSelected()) {
            notificationManager.removeSubscription("user:" + principal.getName(),
                    currentSubscription.getNotification().getName(),
                    currentDoc.getId());
        } else {
            notificationManager.addSubscription(NotificationConstants.USER_PREFIX + principal.getName(),
                    currentSubscription.getNotification().getName(),
                    currentDoc, false, principal, "");
        }
        getNotificationsList();
    }

    @Observer(value=EventNames.DOCUMENT_SELECTION_CHANGED, create=false)
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
    public List<String> getSelectedNotifications() throws ClientException {
        return getSubscriptionsForCurrentUser();
    }

    /**
     * Returns the notifications that the user already subscribed for.
     */
    private List<String> getSubscriptionsForCurrentUser()
            throws ClientException {

        DocumentModel currentDoc = navigationContext.getCurrentDocument();
        NuxeoPrincipal principal = (NuxeoPrincipal) FacesContext
                .getCurrentInstance().getExternalContext().getUserPrincipal();
        List<String> subscriptions;
        try {
            subscriptions = notificationManager.getSubscriptionsForUserOnDocument("user:"
                    + principal.getName(), currentDoc.getId());
        } catch (ClassNotFoundException e) {
            throw new ClientException(e.getMessage());
        }
        return subscriptions;
    }

    public SelectableSubscription getCurrentSubscription() {
        return currentSubscription;
    }

    public void setCurrentSubscription(
            SelectableSubscription currentSubscription) {
        this.currentSubscription = currentSubscription;
    }

    public List<SelectableSubscription> getNotificationList() {
        return notificationList;
    }

    public void setNotificationList(
            List<SelectableSubscription> notificationList) {
        this.notificationList = notificationList;
    }

    public List<Notification> getInheritedNotifications() {
        return inheritedNotifications;
    }

    public void setInheritedNotifications(List<Notification> inheritedNotifications) {
        this.inheritedNotifications = inheritedNotifications;
    }

}
