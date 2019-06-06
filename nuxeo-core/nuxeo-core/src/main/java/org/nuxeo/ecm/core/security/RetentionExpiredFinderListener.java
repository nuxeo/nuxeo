/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.security;

import java.util.Calendar;

import org.apache.commons.lang3.time.FastDateFormat;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.runtime.api.Framework;

/**
 * Listener triggering the check of expired retentions.
 *
 * @since 11.1
 */
public class RetentionExpiredFinderListener implements EventListener {

    public static final String QUERY = "SELECT * FROM Document, Relation"
            + " WHERE ecm:isProxy = 0 AND ecm:retainUntil < TIMESTAMP '%s'";

    public static final FastDateFormat FORMATTER = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss");

    public void handleEvent(Event event) {
        BulkService bulkService = Framework.getService(BulkService.class);
        RepositoryService repositoryService = Framework.getService(RepositoryService.class);

        Calendar now = Calendar.getInstance();
        String formattedDate = FORMATTER.format(now);
        String nxql = String.format(QUERY, formattedDate);

        for (String repositoryName : repositoryService.getRepositoryNames()) {
            BulkCommand command = new BulkCommand.Builder(RetentionExpiredAction.ACTION_NAME, nxql).user(
                    SecurityConstants.SYSTEM_USERNAME).repository(repositoryName).build();
            bulkService.submit(command);
        }
    }

}
