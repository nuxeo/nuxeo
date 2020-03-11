/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.ecm.platform.ec.notification.email;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.nuxeo.mail.MailConstants.CONFIGURATION_MAIL_SMTP_PASSWORD;
import static org.nuxeo.mail.MailConstants.CONFIGURATION_MAIL_SMTP_USER;

import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

import javax.naming.RefAddr;
import javax.naming.StringRefAddr;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * Tests the {@link EmailResourceFactory}.
 *
 * @since 10.2
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
public class TestEmailResourceFactory {

    @Test
    public void testToProperties() {

        EmailResourceFactory factory = new EmailResourceFactory();
        Vector<RefAddr> initialProperties = new Vector<>();

        // null property
        Enumeration<RefAddr> attributes = initialProperties.elements();
        Properties properties = factory.toProperties(attributes);
        assertNull(properties.getProperty("non.existing.property"));

        // raw property
        initialProperties.add(new StringRefAddr(CONFIGURATION_MAIL_SMTP_USER, "joe"));
        attributes = initialProperties.elements();
        properties = factory.toProperties(attributes);
        assertEquals("joe", properties.getProperty(CONFIGURATION_MAIL_SMTP_USER));

        // var property, needs to be expanded
        Framework.getProperties().put("mail.transport.password", "varExpandedPassword");
        initialProperties.add(new StringRefAddr(CONFIGURATION_MAIL_SMTP_PASSWORD, "${mail.transport.password}"));
        attributes = initialProperties.elements();
        properties = factory.toProperties(attributes);
        assertEquals("varExpandedPassword", properties.getProperty(CONFIGURATION_MAIL_SMTP_PASSWORD));
        Framework.getProperties().remove("mail.transport.password");
    }
}
