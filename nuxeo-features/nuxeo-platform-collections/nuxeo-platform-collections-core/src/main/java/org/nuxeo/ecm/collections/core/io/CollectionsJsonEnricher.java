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
 *     "Guillaume Renard"
 */

package org.nuxeo.ecm.collections.core.io;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;
import java.util.List;

import org.nuxeo.ecm.collections.api.CollectionManager;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.marshallers.json.enrichers.AbstractJsonEnricher;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.SessionWrapper;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Enricher that add the collections the document belongs to.
 *
 * @since 8.1
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class CollectionsJsonEnricher extends AbstractJsonEnricher<DocumentModel> {

    public static final String NAME = "collections";

    public CollectionsJsonEnricher() {
        super(NAME);
    }

    @Override
    public void write(JsonGenerator jg, DocumentModel document) throws IOException {
        jg.writeFieldName(NAME);
        jg.writeStartArray();
        CollectionManager collectionManager = Framework.getService(CollectionManager.class);
        try (SessionWrapper wrapper = ctx.getSession(document)) {
            List<DocumentModel> collections = collectionManager.getVisibleCollection(document, wrapper.getSession());
            for (DocumentModel collection : collections) {
                writeEntity(collection, jg);
            }
        } finally {
            jg.writeEndArray();
        }
    }

}
