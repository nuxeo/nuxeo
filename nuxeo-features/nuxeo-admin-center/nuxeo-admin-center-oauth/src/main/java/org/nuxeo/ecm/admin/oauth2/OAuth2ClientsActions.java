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

import static org.nuxeo.ecm.platform.oauth2.clients.ClientRegistry.OAUTH2CLIENT_DIRECTORY_NAME;
import static org.nuxeo.ecm.platform.oauth2.clients.ClientRegistry.OAUTH2CLIENT_SCHEMA;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.admin.oauth.DirectoryBasedEditor;
import org.nuxeo.ecm.platform.oauth2.clients.OAuth2Client;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;

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

    public void validateClient(FacesContext context, UIComponent component, Object value) {
        if (!(value instanceof String) || !OAuth2Client.isRedirectURIValid((String) value)) {
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    ComponentUtils.translate(context, "label.oauth2.invalid.redirectURI"), null);
            throw new ValidatorException(message);
        }
    }

}
