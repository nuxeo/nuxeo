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
import org.nuxeo.ecm.platform.groups.audit.service.rendering.AclExcelLayoutBuilder;
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

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected transient ResourcesAccessor resourcesAccessor;

    @In(create = true, required = false)
    protected NuxeoPrincipal currentNuxeoPrincipal;

    @In(create = true)
    protected DocumentsListsManager documentsListsManager;

    // Sample code to show how to retrieve the list of selected documents in the
    // content listing view
    protected List<DocumentModel> getCurrentlySelectedDocuments() {

        if (navigationContext.getCurrentDocument().isFolder()) {
            return documentsListsManager.getWorkingList(DocumentsListsManager.CURRENT_DOCUMENT_SELECTION);
        } else {
            return null;
        }
    }

    public String buildAndExport() throws ClientException{
    	log.info("start XLS export");
    	File tmpFile = null;
		try {
			tmpFile = File.createTempFile("rights-", ".xls");
		} catch (IOException e) {
			facesMessages.add(StatusMessage.Severity.ERROR,
					e.getMessage()
                    /*resourcesAccessor.getMessages().get("invalid_operation")*/,
                    null);
			log.error(e,e);
			return "";
		}
        tmpFile.deleteOnExit();

        buildAndDownload(tmpFile);

    	return "";
    }

	private void buildAndDownload(final File tmpFile)
			throws ClientException {
		final FacesContext context = FacesContext.getCurrentInstance();
		final DocumentModel doc = navigationContext.getCurrentDocument();

        UnrestrictedSessionRunner runner = new UnrestrictedSessionRunner(doc.getRepositoryName()) {
			@Override
			public void run() throws ClientException {
		        AclExcelLayoutBuilder v = new AclExcelLayoutBuilder();
		        v.renderAudit(session, doc);

		    	ComponentUtils.downloadFile(context, "rights.xls",
		                tmpFile);
			}
		};
		runner.runUnrestricted();
	}



	// This the method that will be called when the action button/link is
    // clicked
    public String doGet() {
        String message = "Hello from ploum : ";
        List<DocumentModel> selectedDocs = getCurrentlySelectedDocuments();
        if (selectedDocs != null) {
            message = message + " (" + selectedDocs.size()
                    + " documents selected)";
        }
        facesMessages.add(StatusMessage.Severity.INFO, message);

        // if you need to change the current document and let Nuxeo
        // select the correct view
        // you can use navigationContext and return the view
        //
        // return navigationContext.navigateToDocument(doc);

        // If you want to explicitly go to a given view
        // just return the outcome string associated to the view
        //
        // return "someView";

        // stay on the same view
        return null;
    }

    // this method will be called by the action system to determine if the
    // action should be available
    //
    // the return value can depend on the context,
    // you can use the navigationContext to get the currentDocument,
    // currentWorkspace ...
    // you can cache the value in a member variable as long as the Bean stays
    // Event scoped
    //
    // if you don't need this, you should remove the filter in the associated
    // action contribution
    public boolean accept() {
        return true;
    }
}
