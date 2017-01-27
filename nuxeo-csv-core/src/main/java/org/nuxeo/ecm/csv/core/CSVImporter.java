/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.csv.core;

import java.io.File;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.csv.core.CSVImportLog.Status;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.7
 */
public interface CSVImporter {

    @Deprecated
    String launchImport(CoreSession session, String parentPath, File csvFile, String csvFileName,
            CSVImporterOptions options);

    /**
     * @since 9.1
     */
    String launchImport(CoreSession session, String parentPath, Blob blob, CSVImporterOptions options);

    CSVImportStatus getImportStatus(String id);

    List<CSVImportLog> getImportLogs(String id);

    List<CSVImportLog> getImportLogs(String id, Status... status);

    List<CSVImportLog> getLastImportLogs(String id, int max);

    default List<CSVImportLog> getLastImportLogs(String id, int max, Status... status) {
        return null;
    }

    CSVImportResult getImportResult(String id);
}
