/*
 * (C) Copyright 2013 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * Contributors:
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.platform.groups.audit.service;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import net.sf.jxls.transformer.XLSTransformer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Excel Export service generating Excel report file
 *
 * @since 5.7
 */
public class ExcelExportServiceImpl extends DefaultComponent implements
        ExcelExportService {

    public static final Log log = LogFactory.getLog(ExcelExportServiceImpl.class);

    public static final String EXCEL_EXPORT_EP = "excelExportFactory";

    protected static final Map<String, ExcelExportServiceDescriptor> exportExcelRegistry = new HashMap<String, ExcelExportServiceDescriptor>();

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        if (EXCEL_EXPORT_EP.equals(extensionPoint)) {
            ExcelExportServiceDescriptor desc = (ExcelExportServiceDescriptor) contribution;
            exportExcelRegistry.put(desc.getName(), desc);
        } else {
            log.error("Unknown extension point " + extensionPoint);
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
    }

    /**
     * Get excel export for a given name (contributed)
     */
    @Override
    public File getExcelReport(String exportName) {
        XLSTransformer transformer = new XLSTransformer();
        File resultReport = null;
        try {
            resultReport = new File(getWorkingDir(), "audit-groups.xls");
            resultReport.createNewFile();
            ExcelExportServiceDescriptor descriptor = exportExcelRegistry.get(exportName);
            transformer.transformXLS(
                    descriptor.getTemplate().getAbsolutePath(),
                    descriptor.getFactory().getDataToInject(),
                    resultReport.getAbsolutePath());
        } catch (Exception e) {
            log.error("Unable to create excel report result file:", e);
        }
        return resultReport;
    }

    /**
     * Get excel export for a given name and given data
     */
    @Override
    public File getExcelReport(String exportName, Map<String, Object> data) {
        XLSTransformer transformer = new XLSTransformer();
        File resultReport = null;
        try {
            resultReport = new File(getWorkingDir(), "audit-groups.xls");
            resultReport.createNewFile();
            ExcelExportServiceDescriptor descriptor = exportExcelRegistry.get(exportName);
            transformer.transformXLS(
                    descriptor.getTemplate().getAbsolutePath(), data,
                    resultReport.getAbsolutePath());
        } catch (Exception e) {
            log.error("Unable to create excel report result file:", e);
        }
        return resultReport;
    }

    protected File getWorkingDir() {
        String dirPath = System.getProperty("java.io.tmpdir")
                + "/NXExcelExport" + System.currentTimeMillis();
        File workingDir = new File(dirPath);
        if (workingDir.exists()) {
            FileUtils.deleteTree(workingDir);
        }
        workingDir.mkdir();
        return workingDir;
    }

}
