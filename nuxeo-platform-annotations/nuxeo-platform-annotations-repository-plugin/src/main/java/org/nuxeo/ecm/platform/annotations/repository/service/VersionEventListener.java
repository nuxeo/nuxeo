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
 *     troger
 */
package org.nuxeo.ecm.platform.annotations.repository.service;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.runtime.api.Framework;


/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 *
 */
public class VersionEventListener implements PostCommitEventListener {

    private static final Log log = LogFactory.getLog(VersionEventListener.class);

    private GraphManagerEventListener manager;

    private List<String> eventNames;

    public VersionEventListener() {
        try {
            AnnotationsRepositoryConfigurationService service = Framework.getService(AnnotationsRepositoryConfigurationService.class);
            eventNames = service.getEventIds();
            manager = service.getGraphManagerEventListener();
        } catch (Exception e) {
            log.error(e);
        }
    }

    public void handleEvent(EventBundle events) throws ClientException {
        for (Event event : events) {
            if (eventNames.contains(event.getName())) {
                manager.manage(event);
            }
        }
    }


}
