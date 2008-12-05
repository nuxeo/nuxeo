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
 * $Id: NXRuntimeEventListener.java 30799 2008-03-01 12:36:18Z bstefanescu $
 */

package org.nuxeo.ecm.core.listener.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.core.listener.AbstractEventListener;
import org.nuxeo.ecm.core.listener.AsynchronousEventListener;
import org.nuxeo.ecm.core.listener.DocumentModelEventListener;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.event.Event;
import org.nuxeo.runtime.services.event.EventService;

/**
 * NXRuntime event Listener.
 * <p>
 * This is a bridge from core events to NXRuntime event service.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class NXRuntimeEventListener extends AbstractEventListener implements
        AsynchronousEventListener, DocumentModelEventListener {

    private static final Log log = LogFactory.getLog(NXRuntimeEventListener.class);

    @Override
    public void handleEvent(CoreEvent coreEvent) {
        log.debug("notifyEvent");
        EventService service = (EventService) Framework.getRuntime().getComponent(
                EventService.NAME);
        service.sendEvent(makeRuntimeEventFromCoreEvent(coreEvent));
    }

    private static Event makeRuntimeEventFromCoreEvent(CoreEvent coreEvent) {
        return new Event("NXCoreEvents", coreEvent.getEventId(),
                coreEvent.getSource(), coreEvent.getInfo());
    }

}
