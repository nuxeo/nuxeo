/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.routing.test;

import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;

/**
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 */
public class CounterListener implements EventListener {
    protected static int counter = 0;

    @Override
    public void handleEvent(Event event) {
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
