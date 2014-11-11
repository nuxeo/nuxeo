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
import java.util.Properties;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
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
import org.nuxeo.ecm.platform.events.api.DocumentMessage;
import org.nuxeo.ecm.platform.events.api.DocumentMessageProducerException;
import org.nuxeo.ecm.platform.events.api.EventMessage;
import org.nuxeo.ecm.platform.events.api.JMSConstant;
import org.nuxeo.ecm.platform.events.api.NXCoreEvent;
import org.nuxeo.runtime.api.Framework;

/**
 * JMS Document Message Producer.
 * <p>
 * Deals with sending DocumentMessage objects as JMS ObjectMessage.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public final class JMSDocumentMessageProducer {
//XXX: in the process of being refactored
    private static boolean transacted;
    private static boolean isDeliveryPersistent;
    private static boolean isDisableMessageID;
    private static boolean isDisableMessageTimestamp;
    private static final Log log = LogFactory.getLog(JMSDocumentMessageProducer.class);
    static {
        Properties runtime = Framework.getRuntime().getProperties();
        transacted = new Boolean(runtime.getProperty("jms.useTransactedConnection"));
        isDeliveryPersistent = new Boolean(runtime.getProperty("jms.isDeliveryPersistent"));
        isDisableMessageID = new Boolean(runtime.getProperty("jms.isDisableMessageID"));
        isDisableMessageTimestamp = new Boolean(runtime.getProperty("jms.isDisableMessageTimestamp"));
    }

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
    public static void sendNXCoreEventMessages(List<NXCoreEvent> messages,
            String connection, String destination)
            throws DocumentMessageProducerException {
        DocumentMessageProducerException exception = null;
        try {
            sendNXCoreEventMessages(messages, connection, destination,
                    new InitialContext());
        } catch (NamingException ne) {
            exception = new DocumentMessageProducerException(ne);
        }

        if (exception != null) {
            throw exception;
        }
    }
    public static void sendEventMessages(List<EventMessage> messages, String connection, String destination) 
            throws DocumentMessageProducerException {
        DocumentMessageProducerException exception = null;
        try {
            sendEventMessages(messages, connection, destination, new InitialContext());
        } catch (NamingException ne) {
            exception = new DocumentMessageProducerException(ne);
        }

        if (exception != null) {
            throw exception;
        }
    }
    public static void sendDocumentMessages(List<DocumentMessage> messages,
            String connectionFactoryJndiName, String destinationJndiName)
            throws DocumentMessageProducerException {
        DocumentMessageProducerException exception = null;
        try {
            sendDocumentMessages(messages, connectionFactoryJndiName,
                    destinationJndiName, new InitialContext());
        } catch (NamingException ne) {
            exception = new DocumentMessageProducerException(ne);
        }

        if (exception != null) {
            throw exception;
        }
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

    public static void sendMessage(DocumentMessage message, String connectionFactory, String destination)
            throws DocumentMessageProducerException {
        DocumentMessageProducerException exception = null;
        try {
            sendMessage(message, connectionFactory, destination, new InitialContext(), message.getEventId(), JMSConstant.DOCUMENT_MESSAGE);
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
                    destinationJndiName, new InitialContext(), null, null);
        } catch (NamingException ne) {
            exception = new DocumentMessageProducerException(ne);
        }

        if (exception != null) {
            throw exception;
        }
    }    
    private static void sendNXCoreEventMessages(List<NXCoreEvent> messages,
            String connectionFactoryJndiName, String destinationJndiName,
            Context ctx) throws DocumentMessageProducerException {

        MessageProducer messageProducer = null;
        Connection connection = null;
        Session session = null;

        try {
            TopicConnectionFactory connectionFactory = getJmsConnectionFactory(
                    connectionFactoryJndiName, ctx);
            connection = connectionFactory.createTopicConnection();
            session = connection.createSession(transacted, Session.AUTO_ACKNOWLEDGE);
            Destination destination = getJmsDestination(destinationJndiName, ctx);
            messageProducer = session.createProducer(destination);
            messageProducer.setDeliveryMode(isDeliveryPersistent ? DeliveryMode.PERSISTENT : DeliveryMode.NON_PERSISTENT);
            messageProducer.setDisableMessageID(isDisableMessageID);
            messageProducer.setDisableMessageTimestamp(isDisableMessageTimestamp);
            for (NXCoreEvent message : messages) {
                ObjectMessage objectMessage = session.createObjectMessage(message);
                objectMessage.setStringProperty(JMSConstant.NUXEO_MESSAGE_TYPE, JMSConstant.NXCORE_EVENT);
                messageProducer.send(objectMessage);
            }

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
    private static void sendEventMessages(List<EventMessage> messages,
            String connectionFactoryJndiName, String destinationJndiName,
            Context ctx) throws DocumentMessageProducerException {

        MessageProducer messageProducer = null;
        Connection connection = null;
        Session session = null;

        try {
            TopicConnectionFactory connectionFactory = getJmsConnectionFactory(
                    connectionFactoryJndiName, ctx);
            connection = connectionFactory.createTopicConnection();
            session = connection.createSession(transacted, Session.AUTO_ACKNOWLEDGE);
            Destination destination = getJmsDestination(destinationJndiName, ctx);
            messageProducer = session.createProducer(destination);
            messageProducer.setDeliveryMode(isDeliveryPersistent ? DeliveryMode.PERSISTENT : DeliveryMode.NON_PERSISTENT);
            messageProducer.setDisableMessageID(isDisableMessageID);
            messageProducer.setDisableMessageTimestamp(isDisableMessageTimestamp);
            for (EventMessage message : messages) {
                ObjectMessage objectMessage = session.createObjectMessage(message);
                objectMessage.setStringProperty(JMSConstant.NUXEO_EVENT_ID, message.getEventId());
                objectMessage.setStringProperty(JMSConstant.NUXEO_MESSAGE_TYPE, JMSConstant.EVENT_MESSAGE);
                messageProducer.send(objectMessage);
            }

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
    private static void sendDocumentMessages(List<DocumentMessage> messages,
            String connectionFactoryJndiName, String destinationJndiName,
            Context ctx) throws DocumentMessageProducerException {

        MessageProducer messageProducer = null;
        Connection connection = null;
        Session session = null;

        try {
            TopicConnectionFactory connectionFactory = getJmsConnectionFactory(
                    connectionFactoryJndiName, ctx);
            connection = connectionFactory.createTopicConnection();
            session = connection.createSession(transacted, Session.AUTO_ACKNOWLEDGE);
            Destination destination = getJmsDestination(destinationJndiName, ctx);
            messageProducer = session.createProducer(destination);
            messageProducer.setDeliveryMode(isDeliveryPersistent ? DeliveryMode.PERSISTENT : DeliveryMode.NON_PERSISTENT);
            messageProducer.setDisableMessageID(isDisableMessageID);
            messageProducer.setDisableMessageTimestamp(isDisableMessageTimestamp);
            for (DocumentMessage message : messages) {
                ObjectMessage objectMessage = session.createObjectMessage(message);
                objectMessage.setStringProperty(JMSConstant.NUXEO_EVENT_ID, message.getEventId());
                objectMessage.setStringProperty(JMSConstant.NUXEO_MESSAGE_TYPE, JMSConstant.DOCUMENT_MESSAGE);
                messageProducer.send(objectMessage);
            }

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
    private static void sendMessages(List<Serializable> messages,
            String connectionFactoryJndiName, String destinationJndiName,
            Context ctx) throws DocumentMessageProducerException {

        MessageProducer messageProducer = null;
        Connection connection = null;
        Session session = null;

        try {
            TopicConnectionFactory connectionFactory = getJmsConnectionFactory(
                    connectionFactoryJndiName, ctx);
            connection = connectionFactory.createTopicConnection();
            session = connection.createSession(transacted, Session.AUTO_ACKNOWLEDGE);
            Destination destination = getJmsDestination(destinationJndiName, ctx);
            messageProducer = session.createProducer(destination);
            messageProducer.setDeliveryMode(isDeliveryPersistent ? DeliveryMode.PERSISTENT : DeliveryMode.NON_PERSISTENT);
            messageProducer.setDisableMessageID(isDisableMessageID);
            messageProducer.setDisableMessageTimestamp(isDisableMessageTimestamp);
            for (Serializable message : messages) {
                ObjectMessage objectMessage = session.createObjectMessage(message);
                messageProducer.send(objectMessage);
            }

        } catch (JMSException je) {
            log.error(
                    "An error occured while trying to produce a JMS object message",
                    je);
        } finally {
            try {
                if (messageProducer != null)
                    messageProducer.close();
                if (session != null)
                    session.close();
                if (connection != null)
                    connection.close();
            } catch (JMSException je) {
                log.error("An error during JMS cleanup", je);
            }
        }

    }

    private static void sendMessage(Serializable message,
            String connectionFactoryJndiName, String destinationJndiName,
            Context ctx, String eventId, String messageType) throws DocumentMessageProducerException {


        try {
            TopicConnectionFactory connectionFactory = getJmsConnectionFactory(
                    connectionFactoryJndiName, ctx);
            Connection connection = connectionFactory.createConnection();
            Session session = connection.createSession(transacted, Session.AUTO_ACKNOWLEDGE);
            Destination destination = getJmsDestination(destinationJndiName, ctx);
            MessageProducer messageProducer = session.createProducer(destination);
            messageProducer.setDeliveryMode(isDeliveryPersistent ? DeliveryMode.PERSISTENT : DeliveryMode.NON_PERSISTENT);
            messageProducer.setDisableMessageID(isDisableMessageID);
            messageProducer.setDisableMessageTimestamp(isDisableMessageTimestamp);
            ObjectMessage objectMessage = session.createObjectMessage(message);
            if(eventId != null) {
                objectMessage.setStringProperty(JMSConstant.NUXEO_EVENT_ID, eventId);
            }
            if(messageType != null) {
                objectMessage.setStringProperty(JMSConstant.NUXEO_MESSAGE_TYPE, messageType);
            }
            messageProducer.send(objectMessage);
            messageProducer.close();
            session.close();
            connection.close();
        } catch (JMSException je) {
            log.error(
                    "An error occured while trying to produce a JMS object message",
                    je);
        }
    }

}
