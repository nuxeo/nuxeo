/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.platform.groups.audit.seam;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.activation.MimetypesFileTypeMap;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.platform.contentview.seam.ContentViewActions;
import org.nuxeo.ecm.platform.groups.audit.service.ExcelExportService;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Export group manager seam bean to export groups definition in excel file
 */
@Name("exportGroupManagementActions")
@Scope(ScopeType.CONVERSATION)
@Install(precedence = Install.APPLICATION)
public class ExportGroupManagementActions implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final Log log = LogFactory.getLog(ExportGroupManagementActions.class);

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected transient ContentViewActions contentViewActions;

    public String downloadExcelAllGroupsExport() {
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext econtext = context.getExternalContext();
        HttpServletResponse response = (HttpServletResponse) econtext.getResponse();
        File excelReport = excelExportAllGroupsDefinition();

        response.setContentType(new MimetypesFileTypeMap().getContentType(excelReport));
        response.setHeader("Content-disposition", "attachment; filename=\"" + excelReport.getName() + "\"");
        response.setHeader("Content-Length", String.valueOf(excelReport.length()));
        try {
            ServletOutputStream os = response.getOutputStream();
            InputStream in = new FileInputStream(excelReport);
            IOUtils.copy(in, os);
            os.flush();
            in.close();
            os.close();
            context.responseComplete();
        } catch (IOException e) {
            log.error("Failure : " + e.getMessage());
        }
        return null;
    }

    public String downloadExcelListedGroupsExport() {
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext econtext = context.getExternalContext();
        HttpServletResponse response = (HttpServletResponse) econtext.getResponse();
        File excelReport = excelExportListedGroupsDefinition();

        response.setContentType(new MimetypesFileTypeMap().getContentType(excelReport));
        response.setHeader("Content-disposition", "attachment; filename=\"" + excelReport.getName() + "\"");
        response.setHeader("Content-Length", String.valueOf(excelReport.length()));
        try {
            ServletOutputStream os = response.getOutputStream();
            InputStream in = new FileInputStream(excelReport);
            IOUtils.copy(in, os);
            os.flush();
            in.close();
            os.close();
            context.responseComplete();
        } catch (IOException e) {
            log.error("Failure : " + e.getMessage());
        }
        return null;
    }

    protected File excelExportAllGroupsDefinition() {
        ExcelExportService exportService = Framework.getService(ExcelExportService.class);
        return exportService.getExcelReport("exportAllGroupsAudit");
    }

    protected File excelExportListedGroupsDefinition() {
        ExcelExportService exportService = Framework.getService(ExcelExportService.class);
        return exportService.getExcelReport("exportListedGroupsAudit", getDataInject());
    }

    private Map<String, Object> getDataInject() {
        UserManager userManager = Framework.getService(UserManager.class);
        List<NuxeoGroup> groups = new ArrayList<NuxeoGroup>();
        PageProvider currentPP = contentViewActions.getCurrentContentView().getCurrentPageProvider();
        List<DocumentModel> groupModels = (ArrayList<DocumentModel>) currentPP.getCurrentPage();
        for (DocumentModel groupModel : groupModels) {
            NuxeoGroup group = userManager.getGroup(groupModel.getId());
            groups.add(group);
        }
        Map<String, Object> beans = new HashMap<String, Object>();
        beans.put("groups", groups);
        beans.put("userManager", userManager);
        return beans;
    }
}
