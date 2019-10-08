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
 *     Nour AL KOTOB
 */
package org.nuxeo.ecm.platform.audit.io;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_CATEGORY;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_COMMENT;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_DOC_LIFE_CYCLE;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_DOC_PATH;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_DOC_TYPE;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_DOC_UUID;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_EVENT_DATE;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_EVENT_ID;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_ID;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_LOG_DATE;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_PRINCIPAL_NAME;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_REPOSITORY_ID;
import static org.nuxeo.ecm.platform.audit.io.LogEntryJsonWriter.ENTITY_TYPE;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVPrinter;
import org.nuxeo.ecm.core.io.marshallers.csv.AbstractCSVWriter;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.platform.audit.api.LogEntry;

/**
 * Convert {@link LogEntry} to CSV only keeping default and fetched properties if any.
 *
 * @since 11.1
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class LogEntryCSVWriter extends AbstractCSVWriter<LogEntry> {

    public static final List<String> DEFAULT_PROPERTIES = Arrays.asList(LOG_ID, LOG_CATEGORY, LOG_PRINCIPAL_NAME,
            LOG_COMMENT, LOG_DOC_LIFE_CYCLE, LOG_DOC_PATH, LOG_DOC_TYPE, LOG_DOC_UUID, LOG_EVENT_ID, LOG_REPOSITORY_ID,
            LOG_EVENT_DATE, LOG_LOG_DATE);

    @Override
    protected void write(LogEntry entity, CSVPrinter printer) throws IOException {
        Set<String> propertiesToFetch = new HashSet<>(getPropertiesToFetch());
        if (propertiesToFetch.contains(LOG_ID)) {
            printer.print(entity.getId());
        }
        if (propertiesToFetch.contains(LOG_CATEGORY)) {
            printer.print(entity.getCategory());
        }
        if (propertiesToFetch.contains(LOG_PRINCIPAL_NAME)) {
            printer.print(entity.getPrincipalName());
        }
        if (propertiesToFetch.contains(LOG_COMMENT)) {
            printer.print(entity.getComment());
        }
        if (propertiesToFetch.contains(LOG_DOC_LIFE_CYCLE)) {
            printer.print(entity.getDocLifeCycle());
        }
        if (propertiesToFetch.contains(LOG_DOC_PATH)) {
            printer.print(entity.getDocPath());
        }
        if (propertiesToFetch.contains(LOG_DOC_TYPE)) {
            printer.print(entity.getDocType());
        }
        if (propertiesToFetch.contains(LOG_DOC_UUID)) {
            printer.print(entity.getDocUUID());
        }
        if (propertiesToFetch.contains(LOG_EVENT_ID)) {
            printer.print(entity.getEventId());
        }
        if (propertiesToFetch.contains(LOG_REPOSITORY_ID)) {
            printer.print(entity.getRepositoryId());
        }
        if (propertiesToFetch.contains(LOG_EVENT_DATE)) {
            printer.print(entity.getEventDate());
        }
        if (propertiesToFetch.contains(LOG_LOG_DATE)) {
            printer.print(entity.getLogDate());
        }
    }

    @Override
    protected void writeHeader(LogEntry entity, CSVPrinter printer) throws IOException {
        List<String> properties = getPropertiesToFetch();
        for (String property : properties) {
            printer.print(property);
        }
        printer.println();
    }

    /**
     * Gets the properties to fetch if specified. Otherwise returns all default properties.
     *
     * @return the list of properties to fetch
     */
    protected List<String> getPropertiesToFetch() {
        Set<String> fetched = ctx.getFetched(ENTITY_TYPE);
        List<String> propertiesToFetch = DEFAULT_PROPERTIES.stream()
                                                           .filter(fetched::contains)
                                                           .collect(Collectors.toList());
        // if no particular property to fetch, fetch all default properties
        return propertiesToFetch.isEmpty() ? DEFAULT_PROPERTIES : propertiesToFetch;
    }

}
