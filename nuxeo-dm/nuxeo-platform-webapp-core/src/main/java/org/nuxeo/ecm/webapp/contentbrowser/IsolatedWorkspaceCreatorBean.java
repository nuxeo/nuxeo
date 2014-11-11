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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.webapp.contentbrowser;

import static org.jboss.seam.ScopeType.STATELESS;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import org.jboss.annotation.ejb.SerializedConcurrentAccess;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.webapp.documenttemplates.DocumentTemplatesActions;
import org.nuxeo.ecm.webapp.security.SecurityActions;

@Name("isolatedWorkspaceCreator")
@Scope(STATELESS)
@SerializedConcurrentAccess
public class IsolatedWorkspaceCreatorBean {

    @In(create = true)
    protected transient Principal currentUser;

    @In(create = true)
    protected transient DocumentTemplatesActions documentTemplatesActions;

    @In(create = true)
    protected transient UserManager userManager;

    @In(create = true)
    protected transient SecurityActions securityActions;

    public String createIsolatedWorkspace() throws ClientException {

        String result = documentTemplatesActions.createDocumentFromTemplate();
        // String result = documentActions.saveDocument();
        List<String> principalsName = new ArrayList<String>();
        principalsName.add(currentUser.getName());
        principalsName.addAll(userManager.getAdministratorsGroups());

        // Grant to principalList
        for (String principalName : principalsName) {
            securityActions.addPermission(principalName,
                    SecurityConstants.EVERYTHING, true);
        }

        // DENY at root
        securityActions.addPermission(SecurityConstants.EVERYONE,
                SecurityConstants.EVERYTHING, false);
        securityActions.updateSecurityOnDocument();

        return result;
    }

}
