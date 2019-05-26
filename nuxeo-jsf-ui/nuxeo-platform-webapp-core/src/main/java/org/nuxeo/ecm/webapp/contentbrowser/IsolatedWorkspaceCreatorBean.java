/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.webapp.contentbrowser;

import static org.jboss.seam.ScopeType.*;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.webapp.documenttemplates.DocumentTemplatesActions;
import org.nuxeo.ecm.webapp.security.SecurityActions;

@Name("isolatedWorkspaceCreator")
@Scope(STATELESS)
public class IsolatedWorkspaceCreatorBean {

    @In(create = true)
    protected transient Principal currentUser;

    @In(create = true)
    protected transient DocumentTemplatesActions documentTemplatesActions;

    @In(create = true)
    protected transient UserManager userManager;

    @In(create = true)
    protected transient SecurityActions securityActions;

    public String createIsolatedWorkspace() {

        String result = documentTemplatesActions.createDocumentFromTemplate();
        // String result = documentActions.saveDocument();
        List<String> principalsName = new ArrayList<>();
        principalsName.add(currentUser.getName());
        principalsName.addAll(userManager.getAdministratorsGroups());

        // Grant to principalList
        for (String principalName : principalsName) {
            securityActions.addPermission(principalName, SecurityConstants.EVERYTHING, true);
        }

        // DENY at root
        securityActions.addPermission(SecurityConstants.EVERYONE, SecurityConstants.EVERYTHING, false);
        securityActions.updateSecurityOnDocument();

        return result;
    }

}
