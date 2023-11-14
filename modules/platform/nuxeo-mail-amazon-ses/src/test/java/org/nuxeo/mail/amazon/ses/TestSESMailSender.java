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
 *
 */

package org.nuxeo.mail.amazon.ses;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.mail.MailException;
import org.nuxeo.mail.MailMessage;
import org.nuxeo.mail.MailService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.amazonaws.services.simpleemail.model.AmazonSimpleEmailServiceException;

/**
 * @since 2023.4
 */
@RunWith(FeaturesRunner.class)
@Features(SESFeature.class)
public class TestSESMailSender {

    protected static final String SES_SUCCESS_SIMULATOR_MAIL = "success@simulator.amazonses.com";

    @Inject
    protected MailService mailService;

    @Test
    public void testSendMailWithDefaultFrom() {
        var mailMessage = getBaseMessageBuilder().build(); // no explicit from

        // If no exception is thrown, the message has been handled successfully
        mailService.sendMail(mailMessage);
    }

    @Test
    public void testSendMailWithExplicitFrom() {
        var mailMessage = getBaseMessageBuilder().from("foo@bar.com").build();

        // The explicit from should be used and fail as it is not verified at SES
        assertSESFail(mailMessage);
    }

    @Test
    public void testSendMailMalformed() {
        var mailMessage = getBaseMessageBuilder().cc("malformedAddress").build();

        // Should fail as the recipients can't hold malformed addresses
        assertSESFail(mailMessage);
    }

    @Test
    @Deploy("org.nuxeo.mail.amazon.ses.test:OSGI-INF/test-ses-wrong-sender-contrib.xml")
    public void testSendMailWithWrongCredentials() {
        var mailMessage = getBaseMessageBuilder().senderName("wrongSender").build();

        // Credentials from the wrongSender were successfully used
        assertSESFail(mailMessage);
    }

    protected MailMessage.Builder getBaseMessageBuilder() {
        return new MailMessage.Builder(SES_SUCCESS_SIMULATOR_MAIL);
    }

    protected void assertSESFail(MailMessage mailMessage) {
        var t = assertThrows("An error occurred while sending a mail", MailException.class,
                () -> mailService.sendMail(mailMessage));
        assertTrue(t.getCause() instanceof AmazonSimpleEmailServiceException);
    }
}
