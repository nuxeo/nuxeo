/*
 * (C) Copyright 2006-2012 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

    private static boolean ingestionInProgress = false;

    public static String START_EVENT = "ScanIngestionStart";

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
