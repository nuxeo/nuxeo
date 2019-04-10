/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.multi.tenant;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.6
 */
public class MultiTenantPrincipal extends NuxeoPrincipalImpl {

    public MultiTenantPrincipal(NuxeoPrincipalImpl principal) throws ClientException {
        super(principal.getName(), principal.isAnonymous(), principal.isAdministrator());
        setConfig(principal.getConfig());
        setModel(principal.getModel());
        setVirtualGroups(principal.getVirtualGroups());
    }

    @Override
    public String getTenantId() {
        try {
            return (String) model.getPropertyValue("user:tenantId");
        } catch (ClientException e) {
            return null;
        }
    }

}
