/*
 * (C) Copyright 2013-2019 Nuxeo (http://nuxeo.com/) and others.
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
import static java.util.stream.Collectors.toMap;
import static org.junit.Assert.assertEquals;
import static org.nuxeo.mail.MailConstants.CONFIGURATION_MAIL_FROM;
import static org.nuxeo.mail.MailConstants.CONFIGURATION_MAIL_PREFIX;
import static org.nuxeo.mail.MailConstants.CONFIGURATION_MAIL_SMTP_FROM;
import static org.nuxeo.mail.MailConstants.CONFIGURATION_MAIL_SMTP_HOST;
import static org.nuxeo.mail.MailConstants.CONFIGURATION_MAIL_SMTP_PORT;
import static org.nuxeo.mail.MailConstants.CONFIGURATION_MAIL_TRANSPORT_PROTOCOL;
import static org.nuxeo.mail.MailConstants.DEFAULT_MAIL_JNDI_NAME;
import static org.nuxeo.mail.MailConstants.NUXEO_CONFIGURATION_MAIL_TRANSPORT_HOST;
import static org.nuxeo.mail.MailConstants.NUXEO_CONFIGURATION_MAIL_TRANSPORT_PORT;
import static org.nuxeo.mail.MailConstants.NUXEO_CONFIGURATION_MAIL_TRANSPORT_PROTOCOL;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.mail.Session;
import javax.naming.Context;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.runners.model.FrameworkMethod;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.jtajca.NuxeoContainer;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

import com.dumbster.smtp.SimpleSmtpServer;
import com.dumbster.smtp.SmtpMessage;
import com.google.inject.Binder;

/**
 * @since 11.1
 */
@Deploy("org.nuxeo.runtime.jtajca")
@Features(RuntimeFeature.class)
public class SmtpMailServerFeature implements RunnerFeature {

    private static final Logger log = LogManager.getLogger(SmtpMailServerFeature.class);

    protected static final int RETRIES = 1000;

    protected static final String SERVER_HOST = "127.0.0.1";

    protected static final String DEFAULT_MAIL_SENDER = "noreply@nuxeo.com";

    /**
     * Pattern to parse and retrieve the text date in {@link DateTimeFormatter#RFC_1123_DATE_TIME} format.
     * <p/>
     * The date filled in the smtp message should be in {@link DateTimeFormatter#RFC_1123_DATE_TIME} format, for example
     * {@code Wed, 4 Dec 2019 14:27:30 +0100}, but in some cases we can encountered
     * {@code Wed, 4 Dec 2019 14:27:30 +0100 (CET)} which cannot be parsed.
     **/
    protected static final Pattern MAIL_DATE_PATTERN = Pattern.compile("(.*)\\s(\\(\\w*\\))$");

    protected SimpleSmtpServer server;

    protected MailsResult result = new MailsResult();

    protected Map<String, Object> backupProperties;

    @Override
    public void configure(FeaturesRunner runner, Binder binder) {
        binder.bind(MailsResult.class).toInstance(result);
    }

    @Override
    public void beforeMethodRun(FeaturesRunner runner, FrameworkMethod method, Object test) {
        start();
    }

    @Override
    public void afterMethodRun(FeaturesRunner runner, FrameworkMethod method, Object test) {
        stop();
    }

    /**
     * Starts a dummy SMTP server {@link SimpleSmtpServer}.
     */
    protected void start() {
        // Try to find an available server port number
        int serverPort = getFreePort();

        // Create the server and start it
        server = SimpleSmtpServer.start(serverPort);
        log.debug("Fake smtp server started on port: {}", serverPort);

        // backup previous Framework properties
        Properties frameworkProperties = Framework.getProperties();
        backupProperties = frameworkProperties.stringPropertyNames()
                                              .stream()
                                              .filter(k -> k.startsWith(CONFIGURATION_MAIL_PREFIX))
                                              .collect(toMap(Function.identity(), frameworkProperties::get));

        // build Properties for javax.mail.Session retrieval
        Properties properties = new Properties();
        properties.putAll(backupProperties);
        properties.put(CONFIGURATION_MAIL_TRANSPORT_PROTOCOL, "smtp");
        properties.put(CONFIGURATION_MAIL_SMTP_HOST, SERVER_HOST);
        properties.put(CONFIGURATION_MAIL_SMTP_PORT, String.valueOf(serverPort));
        properties.putIfAbsent(CONFIGURATION_MAIL_SMTP_FROM, DEFAULT_MAIL_SENDER);
        properties.putIfAbsent(CONFIGURATION_MAIL_FROM, DEFAULT_MAIL_SENDER);

        binding(properties);

        // apply configuration to Framework
        frameworkProperties.put(NUXEO_CONFIGURATION_MAIL_TRANSPORT_PROTOCOL, "smtp");
        frameworkProperties.put(NUXEO_CONFIGURATION_MAIL_TRANSPORT_HOST, SERVER_HOST);
        frameworkProperties.put(NUXEO_CONFIGURATION_MAIL_TRANSPORT_PORT, String.valueOf(serverPort));
    }

    /**
     * Stops the dummy server.
     */
    protected void stop() {
        if (server != null) {
            server.stop();
        }
        unbind();
        clear();
    }

    /**
     * Binds the {@link MailConstants#DEFAULT_MAIL_JNDI_NAME} resource to a mail {@link Session}.
     */
    protected void binding(Properties properties) {
        try {
            Context context = NuxeoContainer.getRootContext();
            context.bind(DEFAULT_MAIL_JNDI_NAME, MailSessionBuilder.fromProperties(properties).build());
        } catch (NamingException ne) {
            throw new NuxeoException("Unable to bind the SMTP server in jndi", ne);
        }
    }

    /**
     * Unbinds the {@link MailConstants#DEFAULT_MAIL_JNDI_NAME} resource.
     */
    protected void unbind() {
        try {
            Context context = NuxeoContainer.getRootContext();
            context.unbind(DEFAULT_MAIL_JNDI_NAME);
        } catch (NameNotFoundException nnf) {
            log.trace("{} is not found", DEFAULT_MAIL_JNDI_NAME, nnf);
        } catch (NamingException ne) {
            throw new NuxeoException("Unable to unbind the SMTP server in jndi", ne);
        }
    }

    /**
     * Removes the added properties during mail processing.
     *
     * @since 11.1
     */
    protected void clear() {
        var frameworkProperties = Framework.getProperties();
        frameworkProperties.remove(NUXEO_CONFIGURATION_MAIL_TRANSPORT_PROTOCOL);
        frameworkProperties.remove(NUXEO_CONFIGURATION_MAIL_TRANSPORT_HOST);
        frameworkProperties.remove(NUXEO_CONFIGURATION_MAIL_TRANSPORT_PORT);
        // restore backup properties
        frameworkProperties.putAll(backupProperties);
    }

    /**
     * Try to find a free port on which a socket will listening.
     *
     * @return a free port number if any
     * @throws NuxeoException if we cannot find a free port
     * @since 11.1
     */
    protected int getFreePort() {
        int retryCount = 0;
        while (retryCount < RETRIES) {
            try (ServerSocket socket = new ServerSocket(0)) {
                socket.setReuseAddress(true);
                return socket.getLocalPort();
            } catch (IOException e) {
                retryCount++;
                log.trace("Failed to allocate port on retry {}", retryCount, e);
            }
        }
        throw new NuxeoException(String.format("Unable to find free port after %d retries", retryCount));
    }

    /**
     * Wrapper of the mails messages {@link MailMessage}.
     */
    public class MailsResult {

        public List<MailMessage> getMails() {
            if (isNull(server)) {
                log.trace("No server available");
                return List.of();
            }

            Iterable<SmtpMessage> iterable = () -> server.getReceivedEmail();
            return StreamSupport.stream(iterable.spliterator(), false).map(this::convert).collect(Collectors.toList());
        }

        public List<MailMessage> getMailsBySubject(String subject) {
            return getMails().stream().filter(e -> subject.equals(e.getSubject())).collect(Collectors.toList());
        }

        public boolean hasSubject(String subject) {
            return getMails().stream().anyMatch(e -> subject.equals(e.getSubject()));
        }

        public void assertSender(String sender, int expectedMailsCount) {
            assertEquals(expectedMailsCount, getMailsBySender(sender).size());
        }

        public void assertRecipient(String recipient, int expectedMailsCount) {
            assertEquals(expectedMailsCount, getMailsByRecipient(recipient).size());
        }

        public List<MailMessage> getMailsBySender(String from) {
            return getMails().stream().filter(e -> e.getSenders().contains(from)).collect(Collectors.toList());
        }

        public List<MailMessage> getMailsByRecipient(String recipient) {
            return getMails().stream().filter(e -> e.getRecipients().contains(recipient)).collect(Collectors.toList());
        }

        public int getSize() {
            return server != null ? server.getReceivedEmailSize() : 0;
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
                    Arrays.asList(sm.getHeaderValues("From")), //
                    Arrays.asList(sm.getHeaderValues("To")), //
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
