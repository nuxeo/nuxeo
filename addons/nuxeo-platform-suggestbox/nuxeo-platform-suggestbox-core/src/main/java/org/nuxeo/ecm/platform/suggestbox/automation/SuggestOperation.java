/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:glefevre@nuxeo.com">Gildas</a>
 */
package org.nuxeo.ecm.platform.suggestbox.automation;

import java.util.List;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.platform.suggestbox.service.Suggestion;
import org.nuxeo.ecm.platform.suggestbox.service.SuggestionContext;
import org.nuxeo.ecm.platform.suggestbox.service.SuggestionService;

/**
 * Operation used to suggest result by getting and calling all the suggesters
 * defined in contributions.
 *
 * @since 6.0
 */
@Operation(id = SuggestOperation.ID, category = Constants.CAT_UI, label = "Suggesters launcher", description = "Get and launch the suggesters defined and return a list of Suggestion objects.", addToStudio = false)
public class SuggestOperation {

    public static final String ID = "Search.SuggestersLauncher";

    private static final String SUGGESTER_GROUP = "searchbox";

    @Context
    protected CoreSession session;

    @Context
    protected SuggestionService serviceSuggestion;

    @Param(name = "searchTerm", required = false)
    protected String searchTerm;

    @OperationMethod
    public Blob run() throws Exception {
        JSONArray result = new JSONArray();

        SuggestionContext suggestionContext = new SuggestionContext(
                SUGGESTER_GROUP,
                session.getPrincipal());
        suggestionContext.withSession(session);

        List<Suggestion> listSuggestions = serviceSuggestion.suggest(
                searchTerm,
                suggestionContext);

        // For each suggestion, create a JSON object and add it to the result
        for (Suggestion suggestion : listSuggestions) {
            JSONObject suggestionJSON = new JSONObject();
            suggestionJSON.put("id",
                    suggestion.getId());
            suggestionJSON.put(
                    "label",
                    suggestion.getLabel());
            suggestionJSON.put(
                    "type",
                    suggestion.getType());
            suggestionJSON.put(
                    "icon",
                    suggestion.getIconURL());
            suggestionJSON.put("url", suggestion.getObjectUrl());

            result.add(suggestionJSON);
        }

        return new StringBlob(
                result.toString(),
                "application/json");
    }
}
