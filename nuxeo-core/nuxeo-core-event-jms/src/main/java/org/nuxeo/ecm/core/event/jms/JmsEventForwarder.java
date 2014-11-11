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
 */
package org.nuxeo.ecm.core.event.jms;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.core.event.ReconnectedEventBundle;

/**
 * Forwards Core EventBundles to JMS topics.
 *
 * @author Tiry
 */
public class JmsEventForwarder implements PostCommitEventListener {

    public static final String NUXEO_JMS_TOPIC = "topic/NuxeoMessages";

    private static final Log log = LogFactory.getLog(JmsEventForwarder.class);

    protected boolean jmsBusIsActive = true;

    protected void produceJMSMessage(SerializableEventBundle message) throws JMSBusNotActiveException {
        InitialContext ctx;
        Topic nuxeoTopic;
        try {
            ctx = new InitialContext();
            nuxeoTopic = (Topic) ctx.lookup(NUXEO_JMS_TOPIC);
        }
        catch (NamingException e) {
            jmsBusIsActive = false;
            throw new JMSBusNotActiveException(e);
        }

        TopicConnection nuxeoTopicConnection = null;
        TopicSession nuxeoTopicSession = null;
        TopicPublisher nuxeoMessagePublisher = null;
        try {
            TopicConnectionFactory factory = (TopicConnectionFactory) ctx.lookup("TopicConnectionFactory");
            nuxeoTopicConnection = factory.createTopicConnection();
            nuxeoTopicSession = nuxeoTopicConnection.createTopicSession(false,
                    TopicSession.AUTO_ACKNOWLEDGE);

            ObjectMessage jmsMessage = nuxeoTopicSession.createObjectMessage(message);

            // add Headers for JMS message
            jmsMessage.setStringProperty("BundleEvent", message.getEventBundleName());

            nuxeoMessagePublisher = nuxeoTopicSession.createPublisher(nuxeoTopic);

            nuxeoMessagePublisher.send(jmsMessage);
            log.debug("Event bundle " + message.getEventBundleName() + " forwarded to JMS topic");

        } catch (Exception e) {
            log.error("Error during JMS forwarding", e);
        } finally {
            if (nuxeoTopicSession != null) {
                try {
                    if (nuxeoMessagePublisher != null) {
                        nuxeoMessagePublisher.close();
                    }
                    nuxeoTopicConnection.close();
                    nuxeoTopicSession.close();
                } catch (JMSException e) {
                    log.error("Error during JMS cleanup", e);
                }
            }
        }
    }

    public void handleEvent(EventBundle events) throws ClientException {
        if (!canForwardMessage(events)) {
            return;
        }
        try {
            produceJMSMessage(new SerializableEventBundle(events));
        }
        catch (JMSBusNotActiveException e) {
            log.debug("JMS Bus is not active, cannot forward message");
        }
    }

    protected boolean canForwardMessage(EventBundle events) {
        // Check Bus is Active
        if (!jmsBusIsActive) {
            log.debug("JMS Bus is not active, cannot forward message");
            return false;
        }
        if (events instanceof ReconnectedEventBundle) {
            if (((ReconnectedEventBundle) events).comesFromJMS()) {
                log.debug("Message already comes from JMS bus, not forwarding");
                return false;
            }
        }
        return true;
    }

}
