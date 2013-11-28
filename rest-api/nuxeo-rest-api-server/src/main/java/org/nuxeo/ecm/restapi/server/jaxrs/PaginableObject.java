/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.restapi.server.jaxrs;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.ecm.automation.core.util.Paginable;
import org.nuxeo.ecm.automation.core.util.PaginablePageProvider;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

/**
 * Paginable WebObject.
 * <p>
 * To be extended by WebObject returning paginable entries based on a
 * {@link PageProvider}.
 *
 * @since 5.8
 */
public abstract class PaginableObject<T> extends DefaultObject {

    protected Long currentPageIndex;

    protected Long pageSize;

    protected String maxResults;

    @Override
    protected void initialize(Object... args) {
        super.initialize(args);

        final HttpServletRequest request = ctx.getRequest();
        currentPageIndex = extractLongParam(request, "currentPageIndex", 0L);
        pageSize = extractLongParam(request, "pageSize", 50L);
        maxResults = request.getParameter("maxResults");
    }

    protected abstract PageProviderDefinition getPageProviderDefinition();

    protected Object[] getParams() {
        return new Object[] {};
    }

    protected DocumentModel getSearchDocument() throws ClientException {
        return null;
    }

    @SuppressWarnings("unchecked")
    public Paginable<T> getPaginableEntries() throws ClientException {
        PageProviderDefinition ppDefinition = getPageProviderDefinition();
        if (ppDefinition == null) {
            throw new ClientException("Page provider given not found");
        }

        PageProviderService pps = Framework.getLocalService(PageProviderService.class);
        Map<String, Serializable> props = new HashMap<String, Serializable>();
        props.put(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY,
                (Serializable) ctx.getCoreSession());

        return getPaginableEntries((PageProvider<T>) pps.getPageProvider("",
                ppDefinition, getSearchDocument(), null, pageSize,
                currentPageIndex, props, getParams()));
    }

    protected Paginable<T> getPaginableEntries(PageProvider<T> pageProvider) {
        return new PaginablePageProvider<>(pageProvider);
    }

    protected Long extractLongParam(HttpServletRequest request,
            String paramName, Long defaultValue) {
        String strParam = request.getParameter(paramName);
        return strParam == null ? defaultValue : Long.parseLong(strParam);
    }

}
