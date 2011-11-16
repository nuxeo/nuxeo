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
import org.nuxeo.ecm.platform.suggestbox.service.Suggestion;
import org.nuxeo.ecm.platform.suggestbox.service.SuggestionContext;
import org.nuxeo.ecm.platform.suggestbox.service.SuggestionException;
import org.nuxeo.ecm.platform.suggestbox.service.SuggestionHandlingException;
import org.nuxeo.ecm.platform.suggestbox.service.SuggestionService;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;
import org.nuxeo.runtime.api.Framework;

import edu.emory.mathcs.backport.java.util.Collections;

@Name("suggestboxActions")
@Scope(CONVERSATION)
@AutomaticDocumentBasedInvalidation
public class SuggestboxActions implements Serializable {

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
        SuggestionContext ctx = getSuggestionContext();
        try {
            return service.suggest(input.toString(), ctx);
        } catch (SuggestionException e) {
            // log the exception rather than trying to display it since this
            // method is called by ajax events when typing in the searchbox.
            log.error(e, e);
            return Collections.emptyList();
        }
    }

    protected SuggestionContext getSuggestionContext() {
        SuggestionContext ctx = new SuggestionContext("searchbox",
                documentManager.getPrincipal()).withSession(documentManager).withCurrentDocument(
                navigationContext.getCurrentDocument()).withLocale(locale).withMessages(
                messages);
        return ctx;
    }

    public Object handleSelection(Suggestion selectedSuggestion)
            throws SuggestionHandlingException {
        setSearchKeywords("");
        SuggestionService service = Framework.getLocalService(SuggestionService.class);
        SuggestionContext ctx = getSuggestionContext();
        return service.handleSelection(selectedSuggestion, ctx);
    }

}
