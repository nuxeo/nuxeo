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

package org.nuxeo.ecm.core.api;

import org.nuxeo.ecm.core.api.operation.Operation;
import org.nuxeo.ecm.core.api.operation.ProgressMonitor;
import org.nuxeo.ecm.core.listener.CoreEventListenerService;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public final class DefaultProgressMonitor implements ProgressMonitor {

    public final static DefaultProgressMonitor INSTANCE = new DefaultProgressMonitor();

    private CoreEventListenerService eventService;

    private DefaultProgressMonitor() {}

    /**
     * @return the eventService.
     */
    public CoreEventListenerService getEventService() {
        if (eventService == null) {
            eventService = Framework.getLocalService(CoreEventListenerService.class);
        }
        return eventService;
    }

    public void started(Operation<?> cmd) {
        //TODO: commented out temporarily
        //getEventService().fireCommandStarted(cmd);
    }

    public void terminated(Operation<?> cmd) {
        // TODO: commented out temporarily
        //getEventService().fireCommandTerminated(cmd);
    }

    public void done(Operation<?> cmd, int percent) {
        // do nothing
    }

}
