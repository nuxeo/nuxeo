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
 *     Thomas Roger
 */

package org.nuxeo.ecm.restapi.server.jaxrs;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.ecm.automation.core.util.Paginable;
import org.nuxeo.ecm.automation.core.util.PaginablePageProvider;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

/**
 * Paginable WebObject.
 * <p>
 * To be extended by WebObject returning paginable entries based on a {@link PageProvider}.
 *
 * @since 5.8
 */
public abstract class PaginableObject<T> extends DefaultObject {

    protected Long currentPageIndex;

    protected Long offset;

    protected Long pageSize;

    protected String maxResults;

    @Override
    protected void initialize(Object... args) {
        super.initialize(args);

        final HttpServletRequest request = ctx.getRequest();
        currentPageIndex = extractLongParam(request, "currentPageIndex", 0L);
        String offsetParam = request.getParameter("offset");
        offset = offsetParam == null ? null : Long.parseLong(offsetParam);
        pageSize = extractLongParam(request, "pageSize", 50L);
        maxResults = request.getParameter("maxResults");
    }

    protected abstract PageProviderDefinition getPageProviderDefinition();

    protected Object[] getParams() {
        return new Object[] {};
    }

    protected DocumentModel getSearchDocument() {
        return null;
    }

    @SuppressWarnings("unchecked")
    public Paginable<T> getPaginableEntries() {
        PageProviderDefinition ppDefinition = getPageProviderDefinition();
        if (ppDefinition == null) {
            throw new NuxeoException("Page provider given not found");
        }

        PageProviderService pps = Framework.getService(PageProviderService.class);
        Map<String, Serializable> props = new HashMap<>();
        props.put(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY, (Serializable) ctx.getCoreSession());

        return getPaginableEntries((PageProvider<T>) pps.getPageProvider("", ppDefinition, getSearchDocument(), null,
                pageSize, currentPageIndex, offset, props, null, null, getParams()));
    }

    protected Paginable<T> getPaginableEntries(PageProvider<T> pageProvider) {
        return new PaginablePageProvider<>(pageProvider);
    }

    protected Long extractLongParam(HttpServletRequest request, String paramName, Long defaultValue) {
        String strParam = request.getParameter(paramName);
        return strParam == null ? defaultValue : Long.parseLong(strParam);
    }

}
