/**
 *
 */

package org.nuxeo.ecm.platform.groups.audit.seam;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.platform.groups.audit.service.acl.AclExcelLayoutBuilder;
import org.nuxeo.ecm.platform.groups.audit.service.acl.IAclExcelLayoutBuilder;
import org.nuxeo.ecm.platform.groups.audit.service.acl.ReportLayoutSettings;
import org.nuxeo.ecm.platform.groups.audit.service.acl.filter.AcceptsGroupOnly;
import org.nuxeo.ecm.platform.groups.audit.service.acl.filter.IContentFilter;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;

@Name("excelExportRightsAction")
@Scope(ScopeType.EVENT)
public class ExcelExportRightsActionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(ExcelExportRightsActionBean.class);

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected NavigationContext navigationContext;

    @In(create = true)
    protected DocumentsListsManager documentsListsManager;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected transient ResourcesAccessor resourcesAccessor;

    @In(create = true, required = false)
    protected NuxeoPrincipal currentNuxeoPrincipal;

    public String doGet() {
        try {
            buildAndDownload();
            facesMessages.add(StatusMessage.Severity.INFO, "doGet");
        } catch (Exception e) {
            log.error(e, e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    "doGet error: " + e.getMessage());
        }
        return null;
    }

    public boolean accept() {
        return true;
    }

    protected List<DocumentModel> getCurrentlySelectedDocuments() {
        if (navigationContext.getCurrentDocument().isFolder()) {
            return documentsListsManager.getWorkingList(DocumentsListsManager.CURRENT_DOCUMENT_SELECTION);
        } else {
            return null;
        }
    }

    /* XLS REPORT */

    public void buildAndDownload() throws ClientException, IOException {
        File tmpFile = File.createTempFile("rights-", ".xls");
        tmpFile.deleteOnExit();
        buildAndDownload(tmpFile);
    }

    protected void buildAndDownload(final File tmpFile) throws ClientException {
        final FacesContext context = FacesContext.getCurrentInstance();
        final DocumentModel doc = navigationContext.getCurrentDocument();

        UnrestrictedSessionRunner runner = new UnrestrictedSessionRunner(
                doc.getRepositoryName()) {
            @Override
            public void run() throws ClientException {
                log.info("start audit, export in " + tmpFile);

                // configure audit
                ReportLayoutSettings s = AclExcelLayoutBuilder.defaultLayout();
                s.setPageSize(1000);
                IContentFilter f = new AcceptsGroupOnly();

                // do work
                IAclExcelLayoutBuilder v = new AclExcelLayoutBuilder(s, f);
                v.renderAudit(session, doc);

                try {
                    v.getExcel().save(tmpFile);
                } catch (IOException e) {
                    log.error(e, e);
                }
                ComponentUtils.downloadFile(context, "rights.xls", tmpFile);
            }
        };
        runner.runUnrestricted();
    }
}
