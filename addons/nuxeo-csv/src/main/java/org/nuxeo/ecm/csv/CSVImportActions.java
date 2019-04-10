package org.nuxeo.ecm.csv;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
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

    protected Blob blob;

    public void uploadListener(UploadEvent event) throws Exception {
        UploadItem item = event.getUploadItem();
        blob = new FileBlob(item.getFile());
        blob.setFilename(FilenameUtils.getName(item.getFileName()));
    }

    public void importCSVFile() {
        if (blob != null) {
            CSVImporter csvImporter = new CSVImporter(
                    CSVImporterOptions.DEFAULT_OPTIONS);
            csvImporter.run(documentManager,
                    navigationContext.getCurrentDocument().getPathAsString(),
                    blob);
        }
    }

    public String getImportingCSVFilename() {
        WorkManager workManager = Framework.getLocalService(WorkManager.class);
        Work work = new CSVImporterWork(navigationContext.getCurrentDocument());
        int[] pos = new int[1];
        work = workManager.find(work, null, true, pos);
        return work != null ? ((CSVImporterWork) work).getCSVFilename()
                : "";
    }

    public boolean isRunning() {
        WorkManager workManager = Framework.getLocalService(WorkManager.class);
        Work work = new CSVImporterWork(navigationContext.getCurrentDocument());
        int[] pos = new int[1];
        work = workManager.find(work, null, true, pos);
        return work != null;
    }

    public boolean isComplete() {
        WorkManager workManager = Framework.getLocalService(WorkManager.class);
        Work work = new CSVImporterWork(navigationContext.getCurrentDocument());
        int[] pos = new int[1];
        work = workManager.find(work, Work.State.COMPLETED, true, pos);
        return work != null && work.getState().equals(Work.State.COMPLETED);
    }

    public List<CSVImportLog> getLastLogs(int maxLogs) {
        WorkManager workManager = Framework.getLocalService(WorkManager.class);
        Work workId = new CSVImporterWork(
                navigationContext.getCurrentDocument());
        int[] pos = new int[1];
        Work work = workManager.find(workId, null, true, pos);
        if (work == null) {
            work = workManager.find(workId, Work.State.COMPLETED, true, pos);
            if (work == null) {
                return Collections.emptyList();
            }
        }
        List<CSVImportLog> importLogs = ((CSVImporterWork) work).getImportLogs();
        int max = maxLogs > importLogs.size() ? importLogs.size() : maxLogs;
        return importLogs.subList(importLogs.size() - max, max);
    }

    public List<CSVImportLog> getSkippedAndErrorLogs() {
        WorkManager workManager = Framework.getLocalService(WorkManager.class);
        Work workId = new CSVImporterWork(
                navigationContext.getCurrentDocument());
        int[] pos = new int[1];
        Work work = workManager.find(workId, null, true, pos);
        if (work == null) {
            work = workManager.find(workId, Work.State.COMPLETED, true, pos);
            if (work == null) {
                return Collections.emptyList();
            }
        }
        List<CSVImportLog> importLogs = ((CSVImporterWork) work).getImportLogs();
        return filterImportLogs(importLogs, CSVImportLog.Status.SKIPPED,
                CSVImportLog.Status.ERROR);
    }

    protected List<CSVImportLog> filterImportLogs(
            List<CSVImportLog> importLogs, CSVImportLog.Status... statusToKeep) {
        List<CSVImportLog.Status> status = Arrays.asList(statusToKeep);
        List<CSVImportLog> filteredLogs = new ArrayList<CSVImportLog>();
        for (CSVImportLog log : importLogs) {
            if (status.contains(log.getStatus())) {
                filteredLogs.add(log);
            }
        }
        return filteredLogs;
    }

    public CSVImportResult getImportResult() {
        WorkManager workManager = Framework.getLocalService(WorkManager.class);
        Work work = new CSVImporterWork(navigationContext.getCurrentDocument());
        int[] pos = new int[1];
        work = workManager.find(work, Work.State.COMPLETED, true, pos);
        if (work == null) {
            return null;
        }

        List<CSVImportLog> importLogs = ((CSVImporterWork) work).getImportLogs();
        return computeImportResult(importLogs);
    }

    protected CSVImportResult computeImportResult(List<CSVImportLog> importLogs) {
        long totalLineCount = importLogs.size();
        long successLineCount = 0;
        long skippedLineCount = 0;
        long errorLineCount = 0;
        for (CSVImportLog importLog : importLogs) {
            if (importLog.isSuccess()) {
                successLineCount++;
            } else if (importLog.isSkipped()) {
                skippedLineCount++;
            } else if (importLog.isError()) {
                errorLineCount++;
            }
        }
        return new CSVImportResult(totalLineCount, successLineCount,
                skippedLineCount, errorLineCount);
    }

}
