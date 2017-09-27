/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Stephane Lacoin (Nuxeo EP Software Engineer)
 */
package org.nuxeo.ecm.platform.audit.service;

import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.platform.audit.api.FilterMapEntry;
import org.nuxeo.ecm.platform.audit.api.LogEntry;

/**
 * Minimal interface extracted to be able to share some code inside the {@link AbstractAuditBackend}
 * 
 * @author tiry
 */
public interface BaseLogEntryProvider {

    void addLogEntry(LogEntry entry);

    /**
     * Returns the logs given a doc uuid and a repository id.
     *
     * @param uuid the document uuid
     * @param repositoryId the repository id
     * @return a list of log entries
     * @since 8.4
     */
    List<LogEntry> getLogEntriesFor(String uuid, String repositoryId);

    /**
     * Returns the logs given a doc uuid.
     *
     * @param uuid the document uuid
     * @return a list of log entries
     * @since 8.4
     * @deprecated since 8.4, use {@link #getLogEntriesFor(String, String))} instead.
     */
    @Deprecated
    List<LogEntry> getLogEntriesFor(String uuid);

    /**
     * Returns the logs given a doc uuid, a map of filters and a default sort.
     *
     * @param uuid the document uuid
     * @param filterMap the map of filters to apply
     * @param doDefaultSort the default sort to set
     * @return a list of log entries
     * @since 8.4
     * @deprecated since 8.4
     */
    @Deprecated
    List<LogEntry> getLogEntriesFor(String uuid, Map<String, FilterMapEntry> filterMap, boolean doDefaultSort);

    int removeEntries(String eventId, String pathPattern);

}
