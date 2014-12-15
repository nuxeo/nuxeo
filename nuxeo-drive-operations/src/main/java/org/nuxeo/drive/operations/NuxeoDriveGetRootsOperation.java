/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Olivier Grisel <ogrisel@nuxeo.com>
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.operations;

import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.drive.service.SynchronizationRoots;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
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
    public DocumentModelList run() throws ClientException {

        // By default get synchronization roots from all repositories, except if
        // a specific repository name is passed as a request header
        boolean allRepositories = true;
        HttpServletRequest request = (HttpServletRequest) ctx.get("request");
        if (request != null) {
            String respositoryName = request.getHeader("X-NXRepository");
            if (!StringUtils.isEmpty(respositoryName)) {
                allRepositories = false;
            }
        }
        NuxeoDriveManager driveManager = Framework.getLocalService(NuxeoDriveManager.class);
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
