/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     narcis
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.ec.notification;

/**
 * @author <a href="mailto:npaslaru@nuxeo.com">Narcis Paslaru</a>
 * @author <a href="mailto:tmartins@nuxeo.com">Thierry Martins</a>
 */
public class NotificationConstants {

    /**
     * A event property to block the notification service. (use Boolen.TRUE as the value to block)
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

    public static final String USER_URL_KEY = "userUrl";

    public static final String DOCUMENT_TITLE_KEY = "docTitle";

    public static final String EVENT_ID_KEY = "eventId";

    public static final String USER_PREFIX = "user:";

    public static final String GROUP_PREFIX = "group:";

    public static final String DOCUMENT_VERSION = "docVersion";

    public static final String DOCUMENT_STATE = "docState";

    public static final String DOCUMENT_CREATED = "docCreated";

    public static final String DOCUMENT_LOCATION = "docLocation";

    public static final String DOCUMENT_MAIN_FILE = "docMainFileUrl";

    public static final String IS_JSF_UI = "isJSFUI";

    /** @since 11.1 **/
    public static final String SERVER_URL_PREFIX = "serverUrlPrefix";

    private NotificationConstants() {
    }

}
