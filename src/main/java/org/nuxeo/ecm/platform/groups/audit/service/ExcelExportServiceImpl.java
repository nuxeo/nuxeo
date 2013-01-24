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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.jxls.exception.ParsePropertyException;
import net.sf.jxls.transformer.XLSTransformer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.platform.contentview.jsf.ContentView;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Excel Export service generating Excel report file
 * 
 */
public class ExcelExportServiceImpl implements ExcelExportService {

    public static final Log log = LogFactory.getLog(ExcelExportServiceImpl.class);

    @Override
    public File getExcelAllGroupsAuditReport() throws ClientException {
        UserManager userManager = Framework.getLocalService(UserManager.class);
        File template = getFileFromPath("templates/audit-groups-template.xls");
        List<String> groupsId = new ArrayList<String>();
        List<NuxeoGroup> groups = new ArrayList<NuxeoGroup>();
        groupsId = userManager.getGroupIds();
        for (String groupId : groupsId) {
            NuxeoGroup group = userManager.getGroup(groupId);
            groups.add(group);
        }
        Map beans = new HashMap();
        beans.put("groups", groups);
        beans.put("userManager", userManager);
        XLSTransformer transformer = new XLSTransformer();
        File resultReport = null;
        try {
            resultReport = new File(getWorkingDir(), "audit-groups.xls");
            resultReport.createNewFile();
            transformer.transformXLS(template.getAbsolutePath(), beans,
                    resultReport.getAbsolutePath());
        } catch (IOException e) {
            log.debug("Unable to create excel report result file:"
                    + e.getCause().getMessage());
        } catch (ParsePropertyException e) {
            log.debug("Unable to apply excel template to generate report:"
                    + e.getCause().getMessage());
        } catch (InvalidFormatException e) {
            log.debug("Unable to apply excel template to generate report:"
                    + e.getCause().getMessage());
        }
        return resultReport;
    }

    @Override
    public File getExcelListedGroupsAuditReport(ContentView contentView)
            throws ClientException {
        UserManager userManager = Framework.getLocalService(UserManager.class);
        File template = getFileFromPath("templates/audit-groups-template.xls");
        List<String> groupsId = new ArrayList<String>();
        List<NuxeoGroup> groups = new ArrayList<NuxeoGroup>();
        PageProvider currentPP = contentView.getCurrentPageProvider();
        List<DocumentModel> groupModels = (ArrayList<DocumentModel>) currentPP.getCurrentPage();
        for (DocumentModel groupModel : groupModels) {
            NuxeoGroup group = userManager.getGroup(groupModel.getId());
            groups.add(group);
        }
        Map beans = new HashMap();
        beans.put("groups", groups);
        beans.put("userManager", userManager);
        XLSTransformer transformer = new XLSTransformer();
        File resultReport = null;
        try {
            resultReport = new File(getWorkingDir(), "audit-groups.xls");
            resultReport.createNewFile();
            transformer.transformXLS(template.getAbsolutePath(), beans,
                    resultReport.getAbsolutePath());
        } catch (IOException e) {
            log.debug("Unable to create excel report result file:"
                    + e.getCause().getMessage());
        } catch (ParsePropertyException e) {
            log.debug("Unable to apply excel template to generate report:"
                    + e.getCause().getMessage());
        } catch (InvalidFormatException e) {
            log.debug("Unable to apply excel template to generate report:"
                    + e.getCause().getMessage());
        }
        return resultReport;
    }

    private static File getFileFromPath(String path) {
        return FileUtils.getResourceFileFromContext(path);
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
