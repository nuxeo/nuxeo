package org.nuxeo.ecm.csv;

import static org.nuxeo.ecm.csv.CSVImportLog.*;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.runtime.api.Framework;
import org.richfaces.event.UploadEvent;
import org.richfaces.model.UploadItem;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.7
 */
@Scope(ScopeType.CONVERSATION)
@Name("csvImportActions")
@Install(precedence = Install.FRAMEWORK)
public class CSVImportActions implements Serializable {

    private static final long serialVersionUID = 1L;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true, required = false)
    protected transient NavigationContext navigationContext;

    protected Blob csvBlob;

    protected CSVImportId csvImportId;

    public void uploadListener(UploadEvent event) throws Exception {
        UploadItem item = event.getUploadItem();
        csvBlob = new FileBlob(item.getFile());
        csvBlob.setFilename(FilenameUtils.getName(item.getFileName()));
    }

    public void importCSVFile() {
        if (csvBlob != null) {
            CSVImporter csvImporter = Framework.getLocalService(CSVImporter.class);
            csvImportId = csvImporter.launchImport(documentManager,
                    navigationContext.getCurrentDocument().getPathAsString(),
                    csvBlob, CSVImporterOptions.DEFAULT_OPTIONS);
        }
    }

    public String getImportingCSVFilename() {
        return csvBlob != null ? csvBlob.getFilename() : "";
    }

    public CSVImportStatus getImportStatus() {
        if (csvImportId == null) {
            return null;
        }
        CSVImporter csvImporter = Framework.getLocalService(CSVImporter.class);
        return csvImporter.getImportStatus(csvImportId);
    }

    public List<CSVImportLog> getLastLogs(int maxLogs) {
        if (csvImportId == null) {
            return Collections.emptyList();
        }
        CSVImporter csvImporter = Framework.getLocalService(CSVImporter.class);
        return csvImporter.getLastImportLogs(csvImportId,
                maxLogs);
    }

    public List<CSVImportLog> getSkippedAndErrorLogs() {
        if (csvImportId == null) {
            return Collections.emptyList();
        }
        CSVImporter csvImporter = Framework.getLocalService(CSVImporter.class);
        return csvImporter.getImportLogs(csvImportId,
                Status.SKIPPED, Status.ERROR);
    }

    public CSVImportResult getImportResult() {
        if (csvImportId == null) {
            return null;
        }
        CSVImporter csvImporter = Framework.getLocalService(CSVImporter.class);
        return csvImporter.getImportResult(csvImportId);
    }

    @Observer(EventNames.NAVIGATE_TO_DOCUMENT)
    public void resetState() {
        csvBlob = null;
        csvImportId = null;
    }
}
