/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.ecm.admin.oauth;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.platform.oauth.tokens.NuxeoOAuthToken;
import org.nuxeo.ecm.platform.oauth.tokens.OAuthTokenStoreImpl;

@Name("oauthConsumersTokensActions")
@Scope(ScopeType.CONVERSATION)
public class OAuthConsumersTokensActionBean extends DirectoryBasedEditor {

    private static final long serialVersionUID = 1L;

    @Override
    protected Map<String, Serializable> getQueryFilter() {
        Map<String, Serializable> filter = new HashMap<>();
        filter.put("clientToken", 0);
        return filter;
    }

    @Override
    protected String getDirectoryName() {
        return OAuthTokenStoreImpl.DIRECTORY_NAME;
    }

    @Override
    protected String getSchemaName() {
        return NuxeoOAuthToken.SCHEMA;
    }

}
