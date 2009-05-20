package org.nuxeo.dam.platform.context;

import java.util.HashMap;
import java.util.Map;

import javax.faces.application.FacesMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;

@Name("documentActions")
public class DocumentActionsBean {

    private static final Log log = LogFactory.getLog(DocumentActionsBean.class);

    public DocumentModel changeableDocument;

    public DocumentModel getChangeableDocument() throws ClientException {
        createChangeableDocument();

        return changeableDocument;
    }

    public static final String BATCH_TYPE_NAME = "Workspace";

    public static final String IMPORTSET_ROOT_PATH = "/domain/import-sets";

    @In
    protected transient CoreSession documentManager;

    public CoreSession getDocumentManager() throws ClientException {
        return documentManager;
    }

    @In(create = true, required = false)
    protected FacesMessages facesMessages;

    @In(create = true, required = false)
    protected NavigationContext navigationContext;

    @In(create = true)
    // won't inject this because of seam problem after activation
    // ::protected Map<String, String> messages;
    protected ResourcesAccessor resourcesAccessor;

    public String createChangeableDocument() throws ClientException {
        Map<String, Object> context = new HashMap<String, Object>();
        context.put(CoreEventConstants.PARENT_PATH, IMPORTSET_ROOT_PATH);
        changeableDocument = getDocumentManager().createDocumentModel(
                BATCH_TYPE_NAME, context);

        return null;
    }

    public String createDocument() throws ClientException {
        String title = (String) changeableDocument.getProperty("dublincore",
                "title");
        if (title == null) {
            title = "";
        }
        String name = IdUtils.generateId(title);
        // set parent path and name for document model
        changeableDocument.setPathInfo(IMPORTSET_ROOT_PATH, name);

        changeableDocument = getDocumentManager().createDocument(
                changeableDocument);
        getDocumentManager().save();

        logDocumentWithTitle("Created the document: ", changeableDocument);
        facesMessages.add(FacesMessage.SEVERITY_INFO,
                resourcesAccessor.getMessages().get("document_saved"),
                resourcesAccessor.getMessages().get(
                        changeableDocument.getType()));

        return null;
    }

    public boolean newImportSetCreationInProgress() {
        return (changeableDocument != null);
    }

    // **********************

    public String goBack() {
        changeableDocument = null;

        return null;
    }

    /**
     * Logs a {@link DocumentModel} title and the passed string (info).
     */
    public void logDocumentWithTitle(String someLogString,
            DocumentModel document) {
        if (null != document) {
            log.trace('[' + getClass().getSimpleName() + "] " + someLogString
                    + ' ' + document.getId());
            log.debug("CURRENT DOC PATH: " + document.getPathAsString());
        } else {
            log.trace('[' + getClass().getSimpleName() + "] " + someLogString
                    + " NULL DOC");
        }
    }

}
