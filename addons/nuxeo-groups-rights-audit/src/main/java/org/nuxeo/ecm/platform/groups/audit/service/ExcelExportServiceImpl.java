/*
 * (C) Copyright 2013-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Contributors:
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.platform.groups.audit.service;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import net.sf.jxls.exception.ParsePropertyException;
import net.sf.jxls.transformer.XLSTransformer;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import org.nuxeo.common.Environment;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Excel Export service generating Excel report file
 *
 * @since 5.7
 */
public class ExcelExportServiceImpl extends DefaultComponent implements ExcelExportService {

    public static final Log log = LogFactory.getLog(ExcelExportServiceImpl.class);

    public static final String EXCEL_EXPORT_EP = "excelExportFactory";

    protected static final Map<String, ExcelExportServiceDescriptor> exportExcelRegistry = new HashMap<>();

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (EXCEL_EXPORT_EP.equals(extensionPoint)) {
            ExcelExportServiceDescriptor desc = (ExcelExportServiceDescriptor) contribution;
            exportExcelRegistry.put(desc.getName(), desc);
        } else {
            log.error("Unknown extension point " + extensionPoint);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
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
            transformer.transformXLS(descriptor.getTemplate().getAbsolutePath(), descriptor.getFactory()
                                                                                           .getDataToInject(),
                    resultReport.getAbsolutePath());
        } catch (IOException | ParsePropertyException | InvalidFormatException e) {
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
            transformer.transformXLS(descriptor.getTemplate().getAbsolutePath(), data, resultReport.getAbsolutePath());
        } catch (IOException | ParsePropertyException | InvalidFormatException e) {
            log.error("Unable to create excel report result file:", e);
        }
        return resultReport;
    }

    protected File getWorkingDir() {
        File workingDir = new File(Environment.getDefault().getTemp(), "NXExcelExport" + System.currentTimeMillis());
        if (workingDir.exists()) {
            FileUtils.deleteQuietly(workingDir);
        }
        workingDir.mkdirs();
        return workingDir;
    }

}
