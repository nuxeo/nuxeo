/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.versioning;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.CoreService;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;

/**
 * Asynchronous listener that calls the orphan versions cleanup service. Designed to be called periodically by a
 * scheduler.
 *
 * @since 9.1
 */
public class OrphanVersionCleanupListener implements PostCommitEventListener {

    private static final Log log = LogFactory.getLog(OrphanVersionCleanupListener.class);

    public static final long DEFAULT_COMMIT_SIZE = 1000;

    /**
     * Configuration property for the maximum number of orphan versions to delete in one transaction. Default is
     * {@value #DEFAULT_COMMIT_SIZE}.
     */
    public static final String DEFAULT_COMMIT_SIZE_PROP = "org.nuxeo.orphanVersionsCleanup.commitSize";

    /**
     * Gets the maximum number of orphan versions to delete in one transaction.
     */
    protected long getCommitSize() {
        ConfigurationService configurationService = Framework.getService(ConfigurationService.class);
        String commitSize = configurationService.getProperty(DEFAULT_COMMIT_SIZE_PROP);
        if (commitSize == null) {
            return DEFAULT_COMMIT_SIZE;
        }
        try {
            return Long.parseLong(commitSize);
        } catch (NumberFormatException e) {
            log.error("Invalid configuration property " + DEFAULT_COMMIT_SIZE_PROP, e);
            return DEFAULT_COMMIT_SIZE;
        }
    }

    @Override
    public void handleEvent(EventBundle events) {
        CoreService coreService = Framework.getService(CoreService.class);
        if (coreService == null) {
            // CoreService failed to start, no need to go further
            return;
        }
        log.debug("Starting orphan versions cleanup");
        long n = coreService.cleanupOrphanVersions(getCommitSize());
        log.debug("Number of orphan versions deleted: " + n);
    }

}
