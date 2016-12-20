/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.core.Events;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.CoreSession.CopyOption;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.action.TypesTool;
import org.nuxeo.ecm.webapp.base.InputController;
import org.nuxeo.ecm.webapp.contentbrowser.DocumentActions;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.runtime.api.Framework;

/**
 * Implementation for the documentTemplatesBean component available on the session.
 */
@Name("documentTemplatesActions")
@Scope(CONVERSATION)
public class DocumentTemplatesActionsBean extends InputController implements DocumentTemplatesActions, Serializable {

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

    @Override
    @Factory(value = "availableTemplates", scope = EVENT)
    public DocumentModelList templatesListFactory() {
        templates = getTemplates();
        return templates;
    }

    @Override
    public DocumentModelList getTemplates(String targetTypeName) {
        if (documentManager == null) {
            log.error("Unable to access documentManager");
            return null;
        }

        String query = "SELECT * FROM Document where ecm:primaryType = '%s' AND ecm:path STARTSWITH %s";
        DocumentModelList tl = documentManager.query(String.format(query, TemplateRoot,
                NXQL.escapeString(navigationContext.getCurrentDomainPath())));

        if (tl.isEmpty()) {
            templates = tl;
        } else {
            templates = documentManager.getChildren(tl.get(0).getRef(), targetTypeName);
            List<DocumentModel> deleted = new ArrayList<>();
            for (DocumentModel current : templates) {
                if (LifeCycleConstants.DELETED_STATE.equals(current.getCurrentLifeCycleState())) {
                    deleted.add(current);
                }
            }
            templates.removeAll(deleted);
        }
        return templates;
    }

    @Override
    public DocumentModelList getTemplates() {
        if (targetType == null || targetType.equals("")) {
            targetType = typesTool.getSelectedType().getId();
        }
        return getTemplates(targetType);
    }

    @Override
    public String createDocumentFromTemplate(DocumentModel doc, String templateId) {
        selectedTemplateId = templateId;
        return createDocumentFromTemplate(doc);
    }

    @Override
    public String createDocumentFromTemplate(DocumentModel doc) {

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

        PathSegmentService pss = Framework.getService(PathSegmentService.class);
        String name = pss.generatePathSegment(doc);
        DocumentModel created = documentManager.copy(new IdRef(selectedTemplateId), currentDocRef, name,
                CopyOption.RESET_CREATOR);

        // Update from user input.
        // This part is for now harcoded for Workspace type.
        String title = (String) doc.getProperty("dublincore", "title");
        created.setProperty("dublincore", "title", title);

        String descr = (String) doc.getProperty("dublincore", "description");
        created.setProperty("dublincore", "description", descr);

        Blob blob = (Blob) doc.getProperty("file", "content");
        if (blob != null) {
            created.setProperty("file", "content", blob);
        }

        created = documentManager.saveDocument(created);
        documentManager.save();

        selectedTemplateId = "";

        logDocumentWithTitle("Created the document: ", created);
        facesMessages.add(StatusMessage.Severity.INFO, resourcesAccessor.getMessages().get("document_saved"),
                resourcesAccessor.getMessages().get(created.getType()));
        Events.instance().raiseEvent(EventNames.DOCUMENT_CHILDREN_CHANGED, currentDocument);
        return navigationContext.navigateToDocument(created, "after-create");
    }

    @Override
    public String createDocumentFromTemplate() {
        return createDocumentFromTemplate(changeableDocument);
    }

    @Override
    public String getSelectedTemplateId() {
        return selectedTemplateId;
    }

    @Override
    public void setSelectedTemplateId(String requestedId) {
        selectedTemplateId = requestedId;
    }

    @Override
    public String getTargetType() {
        return targetType;
    }

    @Override
    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    @Override
    @Observer(value = { DOCUMENT_CHILDREN_CHANGED }, create = false)
    @BypassInterceptors
    public void documentChildrenChanged() {
        if (templates != null) {
            templates.clear();
        }
    }

    @Override
    @Observer(value = { DOMAIN_SELECTION_CHANGED }, create = false)
    @BypassInterceptors
    public void domainChanged() {
        if (templates != null) {
            templates.clear();
        }
    }

}
