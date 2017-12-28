/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Funsho David
 */

package org.nuxeo.elasticsearch.io;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.marshallers.json.enrichers.AbstractJsonEnricher;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.platform.query.api.PageProvider;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * @since 9.1
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class HighlightJsonEnricher extends AbstractJsonEnricher<DocumentModel> {

    public HighlightJsonEnricher() {
        super(PageProvider.HIGHLIGHT_CTX_DATA);
    }

    @Override
    public void write(JsonGenerator jg, DocumentModel document) throws IOException {
        jg.writeArrayFieldStart(PageProvider.HIGHLIGHT_CTX_DATA);
        Map<String, List<String>> h = (Map<String, List<String>>) document.getContextData(
                PageProvider.HIGHLIGHT_CTX_DATA);
        if (h != null) {
            for (String field : h.keySet()) {
                jg.writeStartObject();
                jg.writeStringField("field", field);
                jg.writeObjectField("segments", h.get(field));
                jg.writeEndObject();
            }
        }
        jg.writeEndArray();
    }
}
