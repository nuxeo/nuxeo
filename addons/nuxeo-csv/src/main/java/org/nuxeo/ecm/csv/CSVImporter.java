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

import static org.nuxeo.ecm.csv.CSVImportLog.*;

import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.7
 */
public interface CSVImporter {

    CSVImportId launchImport(CoreSession session, String parentPath,
            Blob csvBlob, CSVImporterOptions options);

    CSVImportStatus getImportStatus(CSVImportId id);

    List<CSVImportLog> getImportLogs(CSVImportId id);

    List<CSVImportLog> getImportLogs(CSVImportId id, Status... status);

    List<CSVImportLog> getLastImportLogs(CSVImportId id, int max);

    List<CSVImportLog> getLastImportLogs(CSVImportId id, int max, Status... status);

    CSVImportResult getImportResult(CSVImportId id);
}
