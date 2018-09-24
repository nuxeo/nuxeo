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
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.bulk.BulkService;

/**
 * @since 10.2
 */
@Operation(id = RunBulkAction.ID, category = Constants.CAT_SERVICES, label = "Run a bulk", addToStudio = true, description = "Run a bulk on a set of documents expressed by a NXQL.")
public class RunBulkAction {

    public static final String ID = "Bulk.RunAction";

    @Context
    protected BulkService service;

    @Context
    protected CoreSession session;

    @Param(name = "query", required = true)
    protected String query;

    @Param(name = "action", required = true)
    protected String action;

    @Param(name = "parameters", required = false)
    protected Map<String, Serializable> parameters = new HashMap<>();

    @OperationMethod
    public Blob run() throws IOException {
        String repositoryName = session.getRepositoryName();
        String userName = session.getPrincipal().getName();

        BulkCommand command = new BulkCommand().withRepository(repositoryName)
                                               .withAction(action)
                                               .withUsername(userName)
                                               .withQuery(query)
                                               .withParams(parameters);
        String commandId = service.submit(command);
        return Blobs.createJSONBlobFromValue(Collections.singletonMap("commandId", commandId));
    }

}
