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
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;

@Name("importActions")
public class ImportActionsBean {

    private static final Log log = LogFactory.getLog(ImportActionsBean.class);

    public DocumentModel newImportSet;

    public static final String BATCH_TYPE_NAME = "ImportSet";

    public static final String IMPORTSET_ROOT_PATH = "/domain/import-sets";

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true, required = false)
    protected FacesMessages facesMessages;

    @In(create = true)
    // won't inject this because of seam problem after activation
    // ::protected Map<String, String> messages;
    protected ResourcesAccessor resourcesAccessor;

    public DocumentModel getImportSet() throws ClientException {
        if (newImportSet == null) {
            Map<String, Object> context = new HashMap<String, Object>();
            context.put(CoreEventConstants.PARENT_PATH, IMPORTSET_ROOT_PATH);
            newImportSet = documentManager.createDocumentModel(BATCH_TYPE_NAME,
                    context);
        }

        return newImportSet;
    }

    public String createImportSet() throws ClientException {
        String title = (String) newImportSet.getProperty("dublincore", "title");
        if (title == null) {
            title = "";
        }
        String name = IdUtils.generateId(title);
        // set parent path and name for document model
        newImportSet.setPathInfo(IMPORTSET_ROOT_PATH, name);

        newImportSet = documentManager.createDocument(newImportSet);
        documentManager.save();

        logDocumentWithTitle("Created the document: ", newImportSet);
        facesMessages.add(FacesMessage.SEVERITY_INFO,
                resourcesAccessor.getMessages().get("document_saved"),
                resourcesAccessor.getMessages().get(newImportSet.getType()));

        invalidateImportContext();
        return "nxstartup";
    }

    public String cancel() {
        invalidateImportContext();

        return "nxstartup";
    }

    public void invalidateImportContext() {
        newImportSet = null;
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
