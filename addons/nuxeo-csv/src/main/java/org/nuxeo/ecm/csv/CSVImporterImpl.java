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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.7
 */
public class CSVImporterImpl implements CSVImporter {

    @Override
    public CSVImportId launchImport(CoreSession session, String parentPath,
            Blob csvBlob, CSVImporterOptions options) {
        CSVImporterWork work = new CSVImporterWork(session.getRepositoryName(),
                parentPath, session.getPrincipal().getName(), csvBlob, options);
        WorkManager workManager = Framework.getLocalService(WorkManager.class);
        workManager.schedule(work,
                WorkManager.Scheduling.IF_NOT_RUNNING_OR_SCHEDULED);
        return work.getId();
    }

    @Override
    public CSVImportStatus getImportStatus(CSVImportId id) {
        WorkManager workManager = Framework.getLocalService(WorkManager.class);
        Work workId = new CSVImporterWork(id);
        int[] pos = new int[1];
        Work work = workManager.find(workId, null, true, pos);
        if (work == null) {
            // completed?
            work = workManager.find(workId, Work.State.COMPLETED, true, pos);
            return work != null ? new CSVImportStatus(
                    CSVImportStatus.State.COMPLETED) : null;
        } else if (work.getState() == Work.State.SCHEDULED) {
            String queueId = workManager.getCategoryQueueId(CSVImporterWork.CATEGORY_CSV_IMPORTER);
            int queueSize = workManager.listWork(queueId, Work.State.SCHEDULED).size();
            return new CSVImportStatus(CSVImportStatus.State.SCHEDULED,
                    pos[0] + 1, queueSize);
        } else { // RUNNING
            return new CSVImportStatus(CSVImportStatus.State.RUNNING);
        }
    }

    @Override
    public List<CSVImportLog> getImportLogs(CSVImportId id) {
        return getLastImportLogs(id, -1);
    }

    @Override
    public List<CSVImportLog> getImportLogs(CSVImportId id,
            CSVImportLog.Status... status) {
        return getLastImportLogs(id, -1, status);
    }

    @Override
    public List<CSVImportLog> getLastImportLogs(CSVImportId id, int max) {
        WorkManager workManager = Framework.getLocalService(WorkManager.class);
        Work workId = new CSVImporterWork(id);
        int[] pos = new int[1];
        Work work = workManager.find(workId, null, true, pos);
        if (work == null) {
            work = workManager.find(workId, Work.State.COMPLETED, true, pos);
            if (work == null) {
                return Collections.emptyList();
            }
        }
        List<CSVImportLog> importLogs = ((CSVImporterWork) work).getImportLogs();
        max = (max == -1 || max > importLogs.size()) ? importLogs.size() : max;
        return importLogs.subList(importLogs.size() - max, importLogs.size());
    }

    @Override
    public List<CSVImportLog> getLastImportLogs(CSVImportId id, int max,
            CSVImportLog.Status... status) {
        List<CSVImportLog> importLogs = getLastImportLogs(id, max);
        return status.length == 0 ? importLogs : filterImportLogs(importLogs,
                status);
    }

    protected List<CSVImportLog> filterImportLogs(
            List<CSVImportLog> importLogs, CSVImportLog.Status... status) {
        List<CSVImportLog.Status> statusList = Arrays.asList(status);
        List<CSVImportLog> filteredLogs = new ArrayList<CSVImportLog>();
        for (CSVImportLog log : importLogs) {
            if (statusList.contains(log.getStatus())) {
                filteredLogs.add(log);
            }
        }
        return filteredLogs;
    }

    @Override
    public CSVImportResult getImportResult(CSVImportId id) {
        WorkManager workManager = Framework.getLocalService(WorkManager.class);
        Work work = new CSVImporterWork(id);
        int[] pos = new int[1];
        work = workManager.find(work, Work.State.COMPLETED, true, pos);
        if (work == null) {
            return null;
        }

        List<CSVImportLog> importLogs = ((CSVImporterWork) work).getImportLogs();
        return CSVImportResult.fromImportLogs(importLogs);
    }

}
