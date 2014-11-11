/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: $
 */
package org.nuxeo.ecm.platform.scheduler.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.platform.api.ECM;
import org.nuxeo.ecm.platform.api.Platform;

// import org.nuxeo.ecm.platform.api.RepositoryDescriptor;

public class TestEventListener implements EventListener
{

    private static final Log log = LogFactory.getLog(EventListener.class);

    /**
     * Example method to connect to the session and get the root.
     */
    @SuppressWarnings("unused")
    private void doSomething(CoreEvent event) {
        Platform platform = ECM.getPlatform();
        try {
            // In case there are several repositories, you can use:
            // RepositoryDescriptor rd = platform.getRepositories()[0];
            // CoreSession session = platform.openRepository(rd.name);
            log.debug("Opening repository");
            CoreSession session = platform.openRepository("demo");
            log.debug("Getting root");
            DocumentModel root = session.getRootDocument();
            log.info("Root uuid: " + root.getId());
            // ... do something from the root
            CoreInstance.getInstance().close(session);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void handleEvent(Event event) throws ClientException {
         if (event.getName().equals("testEvent")) {
                log.info("Received event!");
                // note we were called
                Whiteboard.getWhiteboard().incrementCount();
                // doSomething(event);
            }
    }

}
