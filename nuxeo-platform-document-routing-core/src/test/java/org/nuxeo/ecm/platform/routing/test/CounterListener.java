/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.ecm.platform.routing.test;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;

/**
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 *
 */
public class CounterListener implements EventListener{
    protected static int counter = 0;

    public void handleEvent(Event event) throws ClientException {
        DocumentEventContext docEventContext = null;
        if (!(event.getContext() instanceof DocumentEventContext)) {
            return;

        }
        docEventContext = (DocumentEventContext) event.getContext();
        String category = docEventContext.getCategory();
        if (DocumentRoutingConstants.ROUTING_CATEGORY.equals(category)) {
            counter++;
        }
    }

    public static int getCounter() {
        return counter;
    }

    public static void resetCouner() {
        counter = 0;
    }
}
