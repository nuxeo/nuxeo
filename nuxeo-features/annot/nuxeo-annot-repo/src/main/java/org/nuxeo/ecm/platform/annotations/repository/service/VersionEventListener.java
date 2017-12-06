/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     troger
 */
package org.nuxeo.ecm.platform.annotations.repository.service;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class VersionEventListener implements PostCommitEventListener {

    private static final Log log = LogFactory.getLog(VersionEventListener.class);

    public VersionEventListener() {
    }

    public void handleEvent(EventBundle events) {
        AnnotationsRepositoryConfigurationService service = Framework.getService(AnnotationsRepositoryConfigurationService.class);
        GraphManagerEventListener manager = service.getGraphManagerEventListener();
        List<String> eventNames = service.getEventIds();

        boolean processEvents = false;
        for (String eventName : eventNames) {
            if (events.containsEventName(eventName)) {
                processEvents = true;
                break;
            }
        }

        if (processEvents) {
            for (Event event : events) {
                if (eventNames.contains(event.getName())) {
                    log.debug("Handling " + event.getName() + " event");
                    manager.manage(event);
                }
            }
        }
    }

}
