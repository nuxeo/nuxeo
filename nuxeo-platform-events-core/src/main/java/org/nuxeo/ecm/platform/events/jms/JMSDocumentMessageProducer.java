/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 *
 * $Id: JMSDocumentMessageProducer.java 1277 2006-07-22 00:44:40Z janguenot $
 */

package org.nuxeo.ecm.platform.events.jms;

import java.io.Serializable;
import java.util.List;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.TopicConnectionFactory;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.events.api.DocumentMessageProducerException;

/**
 * JMS Document Message Producer.
 * <p>
 * Deals with sending DocumentMessage objects as JMS ObjectMessage.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public final class JMSDocumentMessageProducer {

    private static final Log log = LogFactory.getLog(JMSDocumentMessageProducer.class);

    // Utility class.
    private JMSDocumentMessageProducer() {
    }

    private static TopicConnectionFactory getJmsConnectionFactory(
            String jmsConnectionFactoryJndiName, Context ctx)
            throws DocumentMessageProducerException {

        TopicConnectionFactory jmsConnectionFactory;

        try {
            jmsConnectionFactory = (TopicConnectionFactory) ctx.lookup(jmsConnectionFactoryJndiName);

        } catch (ClassCastException cce) {
            throw new DocumentMessageProducerException(cce);
        } catch (NamingException ne) {
            throw new DocumentMessageProducerException(ne);
        }

        return jmsConnectionFactory;
    }

    private static Destination getJmsDestination(String jmsDestinationJndiName,
            Context ctx) throws DocumentMessageProducerException {

        Destination jmsDestination;

        try {
            jmsDestination = (Destination) ctx.lookup(jmsDestinationJndiName);

        } catch (ClassCastException cce) {
            throw new DocumentMessageProducerException(cce);
        } catch (NamingException ne) {
            throw new DocumentMessageProducerException(ne);
        }

        return jmsDestination;
    }

    public static void sendMessages(List<Serializable> messages,
            String connectionFactoryJndiName, String destinationJndiName)
            throws DocumentMessageProducerException {
        DocumentMessageProducerException exception = null;
        try {
            sendMessages(messages, connectionFactoryJndiName,
                    destinationJndiName, new InitialContext());
        } catch (NamingException ne) {
            exception = new DocumentMessageProducerException(ne);
        }

        if (exception != null) {
            throw exception;
        }
    }

    public static void sendMessage(Serializable message,
            String connectionFactoryJndiName, String destinationJndiName)
            throws DocumentMessageProducerException {
        DocumentMessageProducerException exception = null;
        try {
            sendMessage(message, connectionFactoryJndiName,
                    destinationJndiName, new InitialContext());
        } catch (NamingException ne) {
            exception = new DocumentMessageProducerException(ne);
        }

        if (exception != null) {
            throw exception;
        }
    }

    private static void sendMessages(List<Serializable> messages,
            String connectionFactoryJndiName, String destinationJndiName,
            Context ctx) throws DocumentMessageProducerException {

        MessageProducer messageProducer = null;
        Connection connection = null;
        Session session = null;

        try {

            // :XXX: this should be initialized only once service side at
            // startup time. See JMSDocumentMessageProducerSercice.

            TopicConnectionFactory connectionFactory = getJmsConnectionFactory(
                    connectionFactoryJndiName, ctx);
            log.trace("Found connection factory :"
                    + connectionFactory.toString());

            connection = connectionFactory.createTopicConnection();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            log.trace("Connection and session are initialized !");

            Destination destination = getJmsDestination(destinationJndiName,
                    ctx);

            log.trace("Found destination : " + destination.toString());

            messageProducer = session.createProducer(destination);

            for (Serializable message : messages) {
                ObjectMessage objectMessage = session.createObjectMessage(message);

                log.trace("Object Message generated");

                messageProducer.send(objectMessage);
            }

            log.trace("Message in the pipe !");
            log.trace("House keeping");

        } catch (JMSException je) {
            log.error(
                    "An error occured while trying to produce a JMS object message",
                    je);
        } finally {
            try {
                if (messageProducer != null) {
                    messageProducer.close();
                }
                if (session != null) {
                    session.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (JMSException je) {
                log.error("An error during JMS cleanup", je);
            }
        }
    }

    private static void sendMessage(Serializable message,
            String connectionFactoryJndiName, String destinationJndiName,
            Context ctx) throws DocumentMessageProducerException {

        try {

            // :XXX: this should be initialized only once service side at
            // startup time. See JMSDocumentMessageProducerSercice.

            TopicConnectionFactory connectionFactory = getJmsConnectionFactory(
                    connectionFactoryJndiName, ctx);
            log.trace("Found connection factory :"
                    + connectionFactory.toString());

            Connection connection = connectionFactory.createTopicConnection();

            Session session = connection.createSession(false,
                    Session.AUTO_ACKNOWLEDGE);

            log.trace("Connection and session are initialized !");

            Destination destination = getJmsDestination(destinationJndiName,
                    ctx);

            log.trace("Found destination : " + destination.toString());

            MessageProducer messageProducer = session.createProducer(destination);
            ObjectMessage objectMessage = session.createObjectMessage(message);

            log.trace("Object Message generated");

            messageProducer.send(objectMessage);

            log.trace("Message in the pipe !");

            messageProducer.close();
            session.close();
            connection.close();

            log.trace("House keeping");

        } catch (JMSException je) {
            log.error(
                    "An error occured while trying to produce a JMS object message",
                    je);
        }
    }

}
