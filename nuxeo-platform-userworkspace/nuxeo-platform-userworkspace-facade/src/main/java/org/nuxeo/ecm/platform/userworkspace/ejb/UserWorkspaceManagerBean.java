/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     btatar
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.userworkspace.ejb;

import javax.annotation.PostConstruct;
import javax.ejb.PostActivate;
import javax.ejb.Stateless;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.runtime.api.Framework;

/**
 * @author btatar
 */
@Stateless
public class UserWorkspaceManagerBean implements UserWorkspaceService {

    private static final long serialVersionUID = -5097419897615224328L;

    UserWorkspaceService userWorkspaceService;

    @PostActivate
    @PostConstruct
    public void initialize() {
        userWorkspaceService = Framework.getLocalService(
                UserWorkspaceService.class);
    }

    public DocumentModel getCurrentUserPersonalWorkspace(String userName,
            DocumentModel currentDocument) throws ClientException {
        return userWorkspaceService.getCurrentUserPersonalWorkspace(userName,
                currentDocument);
    }

    public DocumentModel getCurrentUserPersonalWorkspace(CoreSession userCoreSession, DocumentModel context)
            throws ClientException {
         return userWorkspaceService.getCurrentUserPersonalWorkspace(userCoreSession, context);
    }
}
