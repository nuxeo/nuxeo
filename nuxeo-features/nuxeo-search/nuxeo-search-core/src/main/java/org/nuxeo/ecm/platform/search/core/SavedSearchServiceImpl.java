/*
 * (C) Copyright 2016-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Gabriel Barata <gbarata@nuxeo.com>
 */
package org.nuxeo.ecm.platform.search.core;

import java.io.IOException;
import java.security.Principal;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 8.3
 */
public class SavedSearchServiceImpl implements SavedSearchService {

    @Override
    public SavedSearch createSavedSearch(CoreSession session, String title, String queryParams,
            Map<String, String> namedParams, String query, String queryLanguage, String pageProviderName,
            Long pageSize, Long currentPageIndex, Long maxResults, String sortBy, String sortOrder,
            String contentViewData) throws InvalidSearchParameterException, IOException {
        if (StringUtils.isEmpty(title)) {
            throw new InvalidSearchParameterException("title cannot be empty");
        }

        if ((!StringUtils.isEmpty(query) || !StringUtils.isEmpty(queryLanguage))
                && !StringUtils.isEmpty(pageProviderName)) {
            throw new InvalidSearchParameterException("query and page provider parameters are mutually exclusive"
                    + " (query, queryLanguage, pageProviderName)");
        }

        if (StringUtils.isEmpty(query) && StringUtils.isEmpty(queryLanguage)
                && StringUtils.isEmpty(pageProviderName)) {
            throw new InvalidSearchParameterException("query or page provider parameters are missing"
                    + " (query, queryLanguage, pageProviderName)");
        }

        if (!StringUtils.isEmpty(query) && StringUtils.isEmpty(queryLanguage)) {
            throw new InvalidSearchParameterException("queryLanguage parameter is missing");
        }

        if (StringUtils.isEmpty(query) && !StringUtils.isEmpty(queryLanguage)) {
            throw new InvalidSearchParameterException("query parameter is missing");
        }

        UserWorkspaceService userWorkspaceService = Framework.getService(UserWorkspaceService.class);
        DocumentModel uws = userWorkspaceService.getCurrentUserPersonalWorkspace(session);

        String searchDocumentType = (!StringUtils.isEmpty(pageProviderName)) ? Framework.getService(
                PageProviderService.class).getPageProviderDefinition(pageProviderName).getSearchDocumentType()
                : null;

        DocumentModel savedSearchDoc = session.createDocumentModel(uws.getPathAsString(), title,
                searchDocumentType != null ? searchDocumentType
                        : SavedSearchConstants.PARAMETERIZED_SAVED_SEARCH_TYPE_NAME);

        SavedSearch savedSearch = savedSearchDoc.getAdapter(SavedSearch.class);
        savedSearch.setTitle(title);
        savedSearch.setQueryParams(queryParams);
        savedSearch.setNamedParams(namedParams);
        savedSearch.setQuery(query);
        savedSearch.setQueryLanguage(queryLanguage);
        savedSearch.setPageProviderName(pageProviderName);
        savedSearch.setPageSize(pageSize);
        savedSearch.setCurrentPageIndex(currentPageIndex);
        savedSearch.setMaxResults(maxResults);
        savedSearch.setSortBy(sortBy);
        savedSearch.setSortOrder(sortOrder);
        savedSearch.setContentViewData(contentViewData);

        savedSearchDoc = session.createDocument(savedSearchDoc);
        savedSearch = savedSearchDoc.getAdapter(SavedSearch.class);

        ACP acp = savedSearchDoc.getACP();
        ACL acl = acp.getOrCreateACL(ACL.LOCAL_ACL);
        Principal principal = session.getPrincipal();
        if (principal != null) {
            acl.add(new ACE(principal.getName(), SecurityConstants.EVERYTHING, true));
        }

        acp.addACL(acl);
        savedSearchDoc.setACP(acp, true);

        return savedSearch;
    }

    @Override
    public SavedSearch getSavedSearch(CoreSession session, String id) {
        DocumentRef docRef = new IdRef(id);
        DocumentModel savedSearchDoc = session.getDocument(docRef);
        if (savedSearchDoc != null) {
            return savedSearchDoc.getAdapter(SavedSearch.class);
        }
        return null;
    }

    @Override
    public SavedSearch saveSavedSearch(CoreSession session, SavedSearch search) throws InvalidSearchParameterException,
            IOException {
        if (StringUtils.isEmpty(search.getTitle())) {
            throw new InvalidSearchParameterException("title cannot be empty");
        }

        if ((!StringUtils.isEmpty(search.getQuery()) || !StringUtils.isEmpty(search.getQueryLanguage()))
                && !StringUtils.isEmpty(search.getPageProviderName())) {
            throw new InvalidSearchParameterException("query and page provider parameters are mutually exclusive"
                    + " (query, queryLanguage, pageProviderName)");
        }

        if (StringUtils.isEmpty(search.getQuery()) && StringUtils.isEmpty(search.getQueryLanguage())
                && StringUtils.isEmpty(search.getPageProviderName())) {
            throw new InvalidSearchParameterException("query or page provider parameters are missing"
                    + " (query, queryLanguage, pageProviderName)");
        }

        if (!StringUtils.isEmpty(search.getQuery()) && StringUtils.isEmpty(search.getQueryLanguage())) {
            throw new InvalidSearchParameterException("queryLanguage parameter is missing");
        }

        if (StringUtils.isEmpty(search.getQuery()) && !StringUtils.isEmpty(search.getQueryLanguage())) {
            throw new InvalidSearchParameterException("query parameter is missing");
        }

        DocumentModel doc = session.saveDocument(search.getDocument());
        search.setDocument(doc);

        return search;
    }

    @Override
    public void deleteSavedSearch(CoreSession session, SavedSearch search) {
        session.removeDocument(new IdRef(search.getId()));
    }
}
