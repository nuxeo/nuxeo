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
 * $Id$
 */

package org.nuxeo.ecm.platform.events.facade.ejb;

import java.util.List;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.events.api.DocumentMessage;
import org.nuxeo.ecm.platform.events.api.DocumentMessageProducer;
import org.nuxeo.ecm.platform.events.api.EventMessage;
import org.nuxeo.ecm.platform.events.api.NXCoreEvent;
import org.nuxeo.ecm.platform.events.api.delegate.DocumentMessageProducerBusinessDelegate;

/**
 * Session facade for Document Message Producer service.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
@Stateless
@Local(DocumentMessageProducer.class)
@Remote(DocumentMessageProducer.class)
public class JMSDocumentMessageProducerBean implements DocumentMessageProducer {

    private static final Log log = LogFactory.getLog(JMSDocumentMessageProducerBean.class);

    private DocumentMessageProducer service;

    protected DocumentMessageProducer getService() {
        if (service == null) {
            service = DocumentMessageProducerBusinessDelegate.getLocalDocumentMessageProducer();
        }
        return service;
    }

    /**
     * @deprecated Use {@link #produce(EventMessage)} instead
     */
    @Deprecated
    public void produce(DocumentMessage message) {
        if (getService() != null) {
            getService().produce(message);
        } else {
            log.error("Impossible to lookup DocumentMessageProducer service."
                    + "Cannot forward messages on JMS topic.");
        }
    }

    public void produce(EventMessage message) {
        if (getService() != null) {
            getService().produce(message);
        } else {
            log.error("Impossible to lookup DocumentMessageProducer service."
                    + "Cannot forward messages on JMS topic.");
        }
    }

    public void produce(NXCoreEvent event) {
        if (getService() != null) {
            getService().produce(event);
        } else {
            log.error("Impossible to lookup DocumentMessageProducer service."
                    + "Cannot forward messages on JMS topic.");
        }
    }

    public void produceCoreEvents(List<NXCoreEvent> events) {
        if (getService() != null) {
            getService().produceCoreEvents(events);
        } else {
            log.error("Impossible to lookup DocumentMessageProducer service."
                    + "Cannot forward messages on JMS topic.");
        }
    }

    public void produceEventMessages(List<EventMessage> messages) {
        if (getService() != null) {
            getService().produceEventMessages(messages);
        } else {
            log.error("Impossible to lookup DocumentMessageProducer service."
                    + "Cannot forward messages on JMS topic.");
        }
    }

}
