/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.mail;

/**
 * @since 2023.4
 * @deprecated since 2023.4 Compatibility implementation of MailSender relying on a MailSession available through JNDI.
 *             <p>
 *             Use a {@link MailSenderDescriptor} to configure your {@link MailSender} properly.
 */
@Deprecated(since = "2023.4")
public class JndiSMTPMailSender extends SMTPMailSender {

    public static final String JNDI_SESSION_NAME = "jndiSessionName";

    public JndiSMTPMailSender(String jndiName) {
        super(MailSessionBuilder.fromJndi(jndiName).build());
    }

    public JndiSMTPMailSender(MailSenderDescriptor descriptor) {
        super(MailSessionBuilder.fromJndi(descriptor.properties.get(JNDI_SESSION_NAME)).build());
    }

}
