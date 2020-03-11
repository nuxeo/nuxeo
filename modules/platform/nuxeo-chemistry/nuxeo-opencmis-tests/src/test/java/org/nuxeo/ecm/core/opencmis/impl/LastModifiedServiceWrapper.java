/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.core.opencmis.impl;

import java.math.BigInteger;
import java.util.GregorianCalendar;

import javax.servlet.http.HttpServletResponse;

import org.apache.chemistry.opencmis.commons.data.ExtensionsData;
import org.apache.chemistry.opencmis.commons.data.ObjectInFolderList;
import org.apache.chemistry.opencmis.commons.enums.IncludeRelationships;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.chemistry.opencmis.commons.impl.DateTimeHelper;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.server.CmisService;
import org.apache.chemistry.opencmis.server.support.wrapper.AbstractCmisServiceWrapper;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoCmisService;

/**
 * Test service wrapper that adds "Last-Modified" header to GetChildren NavigationService response.
 *
 * @since 6.0
 */
public class LastModifiedServiceWrapper extends AbstractCmisServiceWrapper {

    public LastModifiedServiceWrapper(CmisService service) {
        super(service);
    }

    @Override
    public ObjectInFolderList getChildren(String repositoryId, String folderId, String filter, String orderBy,
            Boolean includeAllowableActions, IncludeRelationships includeRelationships, String renditionFilter,
            Boolean includePathSegment, BigInteger maxItems, BigInteger skipCount, ExtensionsData extension) {
        GregorianCalendar lastModified;
        DocumentModel doc = getDocumentModel(folderId);
        lastModified = (GregorianCalendar) doc.getPropertyValue("dc:modified");

        ObjectInFolderList children = getWrappedService().getChildren(repositoryId, folderId, filter, orderBy,
                includeAllowableActions, includeRelationships, renditionFilter, includePathSegment, maxItems,
                skipCount, extension);

        String lastModifiedResHeader = DateTimeHelper.formatHttpDateTime(lastModified);
        setResponseHeader("Last-Modified", lastModifiedResHeader);
        return children;
    }

    private DocumentModel getDocumentModel(String id) {
        NuxeoCmisService nuxeoCmisService = NuxeoCmisService.extractFromCmisService(this);
        CoreSession coreSession = nuxeoCmisService.getCoreSession();
        DocumentRef docRef = new IdRef(id);
        if (!coreSession.exists(docRef)) {
            throw new CmisObjectNotFoundException(docRef.toString());
        }
        DocumentModel doc = coreSession.getDocument(docRef);
        if (nuxeoCmisService.isFilteredOut(doc)) {
            throw new CmisObjectNotFoundException(docRef.toString());
        }
        return doc;
    }

    private void setResponseHeader(String headerName, String headerValue) {
        HttpServletResponse response = (HttpServletResponse) getCallContext().get(CallContext.HTTP_SERVLET_RESPONSE);
        response.setHeader(headerName, headerValue);
    }

}
