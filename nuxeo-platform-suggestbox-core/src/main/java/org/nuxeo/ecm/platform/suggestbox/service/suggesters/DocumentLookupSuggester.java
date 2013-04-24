/*
 * (C) Copyright 2010-2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Olivier Grisel
 */
package org.nuxeo.ecm.platform.suggestbox.service.suggesters;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
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
 * Perform a NXQL full-text query (on the title by default) on the repository
 * and suggest to navigate to the top documents matching that query.
 *
 * @author ogrisel
 */
public class DocumentLookupSuggester implements Suggester {

    protected String providerName = "DEFAULT_DOCUMENT_SUGGESTION";

    protected SuggesterDescriptor descriptor;

    @Override
    public void initWithParameters(SuggesterDescriptor descriptor) {
        this.descriptor = descriptor;
        String providerName = descriptor.getParameters().get("providerName");
        if (providerName != null) {
            this.providerName = providerName;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Suggestion> suggest(String userInput, SuggestionContext context)
            throws SuggestionException {
        PageProviderService ppService = Framework.getLocalService(PageProviderService.class);
        if (ppService == null) {
            throw new SuggestionException("PageProviderService is not active");
        }
        Map<String, Serializable> props = new HashMap<String, Serializable>();
        props.put(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY,
                (Serializable) context.session);
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
            PageProvider<DocumentModel> pp = (PageProvider<DocumentModel>) ppService.getPageProvider(
                    providerName, null, null, null, props,
                    new Object[] { userInput });
            for (DocumentModel doc : pp.getCurrentPage()) {
                suggestions.add(DocumentSuggestion.fromDocumentModel(doc));
            }
            return suggestions;
        } catch (ClientException e) {
            throw new SuggestionException(String.format(
                    "Suggester '%s' failed to perform query with input '%s'",
                    descriptor.getName(), userInput), e);
        }
    }
}
