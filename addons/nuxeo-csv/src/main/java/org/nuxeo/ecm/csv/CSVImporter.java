package org.nuxeo.ecm.csv;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.7
 */
public class CSVImporter {

    protected final CSVImporterOptions options;

    protected CSVImporterWork work;

    public CSVImporter(CSVImporterOptions options) {
        this.options = options;
    }

    public void run(CoreSession session, String parentPath, Blob csv) {
        work = new CSVImporterWork(session.getRepositoryName(), parentPath,
                session.getPrincipal().getName(), csv, options);
        WorkManager workManager = Framework.getLocalService(WorkManager.class);
        workManager.schedule(work,
                WorkManager.Scheduling.IF_NOT_RUNNING_OR_SCHEDULED);
    }

    public CSVImporterWork getWork() {
        return work;
    }

}
