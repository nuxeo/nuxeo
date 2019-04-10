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

package org.nuxeo.ecm.csv.jsf;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.csv.core.CSVImportLog;
import org.nuxeo.ecm.csv.core.CSVImportResult;
import org.nuxeo.ecm.csv.core.CSVImportStatus;
import org.nuxeo.ecm.csv.core.CSVImporter;
import org.nuxeo.ecm.csv.core.CSVImporterOptions;
import org.nuxeo.ecm.csv.core.CSVImporterOptions.ImportMode;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.runtime.api.Framework;
import org.richfaces.event.FileUploadEvent;
import org.richfaces.model.UploadedFile;

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

    protected File csvFile;

    protected String csvFileName;

    protected boolean notifyUserByEmail = false;

    protected String csvImportId;

    /**
     * @since 8.4
     */
    protected Boolean useImportMode = false;

    public boolean getNotifyUserByEmail() {
        return notifyUserByEmail;
    }

    public void setNotifyUserByEmail(boolean notifyUserByEmail) {
        this.notifyUserByEmail = notifyUserByEmail;
    }

    public Boolean getUseImportMode() {
        return useImportMode;
    }

    public void setUseImportMode(Boolean importMode) {
        this.useImportMode = importMode;
    }

    protected ImportMode getImportMode() {
        return useImportMode ? ImportMode.IMPORT : ImportMode.CREATE;
    }

    public void uploadListener(FileUploadEvent event) throws Exception {
        UploadedFile item = event.getUploadedFile();
        // FIXME: check if this needs to be tracked for deletion
        csvFile = Framework.createTempFile("FileManageActionsFile", null);
        try (InputStream in = event.getUploadedFile().getInputStream()) {
            FileUtils.copyInputStreamToFile(in, csvFile);
        }
        csvFileName = FilenameUtils.getName(item.getName());
    }

    public void importCSVFile() throws IOException {
        if (csvFile != null) {
            CSVImporterOptions options = new CSVImporterOptions.Builder().sendEmail(notifyUserByEmail)
                                                                         .importMode(getImportMode())
                                                                         .build();
            CSVImporter csvImporter = Framework.getService(CSVImporter.class);
            csvImportId = csvImporter.launchImport(documentManager,
                    navigationContext.getCurrentDocument().getPathAsString(),
                    Blobs.createBlob(csvFile, null, null, csvFileName), options);
        }
    }

    public String getImportingCSVFilename() {
        return csvFileName;
    }

    public CSVImportStatus getImportStatus() {
        if (csvImportId == null) {
            return null;
        }
        CSVImporter csvImporter = Framework.getService(CSVImporter.class);
        return csvImporter.getImportStatus(csvImportId);
    }

    public List<CSVImportLog> getLastLogs(int maxLogs) {
        if (csvImportId == null) {
            return Collections.emptyList();
        }
        CSVImporter csvImporter = Framework.getService(CSVImporter.class);
        return csvImporter.getLastImportLogs(csvImportId, maxLogs);
    }

    public List<CSVImportLog> getSkippedAndErrorLogs() {
        if (csvImportId == null) {
            return Collections.emptyList();
        }
        CSVImporter csvImporter = Framework.getService(CSVImporter.class);
        return csvImporter.getImportLogs(csvImportId, CSVImportLog.Status.SKIPPED, CSVImportLog.Status.ERROR);
    }

    public CSVImportResult getImportResult() {
        if (csvImportId == null) {
            return null;
        }
        CSVImporter csvImporter = Framework.getService(CSVImporter.class);
        return csvImporter.getImportResult(csvImportId);
    }

    @Observer(EventNames.NAVIGATE_TO_DOCUMENT)
    public void resetState() {
        csvFile = null;
        csvFileName = null;
        csvImportId = null;
        notifyUserByEmail = false;
    }
}
