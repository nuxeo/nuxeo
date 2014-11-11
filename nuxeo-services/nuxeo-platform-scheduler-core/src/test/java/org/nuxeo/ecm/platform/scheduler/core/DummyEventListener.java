/*
 * (C) Copyright 2007-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.scheduler.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;

public class DummyEventListener implements EventListener {

    private static final Log log = LogFactory.getLog(EventListener.class);

    @Override
    public void handleEvent(Event event) throws ClientException {
        if (event.getName().equals("testEvent")) {
            log.info("Received event!");
            // note we were called
            String flag = (String) event.getContext().getProperty("flag");
            if ("1".equals(flag)) {
                Whiteboard.getWhiteboard().decreaseCount();
            } else {
                Whiteboard.getWhiteboard().incrementCount();
            }
        }
    }

}
