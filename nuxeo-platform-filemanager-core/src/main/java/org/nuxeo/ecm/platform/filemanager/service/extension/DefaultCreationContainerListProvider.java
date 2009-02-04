/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: DefaultCreationContainerListProvider.java 30594 2008-02-26 17:21:10Z ogrisel $
 */

package org.nuxeo.ecm.platform.filemanager.service.extension;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.search.api.client.querymodel.QueryModel;
import org.nuxeo.ecm.core.search.api.client.querymodel.QueryModelService;
import org.nuxeo.ecm.core.search.api.client.querymodel.descriptor.QueryModelDescriptor;
import org.nuxeo.runtime.api.Framework;

/**
 * Default contribution to the CreationContainerListProvider extension point
 * that find the list of Workspaces the user has the right to create new
 * document into.
 * <p>
 * The filtered list is sorted
 *
 * @author Olivier Grisel <ogrisel@nuxeo.com>
 */
public class DefaultCreationContainerListProvider extends
        AbstractCreationContainerListProvider {

    public static final String CONTAINER_LIST_PROVIDER_QM = "DEFAULT_CREATION_CONTAINER_LIST_PROVIDER";

    protected QueryModelService qmService;

    protected QueryModelService getQueryModelService() {
        if (qmService == null) {
            // TODO: update this to a service lookup NXP-2132
            qmService = (QueryModelService) Framework.getRuntime().getComponent(
                    QueryModelService.NAME);
        }
        return qmService;
    }

    public DocumentModelList getCreationContainerList(
            CoreSession documentManager, String docType) throws Exception {
        // fetch the CONTAINER_LIST_PROVIDER_QM query model
        QueryModelDescriptor descriptor = getQueryModelService().getQueryModelDescriptor(
                CONTAINER_LIST_PROVIDER_QM);
        // assume the QM is stateless
        QueryModel qm = new QueryModel(descriptor);
        DocumentModelList allContainers = qm.getDocuments(documentManager,
                new Object[0]);
        DocumentModelList filteredContainers = new DocumentModelListImpl();
        for (DocumentModel container : allContainers) {
            if (documentManager.hasPermission(container.getRef(),
                    SecurityConstants.ADD_CHILDREN)) {
                filteredContainers.add(container);
            }
        }
        return filteredContainers;
    }

}
