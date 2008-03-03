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
 * $Id:JMSDocumentMessageProducerService.java 3386 2006-09-29 13:32:49Z janguenot $
 */

package org.nuxeo.ecm.platform.events.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.events.api.DocumentMessage;
import org.nuxeo.ecm.platform.events.api.DocumentMessageProducer;
import org.nuxeo.ecm.platform.events.api.DocumentMessageProducerException;
import org.nuxeo.ecm.platform.events.api.EventMessage;
import org.nuxeo.ecm.platform.events.api.NXCoreEvent;
import org.nuxeo.ecm.platform.events.jms.JMSDocumentMessageProducer;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Service that produces JMS Message Document.
 *
 * @author : <a href="ja@nuxeo.com">Julien Anguenot</a>
 */
public class JMSDocumentMessageProducerService extends DefaultComponent
        implements DocumentMessageProducer {

    public static final ComponentName NAME = new ComponentName(
            "org.nuxeo.ecm.platform.events.service.JMSDocumentMessageProducer");

    private static final String DESTINATION_JNDI_NAME = "topic/NXPMessages";

    private static final String CORE_EVENTS_DESTINATION = "topic/NXCoreEvents";

    private static final Log log = LogFactory.getLog(JMSDocumentMessageProducerService.class);

    // :TODO: Ensure XA
    private static final String XA_TOPIC_CONNECTION_FACTORY = "JmsNX";

    // private static final String XA_TOPIC_CONNECTION_FACTORY =
    // "TopicConnectionFactory";

    /**
     * @deprecated Use {@link #produce(EventMessage)} instead TODO: remove in
     *             5.2
     */
    @Deprecated
    public void produce(DocumentMessage message) {
        if (message == null) {
            log.debug("Incoming message is null. Cancelling...");
        }
        log.debug("Producing message..............................");
        try {
            JMSDocumentMessageProducer.sendMessage(message,
                    XA_TOPIC_CONNECTION_FACTORY, DESTINATION_JNDI_NAME);
            JMSDocumentMessageProducer.sendMessage(message.getId(),
                    XA_TOPIC_CONNECTION_FACTORY, CORE_EVENTS_DESTINATION);
            log.debug("produce() done ! for eventId=" + message.getEventId());
        } catch (DocumentMessageProducerException e) {
            log.error("En error occured while trying to send a JMS message on"
                    + "on the " + DESTINATION_JNDI_NAME + " topic destination");
            e.printStackTrace();
        }
    }

    public void produceEventMessages(List<EventMessage> messages) {
        if ((messages == null) || (messages.size() == 0)) {
            log.debug("Incoming messages list is null. Cancelling...");
        }

        List<Serializable> sMessages = new ArrayList<Serializable>();
        sMessages.addAll(messages);

        log.debug("Producing message..............................");
        try {
            JMSDocumentMessageProducer.sendMessages(sMessages,
                    XA_TOPIC_CONNECTION_FACTORY, DESTINATION_JNDI_NAME);

        } catch (DocumentMessageProducerException e) {
            log.error("En error occured while trying to send a JMS message on"
                    + "on the " + DESTINATION_JNDI_NAME + " topic destination");
            e.printStackTrace();
        }
    }

    public void produce(EventMessage message) {
        if (message == null) {
            log.debug("Incoming message is null. Cancelling...");
        }
        List<EventMessage> messages = new ArrayList<EventMessage>(1);
        messages.add(message);
        produceEventMessages(messages);
    }


    public void produceCoreEvents(List<NXCoreEvent> events) {
        if (events == null) {
            log.debug("Incoming message List is null. Cancelling...");
        }

        List<Serializable> sMessages = new ArrayList<Serializable>();
        sMessages.addAll(events);

        log.debug("Producing NXCoreEvent message..............................");
        try {
            JMSDocumentMessageProducer.sendMessages(sMessages,
                    XA_TOPIC_CONNECTION_FACTORY, CORE_EVENTS_DESTINATION);
            log.debug("produce() done !");
        } catch (DocumentMessageProducerException e) {
            log.error("En error occured while trying to send a JMS message on"
                    + "on the " + CORE_EVENTS_DESTINATION
                    + " topic destination");
            e.printStackTrace();
        }

    }

    public void produce(NXCoreEvent event) {
        if (event == null) {
            log.debug("Incoming message is null. Cancelling...");
        }
        List<NXCoreEvent> events = new ArrayList<NXCoreEvent>(1);
        events.add(event);

        produceCoreEvents(events);
    }

}
