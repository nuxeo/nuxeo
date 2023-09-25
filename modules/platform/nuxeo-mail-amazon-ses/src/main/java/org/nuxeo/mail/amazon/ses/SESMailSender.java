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

package org.nuxeo.mail.amazon.ses;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.mail.MessagingException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.mail.MailException;
import org.nuxeo.mail.MailMessage;
import org.nuxeo.mail.MailSender;
import org.nuxeo.mail.MailSenderDescriptor;
import org.nuxeo.mail.MimeMessageHelper;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.aws.AWSConfigurationService;
import org.nuxeo.runtime.aws.NuxeoAWSCredentialsProvider;
import org.nuxeo.runtime.aws.NuxeoAWSRegionProvider;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.AmazonSimpleEmailServiceException;
import com.amazonaws.services.simpleemail.model.RawMessage;
import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;

/**
 * Implementation of {@link MailSender} building {@link RawMessage}s and sending them via Amazon SES.
 *
 * @since 2023.4
 */
public class SESMailSender implements MailSender {

    private static final Logger log = LogManager.getLogger(SESMailSender.class);

    protected static final String AWS_CONFIGURATION_ID_KEY = "awsConfigurationId";

    protected final AmazonSimpleEmailService client;

    public SESMailSender(MailSenderDescriptor descriptor) {
        var configurationId = descriptor.getProperties().get(AWS_CONFIGURATION_ID_KEY);
        var credentialsProvider = new NuxeoAWSCredentialsProvider(configurationId);
        var regionProvider = new NuxeoAWSRegionProvider(configurationId);

        var clientConfiguration = new ClientConfiguration();
        var awsConfigurationService = Framework.getService(AWSConfigurationService.class);
        awsConfigurationService.configureSSL(clientConfiguration);
        awsConfigurationService.configureProxy(clientConfiguration);

        client = AmazonSimpleEmailServiceClientBuilder.standard()
                                                      .withClientConfiguration(clientConfiguration)
                                                      .withCredentials(credentialsProvider)
                                                      .withRegion(regionProvider.getRegion())
                                                      .build();
    }

    @Override
    public void sendMail(MailMessage message) {
        try {
            var mimeMessage = MimeMessageHelper.composeMimeMessage(message);

            var outputStream = new ByteArrayOutputStream();
            mimeMessage.writeTo(outputStream);
            var rawMessage = new RawMessage(ByteBuffer.wrap(outputStream.toByteArray()));
            var sendRawEmailRequest = new SendRawEmailRequest(rawMessage);

            var response = client.sendRawEmail(sendRawEmailRequest);
            log.debug("Successfully sent mail with Amazon SES, messageId: {}", response.getMessageId());
        } catch (MessagingException | IOException | AmazonSimpleEmailServiceException e) {
            throw new MailException("An error occurred while sending a mail with Amazon SES", e);
        }
    }
}
