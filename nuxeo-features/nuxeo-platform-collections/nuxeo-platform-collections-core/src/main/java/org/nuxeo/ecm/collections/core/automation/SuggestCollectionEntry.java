/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package org.nuxeo.ecm.collections.core.automation;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.collections.api.CollectionConstants;
import org.nuxeo.ecm.collections.api.CollectionManager;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.ecm.platform.ui.select2.common.Select2Common;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 5.9.3
 */
@Operation(id = SuggestCollectionEntry.ID, category = Constants.CAT_SERVICES, label = "Get collection suggestion", description = "Get the collection list accessible by the current user. This is returning a blob containing a serialized JSON array..", addToStudio = false)
public class SuggestCollectionEntry {

    public static final String ID = "Collection.Suggestion";

    private static final String PATH = "path";

    @Param(name = "currentPageIndex", required = false)
    protected Integer currentPageIndex = 0;

    @Param(name = "pageSize", required = false)
    protected Integer pageSize = 20;

    @Context
    protected OperationContext ctx;

    @Context
    protected CoreSession session;

    @Context
    protected CollectionManager collectionManager;

    @Param(name = "searchTerm", required = false)
    protected String searchTerm;

    @OperationMethod
    public Blob run() throws ClientException {
        JSONArray result = new JSONArray();
        Map<String, Serializable> props = new HashMap<String, Serializable>();
        props.put(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY,
                (Serializable) session);
        PageProviderService pps = Framework.getLocalService(PageProviderService.class);
        Object[] paramaters = new Object[1];
        paramaters[0] = searchTerm + (searchTerm.endsWith("%") ? "" : "%");
        Long targetPageSize = Long.valueOf(pageSize.longValue());
        Long targetPage = Long.valueOf(currentPageIndex.longValue());
        List<DocumentModel> docs = (DocumentModelList) pps.getPageProvider(
                CollectionConstants.COLLECTION_PAGE_PROVIDER,
                null,
                targetPageSize,
                targetPage,
                props,
                paramaters).getCurrentPage();

        boolean found = false;
        for (DocumentModel doc : docs) {
            JSONObject obj = new JSONObject();
            if (collectionManager.canAddToCollection(doc, session)) {
                obj.element(Select2Common.ID, doc.getId());
            }
            if (doc.getTitle().equals(searchTerm)) {
                found = true;
            }
            obj.element(Select2Common.LABEL, doc.getTitle());
            obj.element(PATH, doc.getPath().toString());
            result.add(obj);
        }

        if (!found && StringUtils.isNotBlank(searchTerm)) {
            JSONObject obj = new JSONObject();
            obj.element(Select2Common.LABEL, searchTerm);
            obj.element(Select2Common.ID, CollectionConstants.MAGIC_PREFIX_ID + searchTerm);
            result.add(0, obj);
        }

        return new StringBlob(result.toString(), "application/json");
    }
}
