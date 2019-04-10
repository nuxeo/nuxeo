/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.multi.tenant;

import static org.jboss.seam.ScopeType.STATELESS;
import static org.jboss.seam.annotations.Install.FRAMEWORK;
import static org.nuxeo.ecm.multi.tenant.Constants.TENANT_ID_PROPERTY;

import java.io.Serializable;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

import org.apache.commons.lang3.StringUtils;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.ecm.webapp.directory.DirectoryUIActionsBean;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.6
 */
@Name("multiTenantActions")
@Scope(STATELESS)
@Install(precedence = FRAMEWORK)
public class MultiTenantActions implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String TENANT_ADMINISTRATORS_VALIDATION_ERROR = "label.tenant.administrators.validation.error";

    @In(create = true)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected NavigationContext navigationContext;

    @In(create = true)
    protected DirectoryUIActionsBean directoryUIActions;

    public List<DocumentModel> getTenants() {
        MultiTenantService multiTenantService = Framework.getService(MultiTenantService.class);
        return multiTenantService.getTenants();
    }

    public boolean isTenantIsolationEnabled() {
        MultiTenantService multiTenantService = Framework.getService(MultiTenantService.class);
        return multiTenantService.isTenantIsolationEnabled(documentManager);
    }

    public void enableTenantIsolation() {
        MultiTenantService multiTenantService = Framework.getService(MultiTenantService.class);
        multiTenantService.enableTenantIsolation(documentManager);
    }

    public void disableTenantIsolation() {
        MultiTenantService multiTenantService = Framework.getService(MultiTenantService.class);
        multiTenantService.disableTenantIsolation(documentManager);
    }

    public boolean isReadOnlyDirectory(String directoryName) {
        MultiTenantService multiTenantService = Framework.getService(MultiTenantService.class);
        if (multiTenantService.isTenantIsolationEnabled(documentManager)) {
            if (multiTenantService.isTenantAdministrator(documentManager.getPrincipal())) {
                DirectoryService directoryService = Framework.getService(DirectoryService.class);
                return !directoryService.getDirectory(directoryName).isMultiTenant();
            }
        }
        return directoryUIActions.isReadOnly(directoryName);
    }

    @SuppressWarnings("unchecked")
    public void validateTenantAdministrators(FacesContext context, UIComponent component, Object value) {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        String currentDocumentTenantId = (String) currentDocument.getPropertyValue(TENANT_ID_PROPERTY);
        NuxeoPrincipal currentUser = documentManager.getPrincipal();
        String currentUserTenantId = currentUser.getTenantId();
        if (!StringUtils.isBlank(currentDocumentTenantId) && !StringUtils.isBlank(currentUserTenantId)
                && currentUserTenantId.equals(currentDocumentTenantId)) {
            String administratorGroup = MultiTenantHelper.computeTenantAdministratorsGroup(currentDocumentTenantId);
            if (currentUser.isMemberOf(administratorGroup)) {
                List<String> users = (List<String>) value;
                if (!users.contains(currentUser.getName())) {
                    FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            ComponentUtils.translate(context, TENANT_ADMINISTRATORS_VALIDATION_ERROR), null);
                    throw new ValidatorException(message);
                }
            }
        }
    }

}
