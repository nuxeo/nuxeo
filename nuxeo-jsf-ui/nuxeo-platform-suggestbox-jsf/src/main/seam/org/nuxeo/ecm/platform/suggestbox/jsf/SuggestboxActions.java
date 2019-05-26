/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Olivier Grisel
 *
 */
package org.nuxeo.ecm.platform.suggestbox.jsf;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.web.RequestParameter;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.contentview.seam.ContentViewActions;
import org.nuxeo.ecm.platform.suggestbox.service.Suggestion;
import org.nuxeo.ecm.platform.suggestbox.service.SuggestionContext;
import org.nuxeo.ecm.platform.suggestbox.service.SuggestionException;
import org.nuxeo.ecm.platform.suggestbox.service.SuggestionService;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;
import org.nuxeo.ecm.platform.ui.web.invalidations.DocumentContextBoundActionBean;
import org.nuxeo.ecm.webapp.tree.nav.MultiNavTreeManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Back seam component for the top right search box using the suggestion service to help decode the user intent and
 * minimize the number of clicks to find the relevant information.
 */
@Name("suggestboxActions")
@Scope(CONVERSATION)
@AutomaticDocumentBasedInvalidation
public class SuggestboxActions extends DocumentContextBoundActionBean implements Serializable {

    private static final Log log = LogFactory.getLog(SuggestboxActions.class);

    private static final long serialVersionUID = 1L;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true)
    protected Map<String, String> messages;

    @In(create = true)
    protected Locale locale;

    @In(create = true)
    protected MultiNavTreeManager multiNavTreeManager;

    /*
     * @In(create = true) protected FacetedSearchActions facetedSearchActions;
     */

    @In(create = true)
    protected ContentViewActions contentViewActions;

    // keep suggestions in cache for maximum 10 seconds to avoid useless and
    // costly re-computation of the suggestions by rich:suggestionbox at
    // selection time
    protected Cached<List<Suggestion>> cachedSuggestions = new Cached<>(10000);

    protected String searchKeywords = "";

    protected String suggesterGroup;

    public String getSearchKeywords() {
        return searchKeywords;
    }

    public void setSearchKeywords(String searchKeywords) {
        this.searchKeywords = searchKeywords;
    }

    public String getSuggesterGroup() {
        return suggesterGroup;
    }

    @RequestParameter
    public void setSuggesterGroup(String suggesterGroup) {
        this.suggesterGroup = suggesterGroup;
    }

    protected SuggestionContext getSuggestionContext() {
        SuggestionContext ctx = new SuggestionContext(suggesterGroup, documentManager.getPrincipal()).withSession(
                documentManager).withCurrentDocument(navigationContext.getCurrentDocument()).withLocale(locale).withMessages(
                messages);
        return ctx;
    }

    /**
     * Callback for the ajax keypress event that triggers the generation of context sensitive action suggestions. The
     * most specific actions (e.g. direct navigation to a document with matching titles) should be suggested in the
     * first position and more generic (traditional free-text search for documents) last.
     */
    public List<Suggestion> getSuggestions(Object input) {
        if (cachedSuggestions.hasExpired(input, locale)) {
            SuggestionService service = Framework.getService(SuggestionService.class);
            SuggestionContext ctx = getSuggestionContext();
            try {
                List<Suggestion> suggestions = service.suggest(input.toString(), ctx);
                cachedSuggestions.cache(suggestions, input, locale);
            } catch (SuggestionException e) {
                // log the exception rather than trying to display it since
                // this
                // method is called by ajax events when typing in the
                // searchbox.
                log.error(e, e);
                return Collections.emptyList();
            }
        }
        return cachedSuggestions.value;
    }

    @Override
    protected void resetBeanCache(DocumentModel newCurrentDocumentModel) {
        cachedSuggestions.expire();
    }

}
