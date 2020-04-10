/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     pierre
 */
package org.nuxeo.ecm.automation.core.operations.services.bulk;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.PageProviderHelper;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.bulk.BulkAdminService;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.io.BulkParameters;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;

/**
 * Automation operation that can run an http enabled Bulk Action.
 *
 * @since 10.2
 */
@Operation(id = BulkRunAction.ID, category = Constants.CAT_SERVICES, label = "Run a bulk command", addToStudio = true, description = "Run a bulk action on a set of documents expressed by a NXQL.")
public class BulkRunAction {

    private static Logger log = LogManager.getLogger(BulkRunAction.class);

    public static final String ID = "Bulk.RunAction";

    @Context
    protected BulkService service;

    @Context
    protected BulkAdminService admin;

    @Context
    protected CoreSession session;

    @Param(name = "query", required = false)
    protected String query;

    @Param(name = "providerName", required = false)
    protected String providerName;

    @Param(name = "queryParams", required = false)
    protected StringList queryParams;

    @Param(name = PageProviderService.NAMED_PARAMETERS, required = false, description = "Named parameters to pass to the page provider to "
            + "fill in query variables.")
    protected Properties namedParameters;

    @Param(name = "quickFilters", required = false, description = "Quick filter " + "properties (separated by comma)")
    protected StringList quickFilters;

    @Param(name = "action", required = true)
    protected String action;

    @Param(name = "repositoryName", required = false)
    protected String repositoryName;

    @Param(name = "bucketSize", required = false)
    protected int bucketSize;

    @Param(name = "batchSize", required = false)
    protected int batchSize;

    @Param(name = "parameters", required = false)
    protected String parametersAsJson;

    @OperationMethod(asyncService = BulkService.class)
    public BulkStatus run() throws IOException, OperationException {

        if (!admin.getActions().contains(action)) {
            throw new NuxeoException("Action '" + action + "' not found", HttpServletResponse.SC_NOT_FOUND);
        }
        if (!admin.isHttpEnabled(action) && !session.getPrincipal().isAdministrator()) {
            throw new NuxeoException("Action '" + action + "' denied", HttpServletResponse.SC_FORBIDDEN);
        }

        if (query == null && providerName == null) {
            throw new OperationException("Query and ProviderName cannot be both null");
        }

        PageProviderDefinition def = query != null ? PageProviderHelper.getQueryPageProviderDefinition(query)
                : PageProviderHelper.getPageProviderDefinition(providerName);

        if (def == null) {
            throw new OperationException("Could not get Provider Definition from either query or provider name");
        }

        PageProvider<?> provider = PageProviderHelper.getPageProvider(session, def, namedParameters, null, null, null,
                null, null, quickFilters, queryParams != null ? queryParams.toArray(new String[0]) : null);
        query = PageProviderHelper.buildQueryStringWithAggregates(provider);

        if (query.contains("?")) {
            throw new OperationException("Query parameters could not be parsed");
        }

        BulkCommand.Builder builder = new BulkCommand.Builder(action, query, session.getPrincipal().getName());
        try {
            builder.params(BulkParameters.paramsToMap(parametersAsJson));
        } catch (IOException e) {
            throw new OperationException("Could not parse parameters, expecting valid json value", e);
        }

        if (repositoryName != null) {
            builder.repository(repositoryName);
        } else {
            builder.repository(session.getRepositoryName());
        }
        if (bucketSize > 0) {
            builder.bucket(bucketSize);
        }
        if (batchSize > 0) {
            builder.batch(batchSize);
        }
        String commandId = service.submit(builder.build());
        return service.getStatus(commandId);
    }
}
