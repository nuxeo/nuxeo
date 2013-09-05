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
 *     dmetzler
 */
package org.nuxeo.ecm.automation.server.jaxrs.adapters;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;

import org.nuxeo.ecm.automation.jaxrs.io.documents.PaginableDocumentModelListImpl;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.ecm.webengine.model.impl.DefaultAdapter;
import org.nuxeo.runtime.api.Framework;

/**
 * Abstract adapter to be used when one want to contribute
 * an adapter base on PageProviders. In order to use it,
 * just override the {@link PaginableAdapter#getPageProviderDefinition()}
 * and {@link PaginableAdapter#getParams()}
 *
 * @since 5.7.2
 */
public abstract class PaginableAdapter extends DefaultAdapter{


    protected Long page;
    protected Long pageSize;
    protected String maxResults;

    @Override
    protected void initialize(Object... args) {
        super.initialize(args);
        final HttpServletRequest request = ctx.getRequest();

        page = extractLongParam(request, "page", 0L);
        pageSize = extractLongParam(request, "pageSize", 50L);
        maxResults = request.getParameter("maxResults");

    }

    @Override
    public <A> A getAdapter(Class<A> adapter) {
        if (adapter.isAssignableFrom(DocumentModelList.class)) {
            try {
                return adapter.cast(getDocs());
            } catch (ClientException e) {
                return null;
            }

        }
        return super.getAdapter(adapter);

    }


    abstract protected PageProviderDefinition getPageProviderDefinition();

    protected Object[] getParams() {
        return new Object[]{};
    }

    @SuppressWarnings("unchecked")
    @GET
    public DocumentModelList getDocs() throws ClientException {

        PageProviderDefinition ppDefinition = getPageProviderDefinition();

        if(ppDefinition == null) {
            throw new ClientException("Page provider given not found");
        }

        PageProviderService pps = Framework.getLocalService(PageProviderService.class);

        Map<String, Serializable> props = new HashMap<String, Serializable>();
        props.put(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY,
                (Serializable) ctx.getCoreSession());

        return new PaginableDocumentModelListImpl(
                (PageProvider<DocumentModel>) pps.getPageProvider("", ppDefinition,
                        null, null, pageSize, page, props, getParams()), "restdocid");


    }


    protected Long extractLongParam(HttpServletRequest request, String paramName,
            Long defaultValue) {
        String strParam = request.getParameter(paramName);
        return strParam == null ? defaultValue : Long.parseLong(strParam);
    }

}
