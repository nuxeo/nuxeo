/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer
 */
package org.nuxeo.wopi;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.runtime.api.Framework;

/**
 * Refreshes the WOPI discovery.
 *
 * @since 10.10
 */
public class WOPIDiscoveryRefreshListener implements PostCommitEventListener {

    private static final Logger log = LogManager.getLogger(WOPIDiscoveryRefreshListener.class);

    @Override
    public void handleEvent(EventBundle events) {
        log.debug("Refreshing WOPI discovery");
        boolean refreshed = Framework.getService(WOPIService.class).refreshDiscovery();
        if (!refreshed) {
            log.error("Cannot refresh WOPI discovery");
        }
    }

}
