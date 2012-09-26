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
