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
 *     narcis
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.ec.notification;

/**
 * @author <a href="mailto:npaslaru@nuxeo.com">Narcis Paslaru</a>
 * @author <a href="mailto:tmartins@nuxeo.com">Thierry Martins</a>
 *
 */
public class NotificationConstants {

    /**
     * A event property to block the notification service.
     * (use Boolen.TRUE as the value to block)
     */
    public static final String DISABLE_NOTIFICATION_SERVICE = "disableNotificationService";

    public static final String SUBJECT_KEY = "subject";

    public static final String TEMPLATE_KEY = "template";

    public static final String SUBJECT_TEMPLATE_KEY = "subjectTemplate";

    public static final String SENDER_KEY = "mail.from";

    /**
     * A string array of recipients a notifications should be sent to.
     */
    public static final String RECIPIENTS_KEY = "recipients";

    public static final String DOCUMENT_KEY = "document";

    public static final String DESTINATION_KEY = "destination";

    public static final String NOTIFICATION_KEY = "notification";

    public static final String DOCUMENT_ID_KEY = "docId";

    public static final String DATE_TIME_KEY = "dateTime";

    public static final String AUTHOR_KEY = "author";

    public static final String PRINCIPAL_AUTHOR_KEY = "principalAuthor";

    public static final String DOCUMENT_URL_KEY = "docUrl";

    public static final String DOCUMENT_TITLE_KEY = "docTitle";

    public static final String EVENT_ID_KEY = "eventId";

    public static final String USER_PREFIX = "user:";

    public static final String GROUP_PREFIX = "group:";

    private NotificationConstants() {
    }

}
