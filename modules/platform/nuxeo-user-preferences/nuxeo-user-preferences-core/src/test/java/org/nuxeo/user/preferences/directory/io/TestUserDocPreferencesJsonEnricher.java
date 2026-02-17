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
package org.nuxeo.user.preferences.directory.io;

import java.util.Map;

import jakarta.inject.Inject;

import org.junit.Test;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.core.io.marshallers.json.document.DocumentModelJsonWriter;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.nuxeo.user.preferences.api.UserPreferencesService;
import org.nuxeo.user.preferences.directory.UserPreferencesFeature;

/**
 * @since 2025.16
 */
@Features(UserPreferencesFeature.class)
public class TestUserDocPreferencesJsonEnricher
        extends AbstractJsonWriterTest.External<DocumentModelJsonWriter, DocumentModel> {

    @Inject
    protected CoreSession session;

    @Inject
    protected TransactionalFeature txFeature;

    @Inject
    protected UserPreferencesService ups;

    @Test
    public void test() throws Exception {
        ups.create(session, new PathRef("/"), Map.of("key1", "value1", "key2", "value2"));
        txFeature.nextTransaction();
        JsonAssert json = jsonAssert(session.getDocument(new PathRef("/")),
                RenderingContext.CtxBuilder.enrichDoc("userPreferences").get());
        json = json.has("contextParameters").isObject();
        json.properties(1);
        json = json.has("userPreferences").isObject();
        json = json.has("preferences").isObject();
        json.has("key1").isEquals("value1");
        json.has("key2").isEquals("value2");
    }

    @Test
    public void testEmpty() throws Exception {
        JsonAssert json = jsonAssert(session.getDocument(new PathRef("/")),
                RenderingContext.CtxBuilder.enrichDoc("userPreferences").get());
        json = json.has("contextParameters").isObject();
        json.properties(1);
        json = json.has("userPreferences").isObject();
        json.has("preferences").isObject().properties(0);
    }
}
