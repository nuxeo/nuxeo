/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.mail;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class Mailer {

    private static final Log log = LogFactory.getLog(Mailer.class);

    protected Properties config;

    protected volatile Session session;

    protected Authenticator auth;

    /**
     * The JNDI session name. If not null JNDI will be used to lookup the default session, otherwise local configuration
     * (through {@link #config}) will be used to create a session.
     */
    protected final String sessionName;

    /**
     * Create a mailer that can be configured using the API.
     *
     * @see #setAuthenticator(Authenticator)
     * @see #setCredentials(String, String)
     * @see #setServer(String)
     */
    public Mailer() {
        this(null, new Properties());
    }

    /**
     * Create a mailer that use the given properties to configure the session.
     *
     * @param config
     */
    public Mailer(Properties config) {
        this(null, config);
    }

    /**
     * Create a mailer using a session that lookup for the session in JNDI under the given session name.
     *
     * @param sessionName
     */
    public Mailer(String sessionName) {
        this(sessionName, new Properties());
    }

    /**
     * Create a mailer using a session that lookup for the session in JNDI under the given session name. If the JNDI
     * binding doesn't exists use the given properties to cinfiugure the session.
     *
     * @param sessionName
     * @param config
     */
    public Mailer(String sessionName, Properties config) {
        this.config = config;
        this.sessionName = sessionName;
        final String user = config.getProperty("mail.smtp.user");
        final String pass = config.getProperty("mail.smtp.password");
        if (user != null && pass != null) {
            setAuthenticator(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(user, pass);
                }
            });
        }
    }

    public void setServer(String host) {
        setServer(host, "25", false);
    }

    public void setServer(String host, boolean ssl) {
        setServer(host, ssl ? "465" : "25", ssl);
    }

    /**
     * Set the SMTP server address to use
     *
     * @param host
     * @param port
     */
    public void setServer(String host, String port) {
        setServer(host, port, false);
    }

    public void setServer(String host, String port, boolean ssl) {
        if (ssl) {
            if (port == null) {
                port = "465";
            }
            config.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            config.put("mail.smtp.ssl.checkserveridentity", "true");
            config.put("mail.smtp.socketFactory.fallback", "false");
            config.put("mail.smtp.socketFactory.port", port);

        } else if (port == null) {
            port = "25";
        }
        config.setProperty("mail.smtp.host", host);
        config.setProperty("mail.smtp.port", port);
        session = null;
    }

    /**
     * Set SMTP credential
     *
     * @param user
     * @param pass
     */
    public void setCredentials(final String user, final String pass) {
        config.setProperty("mail.smtp.auth", "true");
        auth = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, pass);
            }
        };
        session = null;
    }

    public void setAuthenticator(Authenticator auth) {
        config.setProperty("mail.smtp.auth", "true");
        this.auth = auth;
        session = null;
    }

    public void setDebug(boolean debug) {
        config.setProperty("mail.debug", Boolean.toString(debug));
    }

    public Session getSession() {
        if (session == null) {
            synchronized (this) {
                if (session == null) {
                    if (sessionName != null) {
                        try {
                            InitialContext ic = new InitialContext();
                            session = (Session) ic.lookup(sessionName);
                        } catch (NamingException e) {
                            log.warn("Failed to lookup mail session using JNDI name " + sessionName
                                    + ". Falling back on local configuration.");
                            session = Session.getInstance(config, auth);
                        }
                    } else {
                        session = Session.getInstance(config, auth);
                    }
                }
            }
        }
        return session;
    }

    public Properties getConfiguration() {
        return config;
    }

    public void setConfiguration(Properties config) {
        this.config = config;
    }

    public void loadConfiguration(InputStream in) throws IOException {
        config.load(in);
    }

    public void send(MimeMessage message) throws MessagingException {
        Transport.send(message);
    }

    public Message newMessage() {
        return new Message(getSession());
    }

    /**
     * Send a single email.
     */
    public void sendEmail(String from, String to, String subject, String body) throws MessagingException {
        // Here, no Authenticator argument is used (it is null).
        // Authenticators are used to prompt the user for user
        // name and password.
        MimeMessage message = new MimeMessage(getSession());
        // the "from" address may be set in code, or set in the
        // config file under "mail.from" ; here, the latter style is used
        message.setFrom(new InternetAddress(from));
        message.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(to));
        message.setSubject(subject);
        message.setText(body);
        Transport.send(message);
    }

    public static class Message extends MimeMessage {

        public static enum AS {
            FROM, TO, CC, BCC, REPLYTO
        }

        public Message(Session session) {
            super(session);
        }

        public Message(Session session, InputStream in) throws MessagingException {
            super(session, in);
        }

        public Message addTo(String to) throws MessagingException {
            addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(to));
            return this;
        }

        public Message addCc(String cc) throws MessagingException {
            addRecipient(javax.mail.Message.RecipientType.CC, new InternetAddress(cc));
            return this;
        }

        public Message addBcc(String bcc) throws MessagingException {
            addRecipient(javax.mail.Message.RecipientType.BCC, new InternetAddress(bcc));
            return this;
        }

        public Message addFrom(String from) throws MessagingException {
            addFrom(new InternetAddress[] { new InternetAddress(from) });
            return this;
        }

        public void addInfoInMessageHeader(String address, AS as) throws MessagingException {
            switch (as) {
            case FROM:
                addFrom(address);
                break;
            case TO:
                addTo(address);
                break;
            case CC:
                addCc(address);
                break;
            case BCC:
                addBcc(address);
                break;
            case REPLYTO:
                Address[] oldValue = getReplyTo();
                Address[] replyToValue;
                if (getReplyTo() == null) {
                    replyToValue = new Address[1];
                } else {
                    replyToValue = new Address[oldValue.length + 1];
                }
                for (int i = 0; i < oldValue.length; i++) {
                    replyToValue[i] = oldValue[i];
                }
                replyToValue[oldValue.length] = new InternetAddress(address);
                setReplyTo(replyToValue);
                break;
            default:
                throw new MessagingException("Unknown header info " + as.toString());
            }
        }

        public Message setFrom(String from) throws MessagingException {
            setFrom(new InternetAddress(from));
            return this;
        }

        public void send() throws MessagingException {
            Transport.send(this);
        }

    }

}
