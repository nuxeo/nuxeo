/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.multi.tenant;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.usermanager.UserManagerImpl;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.event.EventService;

/**
* Listeners invalidating the principals cache when the tenant administrators
* are changed.
*
* @since 5.9.2
*/
public class TenantAdministratorsListener implements EventListener {

    @Override
    public void handleEvent(Event event) throws ClientException {
        String eventName = event.getName();
        if (!DocumentEventTypes.BEFORE_DOC_UPDATE.equals(eventName)
                || !(event.getContext() instanceof DocumentEventContext)) {
            return;
        }

        DocumentEventContext docCtx = (DocumentEventContext) event.getContext();
        DocumentModel doc = docCtx.getSourceDocument();
        if (doc.hasFacet(Constants.TENANT_CONFIG_FACET)) {
            Property property = doc.getProperty(Constants.TENANT_ADMINISTRATORS_PROPERTY);
            if (property.isDirty()) {
                // flush the principals cache
                UserManager userManager = Framework.getLocalService(UserManager.class);
                if (MultiTenantUserManager.class.isInstance(userManager)) {
                    MultiTenantUserManager multiTenantUserManager = (MultiTenantUserManager) userManager;
                    multiTenantUserManager.invalidateAllPrincipals();
                }
            }
        }
    }

}
