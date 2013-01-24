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

package org.nuxeo.ecm.platform.groups.audit.seam;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Serializable;

import javax.activation.MimetypesFileTypeMap;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.groups.audit.service.ExcelExportService;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;
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
    protected transient ResourcesAccessor resourcesAccessor;

    public String downloadExcelExport() throws ClientException {
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext econtext = context.getExternalContext();
        HttpServletResponse response = (HttpServletResponse) econtext.getResponse();
        File excelReport = excelExportGroupsDefinition();

        response.setContentType(new MimetypesFileTypeMap().getContentType(excelReport));
        response.setHeader("Content-disposition", "attachment; filename=\""
                + excelReport.getName() + "\"");
        response.setHeader("Content-Length",
                String.valueOf(excelReport.length()));
        try {
            ServletOutputStream os = response.getOutputStream();
            InputStream in = new FileInputStream(excelReport);
            FileUtils.copy(in, os);
            os.flush();
            in.close();
            os.close();
            context.responseComplete();
        } catch (Exception e) {
            log.error("Failure : " + e.getMessage());
        }
        return null;
    }

    protected File excelExportGroupsDefinition() throws ClientException {
        ExcelExportService exportService = Framework.getLocalService(ExcelExportService.class);
        return exportService.getExcelGroupsAuditReport();
    }

}
