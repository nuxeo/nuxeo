/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
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
            TokenAuthenticationService tokenAuthenticationService = Framework.getLocalService(TokenAuthenticationService.class);
            currentUserAuthTokenBindings = tokenAuthenticationService.getTokenBindings(currentNuxeoPrincipal.getName());
        }
        return currentUserAuthTokenBindings;
    }

    public void deleteAuthTokenBinding(String tokenId) {

        TokenAuthenticationService tokenAuthenticationService = Framework.getLocalService(TokenAuthenticationService.class);
        tokenAuthenticationService.revokeToken(tokenId);

        reset();
        facesMessages.add(StatusMessage.Severity.INFO,
                messages.get("label.tokenauth.revoked"));
    }

    public void refreshAuthTokenBindings() {
        reset();
    }

    protected void reset() {
        currentUserAuthTokenBindings = null;
    }

}
