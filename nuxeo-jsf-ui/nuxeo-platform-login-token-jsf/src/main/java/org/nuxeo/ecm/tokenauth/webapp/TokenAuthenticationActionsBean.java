/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer
 */

package org.nuxeo.ecm.tokenauth.webapp;

import java.io.Serializable;
import java.util.Map;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.tokenauth.service.TokenAuthenticationService;
import org.nuxeo.runtime.api.Framework;

/**
 * Manages user's authentication token bindings.
 *
 * @author Antoine Taillefer (ataillefer@nuxeo.com)
 * @since 5.7
 */
@Name("tokenAuthenticationActions")
@Scope(ScopeType.CONVERSATION)
public class TokenAuthenticationActionsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @In(create = true)
    private transient NuxeoPrincipal currentNuxeoPrincipal;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected Map<String, String> messages;

    protected DocumentModelList currentUserAuthTokenBindings;

    public DocumentModelList getCurrentUserAuthTokenBindings() {

        if (currentUserAuthTokenBindings == null) {
            TokenAuthenticationService tokenAuthenticationService = Framework.getService(TokenAuthenticationService.class);
            currentUserAuthTokenBindings = tokenAuthenticationService.getTokenBindings(currentNuxeoPrincipal.getName());
        }
        return currentUserAuthTokenBindings;
    }

    public void deleteAuthTokenBinding(String tokenId) {

        TokenAuthenticationService tokenAuthenticationService = Framework.getService(TokenAuthenticationService.class);
        tokenAuthenticationService.revokeToken(tokenId);

        reset();
        facesMessages.add(StatusMessage.Severity.INFO, messages.get("label.tokenauth.revoked"));
    }

    public void deleteAllTokenBindings() throws PropertyException {
        reset();
        TokenAuthenticationService tokenAuthenticationService = Framework.getService(TokenAuthenticationService.class);
        for (DocumentModel tokenBinding : getCurrentUserAuthTokenBindings()) {
            String tokenId = (String) tokenBinding.getPropertyValue("authtoken:token");
            tokenAuthenticationService.revokeToken(tokenId);

        }
        reset();
        facesMessages.add(StatusMessage.Severity.INFO, messages.get("label.tokenauth.revoked"));
    }

    public void refreshAuthTokenBindings() {
        reset();
    }

    protected void reset() {
        currentUserAuthTokenBindings = null;
    }

}
