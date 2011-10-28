/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *    Mariana Cedica
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.suggestbox.jsf;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.platform.contentview.jsf.ContentView;
import org.nuxeo.ecm.platform.contentview.seam.ContentViewActions;
import org.nuxeo.ecm.platform.faceted.search.jsf.FacetedSearchActions;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.ecm.platform.suggestbox.utils.DateMatcher;
import org.nuxeo.ecm.platform.types.adapter.TypeInfo;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;
import org.nuxeo.ecm.platform.ui.web.invalidations.DocumentContextBoundActionBean;
import org.nuxeo.ecm.virtualnavigation.action.MultiNavTreeManager;
import org.nuxeo.ecm.webapp.security.GroupManagementActions;
import org.nuxeo.ecm.webapp.security.UserManagementActions;
import org.nuxeo.ecm.webapp.security.UserSuggestionActionsBean;
import org.nuxeo.runtime.api.Framework;

@Name("suggestboxActions")
@Scope(CONVERSATION)
@AutomaticDocumentBasedInvalidation
public class SuggestboxActions extends
        DocumentContextBoundActionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String FACETED_SEARCH_SUGGESTION = "DEFAULT_DOCUMENT_SUGGESTION";

    public static final String FACETED_SEARCH_DEFAULT_CONTENT_VIEW_NAME = "faceted_search_default";

    public static final String FACETED_SEARCH_DEFAULT_DOCUMENT_TYPE = "FacetedSearchDefault";

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected FacetedSearchActions facetedSearchActions;

    @In(create = true)
    protected ContentViewActions contentViewActions;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true)
    protected transient UserSuggestionActionsBean userSuggestionActions;

    @In(create = true)
    protected UserManagementActions userManagementActions;

    @In(create = true)
    protected GroupManagementActions groupManagementActions;

    @In(create = true)
    protected MultiNavTreeManager multiNavTreeManager;

    public DocumentModel getDocumentModel(String id) throws ClientException {
        return documentManager.getDocument(new IdRef(id));
    }

    protected String searchKeywords = "";

    public String getSearchKeywords() {
        return searchKeywords;
    }

    public void setSearchKeywords(String searchKeywords) {
        this.searchKeywords = searchKeywords;
    }

    @SuppressWarnings("unchecked")
    public List<SearchBoxSuggestion> getSuggestions(Object input)
            throws ClientException {
        List<SearchBoxSuggestion> suggestions = new ArrayList<SearchBoxSuggestion>();
        if (input == null) {
            return suggestions;
        }
        try {
            // Document repository related suggestions
            PageProviderService ppService = Framework.getService(PageProviderService.class);
            Map<String, Serializable> props = new HashMap<String, Serializable>();
            props.put(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY,
                    (Serializable) documentManager);
            PageProvider<DocumentModel> pp = (PageProvider<DocumentModel>) ppService.getPageProvider(
                    FACETED_SEARCH_SUGGESTION, null, null, null, props,
                    new Object[] { input });
            for (DocumentModel doc : pp.getCurrentPage()) {
                suggestions.add(SearchBoxSuggestion.forDocument(doc));
            }

            // Users and groups related suggestions
            List<SearchBoxSuggestion> searchBoxByAuthor = new ArrayList<SearchBoxSuggestion>();
            for (DocumentModel user : getUsersSuggestions(input)) {
                suggestions.add(SearchBoxSuggestion.forUser(user));
                searchBoxByAuthor.add(SearchBoxSuggestion.forDocumentsByAuthor(user));
            }
            for (DocumentModel group : getGroupSuggestions(input)) {
                suggestions.add(SearchBoxSuggestion.forGroup(group));
            }
            suggestions.addAll(searchBoxByAuthor);

            // Handle date related suggestions
            DateMatcher matcher = DateMatcher.fromInput(input.toString());
            if (matcher != null && matcher.hasMatch()) {
                Calendar date = matcher.getDateSuggestion();
                suggestions.add(SearchBoxSuggestion.forDocumentsCreatedAfterDate(date));
                suggestions.add(SearchBoxSuggestion.forDocumentsModifiedAfterDate(date));
                suggestions.add(SearchBoxSuggestion.forDocumentsCreatedBeforeDate(date));
                suggestions.add(SearchBoxSuggestion.forDocumentsModifiedBeforeDate(date));
                // TODO: handle date ranges too
            }

            // always add the classical full-text search
            suggestions.add(SearchBoxSuggestion.forDocumentsByKeyWords(input.toString()));
        } catch (Exception e) {
            throw new ClientException(e);
        }
        return suggestions;
    }

    public String handleSelection(String suggestionType, String suggestionValue)
            throws ClientException, ParseException {
        setSearchKeywords("");
        DateFormat df = new SimpleDateFormat(
                SearchBoxSuggestion.DATE_FORMAT_PATTERN);
        if (suggestionType.equals(SearchBoxSuggestion.DOCUMENT_SUGGESTION)) {
            navigationContext.navigateToRef(new IdRef(suggestionValue));
            return "view_documents";
        } else if (suggestionType.equals(SearchBoxSuggestion.USER_SUGGESTION)) {
            return userManagementActions.viewUser(suggestionValue);
        } else if (suggestionType.equals(SearchBoxSuggestion.GROUP_SUGGESTION)) {
            return groupManagementActions.viewGroup(suggestionValue);
        } else if (suggestionType.equals(SearchBoxSuggestion.DOCUMENTS_BY_AUTHOR_SUGGESTION)) {
            return handleFacetedSearch("fsd:dc_creator", suggestionValue);
        } else if (suggestionType.equals(SearchBoxSuggestion.DOCUMENTS_MODIFIED_AFTER_SUGGESTION)) {
            return handleFacetedSearch("fsd:dc_modified_min",
                    df.parse(suggestionValue));
        } else if (suggestionType.equals(SearchBoxSuggestion.DOCUMENTS_CREATED_AFTER_SUGGESTION)) {
            return handleFacetedSearch("fsd:dc_created_min",
                    df.parse(suggestionValue));
        } else if (suggestionType.equals(SearchBoxSuggestion.DOCUMENTS_MODIFIED_BEFORE_SUGGESTION)) {
            return handleFacetedSearch("fsd:dc_modified_max",
                    df.parse(suggestionValue));
        } else if (suggestionType.equals(SearchBoxSuggestion.DOCUMENTS_CREATED_BEFORE_SUGGESTION)) {
            return handleFacetedSearch("fsd:dc_created_max",
                    df.parse(suggestionValue));
        } else {
            // fallback to basic keyword search suggestion
            return handleFacetedSearch("fsd:ecm_fulltext", suggestionValue);
        }
    }

    protected String handleFacetedSearch(String searchField,
            Serializable searchValue) throws ClientException {
        facetedSearchActions.clearSearch();
        facetedSearchActions.setCurrentContentViewName(null);
        String contentViewName = facetedSearchActions.getCurrentContentViewName();
        ContentView contentView = contentViewActions.getContentView(contentViewName);
        DocumentModel dm = contentView.getSearchDocumentModel();
        Property searchProperty = dm.getProperty(searchField);
        if (searchProperty.isList()) {
            dm.setPropertyValue(searchField,
                    (Serializable) Arrays.asList(searchValue));
        } else {
            dm.setPropertyValue(searchField, searchValue);
        }
        multiNavTreeManager.setSelectedNavigationTree("facetedSearch");
        return "faceted_search_results";
    }

    public String performKerwordsSearch() throws ClientException {
        String outcome = handleFacetedSearch("fsd:ecm_fulltext", searchKeywords);
        setSearchKeywords("");
        return outcome;
    }

    public static class SearchBoxSuggestion {

        public static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd";

        public static final String DOCUMENT_SUGGESTION = "document";

        public static final String USER_SUGGESTION = "user";

        public static final String GROUP_SUGGESTION = "group";

        public static final String DOCUMENTS_BY_AUTHOR_SUGGESTION = "documentsByAuthor";

        public static final String DOCUMENTS_WITH_KEY_WORDS_SUGGESTION = "documentsWithKeyWords";

        public static final String DOCUMENTS_CREATED_AFTER_SUGGESTION = "documentsCreatedAfterDate";

        public static final String DOCUMENTS_MODIFIED_AFTER_SUGGESTION = "documentsModifiedAfterDate";

        public static final String DOCUMENTS_CREATED_BEFORE_SUGGESTION = "documentsCreatedBeforeDate";

        public static final String DOCUMENTS_MODIFIED_BEFORE_SUGGESTION = "documentsModifiedBeforeDate";

        private final String type;

        private final String value;

        private final String label;

        private final String iconURL;

        public SearchBoxSuggestion(String type, String value, String label,
                String iconURL) {
            this.type = type;
            this.label = label;
            this.iconURL = iconURL;
            this.value = value;
        }

        public String getType() {
            return type;
        }

        public String getValue() {
            return value;
        }

        public String getLabel() {
            return label;
        }

        public String getIconURL() {
            return iconURL;
        }

        public static SearchBoxSuggestion forDocument(DocumentModel doc)
                throws ClientException {
            return new SearchBoxSuggestion(DOCUMENT_SUGGESTION, doc.getId(),
                    doc.getTitle(), doc.getAdapter(TypeInfo.class).getIcon());
        }

        public static SearchBoxSuggestion forUser(DocumentModel user)
                throws ClientException {
            return new SearchBoxSuggestion(USER_SUGGESTION, user.getId(),
                    user.getTitle(), "/icons/user.gif");
        }

        public static SearchBoxSuggestion forGroup(DocumentModel group)
                throws ClientException {
            return new SearchBoxSuggestion(GROUP_SUGGESTION, group.getId(),
                    group.getTitle(), "/icons/group.gif");
        }

        public static SearchBoxSuggestion forDocumentsByAuthor(
                DocumentModel user) throws ClientException {
            // TODO handle i18n
            return new SearchBoxSuggestion(DOCUMENTS_BY_AUTHOR_SUGGESTION,
                    user.getId(), "Documents by " + user.getTitle(),
                    "/img/facetedSearch.png");
        }

        public static SearchBoxSuggestion forDocumentsByDate(String type,
                String label, Calendar date) {
            SimpleDateFormat df = new SimpleDateFormat(DATE_FORMAT_PATTERN);
            String formatted = df.format(date.getTime());
            // TODO handle i18n for label
            return new SearchBoxSuggestion(type, formatted, label + formatted,
                    "/img/facetedSearch.png");
        }

        public static SearchBoxSuggestion forDocumentsModifiedBeforeDate(
                Calendar date) {
            return forDocumentsByDate(DOCUMENTS_MODIFIED_BEFORE_SUGGESTION,
                    "Documents modified before ", date);
        }

        public static SearchBoxSuggestion forDocumentsCreatedBeforeDate(
                Calendar date) {
            return forDocumentsByDate(DOCUMENTS_CREATED_BEFORE_SUGGESTION,
                    "Documents created before ", date);
        }

        public static SearchBoxSuggestion forDocumentsModifiedAfterDate(
                Calendar date) {
            return forDocumentsByDate(DOCUMENTS_MODIFIED_AFTER_SUGGESTION,
                    "Documents modified after ", date);
        }

        public static SearchBoxSuggestion forDocumentsCreatedAfterDate(
                Calendar date) {
            return forDocumentsByDate(DOCUMENTS_CREATED_AFTER_SUGGESTION,
                    "Documents created after ", date);
        }

        public static SearchBoxSuggestion forDocumentsByKeyWords(String keyWords)
                throws ClientException {
            // TODO handle i18n
            return new SearchBoxSuggestion(DOCUMENTS_WITH_KEY_WORDS_SUGGESTION,
                    keyWords, "Documents with keywords: '" + keyWords + "'",
                    "/img/facetedSearch.png");
        }

    }

    @Override
    protected void resetBeanCache(DocumentModel newCurrentDocumentModel) {
    }

    public List<DocumentModel> getUsersSuggestions(Object user)
            throws Exception, ClientException {
        return userSuggestionActions.getUserSuggestions(user);
    }

    public List<DocumentModel> getGroupSuggestions(Object user)
            throws Exception, ClientException {
        return userSuggestionActions.getGroupsSuggestions(user);
    }
}
