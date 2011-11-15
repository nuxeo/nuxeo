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
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.ecm.platform.contentview.jsf.ContentView;
import org.nuxeo.ecm.platform.contentview.seam.ContentViewActions;
import org.nuxeo.ecm.platform.faceted.search.jsf.FacetedSearchActions;
import org.nuxeo.ecm.platform.suggestbox.service.CommonSuggestionTypes;
import org.nuxeo.ecm.platform.suggestbox.service.Suggestion;
import org.nuxeo.ecm.platform.suggestbox.service.SuggestionContext;
import org.nuxeo.ecm.platform.suggestbox.service.SuggestionException;
import org.nuxeo.ecm.platform.suggestbox.service.SuggestionService;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;
import org.nuxeo.ecm.platform.util.RepositoryLocation;
import org.nuxeo.ecm.virtualnavigation.action.MultiNavTreeManager;
import org.nuxeo.ecm.webapp.security.GroupManagementActions;
import org.nuxeo.ecm.webapp.security.UserManagementActions;
import org.nuxeo.ecm.webapp.security.UserSuggestionActionsBean;
import org.nuxeo.runtime.api.Framework;

import edu.emory.mathcs.backport.java.util.Collections;

@Name("suggestboxActions")
@Scope(CONVERSATION)
@AutomaticDocumentBasedInvalidation
public class SuggestboxActions implements Serializable {

    private static final Log log = LogFactory.getLog(SuggestboxActions.class);

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

    @In(create = true)
    protected Map<String, String> messages;

    @In(create = true)
    protected Locale locale;

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
    public List<Suggestion> getSuggestions(Object input) {

        SuggestionService service = Framework.getLocalService(SuggestionService.class);
        SuggestionContext ctx = new SuggestionContext("searchbox",
                documentManager.getPrincipal()).withSession(documentManager).withCurrentDocument(
                navigationContext.getCurrentDocument()).withLocale(locale).withMessages(
                messages);
        try {
            return service.suggest(input.toString(), ctx);
        } catch (SuggestionException e) {
            // log the exception rather than trying to display it since this
            // method is called by ajax events when typing in the searchbox.
            log.error(e, e);
            return Collections.emptyList();
        }
    }

    // TODO: move this logic in the Suggestion service with pluggable action
    // handler
    public String handleSelection(Suggestion selectedSuggestion)
            throws ClientException, ParseException {
        setSearchKeywords("");
        String suggestionValue = selectedSuggestion.getValue();
        String suggestionType = selectedSuggestion.getType();
        if (suggestionType.equals(CommonSuggestionTypes.DOCUMENT)) {
            String[] fields = suggestionValue.split("::", 2);
            if (fields.length != 2) {
                log.error("Invalid document location, should be repo name and"
                        + " doc id separated by '::' marker, got: "
                        + suggestionValue);
                return null;
            }
            String repoName = fields[0];
            String docId = fields[1];
            navigationContext.navigateTo(new RepositoryLocation(repoName),
                    new IdRef(docId));
            return "view_documents";
        } else if (suggestionType.equals(CommonSuggestionTypes.USER)) {
            return userManagementActions.viewUser(suggestionValue);
        } else if (suggestionType.equals(CommonSuggestionTypes.GROUP)) {
            return groupManagementActions.viewGroup(suggestionValue);
        } else if (suggestionType.equals(CommonSuggestionTypes.SEARCH_DOCUMENTS)) {
            return handleFacetedSearch(suggestionValue);
        } else {
            // fallback to basic keyword search suggestion
            return handleFacetedSearch("fsd:ecm_fulltext");
        }
    }

    protected String handleFacetedSearch(String suggestionValue)
            throws ClientException {
        DateFormat df = new SimpleDateFormat(Suggestion.DATE_FORMAT_PATTERN);
        facetedSearchActions.clearSearch();
        facetedSearchActions.setCurrentContentViewName(null);
        String contentViewName = facetedSearchActions.getCurrentContentViewName();
        ContentView contentView = contentViewActions.getContentView(contentViewName);
        DocumentModel dm = contentView.getSearchDocumentModel();
        String[] fields = suggestionValue.split(" ", 2);
        if (fields.length != 2) {
            log.error("Invalid search value: should be a fieldname and a"
                    + " value separated by a whitespace, got: "
                    + suggestionValue);
            return null;
        }
        String searchField = fields[0];
        String searchValue = fields[1];

        Property searchProperty = dm.getProperty(searchField);
        if (searchProperty.isList()) {
            // list type is used for multi-valued option fields such as
            // fsd_dc_creator.
            dm.setPropertyValue(searchField,
                    (Serializable) Collections.singleton(searchValue));
        } else if (searchProperty.getField().getType().equals(DateType.INSTANCE)) {
            try {
                dm.setPropertyValue(searchField, df.parse(searchValue));
            } catch (ParseException e) {
                log.error("Invalid date value: " + searchValue);
            }
        } else {
            dm.setPropertyValue(searchField, searchValue);
        }
        multiNavTreeManager.setSelectedNavigationTree("facetedSearch");
        return "faceted_search_results";
    }

    public String performKerwordsSearch() throws ClientException {
        String outcome = handleFacetedSearch("fsd:ecm_fulltext "
                + searchKeywords);
        setSearchKeywords("");
        return outcome;
    }

}
