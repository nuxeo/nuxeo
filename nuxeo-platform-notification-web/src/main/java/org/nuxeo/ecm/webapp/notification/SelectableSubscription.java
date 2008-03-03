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

import org.nuxeo.ecm.platform.notification.api.Notification;

/**
 * Used in the web interface to display the notifications with the information
 * regarding the user's choice.
 *
 * @author <a href="mailto:npaslaru@nuxeo.com">Narcis Paslaru</a>
 *
 */
public class SelectableSubscription {

    private boolean selected;

    private Notification notification;

    public SelectableSubscription() {
    }

    public SelectableSubscription(boolean selected, Notification notification) {
        this.selected = selected;
        this.notification = notification;
    }

    /**
     * @return the notification.
     */
    public Notification getNotification() {
        return notification;
    }

    /**
     * @param notification The notification to set.
     */
    public void setNotification(Notification notification) {
        this.notification = notification;
    }

    /**
     * @return the selected.
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * @param selected The selected to set.
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

}
