/*
 * (C) Copyright 2015-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *      André Justo
 */

package org.nuxeo.ecm.user.center;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.admin.oauth.DirectoryBasedEditor;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.platform.oauth2.providers.NuxeoOAuth2ServiceProvider;
import org.nuxeo.ecm.platform.oauth2.providers.OAuth2ServiceProviderRegistry;
import org.nuxeo.ecm.platform.oauth2.tokens.OAuth2TokenStore;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 7.3
 */
@Name("oauthUserTokens")
@Scope(ScopeType.CONVERSATION)
public class OAuth2UserTokensActions extends DirectoryBasedEditor implements Serializable {

    private static final long serialVersionUID = 1L;

    private Map<String, Serializable> filter = new HashMap<>();

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

    public DocumentModelList getProviderAccounts(String provider, boolean includeShared) {

        NuxeoOAuth2ServiceProvider serviceProvider = (NuxeoOAuth2ServiceProvider) Framework.getService(
                OAuth2ServiceProviderRegistry.class).getProvider(provider);
        OAuth2TokenStore tokenStore = serviceProvider.getCredentialDataStore();

        DocumentModelList filteredEntries = new DocumentModelListImpl();

        if (includeShared) {
            DocumentModelList tokens = tokenStore.query();
            List<String> currentUserGroups = currentUser.getAllGroups();

            for (DocumentModel entry : tokens) {
                String tokenOwner = (String) entry.getProperty(getSchemaName(), "nuxeoLogin");
                boolean isShared = (boolean) entry.getProperty(getSchemaName(), "isShared");
                String sharedWith = (String) entry.getProperty(getSchemaName(), "sharedWith");

                if (tokenOwner.equals(currentUser.getName()) || (isShared && sharedWith == null)) {
                    filteredEntries.add(entry);
                    continue;
                }

                if (!isShared || (sharedWith == null)) {
                    continue;
                }

                List<String> sharedWithList = Arrays.asList(sharedWith.split(","));

                // Iterate list of allowed groups/users
                for (String item : sharedWithList) {
                    if (item.contains(NuxeoGroup.PREFIX)) {
                        item = item.replace(NuxeoGroup.PREFIX, "");
                        if (currentUserGroups.contains(item)) {
                            filteredEntries.add(entry);
                            break;
                        }
                    }

                    if (item.contains(NuxeoPrincipal.PREFIX)) {
                        item = item.replace(NuxeoPrincipal.PREFIX, "");
                        if (item.equals(currentUser.getName())) {
                            filteredEntries.add(entry);
                            break;
                        }
                    }
                }
            }
        } else {
            filter.put("nuxeoLogin", currentUser.getName());
            filteredEntries = tokenStore.query(filter);
        }
        return filteredEntries;
    }

    public DocumentModelList getCurrentUserTokens() {
        filter.clear();
        filter.put("nuxeoLogin", currentUser.getName());
        refresh();
        return getEntries();
    }

    public List<String> getSharedWith() {
        List<String> sharedWith = new ArrayList<>();
        String sharedWithProperty = (String) editableEntry.getProperty(getSchemaName(), "sharedWith");
        if (sharedWithProperty != null) {
            sharedWith = Arrays.asList(sharedWithProperty.split(","));
        }
        return sharedWith;
    }

    public void setSharedWith(List<String> sharedWith) {
        String list = StringUtils.join(sharedWith, ",");
        editableEntry.setProperty(getSchemaName(), "sharedWith", list);
    }
}
