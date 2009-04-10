/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.ec.notification;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.nuxeo.ecm.platform.ec.placeful.Annotation;

@Entity
public class UserSubscription extends Annotation {

    private static final long serialVersionUID = -4511099450448368569L;

    private int usId;

    private String notification;

    private String userId;

    private String docId;

    public UserSubscription() {
        this(null, null, null);
    }

    public UserSubscription(String notification, String user, String docId) {
        this.notification = notification;
        userId = user;
        this.docId = docId;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public int getId() {
        return usId;
    }

    // Not used => remove, except is this is needed for the Entity mechanism (?)
    public void setId(int id) {
        usId = id;
    }

    public String getNotification() {
        return notification;
    }

    // Not used => remove, except is this is needed for the Entity mechanism (?)
    public void setNotification(String notif) {
        notification = notif;
    }

    public String getDocId() {
        return docId;
    }

    // Not used => remove, except is this is needed for the Entity mechanism (?)
    public void setDocId(String docId) {
        this.docId = docId;
    }

    public String getUserId() {
        return userId;
    }

    // Not used => remove, except is this is needed for the Entity mechanism (?)
    public void setUserId(String userId) {
        this.userId = userId;
    }

}
