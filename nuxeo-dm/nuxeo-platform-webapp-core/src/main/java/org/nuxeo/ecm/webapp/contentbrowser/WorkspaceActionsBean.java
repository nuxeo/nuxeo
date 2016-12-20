/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.webapp.contentbrowser;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Begin;
import org.jboss.seam.annotations.End;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.core.Manager;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.el.ContextStringWrapper;
import org.nuxeo.ecm.platform.ui.web.util.BaseURL;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.webapp.base.InputController;
import org.nuxeo.ecm.webapp.documenttemplates.DocumentTemplatesActions;
import org.nuxeo.ecm.webapp.security.PrincipalListManager;
import org.nuxeo.ecm.webapp.security.SecurityActions;
import org.nuxeo.runtime.api.Framework;

/**
 * Action listener that deals with operations with the workspaces.
 * <p>
 * This action listener handles the workspace creation wizard.
 *
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 */
@Name("workspaceActions")
@Scope(CONVERSATION)
public class WorkspaceActionsBean extends InputController implements WorkspaceActions, Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(WorkspaceActionsBean.class);

    @In(create = true, required = false)
    private transient CoreSession documentManager;

    @In(create = true)
    private transient Principal currentUser;

    @In(required = false)
    private transient DocumentActions documentActions;

    @In(create = true)
    private transient PrincipalListManager principalListManager;

    @In(create = true)
    protected transient UserManager userManager;

    @In(create = true)
    private transient SecurityActions securityActions;

    @In(value = "org.jboss.seam.core.manager")
    private transient Manager conversationManager;

    // Wizard implementation : driven by PageFlow addWorkspace

    private Boolean useTemplateFlag;

    @In(required = false)
    private transient DocumentModel tmpWorkspace;

    @In(create = true)
    private transient DocumentModelList availableTemplates;

    // inject wizard vars
    @In(value = "#{selectedTemplateId.value}", required = false)
    private transient String selectedTemplateId;

    @In(value = "#{selectedSecurityModel.value}", required = false)
    private transient String selectedSecurityModel;

    @In(value = "#{selectedOwnerModel.value}", required = false)
    private transient String selectedOwnerModel;

    @In(create = true)
    private transient DocumentTemplatesActions documentTemplatesActions;

    public String cancel() {
        return "view_workspaces";
    }

    // Flag for indicating if template will be used
    @Override
    public void setUseTemplate(Boolean value) {
        useTemplateFlag = value;
    }

    @Override
    public Boolean getUseTemplate() {
        if (availableTemplates == null) {
            return false;
        }
        if (useTemplateFlag == null) {
            return false;
        }
        return useTemplateFlag;
    }

    // initialize pageflow context using Factories
    @Override
    @Factory(value = "selectedTemplateId")
    public ContextStringWrapper FactoryForSelectedTemplateId() {
        return new ContextStringWrapper("none");
    }

    @Override
    @Factory(value = "selectedSecurityModel")
    public ContextStringWrapper FactoryForSelectSecurityModel() {
        return new ContextStringWrapper("inherit");
    }

    @Override
    @Factory(value = "selectedOwnerModel")
    public ContextStringWrapper FactoryForSelectSecurityOwner() {
        return new ContextStringWrapper("me");
    }

    @Override
    @Begin(pageflow = "createWorkspace", nested = true)
    @Factory(value = "tmpWorkspace")
    public DocumentModel getTmpWorkspace() {
        if (tmpWorkspace == null) {
            tmpWorkspace = documentManager.createDocumentModel("Workspace");
        }
        return tmpWorkspace;
    }

    @Override
    @Factory(value = "availableWorkspaceTemplates")
    public DocumentModelList getTemplates() {
        availableTemplates = documentTemplatesActions.getTemplates("Workspace");
        return availableTemplates;
    }

    @Override
    public String finishPageFlow() {
        return "done";
    }

    @Override
    public String getSelectedTemplateDescription() {
        if (selectedTemplateId == null) {
            return "";
        }
        if (selectedTemplateId.equals("none")) {
            return "no_template";
        }
        for (DocumentModel t : getTemplates()) {
            if (selectedTemplateId.equals(t.getId())) {
                return (String) t.getProperty("dublincore", "description");
            }
        }
        return "";
    }

    @Override
    public DocumentModel getSelectedTemplate() {
        if (selectedTemplateId == null) {
            return null;
        }
        if (selectedTemplateId.equals("none")) {
            return null;
        }
        for (DocumentModel t : getTemplates()) {
            if (selectedTemplateId.equals(t.getId())) {
                return t;
            }
        }
        return null;
    }

    // @End(beforeRedirect = true)
    @Override
    @End
    public String createWorkspace() {
        String navResult = null;

        if (useTemplateFlag == null || !useTemplateFlag || selectedTemplateId == null
                || selectedTemplateId.equals("none")) {
            // create the new Workspace without Template
            // and navigate to it
            navResult = documentTemplatesActions.createDocumentFromTemplate(tmpWorkspace, null);
        } else {
            // create the workspace from template
            navResult = documentTemplatesActions.createDocumentFromTemplate(tmpWorkspace, selectedTemplateId);
        }

        if (!selectedSecurityModel.equals("inherit")) {
            List<String> principalsName;

            // get principals list to apply rights to
            if (selectedOwnerModel.equals("me")) {
                principalsName = new ArrayList<String>();
                principalsName.add(currentUser.getName());
            } else {
                principalsName = principalListManager.getSelectedUsers();
            }

            // Force addition of administrators groups
            principalsName.addAll(userManager.getAdministratorsGroups());

            // Grant to principalList
            for (String principalName : principalsName) {
                securityActions.addPermission(principalName, SecurityConstants.EVERYTHING, true);
            }

            // DENY at root
            securityActions.addPermission(SecurityConstants.EVERYONE, SecurityConstants.EVERYTHING, false);
            securityActions.updateSecurityOnDocument();
        }

        String res = navigationContext.navigateToDocument(navigationContext.getCurrentDocument());
        navigationContext.setChangeableDocument(navigationContext.getCurrentDocument());
        return res;
    }

    @End(beforeRedirect = true)
    public String createWorkspaceOld() {
        String navResult = null;

        if (!useTemplateFlag || selectedTemplateId.equals("none")) {
            // create the new Workspace without Template
            // and navigate to it
            navResult = documentActions.saveDocument(tmpWorkspace);
        } else {
            // create the workspace from template

            DocumentRef currentDocRef = navigationContext.getCurrentDocument().getRef();

            // duplicate the template
            String title = (String) tmpWorkspace.getProperty("dublincore", "title");
            String name = Framework.getService(PathSegmentService.class).generatePathSegment(title);
            documentManager.copy(new IdRef(selectedTemplateId), currentDocRef, name);
            DocumentModel created = documentManager.getChild(currentDocRef, name);

            // Update from user input.
            created.setProperty("dublincore", "title", title);
            String descr = (String) tmpWorkspace.getProperty("dublincore", "description");
            if (!descr.equals("")) {
                created.setProperty("dublincore", "description", descr);
            }
            Blob blob = (Blob) tmpWorkspace.getProperty("file", "content");
            if (blob != null) {
                created.setProperty("file", "content", blob);
            }

            created = documentManager.saveDocument(created);
            documentManager.save();

            // navigate to the newly created doc
            navResult = navigationContext.navigateToDocument(created, "after-create");
        }

        if (!selectedSecurityModel.equals("inherit")) {
            List<String> principalsName;

            // get principals list to apply rights to
            if (selectedOwnerModel.equals("me")) {
                principalsName = new ArrayList<String>();
                principalsName.add(currentUser.getName());
            } else {
                principalsName = principalListManager.getSelectedUsers();
            }

            // Grant to principalList
            for (String principalName : principalsName) {
                securityActions.addPermission(principalName, SecurityConstants.EVERYTHING, true);
            }

            // Force addition of administrators groups
            principalsName.addAll(userManager.getAdministratorsGroups());

            // DENY at root
            securityActions.addPermission(SecurityConstants.EVERYONE, SecurityConstants.EVERYTHING, false);
            securityActions.updateSecurityOnDocument();
        }

        String res = navigationContext.navigateToDocument(navigationContext.getCurrentDocument());
        navigationContext.setChangeableDocument(navigationContext.getCurrentDocument());
        return res;
    }

    @Override
    @End(beforeRedirect = true)
    public String exitWizard() {
        return navigationContext.navigateToDocument(navigationContext.getCurrentDocument());
    }

    @Override
    public String getA4JHackingURL() {
        String url = BaseURL.getBaseURL() + "wizards/createWorkspace/a4jUploadHack.faces?";
        url = conversationManager.encodeConversationId(url);
        return url;
    }

}
