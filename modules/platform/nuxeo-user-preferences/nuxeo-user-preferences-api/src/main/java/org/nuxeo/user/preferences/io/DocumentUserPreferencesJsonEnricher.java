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
 *     Guillaume Renard
 */
package org.nuxeo.user.preferences.io;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.marshallers.json.enrichers.AbstractJsonEnricher;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.user.preferences.api.UserPreferencesService;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Enrich {@link DocumentModel} Json.
 * <p>
 * Add current user's preferences on the {@link DocumentModel}.
 * </p>
 * <p>
 * Enabled if parameter enrichers-document=userPreferences is present.
 * </p>
 * <p>
 * Format is:
 *
 * <pre>
 * {
 *   "entity-type":"document",
 *   ...
 *   "contextParameters": {
 *     "userPreferences": {
 *       "entity-type": "userPreferences",
 *       "preferences": {
 *         "key1": "value1",
 *         "key2": "value2"
 *       }
 *   }
 * }
 * </pre>
 *
 * @since 2025.16
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class DocumentUserPreferencesJsonEnricher extends AbstractJsonEnricher<DocumentModel> {

    public static final String NAME = "userPreferences";

    public DocumentUserPreferencesJsonEnricher() {
        super(NAME);
    }

    @Override
    public void write(JsonGenerator jg, DocumentModel document) throws IOException {
        try (RenderingContext.SessionWrapper wrapper = ctx.getSession(document)) {
            if (!wrapper.getSession().exists(document.getRef())) {
                return;
            }
            var prefs = Framework.getService(UserPreferencesService.class).get(wrapper.getSession(), document.getRef());
            jg.writeFieldName(NAME);
            writeEntity(prefs, jg);
        }
    }
}
