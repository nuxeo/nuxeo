/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.event.jms;

import java.rmi.dgc.VMID;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.impl.EventServiceImpl;
import org.nuxeo.ecm.core.event.impl.ReconnectedEventBundleImpl;

/**
 * Default implementation for an {@link EventBundle} that needs to be reconnected
 * to a usable Session.
 *
 * @author tiry
 */
public class ReconnectedJMSEventBundle extends ReconnectedEventBundleImpl {

    private static final Log log = LogFactory.getLog(ReconnectedJMSEventBundle.class);

    private static final long serialVersionUID = 1L;

    protected final SerializableEventBundle jmsEventBundle;

    public ReconnectedJMSEventBundle(SerializableEventBundle jmsEventBundle) {
        this.jmsEventBundle = jmsEventBundle;
    }

    @Override
    protected List<Event> getReconnectedEvents() {
        if (sourceEventBundle == null) {
            try {
                sourceEventBundle = jmsEventBundle.reconstructEventBundle(getReconnectedCoreSession(jmsEventBundle.getCoreInstanceName()));
            } catch (SerializableEventBundle.CannotReconstruct e) {
                log.error("Error while reconstructing Bundle from JMS", e);
                return null;
            }
        }
        return super.getReconnectedEvents();
    }

    @Override
    public String getName() {
        return jmsEventBundle.getEventBundleName();
    }

    @Override
    public VMID getSourceVMID() {
        return jmsEventBundle.getSourceVMID();
    }

    @Override
    public boolean hasRemoteSource() {
        return !getSourceVMID().equals(EventServiceImpl.VMID);
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean comesFromJMS() {
        return true;
    }

}
