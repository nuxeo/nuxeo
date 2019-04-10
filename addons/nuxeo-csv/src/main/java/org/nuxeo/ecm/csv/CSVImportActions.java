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

import static org.nuxeo.ecm.csv.CSVImportLog.Status;

import java.io.File;
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
import org.nuxeo.ecm.core.api.CoreSession;
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

    protected File csvFile;

    protected String csvFileName;

    protected boolean notifyUserByEmail = false;

    protected String csvImportId;

    public boolean getNotifyUserByEmail() {
        return notifyUserByEmail;
    }

    public void setNotifyUserByEmail(boolean notifyUserByEmail) {
        this.notifyUserByEmail = notifyUserByEmail;
    }

    public void uploadListener(UploadEvent event) throws Exception {
        UploadItem item = event.getUploadItem();
        csvFile = item.getFile();
        csvFileName = FilenameUtils.getName(item.getFileName());
    }

    public void importCSVFile() {
        if (csvFile != null) {
            CSVImporterOptions options = new CSVImporterOptions.Builder().sendEmail(notifyUserByEmail).build();
            CSVImporter csvImporter = Framework.getLocalService(CSVImporter.class);
            csvImportId = csvImporter.launchImport(documentManager,
                    navigationContext.getCurrentDocument().getPathAsString(), csvFile, csvFileName, options);
        }
    }

    public String getImportingCSVFilename() {
        return csvFileName;
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
        return csvImporter.getLastImportLogs(csvImportId, maxLogs);
    }

    public List<CSVImportLog> getSkippedAndErrorLogs() {
        if (csvImportId == null) {
            return Collections.emptyList();
        }
        CSVImporter csvImporter = Framework.getLocalService(CSVImporter.class);
        return csvImporter.getImportLogs(csvImportId, Status.SKIPPED, Status.ERROR);
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
        csvFile = null;
        csvFileName = null;
        csvImportId = null;
        notifyUserByEmail = false;
    }
}
