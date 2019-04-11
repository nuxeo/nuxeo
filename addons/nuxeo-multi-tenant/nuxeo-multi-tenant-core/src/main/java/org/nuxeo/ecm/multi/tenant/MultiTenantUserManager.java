/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.multi.tenant;

import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.computedgroups.UserManagerWithComputedGroups;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.6
 */
public class MultiTenantUserManager extends UserManagerWithComputedGroups {

    private static final long serialVersionUID = 1L;

    @Override
    protected NuxeoPrincipal makePrincipal(DocumentModel userEntry, boolean anonymous, List<String> groups)
            {
        NuxeoPrincipal nuxeoPrincipal = super.makePrincipal(userEntry, anonymous, groups);
        if (nuxeoPrincipal instanceof NuxeoPrincipalImpl) {
            nuxeoPrincipal = new MultiTenantPrincipal((NuxeoPrincipalImpl) nuxeoPrincipal);
        }
        return nuxeoPrincipal;
    }

    @Override
    protected boolean useCache() {
        // The default UserManager cache return only NuxeoPrincipalImpl so we can not use it, see NXP-19669.
        return false;
    }
}
