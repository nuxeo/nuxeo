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
 * $Id$
 */

package org.nuxeo.ecm.platform.login.deputy.management.web;

import static org.jboss.seam.ScopeType.*;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.login.deputy.management.DeputyManager;
import org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants;
import org.nuxeo.ecm.platform.usermanager.UserManager;

@Name("deputyActions")
@Scope(ScopeType.CONVERSATION)
public class DeputyActionsBean implements Serializable {

    private static final long serialVersionUID = 23167576454986L;

    public static final String VIEW_DEPUTIES = "view_deputies";

    public static final String NEW_DEPUTY_ID = "new-deputy-id";

    @In(create = true, required = false)
    private transient DeputyManager deputyManager;

    @In(create = true)
    private transient UserManager userManager;

    @In
    private transient NuxeoPrincipal currentUser;

    @In(create = true)
    private transient Map<String, String> messages;

    // Forms parameters

    protected String adminLogin;

    // Back-end Model

    protected DocumentModel editableDeputy;

    public String createDeputy() {
        editableDeputy = deputyManager.newMandate(currentUser.getName(), null);
        return VIEW_DEPUTIES;
    }

    public String setNewDeputy(String deputyId) {
        String schemaName = deputyManager.getDeputySchemaName();
        editableDeputy.setProperty(schemaName, "deputy", deputyId);
        return VIEW_DEPUTIES;
    }

    public String cancelDeputy() {
        editableDeputy = null;
        return VIEW_DEPUTIES;
    }

    public String saveDeputy() {
        if (editableDeputy == null) {
            return null;
        }

        deputyManager.addMandate(editableDeputy);

        editableDeputy = null;

        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, messages.get("message.deputy.created"),
                messages.get("message.deputy.created"));
        FacesContext.getCurrentInstance().addMessage(null, message);

        return VIEW_DEPUTIES;
    }

    public String editDeputy(String deputyId) {
        if (deputyId == null) {
            return null;
        }

        editableDeputy = null;

        List<DocumentModel> deputies = getUserDeputies();

        String schemaName = deputyManager.getDeputySchemaName();

        for (DocumentModel deputy : deputies) {
            if (deputyId.equals((deputy.getProperty(schemaName, "deputy")))) {
                editableDeputy = deputy;
                break;
            }
        }

        if (editableDeputy != null) {
            return VIEW_DEPUTIES;
        } else {
            return null;
        }
    }

    public String deleteDeputy(String deputyId) {
        deputyManager.removeMandate(currentUser.getName(), deputyId);
        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, messages.get("message.deputy.deleted"),
                messages.get("message.deputy.deleted"));
        FacesContext.getCurrentInstance().addMessage(null, message);

        return VIEW_DEPUTIES;
    }

    @Factory(value = "userDeputies", scope = EVENT)
    public List<DocumentModel> getUserDeputies() {
        return deputyManager.getAvalaibleMandates(currentUser.getName());
    }

    @Factory(value = "alternateLogins", scope = EVENT)
    public List<NuxeoPrincipal> getAlternatePrincipals() {
        List<NuxeoPrincipal> result = new ArrayList<>();
        List<String> logins = deputyManager.getPossiblesAlternateLogins(currentUser.getName());

        for (String login : logins) {
            NuxeoPrincipal alternatePrincipal = userManager.getPrincipal(login);
            if (alternatePrincipal != null) {
                result.add(alternatePrincipal);
            }
        }

        return result;
    }

    public String loginAsDeputy(String login) throws IOException, ServletException {
        if ((!currentUser.isAdministrator())
                && (!deputyManager.getPossiblesAlternateLogins(currentUser.getName()).contains(login))) {
            return null;
        }

        if (userManager.getPrincipal(login) == null) {
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    messages.get("message.deputy.nonExistingUser"), messages.get("message.deputy.nonExistingUser"));
            FacesContext.getCurrentInstance().addMessage(null, message);
            return null;
        }

        reconnectAs(login);

        return null;
    }

    protected void reconnectAs(String login) throws ServletException, IOException {
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext eContext = context.getExternalContext();
        Object req = eContext.getRequest();
        Object resp = eContext.getResponse();
        HttpServletRequest request = null;
        HttpServletResponse response = null;
        if (req instanceof HttpServletRequest) {
            request = (HttpServletRequest) req;
        }
        if (resp instanceof HttpServletResponse) {
            response = (HttpServletResponse) resp;
        }

        if ((response != null) && (request != null) && !context.getResponseComplete()) {
            String targetURL = "/" + NXAuthConstants.SWITCH_USER_PAGE;

            request.setAttribute(NXAuthConstants.DISABLE_REDIRECT_REQUEST_KEY, true);
            if (login != null) {
                request.setAttribute(NXAuthConstants.SWITCH_USER_KEY, login);
            }

            request.getRequestDispatcher(targetURL).forward(request, response);
            context.responseComplete();
        }
    }

    public String loginAsOriginal() throws ServletException, IOException {
        reconnectAs(null);
        return null;
    }

    public boolean isMandated() {
        if (currentUser == null) {
            return false;
        }
        if (currentUser.getOriginatingUser() != null) {
            return true;
        }
        return false;
    }

    @Factory(value = "editableDeputy", scope = EVENT)
    public DocumentModel getEditableDeputy() {
        return editableDeputy;
    }

    public String getLoginInformation() {
        if (currentUser == null) {
            return "";
        }

        String originalUser = currentUser.getOriginatingUser();

        if (originalUser != null) {
            return currentUser.getName() + " " + messages.get("label.deputed.by") + " " + originalUser;
        } else {
            return currentUser.getName();
        }
    }

    public void setAdminLogin(String adminLogin) {
        this.adminLogin = adminLogin;
    }

    public String getAdminLogin() {
        return adminLogin;
    }

    public String adminLoginAsDeputy() throws IOException, ServletException {

        if (adminLogin == null) {
            return null;
        }

        if (!currentUser.isAdministrator()) {
            return null;
        }

        if (userManager.getPrincipal(adminLogin) == null) {
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_WARN,
                    messages.get("message.deputy.nonExistingUser"), messages.get("message.deputy.nonExistingUser"));
            FacesContext.getCurrentInstance().addMessage(null, message);

            return null;
        }

        reconnectAs(adminLogin);

        return null;
    }

}
