/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *      André Justo
 */

package org.nuxeo.ecm.user.center;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.admin.oauth.DirectoryBasedEditor;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.oauth2.tokens.OAuth2TokenStore;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Name("oauthUserTokens")
@Scope(ScopeType.CONVERSATION)
public class OAuth2UserTokensActions extends DirectoryBasedEditor implements Serializable {

    private static final long serialVersionUID = 1L;

    private Map<String, Serializable> filter = new HashMap<String, Serializable>();

    @In(create = true)
    protected NuxeoPrincipal currentUser;

    @Override
    protected String getDirectoryName() {
        return OAuth2TokenStore.DIRECTORY_NAME;
    }

    @Override
    protected String getSchemaName() {
        return "oauth2Token";
    }

    @Override
    protected Map<String, Serializable> getQueryFilter() {
        return filter;
    }

    public DocumentModelList getProviderAccounts(String provider) {
        filter.clear();
        filter.put("serviceName", provider);
        refresh();
        return getEntries();
    }

    public DocumentModelList getCurrentUserTokens() {
        filter.clear();
        filter.put("nuxeoLogin", currentUser.getName());
        refresh();
        return getEntries();
    }
}
