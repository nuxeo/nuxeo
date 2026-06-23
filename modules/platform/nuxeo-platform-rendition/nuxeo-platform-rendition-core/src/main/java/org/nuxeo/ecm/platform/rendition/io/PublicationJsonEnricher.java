/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard <grenard@nuxeo.com>
 */
package org.nuxeo.ecm.platform.rendition.io;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;
import static org.nuxeo.ecm.platform.query.api.PageProviderSpec.CORE_SESSION_PROPERTY;

import java.io.IOException;
import java.io.Serializable;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.marshallers.json.enrichers.AbstractJsonEnricher;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.SessionWrapper;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.api.PageProviderSpec;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.core.JsonGenerator;

@Setup(mode = SINGLETON, priority = REFERENCE)
public class PublicationJsonEnricher extends AbstractJsonEnricher<DocumentModel> {

    public static final String NAME = "publications";

    public PublicationJsonEnricher() {
        super(NAME);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void write(JsonGenerator jg, DocumentModel document) throws IOException {
        try (SessionWrapper wrapper = ctx.getSession(document)) {
            if (document.getId() == null || !wrapper.getSession().exists(document.getRef())) {
                return;
            }
            jg.writeObjectFieldStart(NAME);
            var ppService = Framework.getService(PageProviderService.class);
            var pageProvider = (PageProvider<DocumentModel>) ppService.getPageProvider(
                    PageProviderSpec.builder("ALL_PUBLICATION_QUERY")
                                    .pageSize(1L)
                                    .currentPage(0L)
                                    .property(CORE_SESSION_PROPERTY, (Serializable) wrapper.getSession())
                                    .parameters(document.getId(), document.getId())
                                    .build());
            // Force compute total count
            pageProvider.getCurrentPage();
            long resultCount = pageProvider.getResultsCount();
            jg.writeNumberField("resultsCount", resultCount);
            jg.writeEndObject();
        }
    }
}
