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

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * This object is not thread safe. Should be used only in a single thread
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class MessagePublisher {

    protected Topic topic;
    protected TopicConnectionFactory factory;
    private TopicConnection connection;
    private TopicSession session;
    private TopicPublisher publisher;
    protected MessageFactory messageFactory = MessageFactory.DEFAULT;


    public MessagePublisher(Topic topic, TopicConnectionFactory factory) {
        this.factory = factory;
        this.topic = topic;
    }

    public MessagePublisher(Topic topic, TopicConnectionFactory factory, MessageFactory messageFactory) {
        this.factory = factory;
        this.messageFactory = messageFactory;
    }

    /**
     * @return the topic.
     */
    public Topic getTopic() {
        return topic;
    }

    /**
     * @param topic the topic to set.
     */
    public void setTopic(Topic topic) {
        this.topic = topic;
    }

    public void setMessageFactory(MessageFactory factory) {
        if (factory == null) {
            this.messageFactory = MessageFactory.DEFAULT;
        } else {
            this.messageFactory = factory;
        }
    }

    /**
     * @return the factory.
     */
    public MessageFactory getMessageFactory() {
        return messageFactory;
    }

    /**
     * @return the connection.
     */
    public TopicConnection getConnection() throws JMSException {
        if (connection == null) {
            connection = factory.createTopicConnection();
        }
        return connection;
    }

    /**
     * @return the session.
     */
    public TopicSession getSession() throws JMSException {
        if (session == null) {
            session = getConnection().createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
        }
        return session;
    }

    /**
     * @return the publisher.
     */
    public TopicPublisher getPublisher() throws JMSException {
        if (publisher == null) {
            publisher = getSession().createPublisher(topic);
        }
        return publisher;
    }


    public void publish(Object content) throws JMSException {
        Message msg = messageFactory.createMessage(session, content);
        getPublisher().publish(topic, msg);
    }

    public void publish(Message msg) throws JMSException {
        getPublisher().publish(topic, msg);
    }

    public void close() throws JMSException {
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


    public final static MessagePublisher createPublisher(String connectionFactory, String topic) throws NamingException, JMSException {
        return createPublisher(connectionFactory, topic, null);
    }

    public final static MessagePublisher createPublisher(String connectionFactoryName, String topicName, MessageFactory messageFactory) throws NamingException, JMSException {
        Context jndi = new InitialContext();
        TopicConnectionFactory factory = (TopicConnectionFactory) jndi.lookup(connectionFactoryName);
        Topic topic = (Topic) jndi.lookup(topicName);
        return new MessagePublisher(topic, factory, messageFactory);
    }

}
