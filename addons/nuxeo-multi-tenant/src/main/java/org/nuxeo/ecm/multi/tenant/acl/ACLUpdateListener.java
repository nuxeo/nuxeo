/*  
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat <tdelprat@nuxeo.com>
 */

package org.nuxeo.ecm.multi.tenant.acl;

import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.multi.tenant.MultiTenantHelper;
import org.nuxeo.ecm.multi.tenant.MultiTenantService;
import org.nuxeo.runtime.api.Framework;

/**
 * Intercepts ACL changes and restrict Grant to Tenant bound groups for a configured list of global groups
 * 
 * @author tiry
 */
public class ACLUpdateListener implements EventListener {

    @Override
    public void handleEvent(Event event) {

        if (DocumentEventTypes.BEFORE_DOC_SECU_UPDATE.equals(event.getName())) {

            MultiTenantService mts = Framework.getService(MultiTenantService.class);
            if (!mts.isTenantIsolationEnabled(event.getContext().getCoreSession())) {
                return;
            }
            List<String> prohibitedGroups = mts.getProhibitedGroups();

            DocumentModel target = ((DocumentEventContext) event.getContext()).getSourceDocument();
            ACP newACP = (ACP) event.getContext().getProperty(CoreEventConstants.NEW_ACP);

            for (ACL acl : newACP.getACLs()) {
                int idx = 0;
                for (ACE ace : acl.getACEs()) {
                    if (ace.isGranted() && prohibitedGroups.contains(ace.getUsername())) {
                        String tenantId = MultiTenantHelper.getOwningTenantId(target);
                        if (tenantId != null) {
                            acl.set(idx,
                                    new ACE(MultiTenantHelper.computeTenantMembersGroup(tenantId), ace.getPermission(),
                                            ace.isGranted()));
                        }
                    }
                    idx++;
                }
            }
        }
    }
}
