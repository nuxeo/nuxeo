/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.core.jms;

import java.io.Serializable;
import java.util.Properties;

import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class CoreEventPublisher {

    public static final String XA_TOPIC_CONNECTION_FACTORY = "JmsNX";
    public static final String CORE_EVENTS_TOPIC = "topic/NXCoreEvents";

    private static final Log log = LogFactory.getLog(CoreEventPublisher.class);

    private boolean transacted;
    private boolean isDeliveryPersistent;
    private boolean isDisableMessageID;
    private boolean isDisableMessageTimestamp;
    private TopicConnectionFactory topicConnectionFactory;
    private Topic coreEventsTopic;

    private static final CoreEventPublisher instance = new CoreEventPublisher();

    private CoreEventPublisher() {
        configureJMS();
    }

    private void configureJMS() {
        Properties runtime = Framework.getRuntime().getProperties();
        transacted = Boolean.valueOf(runtime.getProperty("jms.useTransactedConnection"));
        isDeliveryPersistent = Boolean.valueOf(runtime.getProperty("jms.isDeliveryPersistent"));
        isDisableMessageID = Boolean.valueOf(runtime.getProperty("jms.isDisableMessageID"));
        isDisableMessageTimestamp = Boolean.valueOf(runtime.getProperty("jms.isDisableMessageTimestamp"));
    }

    public static CoreEventPublisher getInstance() {
        return instance;
    }

    public void reset() {
        topicConnectionFactory = null;
        coreEventsTopic = null;
    }

    private TopicConnectionFactory getTopicConnectionFactory()
            throws NamingException {
        if (topicConnectionFactory == null) {
            Context jndi = new InitialContext();
            topicConnectionFactory = (TopicConnectionFactory) jndi.lookup(XA_TOPIC_CONNECTION_FACTORY);
            if (coreEventsTopic == null) { // initialize the default topic too
                coreEventsTopic = (Topic) jndi.lookup(CORE_EVENTS_TOPIC);
            }
        }
        return topicConnectionFactory;
    }

    private Topic getDefaultTopic() throws NamingException {
        if (coreEventsTopic == null) {
            Context jndi = new InitialContext();
            coreEventsTopic = (Topic) jndi.lookup(CORE_EVENTS_TOPIC);
            if (topicConnectionFactory == null) { // initialize the connection factory too
                topicConnectionFactory = (TopicConnectionFactory) jndi.lookup(XA_TOPIC_CONNECTION_FACTORY);
            }
        }
        return coreEventsTopic;
    }

    /**
     * Retrieves a new JMS Connection from the pool.
     *
     * @return a <code>QueueConnection</code>
     * @throws JMSException if the connection could not be retrieved
     */
    private TopicConnection getTopicConnection() throws JMSException {
        try {
            return getTopicConnectionFactory().createTopicConnection();
        } catch (NamingException e) {
            log.error("Failed too lookup topic connection factory", e);
            throw new JMSException("Failed to lookup topic connection factory: "+e.getMessage());
        }
    }

    public void publish(Serializable content, String eventId) throws JMSException {
        try {
            publish(content, getDefaultTopic(), MessageFactory.DEFAULT, eventId);
        } catch (NamingException e) {
            log.error("Failed to lookup default topic", e);
            throw new JMSException("Failed to lookup default topic");
        }
    }

    public void publish(Topic topic, Serializable content, String eventId) throws JMSException {
        publish(content, topic, MessageFactory.DEFAULT, eventId);
    }

    public void publish(Object content, Topic topic, MessageFactory factory, String eventId)
            throws JMSException {
        TopicConnection connection = null;
        TopicSession session = null;
        TopicPublisher publisher = null;
        try {
            // get a connection from topic connection pool
            connection = getTopicConnection();

            // create a not transacted session
            session = connection.createTopicSession(transacted, TopicSession.AUTO_ACKNOWLEDGE);

            // create the publisher
            publisher = session.createPublisher(topic);
            publisher.setDeliveryMode(
                    isDeliveryPersistent ? DeliveryMode.PERSISTENT : DeliveryMode.NON_PERSISTENT);
            publisher.setDisableMessageID(isDisableMessageID);
            publisher.setDisableMessageTimestamp(isDisableMessageTimestamp);
            // create the message using the given factory
            Message msg = factory.createMessage(session, content);
            if(eventId != null) {
                msg.setStringProperty("NuxeoEventId", eventId);
            }
            // publish the message
            publisher.publish(topic, msg);
        } finally {
            if (publisher != null) {
                publisher.close();
            }
            if (session != null) {
                session.close();
            }
            if (connection != null) {
                connection.close();
            }
        }
    }

    public MessagePublisher createPublisher() throws NamingException {
        return new MessagePublisher(getDefaultTopic(), getTopicConnectionFactory());
    }

}
