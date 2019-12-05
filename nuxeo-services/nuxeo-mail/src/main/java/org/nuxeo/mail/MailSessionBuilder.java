/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *  Contributors:
 *      Kevin Leturc <kleturc@nuxeo.com>
 */

package org.nuxeo.mail;

import static java.lang.Boolean.FALSE;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.nuxeo.mail.MailConstants.CONFIGURATION_MAIL_DEBUG;
import static org.nuxeo.mail.MailConstants.CONFIGURATION_MAIL_TRANSPORT_PROTOCOL;

import java.io.PrintStream;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.api.Framework;

/**
 * Builds your {@link javax.mail.Session}.
 *
 * @since 11.1
 */
public class MailSessionBuilder {

    private static final Logger log = LogManager.getLogger(MailSessionBuilder.class);

    /**
     * Creates a {@link FromBuilder builder} looking for a jndi {@link Session} and falling back on
     * {@link Framework#getProperties()} if not found.
     */
    public static FromBuilder fromNuxeoConf() {
        Properties frameworkProperties = Framework.getProperties();
        Properties properties = frameworkProperties.stringPropertyNames() // no other clean api
                                                   .stream()
                                                   .filter(key -> key.startsWith("mail."))
                                                   .collect(Collectors.toMap(Function.identity(),
                                                           frameworkProperties::getProperty, //
                                                           (v1, v2) -> v2, // should't happen
                                                           Properties::new));
        return fromJndi(Framework.getProperty("jndi.name")).fallbackOn(properties);
    }

    /**
     * Creates a {@link FromJndi builder} looking for a session in jndi.
     */
    public static FromJndi fromJndi(String jndiSessionName) {
        return new FromJndi(jndiSessionName);
    }

    /**
     * Creates a {@link FromProperties builder} instantiating a new session.
     */
    public static FromProperties fromProperties(Properties properties) {
        return new FromProperties(properties);
    }

    public static class FromJndi extends AbstractFrom<FromJndi> {

        protected String jndiSessionName;

        private FromJndi(String jndiSessionName) {
            this.jndiSessionName = Objects.requireNonNull(jndiSessionName, "jndi Mail Session name is required");
        }

        @Override
        protected Session retrieveSession() {
            try {
                log.debug("Lookup for javax.mail.Session with jndi name: {}", jndiSessionName);
                return (Session) new InitialContext().lookup(jndiSessionName);
            } catch (NamingException e) {
                throw new NuxeoException(e);
            }
        }

        public FromProperties fallbackOn(Properties properties) {
            return new FromProperties(properties) {

                @Override
                protected Session retrieveSession() {
                    try {
                        return FromJndi.this.retrieveSession();
                    } catch (NuxeoException e) {
                        log.debug("Lookup failed for javax.mail.Session with jndi name: {}, fallback on properties",
                                jndiSessionName, e);
                        return super.retrieveSession();
                    }
                }

                @Override
                public String toString() {
                    return new ToStringBuilder(this).appendToString(FromJndi.this.toString())
                                                    .appendToString(super.toString())
                                                    .build();
                }
            };
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this).append("jndiSessionName", jndiSessionName).toString();
        }

    }

    public static class FromProperties extends AbstractFrom<FromProperties> {

        protected Properties properties;

        private FromProperties(Properties properties) {
            this.properties = properties;
            // remove the debug properties due to javax.mail.Session initialization which prints to the console
            this.debug = properties != null
                    && Boolean.parseBoolean((String) properties.remove(CONFIGURATION_MAIL_DEBUG));
        }

        @Override
        protected Session retrieveSession() {
            log.debug("Lookup session from properties: {}", properties);
            return Session.getInstance(properties, new MailAuthenticator(properties));
        }

        @Override
        public String toString() {
            ToStringBuilder builder = new ToStringBuilder(this);
            var propertiesAsMap = properties.entrySet()
                                            .stream()
                                            .filter(not(s -> s.getKey().toString().contains("password")))
                                            .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
            builder.append("properties", propertiesAsMap);
            return builder.toString();
        }
    }

    @SuppressWarnings("rawtypes")
    protected abstract static class AbstractFrom<F extends AbstractFrom> implements FromBuilder {

        protected boolean debug;

        /**
         * Enables debug mode for built session.
         *
         * @implNote {@code mail.debug} in nuxeo.conf takes precedence if it's true.
         * @return this builder
         */
        @SuppressWarnings("unchecked")
        public F debug() {
            this.debug = true;
            return (F) this;
        }

        public Session build() {
            log.info("Build a javax.mail.Session from builder: {}", this);
            if (!debug) {
                // check nuxeo.conf - same configuration key
                log.trace("Mail log debug enabled by nuxeo.conf");
                debug = Boolean.parseBoolean(Framework.getProperty(CONFIGURATION_MAIL_DEBUG, FALSE.toString()));
            }
            var session = retrieveSession();
            if (debug) {
                session.setDebugOut(new PrintStream(new MailLogOutputStream()));
                session.setDebug(true);
            }
            // set proper protocol for rfc822
            String protocol = session.getProperty(CONFIGURATION_MAIL_TRANSPORT_PROTOCOL);
            if (isNotEmpty(protocol)) {
                session.setProtocolForAddress("rfc822", protocol);
            }
            return session;
        }

        protected abstract Session retrieveSession();

        @Override
        public Store buildAndConnect() {
            try {
                var session = build();
                var store = session.getStore();
                // handle backward compatibility for MailCoreHelper
                // by default user/password are read from authenticator configured through jndi Resource or Properties
                var user = session.getProperty("user");
                var password = session.getProperty("password");
                store.connect(user, password); // accept nulls
                return store;
            } catch (MessagingException e) {
                throw new NuxeoException("Unable to build/connect javax.mail.Store", e);
            }
        }
    }

    public interface FromBuilder {

        @NotNull
        Session build();

        @NotNull
        Store buildAndConnect();
    }
}
