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
 * Contributors:
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.user.center;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.oauth.tokens.OAuthTokenStoreImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
@Name("authorizedApplicationsActions")
@Scope(CONVERSATION)
public class AuthorizedApplicationsActions implements Serializable {

    private static final long serialVersionUID = 1L;

    @In
    protected NuxeoPrincipal currentUser;

    public List<DocumentModel> getAuthorizedApplications() {
        DirectoryService directoryService = Framework.getService(DirectoryService.class);
        try (Session session = directoryService.open(OAuthTokenStoreImpl.DIRECTORY_NAME)) {
            Map<String, Serializable> queryFilter = getQueryFilter();
            Set<String> emptySet = Collections.emptySet();
            return session.query(queryFilter, emptySet, null, true);
        }
    }

    protected Map<String, Serializable> getQueryFilter() {
        Map<String, Serializable> filter = new HashMap<String, Serializable>();
        filter.put("clientToken", new Integer(0));
        filter.put("nuxeoLogin", currentUser.getName());
        return filter;
    }

    public void revokeAccess(String id) {
        DirectoryService directoryService = Framework.getService(DirectoryService.class);
        try (Session session = directoryService.open(OAuthTokenStoreImpl.DIRECTORY_NAME)) {
            session.deleteEntry(id);
        }
    }

}
