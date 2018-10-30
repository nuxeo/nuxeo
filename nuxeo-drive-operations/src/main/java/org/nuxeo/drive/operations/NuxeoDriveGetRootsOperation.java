/*
 * (C) Copyright 2012-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Olivier Grisel <ogrisel@nuxeo.com>
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.operations;

import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.drive.service.SynchronizationRoots;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.runtime.api.Framework;

/**
 * Fetch the list of synchronization roots for the currently authenticated user.
 */
@Operation(id = NuxeoDriveGetRootsOperation.ID, category = Constants.CAT_SERVICES, label = "Nuxeo Drive: Get Roots")
public class NuxeoDriveGetRootsOperation {

    public static final String ID = "NuxeoDrive.GetRoots";

    @Context
    protected OperationContext ctx;

    @Context
    protected CoreSession session;

    @OperationMethod
    public DocumentModelList run() {
        // By default get synchronization roots from all repositories, except if
        // a specific repository name is passed as a request header
        boolean allRepositories = true;
        HttpServletRequest request = (HttpServletRequest) ctx.get("request");
        if (request != null) {
            String respositoryName = request.getHeader(RenderingContext.REPOSITORY_NAME_REQUEST_HEADER);
            if (!StringUtils.isEmpty(respositoryName)) {
                allRepositories = false;
            }
        }
        NuxeoDriveManager driveManager = Framework.getService(NuxeoDriveManager.class);
        Map<String, SynchronizationRoots> roots = driveManager.getSynchronizationRoots(ctx.getPrincipal());
        DocumentModelList rootDocumentModels = new DocumentModelListImpl();
        for (Map.Entry<String, SynchronizationRoots> rootsEntry : roots.entrySet()) {
            if (session.getRepositoryName().equals(rootsEntry.getKey())) {
                Set<IdRef> references = rootsEntry.getValue().getRefs();
                rootDocumentModels.addAll(session.getDocuments(references.toArray(new DocumentRef[references.size()])));
            } else {
                if (allRepositories) {
                    // XXX: do we really need to implement this now?
                    throw new RuntimeException("Multi repo roots not yet implemented");
                }
            }
        }
        return rootDocumentModels;
    }

}
