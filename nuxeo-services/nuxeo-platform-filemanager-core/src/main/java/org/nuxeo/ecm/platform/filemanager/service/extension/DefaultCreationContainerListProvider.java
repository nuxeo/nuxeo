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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
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

    protected PageProviderService ppService;

    protected PageProviderService getPageProviderService() {
        if (ppService == null) {
            ppService = Framework.getLocalService(PageProviderService.class);
        }
        return ppService;
    }

    @SuppressWarnings("unchecked")
    public DocumentModelList getCreationContainerList(
            CoreSession documentManager, String docType) throws Exception {
        Map<String, Serializable> props = new HashMap<String, Serializable>();
        props.put(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY,
                (Serializable) documentManager);

        PageProvider<DocumentModel> allContainers = (PageProvider<DocumentModel>) getPageProviderService().getPageProvider(
                CONTAINER_LIST_PROVIDER_QM, null, null, null, props);
        DocumentModelList filteredContainers = new DocumentModelListImpl();
        for (DocumentModel container : allContainers.getCurrentPage()) {
            if (documentManager.hasPermission(container.getRef(),
                    SecurityConstants.ADD_CHILDREN)) {
                filteredContainers.add(container);
            }
        }
        return filteredContainers;
    }

}
