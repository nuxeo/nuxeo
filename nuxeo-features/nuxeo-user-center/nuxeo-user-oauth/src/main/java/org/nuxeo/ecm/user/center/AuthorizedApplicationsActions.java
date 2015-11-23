/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
import org.nuxeo.ecm.core.api.ClientException;
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

    protected DirectoryService directoryService;

    public List<DocumentModel> getAuthorizedApplications()
            throws ClientException {
        DirectoryService directoryService = getDirectoryService();
        Session session = directoryService.open(OAuthTokenStoreImpl.DIRECTORY_NAME);
        try {
            Map<String, Serializable> queryFilter = getQueryFilter();
            Set<String> emptySet = Collections.emptySet();
            return session.query(queryFilter, emptySet, null, true);
        } finally {
            session.close();
        }
    }

    protected DirectoryService getDirectoryService() throws ClientException {
        try {
            if (directoryService == null) {
                directoryService = Framework.getService(DirectoryService.class);
            }
            return directoryService;
        } catch (Exception e) {
            throw new ClientException(e);
        }
    }

    protected Map<String, Serializable> getQueryFilter() {
        Map<String, Serializable> filter = new HashMap<String, Serializable>();
        filter.put("clientToken", new Integer(0));
        filter.put("nuxeoLogin", currentUser.getName());
        return filter;
    }

    public void revokeAccess(String id) throws ClientException {
        DirectoryService directoryService = getDirectoryService();
        Session session = directoryService.open(OAuthTokenStoreImpl.DIRECTORY_NAME);
        try {
            session.deleteEntry(id);
        } finally {
            session.close();
        }
    }

}
