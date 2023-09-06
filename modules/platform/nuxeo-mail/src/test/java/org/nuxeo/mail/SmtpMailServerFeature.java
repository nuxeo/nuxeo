/*
 * (C) Copyright 2013-2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Salem Aouana <saouana@nuxeo.com>
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.mail;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElseGet;
import static org.junit.Assert.assertEquals;
import static org.nuxeo.mail.MailConstants.CONFIGURATION_MAIL_SMTP_PORT;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.runners.model.FrameworkMethod;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

import com.dumbster.smtp.SimpleSmtpServer;
import com.dumbster.smtp.SmtpMessage;
import com.google.inject.Binder;

/**
 * @since 11.1
 */
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.runtime.jtajca")
@Deploy("org.nuxeo.mail")
@Deploy("org.nuxeo.mail.test")
public class SmtpMailServerFeature implements RunnerFeature {

    private static final Logger log = LogManager.getLogger(SmtpMailServerFeature.class);

    /**
     * Pattern to parse and retrieve the text date in {@link DateTimeFormatter#RFC_1123_DATE_TIME} format.
     * <p>
     * The date filled in the smtp message should be in {@link DateTimeFormatter#RFC_1123_DATE_TIME} format, for example
     * {@code Wed, 4 Dec 2019 14:27:30 +0100}, but in some cases we can encountered
     * {@code Wed, 4 Dec 2019 14:27:30 +0100 (CET)} which cannot be parsed.
     **/
    protected static final Pattern MAIL_DATE_PATTERN = Pattern.compile("(.*)\\s(\\(\\w*\\))$");

    protected SimpleSmtpServer server;

    protected MailsResult result = new MailsResult();

    @Override
    public void configure(FeaturesRunner runner, Binder binder) {
        binder.bind(MailsResult.class).toInstance(result);
    }

    @Override
    public void start(FeaturesRunner runner) throws Exception {
        try {
            server = SimpleSmtpServer.start(SimpleSmtpServer.AUTO_SMTP_PORT);
            result.skip = 0;
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
        int serverPort = server.getPort();
        log.debug("Fake smtp server started on port: {}", serverPort);

        Framework.getProperties().setProperty("nuxeo.test." + CONFIGURATION_MAIL_SMTP_PORT, String.valueOf(serverPort));
        RuntimeHarness harness = runner.getFeature(RuntimeFeature.class).getHarness();
        harness.deployContrib("org.nuxeo.mail.test", "OSGI-INF/test-smtp-mail-sender-contrib.xml");
    }

    @Override
    public void beforeSetup(FeaturesRunner runner, FrameworkMethod method, Object test) {
        result.clearMails();
    }

    @Override
    public void stop(FeaturesRunner runner) throws Exception {
        if (server != null) {
            server.stop();
        }
    }

    /**
     * Wrapper of the mails messages {@link MailMessage}.
     */
    public class MailsResult {

        protected int skip = 0;

        public Stream<MailMessage> streamMails() {
            if (isNull(server)) {
                log.trace("No server available");
                return Stream.empty();
            }
            return server.getReceivedEmails().stream().map(this::convert).skip(skip);
        }

        public List<MailMessage> getMails() {
            return streamMails().collect(Collectors.toList());
        }

        public void clearMails() {
            if (isNull(server)) {
                log.trace("No server available");
                return;
            }
            this.skip = server.getReceivedEmails().size();
        }

        public List<MailMessage> getMailsBySubject(String subject) {
            return streamMails().filter(e -> subject.equals(e.getSubject())).collect(Collectors.toList());
        }

        public boolean hasSubject(String subject) {
            return streamMails().anyMatch(e -> subject.equals(e.getSubject()));
        }

        public void assertSender(String sender, int expectedMailsCount) {
            assertEquals(expectedMailsCount, getMailsBySender(sender).size());
        }

        public void assertRecipient(String recipient, int expectedMailsCount) {
            assertEquals(expectedMailsCount, getMailsByRecipient(recipient).size());
        }

        public List<MailMessage> getMailsBySender(String from) {
            return streamMails().filter(e -> e.getSenders().contains(from)).collect(Collectors.toList());
        }

        public List<MailMessage> getMailsByRecipient(String recipient) {
            return streamMails().filter(e -> e.getRecipients().contains(recipient)).collect(Collectors.toList());
        }

        public int getSize() {
            return (int) streamMails().count();
        }

        protected MailMessage convert(SmtpMessage sm) {
            ZonedDateTime date = null;
            if (StringUtils.isNoneBlank(sm.getHeaderValue("Date"))) {
                String dateAsText = sm.getHeaderValue("Date");
                Matcher matcher = MAIL_DATE_PATTERN.matcher(dateAsText);
                if (matcher.matches()) {
                    dateAsText = matcher.group(1);
                }
                date = ZonedDateTime.parse(dateAsText, DateTimeFormatter.RFC_1123_DATE_TIME);
            }

            return new MailMessage(sm.getHeaderValue("Message-ID"), //
                    sm.getHeaderValues("From"), //
                    sm.getHeaderValues("To"), //
                    sm.getHeaderValue("Subject"), //
                    sm.getHeaderValue("Content-Type"), //
                    sm.getBody(), date);
        }
    }

    /**
     * Adapter of the {@link SmtpMessage}.
     *
     * @since 11.1
     */
    public static class MailMessage {

        protected String id;

        protected List<String> senders;

        protected List<String> recipients;

        protected String subject;

        protected String content;

        protected ZonedDateTime date;

        protected String contentType;

        public MailMessage(String id, List<String> senders, List<String> recipients, String subject, String contentType,
                String content, ZonedDateTime date) {
            this.id = requireNonNull(id);
            this.senders = requireNonNullElseGet(senders, List::of);
            this.recipients = requireNonNullElseGet(recipients, List::of);
            this.subject = subject;
            this.contentType = contentType;
            this.content = content;
            this.date = date;
        }

        public String getId() {
            return id;
        }

        public List<String> getSenders() {
            return senders;
        }

        public List<String> getRecipients() {
            return recipients;
        }

        public String getSubject() {
            return subject;
        }

        public String getContent() {
            return content;
        }

        public ZonedDateTime getDate() {
            return date;
        }

        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            MailMessage that = (MailMessage) o;

            return new EqualsBuilder().append(id, that.id).isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 37).append(id).toHashCode();
        }
    }

}
