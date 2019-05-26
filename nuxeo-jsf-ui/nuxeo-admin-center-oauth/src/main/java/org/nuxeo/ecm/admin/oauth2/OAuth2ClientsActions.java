/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Arnaud Kervern
 */
package org.nuxeo.ecm.admin.oauth2;

import static org.nuxeo.ecm.platform.oauth2.clients.OAuth2ClientService.OAUTH2CLIENT_DIRECTORY_NAME;
import static org.nuxeo.ecm.platform.oauth2.clients.OAuth2ClientService.OAUTH2CLIENT_SCHEMA;

import java.util.Arrays;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.admin.oauth.DirectoryBasedEditor;
import org.nuxeo.ecm.platform.oauth2.clients.OAuth2Client;
import org.nuxeo.ecm.platform.oauth2.clients.OAuth2ClientService;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 5.9.2
 */
@Name("oauth2ClientsActions")
@Scope(ScopeType.CONVERSATION)
public class OAuth2ClientsActions extends DirectoryBasedEditor {

    private static final long serialVersionUID = 1L;

    @Override
    protected String getDirectoryName() {
        return OAUTH2CLIENT_DIRECTORY_NAME;
    }

    @Override
    protected String getSchemaName() {
        return OAUTH2CLIENT_SCHEMA;
    }

    public void validateRedirectURIs(FacesContext context, UIComponent component, Object value) {
        if (!(value instanceof String)) {
            handleValidationError(context, "label.oauth2.missing.redirectURI");
        }
        List<String> redirectURIs = Arrays.asList(((String) value).split(","));
        if (redirectURIs.isEmpty()) {
            handleValidationError(context, "label.oauth2.missing.redirectURI");
        }
        redirectURIs.stream().map(String::trim).forEach(redirectURI -> {
            if (redirectURI.isEmpty()) {
                handleValidationError(context, "label.oauth2.empty.redirectURI");
            }
            if (!OAuth2Client.isRedirectURIValid(redirectURI)) {
                handleValidationError(context, "label.oauth2.invalid.redirectURIs");
            }
        });
    }

    protected void handleValidationError(FacesContext context, String label) {
        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, ComponentUtils.translate(context, label),
                null);
        throw new ValidatorException(message);
    }

    public void validateClientId(FacesContext context, UIComponent component, Object value) {
        if (!(component instanceof UIInput && value instanceof String)) {
            return;
        }
        Object currentValue = ((UIInput) component).getValue();
        if (currentValue != null && currentValue.equals(value)) {
            return;
        }
        OAuth2ClientService clientService = Framework.getService(OAuth2ClientService.class);
        if (clientService.hasClient((String) value)) {
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    ComponentUtils.translate(context, "label.oauth2.existing.clientId"), null);
            throw new ValidatorException(message);
        }
    }
}
