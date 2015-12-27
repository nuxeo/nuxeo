/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
