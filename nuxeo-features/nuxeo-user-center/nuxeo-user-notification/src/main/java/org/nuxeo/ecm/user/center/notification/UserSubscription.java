/*
 * (C) Copyright 2006-2015 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     dmetzler
 */
package org.nuxeo.ecm.user.center.notification;

/**
 * Simple bean to store subscription the way it used to
 * be when JPA was used for persistence.
 *
 * @since 7.3
 */
public class UserSubscription {

    private final String docid;
    private final String notification;
    private final String username;

    public UserSubscription(String id, String notification, String prefixedUserName) {
        this.docid = id;
        this.notification = notification;
        this.username = prefixedUserName;
    }

    public String getDocId() {
        return docid;
    }

    public String getNotification() {
        return notification;
    }

    /**
     * Returns a prefixed principal id. It means it can be a group.
     * <ul>
     *   <li>user:myusername</li>
     *   <li>group:mygroupname</li>
     * </ul>
     * @return
     */
    public String getUserId() {
        return username;
    }

}
