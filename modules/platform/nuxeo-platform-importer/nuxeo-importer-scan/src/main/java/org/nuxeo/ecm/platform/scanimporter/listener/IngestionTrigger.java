/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * $Id$
 */
package org.nuxeo.ecm.platform.scanimporter.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.platform.scanimporter.processor.ScannedFileImporter;

/**
 * Listen to Scheduler events to check if new scanned files are availables Trigger the importer if not already busy.
 *
 * @author Thierry Delprat
 */
public class IngestionTrigger implements EventListener {

    private static final Log log = LogFactory.getLog(IngestionTrigger.class);

    private static volatile boolean ingestionInProgress = false;

    public static final String START_EVENT = "ScanIngestionStart";

    // fired via the Scheduler
    @Override
    public void handleEvent(Event event) {

        if (!START_EVENT.equals(event.getName())) {
            return;
        }

        if (ingestionInProgress) {
            log.info("Ingestion already in progress, waiting for next wake up");
            return;
        } else {
            log.info("Start injection process");
        }

        ingestionInProgress = true;
        try {
            ScannedFileImporter importer = new ScannedFileImporter();
            if (event.getContext().getProperty("Testing") != null) {
                event.getContext().setProperty("Tested", true);
            } else {
                importer.doImport();
            }
        } finally {
            ingestionInProgress = false;
        }
    }

}
