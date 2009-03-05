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
 *     <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.publishing.rules;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.PermissionProvider;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.publishing.api.AbstractNuxeoCoreValidatorsRule;
import org.nuxeo.ecm.platform.publishing.api.PublishingValidatorException;
import org.nuxeo.runtime.api.Framework;

/**
 * Default NXP validator.
 * <p>
 * Validators here will be principals having manage everything rights in the
 * sections where the document has been published.
 *
 * @author Julien Anguenot
 */
public class DefaultValidatorsRule extends AbstractNuxeoCoreValidatorsRule {

    private static final long serialVersionUID = 1L;

    public String[] computesValidatorsFor(DocumentModel dm)
            throws PublishingValidatorException {
        try {
            login();
            initializeCoreSession(dm.getRepositoryName());
        } catch (Exception e) {
            throw new PublishingValidatorException(e);
        }

        ACP acp;
        try {
            acp = session.getACP(dm.getRef());
        } catch (ClientException ce) {
            throw new PublishingValidatorException(ce);
        }

        /*NXP-1822 Rux: instead of looking for users which have particularly EVERYTHING
         * permission, collect the users which are allowed to accept publishing: with at
         * least WRITE permission
         */
        /*NXP-1981 Rux: use the exported API instead of Core internal SecurityService. The
         * code has to be duplicated, but this way at least we can keep the multi-server
         * deployment working. Instead of using SecurityService.getPermissionsToCheck(),
         * I am replicating the code here based on PermissionProvider.getPermissionsGroups
         * in order to have the same business logic.
         */
//        SecurityService secuService = NXCore.getSecurityService();
//        Set<String> requiredPermissions = new HashSet<String>(
//                Arrays.asList(secuService.getPermissionsToCheck(SecurityConstants.WRITE)));
        PermissionProvider permProvider;
        try {
            permProvider = Framework.getService(PermissionProvider.class);
        } catch (Exception e) {
            throw new PublishingValidatorException(e);
        }

        Set<String> requiredPermissions = new HashSet<String>();
        requiredPermissions.addAll(Arrays.asList(permProvider.getPermissionGroups(
                SecurityConstants.WRITE)));
        requiredPermissions.add(SecurityConstants.WRITE);

        /*Rux: Everything is not added!!! Go workaround*/
        requiredPermissions.add(SecurityConstants.EVERYTHING);
        //String[] reviewers = acp.listUsernamesForPermission(SecurityConstants.EVERYTHING);
        String[] reviewers = acp.listUsernamesForAnyPermission(requiredPermissions);

        try {
            closeCoreSession();
            logout();
        } catch (Exception e) {
            throw new PublishingValidatorException(e);
        }

        return reviewers;
    }

}
