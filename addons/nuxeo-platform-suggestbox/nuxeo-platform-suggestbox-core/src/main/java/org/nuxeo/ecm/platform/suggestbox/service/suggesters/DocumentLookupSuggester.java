/*
 * (C) Copyright 2010-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.platform.suggestbox.service.suggesters;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.ecm.platform.query.nxql.NXQLQueryBuilder;
import org.nuxeo.ecm.platform.suggestbox.service.DocumentSuggestion;
import org.nuxeo.ecm.platform.suggestbox.service.Suggester;
import org.nuxeo.ecm.platform.suggestbox.service.Suggestion;
import org.nuxeo.ecm.platform.suggestbox.service.SuggestionContext;
import org.nuxeo.ecm.platform.suggestbox.service.SuggestionException;
import org.nuxeo.ecm.platform.suggestbox.service.descriptors.SuggesterDescriptor;
import org.nuxeo.runtime.api.Framework;

/**
 * Perform a NXQL full-text query (on the title by default) on the repository and suggest to navigate to the top
 * documents matching that query.
 *
 * @author ogrisel
 */
public class DocumentLookupSuggester implements Suggester {

    protected List<String> highlights = null;

    protected String providerName = "DEFAULT_DOCUMENT_SUGGESTION";

    protected SuggesterDescriptor descriptor;

    @Override
    public void initWithParameters(SuggesterDescriptor descriptor) {
        this.descriptor = descriptor;
        String providerName = descriptor.getParameters().get("providerName");
        if (providerName != null) {
            this.providerName = providerName;
        }
        String highlightFields = descriptor.getParameters().get("highlightFields");
        if (highlightFields != null) {
            if (!StringUtils.isBlank(highlightFields)) {
                String[] fields = highlightFields.split(",");
                highlights = Arrays.asList(fields);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Suggestion> suggest(String userInput, SuggestionContext context) throws SuggestionException {
        PageProviderService ppService = Framework.getService(PageProviderService.class);
        Map<String, Serializable> props = new HashMap<String, Serializable>();
        props.put(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY, (Serializable) context.session);
        userInput = NXQLQueryBuilder.sanitizeFulltextInput(userInput);
        if (userInput.trim().isEmpty()) {
            return Collections.emptyList();
        }
        if (!userInput.endsWith(" ")) {
            // perform a prefix search on the last typed word
            userInput += "*";
        }
        try {
            List<Suggestion> suggestions = new ArrayList<Suggestion>();
            PageProvider<DocumentModel> pp = (PageProvider<DocumentModel>) ppService.getPageProvider(providerName, null,
                    null, null, null, props, highlights, null, new Object[] { userInput });
            for (DocumentModel doc : pp.getCurrentPage()) {
                suggestions.add(DocumentSuggestion.fromDocumentModel(doc));
            }
            return suggestions;
        } catch (QueryParseException e) {
            throw new SuggestionException(String.format("Suggester '%s' failed to perform query with input '%s'",
                    descriptor.getName(), userInput), e);
        }
    }
}
