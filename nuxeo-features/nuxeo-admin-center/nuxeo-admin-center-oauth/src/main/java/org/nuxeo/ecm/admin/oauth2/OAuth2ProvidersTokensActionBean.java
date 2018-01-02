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
 *      Nelson Silva
 */
package org.nuxeo.ecm.admin.oauth2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.admin.oauth.DirectoryBasedEditor;
import org.nuxeo.ecm.platform.oauth2.tokens.OAuth2TokenStore;

@Name("oauth2ProvidersTokensActions")
@Scope(ScopeType.CONVERSATION)
public class OAuth2ProvidersTokensActionBean extends DirectoryBasedEditor {

    private static final long serialVersionUID = 1L;

    @Override
    protected String getDirectoryName() {
        return OAuth2TokenStore.DIRECTORY_NAME;
    }

    @Override
    protected String getSchemaName() {
        return "oauth2Token";
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
