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

import static com.amazonaws.SDKGlobalConfiguration.ACCESS_KEY_ENV_VAR;
import static com.amazonaws.SDKGlobalConfiguration.ALTERNATE_SECRET_KEY_ENV_VAR;
import static com.amazonaws.SDKGlobalConfiguration.AWS_REGION_ENV_VAR;
import static org.apache.commons.lang3.StringUtils.isNoneBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

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

    protected static final String AWS_SES_MAIL_SENDER = "AWS_SES_MAIL_SENDER";

    protected static final String VERIFIED_SENDER_MAIL = System.getenv(AWS_SES_MAIL_SENDER);

    protected static final String SES_SUCCESS_SIMULATOR_MAIL = "success@simulator.amazonses.com";

    @Inject
    protected MailService mailService;

    @Test
    public void testSendMail() {
        assumeAWSDefaultConfigEnabled();
        assumeTrue(isNotBlank(VERIFIED_SENDER_MAIL));
        var mailMessage = getBaseMessageBuilder().from(VERIFIED_SENDER_MAIL).build();

        // If no exception is thrown, the message has been handled successfully
        mailService.sendMail(mailMessage);
    }

    @Test
    public void testSendMailMalformed() {
        assumeAWSDefaultConfigEnabled();
        var mailMessage = getBaseMessageBuilder().build(); // no from

        var t = assertThrows("An error occurred while sending a mail", MailException.class,
                () -> mailService.sendMail(mailMessage));
        assertTrue(t.getCause() instanceof AmazonSimpleEmailServiceException);
    }

    @Test
    @Deploy("org.nuxeo.mail.amazon.ses.test:OSGI-INF/test-ses-wrong-sender-contrib.xml")
    public void testSendMailWithWrongCredentials() {
        assumeTrue(isNotBlank(VERIFIED_SENDER_MAIL));
        var mailMessage = getBaseMessageBuilder().from(VERIFIED_SENDER_MAIL).senderName("wrongSender").build();

        var t = assertThrows("An error occurred while sending a mail", MailException.class,
                () -> mailService.sendMail(mailMessage));
        assertTrue(t.getCause() instanceof AmazonSimpleEmailServiceException);
    }

    protected MailMessage.Builder getBaseMessageBuilder() {
        return new MailMessage.Builder(SES_SUCCESS_SIMULATOR_MAIL).subject("test").content("my content");
    }

    protected void assumeAWSDefaultConfigEnabled() {
        assumeTrue("AWS credentials and/or region are missing in test configuration",
                isNoneBlank(System.getenv(ACCESS_KEY_ENV_VAR), System.getenv(ALTERNATE_SECRET_KEY_ENV_VAR),
                        System.getenv(AWS_REGION_ENV_VAR)));
    }
}
