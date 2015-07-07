/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     dmetzler
 */
package org.nuxeo.ecm.multi.tenant;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.test.annotations.RepositoryInit;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.usermanager.exceptions.UserAlreadyExistsException;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 5.8
 */
public class MultiTenantRepositoryInit implements RepositoryInit {

    @Override
    public void populate(CoreSession session) {
        MultiTenantService mts = Framework.getLocalService(MultiTenantService.class);
        mts.enableTenantIsolation(session);

        DocumentModel domain = null;
        DocumentModel ws = null;

        for (int i = 0; i < 3; i++) {
            domain = session.createDocumentModel("/", "domain" + i, "Domain");
            domain = session.createDocument(domain);

            ws = session.createDocumentModel(domain.getPathAsString(), "ws" + i, "Workspace");
            ws = session.createDocument(ws);

            createUser("user" + i, domain.getName(), session);
        }

    }

    /**
     * @param session
     * @param string
     * @param name
     */
    protected NuxeoPrincipal createUser(String username, String tenant, CoreSession session) {
        UserManager userManager = Framework.getLocalService(UserManager.class);
        DocumentModel user = userManager.getBareUserModel();
        user.setPropertyValue("user:username", username);
        user.setPropertyValue("user:password", username);
        user.setPropertyValue("user:tenantId", tenant);
        try {
            userManager.createUser(user);
        } catch (UserAlreadyExistsException e) {
            // do nothing
        } finally {
            session.save();
        }
        return userManager.getPrincipal(username);
    }

}
