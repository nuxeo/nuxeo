/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.ecm.core.opencmis.impl;

import java.math.BigInteger;
import java.util.GregorianCalendar;

import javax.servlet.http.HttpServletResponse;

import org.apache.chemistry.opencmis.commons.data.ExtensionsData;
import org.apache.chemistry.opencmis.commons.data.ObjectInFolderList;
import org.apache.chemistry.opencmis.commons.enums.IncludeRelationships;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.chemistry.opencmis.commons.impl.DateTimeHelper;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.server.CmisService;
import org.apache.chemistry.opencmis.server.support.wrapper.AbstractCmisServiceWrapper;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoCmisService;

/**
 * Test service wrapper that adds "Last-Modified" header to GetChildren
 * NavigationService response.
 *
 * @since 5.9.6
 */
public class LastModifiedServiceWrapper extends AbstractCmisServiceWrapper {

    public LastModifiedServiceWrapper(CmisService service) {
        super(service);
    }

    @Override
    public ObjectInFolderList getChildren(String repositoryId, String folderId,
            String filter, String orderBy, Boolean includeAllowableActions,
            IncludeRelationships includeRelationships, String renditionFilter,
            Boolean includePathSegment, BigInteger maxItems,
            BigInteger skipCount, ExtensionsData extension) {
        GregorianCalendar lastModified;
        try {
            DocumentModel doc = getDocumentModel(folderId);
            lastModified = (GregorianCalendar) doc.getPropertyValue("dc:modified");
        } catch (ClientException e) {
            throw new CmisRuntimeException(e.toString(), e);
        }

        ObjectInFolderList children = getWrappedService().getChildren(
                repositoryId, folderId, filter, orderBy,
                includeAllowableActions, includeRelationships, renditionFilter,
                includePathSegment, maxItems, skipCount, extension);

        String lastModifiedResHeader = DateTimeHelper.formatHttpDateTime(lastModified);
        setResponseHeader("Last-Modified", lastModifiedResHeader);
        return children;
    }

    private DocumentModel getDocumentModel(String id) throws ClientException {
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
        HttpServletResponse response = (HttpServletResponse) getCallContext().get(
                CallContext.HTTP_SERVLET_RESPONSE);
        response.setHeader(headerName, headerValue);
    }

}
