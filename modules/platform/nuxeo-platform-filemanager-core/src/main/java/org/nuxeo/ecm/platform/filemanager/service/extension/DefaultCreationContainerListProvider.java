/*
 * (C) Copyright 2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Default contribution to the CreationContainerListProvider extension point that find the list of Workspaces the user
 * has the right to create new document into.
 * <p>
 * The filtered list is sorted
 *
 * @author Olivier Grisel <ogrisel@nuxeo.com>
 */
public class DefaultCreationContainerListProvider extends AbstractCreationContainerListProvider {

    public static final String CONTAINER_LIST_PROVIDER_QM = "DEFAULT_CREATION_CONTAINER_LIST_PROVIDER";

    /**
     * @deprecated since 11.1. Use {@link Framework#getService(Class)} with {@link PageProviderService} instead.
     */
    @Deprecated
    protected PageProviderService ppService;

    /**
     * @deprecated since 11.1. Use {@link Framework#getService(Class)} with {@link PageProviderService} instead.
     */
    @Deprecated
    protected PageProviderService getPageProviderService() {
        if (ppService == null) {
            ppService = Framework.getService(PageProviderService.class);
        }
        return ppService;
    }

    @Override
    @SuppressWarnings("unchecked")
    public DocumentModelList getCreationContainerList(CoreSession documentManager, String docType) {
        Map<String, Serializable> props = new HashMap<>();
        props.put(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY, (Serializable) documentManager);

        PageProviderService pageProviderService = Framework.getService(PageProviderService.class);

        PageProvider<DocumentModel> allContainers = (PageProvider<DocumentModel>) pageProviderService.getPageProvider(
                CONTAINER_LIST_PROVIDER_QM, null, null, null, props);
        DocumentModelList filteredContainers = new DocumentModelListImpl();
        for (DocumentModel container : allContainers.getCurrentPage()) {
            if (documentManager.hasPermission(container.getRef(), SecurityConstants.ADD_CHILDREN)) {
                filteredContainers.add(container);
            }
        }
        return filteredContainers;
    }

}
