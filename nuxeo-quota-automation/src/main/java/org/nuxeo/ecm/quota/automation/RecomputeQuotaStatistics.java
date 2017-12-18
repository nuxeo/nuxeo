/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     arameshkumar
 *     Florent Guillaume
 */
package org.nuxeo.ecm.quota.automation;

import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.ecm.quota.QuotaStatsService;
import org.nuxeo.runtime.api.Framework;

/**
 * Recomputes Quota size statistics.
 *
 * @since 10.1
 */
@Operation(id = RecomputeQuotaStatistics.ID, category = "Quotas", //
        label = "Recompute quota statistics on documents, optionally only for a tenant, user or path")
public class RecomputeQuotaStatistics {

    public static final String ID = "Quotas.RecomputeStatistics";

    public static final String SIZE_UPDATER = "documentsSizeUpdater";

    @Param(name = "tenantId", required = false)
    protected String tenantId;

    @Param(name = "username", required = false)
    protected String username;

    @Param(name = "path", required = false)
    protected String path;

    @Param(name = "updaterName", required = false)
    protected String updaterName;

    @Context
    protected CoreSession session;

    @Context
    protected QuotaStatsService quotaStatsService;

    @OperationMethod
    public String run() throws OperationException {
        String docPath;
        if (tenantId != null) {
            if (username != null || path != null) {
                throw new OperationException("Only one of tenantId, username or path can be defined");
            }
            docPath = getTenantPath();
        } else if (username != null) {
            if (path != null) {
                throw new OperationException("Only one of tenantId, username or path can be defined");
            }
            docPath = getUserPersonalWorkspacePath();
        } else {
            docPath = path; // may be null
        }
        if (updaterName == null) {
            updaterName = SIZE_UPDATER;
        }
        String repositoryName = session.getRepositoryName();
        quotaStatsService.launchInitialStatisticsComputation(updaterName, repositoryName, docPath);
        return quotaStatsService.getProgressStatus(updaterName, repositoryName);
    }

    protected String getTenantPath() throws OperationException {
        // TODO this should use the multi-tenant service instead of assuming that tenants are per-domain
        if (tenantId.contains("/")) {
            throw new OperationException("Invalid tenantId: " + tenantId);
        }
        return "/" + tenantId;
    }

    protected String getUserPersonalWorkspacePath() throws OperationException {
        UserWorkspaceService uws = Framework.getService(UserWorkspaceService.class);
        DocumentModel userWorkspace = uws.getUserPersonalWorkspace(username, session.getRootDocument());
        if (userWorkspace == null) {
            throw new OperationException("Invalid username or missing user workspace: " + username);
        }
        return userWorkspace.getPathAsString();
    }

}
