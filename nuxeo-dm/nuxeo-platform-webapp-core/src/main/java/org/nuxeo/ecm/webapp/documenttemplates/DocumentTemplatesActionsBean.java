/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.webapp.documenttemplates;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.ScopeType.EVENT;
import static org.nuxeo.ecm.webapp.helpers.EventNames.DOCUMENT_CHILDREN_CHANGED;
import static org.nuxeo.ecm.webapp.helpers.EventNames.DOMAIN_SELECTION_CHANGED;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.faces.application.FacesMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.action.TypesTool;
import org.nuxeo.ecm.webapp.base.InputController;
import org.nuxeo.ecm.webapp.contentbrowser.DocumentActions;
import org.nuxeo.runtime.api.Framework;

/**
 * Implementation for the documentTemplatesBean component available on the
 * session.
 */
@Name("documentTemplatesActions")
@Scope(CONVERSATION)
public class DocumentTemplatesActionsBean extends InputController implements
        DocumentTemplatesActions, Serializable {

    public static final String TemplateRoot = "TemplateRoot";

    private static final Log log = LogFactory.getLog(DocumentTemplatesActionsBean.class);

    private static final long serialVersionUID = -4031259222075515590L;

    @In(create = true, required = false)
    private transient CoreSession documentManager;

    @In(required = false)
    private transient DocumentActions documentActions;

    @In(required = false)
    private TypesTool typesTool;

    @In(required = false)
    protected DocumentModel changeableDocument;

    @In(required = false, create = true)
    protected transient NavigationContext navigationContext;

    // cached list of templates
    private DocumentModelList templates;

    private String selectedTemplateId;

    private String targetType = "Workspace";

    @Destroy
    public void destroy() {
        log.debug("<destroy> ");
    }

    @Factory(value = "availableTemplates", scope = EVENT)
    public DocumentModelList templatesListFactory() {
        try {
            templates = getTemplates();
        } catch (ClientException e) {
            log.error(e);
        }
        return templates;
    }

    public DocumentModelList getTemplates(String targetTypeName)
            throws ClientException {
        if (documentManager == null) {
            log.error("Unable to access documentManager");
            return null;
        }
        DocumentModelList tl = documentManager.getChildren(
                navigationContext.getCurrentDomain().getRef(), TemplateRoot);

        if (tl.isEmpty()) {
            templates = tl;
        } else {
            templates = documentManager.getChildren(tl.get(0).getRef(),
                    targetTypeName);
            List<DocumentModel> deleted = new ArrayList<DocumentModel>();
            for (Iterator<DocumentModel> it = templates.iterator(); it.hasNext();) {
                DocumentModel current = it.next();
                if (LifeCycleConstants.DELETED_STATE.equals(current.getCurrentLifeCycleState())) {
                    deleted.add(current);
                }
            }
            templates.removeAll(deleted);
        }
        return templates;
    }

    public DocumentModelList getTemplates() throws ClientException {
        if (targetType == null || targetType.equals("")) {
            targetType = typesTool.getSelectedType().getId();
        }
        return getTemplates(targetType);
    }

    public String createDocumentFromTemplate(DocumentModel doc,
            String templateId) throws ClientException {
        selectedTemplateId = templateId;
        return createDocumentFromTemplate(doc);
    }

    public String createDocumentFromTemplate(DocumentModel doc)
            throws ClientException {

        if (documentManager == null) {
            log.error("Unable to access documentManager");
            return null;
        }

        // Currently templating works with Workspace only
        // Hardcoded that way.

        if (selectedTemplateId == null || selectedTemplateId.equals("")) {
            if (documentActions != null) {
                return documentActions.saveDocument(doc);
            } else {
                log.error("Unable to find documentActions");
                return null;
            }
        }

        // Remove this once it is available from the context
        DocumentRef currentDocRef = navigationContext.getCurrentDocument().getRef();

        try {
            PathSegmentService pss;
            try {
                pss = Framework.getService(PathSegmentService.class);
            } catch (Exception e) {
                throw new ClientException(e);
            }
            String name = pss.generatePathSegment(doc);
            DocumentModel created = documentManager.copy(new IdRef(selectedTemplateId), currentDocRef,
                    name);

            // Update from user input.
            // This part is for now harcoded for Workspace type.
            String title = (String) doc.getProperty("dublincore", "title");
            created.setProperty("dublincore", "title", title);

            String descr = (String) doc.getProperty("dublincore", "description");
            if (descr.length() != 0) {
                created.setProperty("dublincore", "description", descr);
            }

            Blob blob = (Blob) doc.getProperty("file", "content");
            if (blob != null) {
                created.setProperty("file", "content", blob);
                String fname = (String) doc.getProperty("file", "filename");
                created.setProperty("file", "filename", fname);
            }

            created = documentManager.saveDocument(created);
            documentManager.save();

            selectedTemplateId = "";

            logDocumentWithTitle("Created the document: ", created);
            facesMessages.add(FacesMessage.SEVERITY_INFO,
                    resourcesAccessor.getMessages().get("document_saved"),
                    resourcesAccessor.getMessages().get(created.getType()));
            return navigationContext.navigateToDocument(created, "after-create");
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    public String createDocumentFromTemplate() throws ClientException {
        return createDocumentFromTemplate(changeableDocument);
    }

    public String getSelectedTemplateId() {
        return selectedTemplateId;
    }

    public void setSelectedTemplateId(String requestedId) {
        selectedTemplateId = requestedId;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    @Observer(value = { DOCUMENT_CHILDREN_CHANGED }, create = false)
    @BypassInterceptors
    public void documentChildrenChanged(DocumentModel targetDoc) {
        // refresh if a child was added to template root
        if (targetDoc != null && targetDoc.getType().equals(TemplateRoot)
                && templates != null) {
            templates.clear();
        }
    }

    @Observer(value = { DOMAIN_SELECTION_CHANGED }, create = false)
    @BypassInterceptors
    public void domainChanged(DocumentModel targetDoc) {
        if (templates != null) {
            templates.clear();
        }
    }

}
