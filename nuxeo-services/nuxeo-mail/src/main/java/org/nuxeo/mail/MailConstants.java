/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Contributors:
 *      Kevin Leturc <kleturc@nuxeo.com>
 */

package org.nuxeo.mail;

/**
 * @since 11.1
 */
public final class MailConstants {

    public static final String CONFIGURATION_MAIL_DEBUG = "mail.debug";

    public static final String CONFIGURATION_MAIL_SMTP_AUTH = "mail.smtp.auth";

    public static final String CONFIGURATION_MAIL_SMTP_HOST = "mail.smtp.host";

    public static final String CONFIGURATION_MAIL_SMTP_PORT = "mail.smtp.port";

    public static final String CONFIGURATION_MAIL_SMTP_PASSWORD = "mail.smtp.password";

    public static final String CONFIGURATION_MAIL_SMTP_STARTTLS_ENABLE = "mail.smtp.starttls.enable";

    public static final String CONFIGURATION_MAIL_SMTP_USER = "mail.smtp.user";

    public static final String CONFIGURATION_MAIL_STORE_PROTOCOL = "mail.store.protocol";

    public static final String CONFIGURATION_MAIL_TRANSPORT_PROTOCOL = "mail.transport.protocol";

    private MailConstants() {
        // not allowed
    }
}
