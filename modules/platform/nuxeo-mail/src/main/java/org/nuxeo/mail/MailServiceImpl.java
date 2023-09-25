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

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.RuntimeMessage;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * {@link MailService} implementation, leveraging {@link MailSender} to effectively send the {@link MailMessage}.
 *
 * @since 2023.3
 */
public class MailServiceImpl extends DefaultComponent implements MailService {

    private static final Logger log = LogManager.getLogger(MailServiceImpl.class);

    public static final String DEFAULT_SENDER = "default";

    public static final String SENDERS_XP = "senders";

    protected final Map<String, MailSender> senders = new HashMap<>();

    @Override
    public void start(ComponentContext context) {
        super.start(context);
        this.<MailSenderDescriptor> getDescriptors(SENDERS_XP).forEach(d -> {
            if (d.klass.equals(JndiSMTPMailSender.class)) {
                var msg = String.format(
                        "The sender: %s contribution uses JndiSMTPMailSender which is deprecated. Please contribute a SMTPMailSender instead",
                        d.name);
                log.warn(msg);
                addRuntimeMessage(RuntimeMessage.Level.WARNING, msg, RuntimeMessage.Source.EXTENSION, d.name);
            }
            senders.put(d.getId(), d.newInstance());
        });
    }

    @Override
    public void stop(ComponentContext context) throws InterruptedException {
        super.stop(context);
        senders.clear();
    }

    @Override
    public void sendMail(MailMessage msg) {
        String senderName = msg.getSenderName();
        if (senders.containsKey(senderName)) {
            senders.get(senderName).sendMail(msg);
        } else {
            throw new NuxeoException("Couldn't send mail. MailSender: " + senderName + " not found");
        }
    }

    /**
     * @return the sender name to use in {@link MailMessage}
     * @deprecated since 2023.3 This is a fallback method to register a {@link JndiSMTPMailSender} on the fly when only
     *             given a custom JNDI session name. Use a {@link MailSenderDescriptor} to define a custom
     *             {@link MailSender} instead
     */
    @Deprecated(since = "2023.3")
    public String registerJndiSMTPSender(String jndiName) {
        log.warn(
                "Registering a JndiSMTPMailSender which is deprecated. Please contribute a SMTPMailSender instead with the properties of the following JNDI session: {}",
                jndiName);
        senders.computeIfAbsent(jndiName, JndiSMTPMailSender::new);
        return jndiName;
    }
}
