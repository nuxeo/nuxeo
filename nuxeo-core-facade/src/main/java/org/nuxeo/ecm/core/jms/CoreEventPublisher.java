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


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class CoreEventPublisher {

    private static final Log log = LogFactory.getLog(CoreEventPublisher.class);

    public final static String XA_TOPIC_CONNECTION_FACTORY = "JmsNX";
    public final static String TOPIC_CONNECTION_FACTORY = "JmsNX";
    public final static String CORE_EVENTS_TOPIC = "topic/NXCoreEvents";

    private boolean isXa = false; //TODO
    private TopicConnectionFactory topicConnectionFactory;
    private Topic coreEventsTopic;

    private static CoreEventPublisher instance = new CoreEventPublisher();
    public static CoreEventPublisher getInstance() {
        return instance;
    }

    public void reset() {
        topicConnectionFactory = null;
        coreEventsTopic = null;
    }

    private final TopicConnectionFactory getTopicConnectionFactory()
            throws NamingException {
        if (topicConnectionFactory == null) {
            Context jndi = new InitialContext();
            topicConnectionFactory = (TopicConnectionFactory) jndi.lookup(isXa
                    ? XA_TOPIC_CONNECTION_FACTORY : TOPIC_CONNECTION_FACTORY);
            if (coreEventsTopic == null) { // initialize the default topic too
                coreEventsTopic = (Topic) jndi.lookup(CORE_EVENTS_TOPIC);
            }
        }
        return topicConnectionFactory;
    }

    private final Topic getDefaultTopic() throws NamingException {
        if (coreEventsTopic == null) {
            Context jndi = new InitialContext();
            coreEventsTopic = (Topic) jndi.lookup(CORE_EVENTS_TOPIC);
            if (topicConnectionFactory == null) { // initialize the connection factory too
                topicConnectionFactory = (TopicConnectionFactory) jndi.lookup(isXa
                        ? XA_TOPIC_CONNECTION_FACTORY : TOPIC_CONNECTION_FACTORY);
            }
        }
        return coreEventsTopic;
    }


    /**
     * Retrieves a new JMS Connection from the pool
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

    public void publish(Serializable content) throws JMSException {
        try {
            publish(content, getDefaultTopic(), MessageFactory.DEFAULT);
        } catch (NamingException e) {
            log.error("Failed to lookup default topic", e);
            throw new JMSException("Failed to lookup default topic");
        }
    }

    public void publish(Topic topic, Serializable content) throws JMSException {
        publish(content, topic, MessageFactory.DEFAULT);
    }

    public void publish(Object content, Topic topic, MessageFactory factory)
    throws JMSException {
        TopicConnection connection = null;
        TopicSession session = null;
        TopicPublisher publisher = null;
        try {
            // get a connection from topic connection pool
            connection = getTopicConnection();
            // create a not transacted session
            session = connection.createTopicSession(false, TopicSession.AUTO_ACKNOWLEDGE);
            // create the publisher
            publisher = session.createPublisher(topic);
            // create the message using the given factory
            Message msg = factory.createMessage(session, content);
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

    public MessagePublisher createPublisher() throws NamingException, JMSException {
        return new MessagePublisher(getDefaultTopic(), getTopicConnectionFactory());
    }

}
