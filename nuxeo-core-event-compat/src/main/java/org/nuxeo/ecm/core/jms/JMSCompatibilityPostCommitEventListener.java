/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 */
package org.nuxeo.ecm.core.jms;

import java.io.Serializable;
import java.security.Principal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jms.JMSException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.api.event.impl.CoreEventImpl;
import org.nuxeo.ecm.core.api.operation.DefaultOperationEvent;
import org.nuxeo.ecm.core.api.operation.Modification;
import org.nuxeo.ecm.core.api.operation.Operation;
import org.nuxeo.ecm.core.api.operation.OperationEvent;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.core.listener.OperationEventFactory;

/**
 * Replacement of the org.nuxeo.ecm.core.event.compat.CompatibilityListener
 * listener which is not postcommit. Without post commit, apogee randomly get
 * events for not yet commited data.
 *
 * @author Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 *
 */
public class JMSCompatibilityPostCommitEventListener implements
        PostCommitEventListener {

    private static final Log log = LogFactory.getLog(JMSCompatibilityPostCommitEventListener.class);

    @Override
    public void handleEvent(EventBundle events) throws ClientException {
        int inOperation = 0;
        Set<DocumentRef> checkedRefs = new HashSet<DocumentRef>();
        for (Event event : events) {
            inOperation = inOperation(inOperation, event);
            /*
             * If we are already in an operation, discard all core events except
             * for operation end events. We need to use the modifications inside
             * these end events.
             */
            if (inOperation > 0 && "!OPERATION_END!".equals(event.getName())) {
                continue;
            }
            if (isChecked(checkedRefs, event)) {
                continue;
            }
            forwardEvent(event);
        }
    }


    protected int inOperation(int inOperation, Event event) {
        String name = event.getName();
        if ("!OPERATION_START!".equals(name)) {
            inOperation++;
        } else if ("!OPERATION_END!".equals(name)) {
            inOperation--;
        }
        return inOperation;
    }


    protected boolean isChecked(Set<DocumentRef> refs, Event event) {
        String id = event.getName();
        if (DocumentEventTypes.DOCUMENT_CREATED.equals(id)
                || DocumentEventTypes.DOCUMENT_CREATED_BY_COPY.equals(id)
                || DocumentEventTypes.DOCUMENT_UPDATED.equals(id)) {
            // stacked events are doc centric events
            DocumentModel doc = (DocumentModel)event.getContext().getArguments()[0];
            DocumentRef ref = doc.getRef();
            if (refs.contains(ref)) {
                return true;
            }
            refs.add(ref);
        }

        return false;
    }

    protected void forwardEvent(Event event) {

        String name = event.getName();
        EventContext context = event.getContext();
        Object[] args = context.getArguments();
        Principal principal = context.getPrincipal();
        Map<String, Serializable> infos = context.getProperties();

        CoreSession session = context.getCoreSession();
        OperationEvent operationEvent = null;
        if ("!OPERATION_START!".equals(name)) {
            Operation<?> operation = (Operation<?>)context.getArguments()[0];
            operationEvent = new DefaultOperationEvent(session, operation.getName(), operation.getModifications(), null);
        } else {

            if (args.length < 1 || !(args[0] instanceof DocumentModel)) {
                return;
            }

            DocumentModel source = (DocumentModel) args[0];

            CoreEventImpl coreEvent = new CoreEventImpl(event.getName(), source, infos, principal, null, null);
            operationEvent = OperationEventFactory.createEvent(session, coreEvent);
        }

        if (operationEvent == null) {
            return;
        }

        try {
            CoreEventPublisher.getInstance().publish(operationEvent, name);
            String operationId = operationEvent.getId();
            log.info("Forwarded " + operationId);
            if (log.isTraceEnabled()) {
                for (Modification modif:operationEvent.getModifications()) {
                    log.trace("Forwarded " + operationId + " : " + modif);
                }
            }
        } catch (JMSException e) {
            log.error("Cannot forward " + event.getName());
        }

    }

}
