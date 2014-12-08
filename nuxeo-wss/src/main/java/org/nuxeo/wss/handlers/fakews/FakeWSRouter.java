/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 */

package org.nuxeo.wss.handlers.fakews;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.wss.CallRouter;
import org.nuxeo.wss.servlet.WSSResponse;
import org.nuxeo.wss.servlet.config.FilterBindingConfig;

public class FakeWSRouter extends CallRouter {

    private static final Log log = LogFactory.getLog(FakeWSRouter.class);

    public static void handleFakeWSRequest(FakeWSRequest wsRequest, WSSResponse wsResponse, FilterBindingConfig config)
            throws Exception {
        FakeWSHandler handler = getHandler(FakeWSHandler.class, config.getTargetService());
        if (handler == null) {
            log.error("Can not find handler for service " + config.getTargetService());
            throw new Exception("No handler found for " + config.getTargetService());
        }
        log.debug("Handle Fake WS request with handler" + handler.getClass().getSimpleName());
        handler.handleRequest(wsRequest, wsResponse);
    }

}
