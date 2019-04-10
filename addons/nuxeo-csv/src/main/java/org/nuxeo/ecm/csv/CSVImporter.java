/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.csv;

import java.io.File;
import java.util.List;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.csv.CSVImportLog.Status;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.7
 */
public interface CSVImporter {

    String launchImport(CoreSession session, String parentPath, File csvFile, String csvFileName,
            CSVImporterOptions options);

    CSVImportStatus getImportStatus(String id);

    List<CSVImportLog> getImportLogs(String id);

    List<CSVImportLog> getImportLogs(String id, Status... status);

    List<CSVImportLog> getLastImportLogs(String id, int max);

    List<CSVImportLog> getLastImportLogs(String id, int max, Status... status);

    CSVImportResult getImportResult(String id);
}
