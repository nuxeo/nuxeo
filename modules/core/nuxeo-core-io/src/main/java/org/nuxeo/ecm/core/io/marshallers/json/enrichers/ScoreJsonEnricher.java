/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */

package org.nuxeo.ecm.core.io.marshallers.json.enrichers;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.platform.query.api.PageProvider;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Enricher that exposes the relevance score of a search result as {@code contextParameters.score}.
 * <p>
 * Activated via request header {@code enrichers-document: score}. The value is a floating-point number whose semantics
 * depend on the search backend: BM25 scores (regular full-text) are unbounded, while cosine-similarity scores (vector
 * search) are in the ~0..1 range. The field is absent when no score is available (e.g. non-search requests).
 *
 * @since 2025.22
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class ScoreJsonEnricher extends AbstractJsonEnricher<DocumentModel> {

    public ScoreJsonEnricher() {
        super(PageProvider.SCORE_CTX_DATA);
    }

    @Override
    public void write(JsonGenerator jg, DocumentModel document) throws IOException {
        var score = document.getContextData(PageProvider.SCORE_CTX_DATA);
        if (score instanceof Number n) {
            jg.writeNumberField(PageProvider.SCORE_CTX_DATA, n.doubleValue());
        }
    }
}
