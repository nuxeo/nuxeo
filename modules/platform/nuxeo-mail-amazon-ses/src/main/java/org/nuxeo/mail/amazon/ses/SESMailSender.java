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

import static org.nuxeo.mail.MailConstants.CONFIGURATION_MAIL_FROM;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

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

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.EmailContent;
import software.amazon.awssdk.services.sesv2.model.RawMessage;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SesV2Exception;

/**
 * Implementation of {@link MailSender} building {@link RawMessage}s and sending them via Amazon SES.
 *
 * @since 2023.4
 */
public class SESMailSender implements MailSender {

    private static final Logger log = LogManager.getLogger(SESMailSender.class);

    protected static final String AWS_CONFIGURATION_ID_KEY = "awsConfigurationId";

    protected final String defaultMailFrom;

    protected final SesV2Client client;

    public SESMailSender(MailSenderDescriptor descriptor) {
        var configurationId = descriptor.getProperties().get(AWS_CONFIGURATION_ID_KEY);
        defaultMailFrom = descriptor.getProperties().get(CONFIGURATION_MAIL_FROM);
        var credentialsProvider = new NuxeoAWSCredentialsProvider(configurationId);
        var regionProvider = new NuxeoAWSRegionProvider(configurationId);
        ApacheHttpClient.Builder httpClientBuilder = ApacheHttpClient.builder();
        var awsConfigurationService = Framework.getService(AWSConfigurationService.class);
        awsConfigurationService.configureSSL(httpClientBuilder);
        awsConfigurationService.configureProxy(httpClientBuilder);
        client = SesV2Client.builder()
                            .httpClient(httpClientBuilder.build())
                            .credentialsProvider(credentialsProvider)
                            .region(regionProvider.getRegion())
                            .build();
    }

    @Override
    public void sendMail(MailMessage message) {
        try {
            var mimeMessage = buildMimeMessage(message);

            var outputStream = new ByteArrayOutputStream();
            mimeMessage.writeTo(outputStream);
            var emailContent = EmailContent.builder()
                                           .raw(b -> b.data(SdkBytes.fromByteArray(outputStream.toByteArray())))
                                           .build();
            var sendEmailRequest = SendEmailRequest.builder().content(emailContent).build();
            var response = client.sendEmail(sendEmailRequest);
            log.debug("Successfully sent mail with Amazon SES, messageId: {}", response.messageId());
        } catch (MessagingException | IOException | SesV2Exception e) {
            throw new MailException("An error occurred while sending a mail with Amazon SES", e);
        }
    }

    protected MimeMessage buildMimeMessage(MailMessage message) throws MessagingException {
        var effectiveMessage = setMissingMandatoryValues(message);
        return MimeMessageHelper.composeMimeMessage(effectiveMessage);
    }

    protected MailMessage setMissingMandatoryValues(MailMessage message) {
        boolean addFrom = message.getFroms().isEmpty();
        boolean addContent = message.getContent() == null;
        if (addFrom || addContent) {
            var builder = new MailMessage.Builder(message);
            if (addFrom) {
                builder.from(defaultMailFrom);
            }
            if (addContent) {
                builder.content("");
            }
            return builder.build();
        }
        return message;
    }

}
