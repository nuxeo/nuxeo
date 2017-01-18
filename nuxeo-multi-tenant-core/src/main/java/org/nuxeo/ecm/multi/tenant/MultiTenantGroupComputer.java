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

import static org.nuxeo.ecm.multi.tenant.Constants.POWER_USERS_GROUP;
import static org.nuxeo.ecm.multi.tenant.Constants.TENANT_ADMINISTRATORS_PROPERTY;
import static org.nuxeo.ecm.multi.tenant.MultiTenantHelper.computeTenantAdministratorsGroup;
import static org.nuxeo.ecm.multi.tenant.MultiTenantHelper.computeTenantMembersGroup;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.platform.computedgroups.AbstractGroupComputer;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.6
 */
public class MultiTenantGroupComputer extends AbstractGroupComputer {

    @Override
    public List<String> getGroupsForUser(final NuxeoPrincipalImpl nuxeoPrincipal) {
        final List<String> groups = new ArrayList<String>();
        final String tenantId = (String) nuxeoPrincipal.getModel().getPropertyValue("user:tenantId");
        if (!StringUtils.isBlank(tenantId)) {
            String defaultRepositoryName = Framework.getLocalService(RepositoryManager.class).getDefaultRepositoryName();

            boolean transactionStarted = false;
            if (!TransactionHelper.isTransactionActive()) {
                TransactionHelper.startTransaction();
                transactionStarted = true;
            }
            try {
                new UnrestrictedSessionRunner(defaultRepositoryName) {
                    @Override
                    public void run() {

                        String query = String.format("SELECT * FROM Document WHERE tenantconfig:tenantId = '%s'",
                                tenantId);
                        List<DocumentModel> docs = session.query(query);
                        if (!docs.isEmpty()) {
                            DocumentModel tenant = docs.get(0);
                            List<String> tenantAdministrators = (List<String>) tenant.getPropertyValue(TENANT_ADMINISTRATORS_PROPERTY);
                            if (tenantAdministrators.contains(nuxeoPrincipal.getName())) {
                                groups.add(computeTenantAdministratorsGroup(tenantId));
                                groups.add(POWER_USERS_GROUP);
                            }
                            groups.add(computeTenantMembersGroup(tenantId));
                        }
                    }
                }.runUnrestricted();
            } finally {
                if (transactionStarted) {
                    TransactionHelper.commitOrRollbackTransaction();
                }
            }
        }
        return groups;
    }

    @Override
    public List<String> getAllGroupIds() {
        return null;
    }

    @Override
    public List<String> getGroupMembers(String s) {
        return null;
    }

    @Override
    public List<String> getParentsGroupNames(String s) {
        return null;
    }

    @Override
    public List<String> getSubGroupsNames(String s) {
        return null;
    }

}
