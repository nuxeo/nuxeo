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
