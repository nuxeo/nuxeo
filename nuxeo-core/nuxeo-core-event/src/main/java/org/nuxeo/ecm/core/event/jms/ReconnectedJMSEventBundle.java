/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * Default implementation for an {@link EventBundle} that need to be reconnected
 * to a usable Session
 *
 * @author tiry
 *
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
