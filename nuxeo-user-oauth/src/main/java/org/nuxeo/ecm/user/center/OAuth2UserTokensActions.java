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

import org.apache.commons.lang.StringUtils;
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
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.platform.oauth2.tokens.OAuth2TokenStore;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

/**
 *
 * @since 7.3
 */
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
        DocumentModelList filteredEntries = new DocumentModelListImpl();
        List<String> currentUserGroups = currentUser.getAllGroups();

        for (DocumentModel entry : super.getEntries()) {
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
