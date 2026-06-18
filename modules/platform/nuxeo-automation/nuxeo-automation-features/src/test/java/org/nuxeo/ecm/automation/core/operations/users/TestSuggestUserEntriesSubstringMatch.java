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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.automation.core.operations.users;

import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.directory.test.DirectoryFeature;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.features.AutomationFeaturesFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.core.storage.mongodb.IgnoreIfDBSMongoDBRepository;
import org.nuxeo.ecm.core.storage.sql.IgnoreIfVCSRepository;
import org.nuxeo.runtime.test.runner.ConditionalIgnore;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Tests that the {@link SuggestUserEntries} operation honors the user directory's substring match type
 * ({@code subinitial}, {@code subany} or {@code subfinal}).
 *
 * @since 2025.22
 */
@RunWith(FeaturesRunner.class)
@Features({ AutomationFeaturesFeature.class, DirectoryFeature.class })
@Deploy("org.nuxeo.ecm.platform.usermanager")
@Deploy("org.nuxeo.ecm.automation.features:test-user-directories-contrib.xml")
public class TestSuggestUserEntriesSubstringMatch {

    @Inject
    protected CoreSession session;

    @Inject
    protected AutomationService automationService;

    /**
     * With the default {@code subinitial} substring match type, an infix-only search term ({@code "New"} appears in the
     * middle of {@code "from New Jersey"}) must not return any user.
     */
    @Test
    public void testSubinitial() throws Exception {
        try (OperationContext ctx = new OperationContext(session)) {
            Map<String, String> params = new HashMap<>();
            params.put("searchType", "USER_TYPE");

            // infix match: not returned with subinitial
            params.put("searchTerm", "New");
            Blob result = (Blob) automationService.run(ctx, SuggestUserEntries.ID, params);
            assertNotNull(result);
            JsonAssert.on(result.getString()).length(0);

            // suffix match: not returned with subinitial
            params.put("searchTerm", "Jersey");
            result = (Blob) automationService.run(ctx, SuggestUserEntries.ID, params);
            assertNotNull(result);
            JsonAssert.on(result.getString()).length(0);
        }
    }

    /**
     * With {@code subany}, an infix-only search term returns matching users (SQL variant).
     */
    @Test
    @ConditionalIgnore(condition = IgnoreIfDBSMongoDBRepository.class, cause = "SQL-only test variant")
    @Deploy("org.nuxeo.ecm.automation.features:test-user-directories-substring-match-subany-sql-contrib.xml")
    public void testSubanySQL() throws Exception {
        assertSubanyResults();
    }

    /**
     * With {@code subany}, an infix-only search term returns matching users (MongoDB variant).
     */
    @Test
    @ConditionalIgnore(condition = IgnoreIfVCSRepository.class, cause = "MongoDB-only test variant")
    @Deploy("org.nuxeo.ecm.automation.features:test-user-directories-substring-match-subany-mongodb-contrib.xml")
    public void testSubanyMongoDB() throws Exception {
        assertSubanyResults();
    }

    /**
     * With {@code subfinal}, only suffix matches are returned; infix-only matches are not (SQL variant).
     */
    @Test
    @ConditionalIgnore(condition = IgnoreIfDBSMongoDBRepository.class, cause = "SQL-only test variant")
    @Deploy("org.nuxeo.ecm.automation.features:test-user-directories-substring-match-subfinal-sql-contrib.xml")
    public void testSubfinalSQL() throws Exception {
        assertSubfinalResults();
    }

    /**
     * With {@code subfinal}, only suffix matches are returned; infix-only matches are not (MongoDB variant).
     */
    @Test
    @ConditionalIgnore(condition = IgnoreIfVCSRepository.class, cause = "MongoDB-only test variant")
    @Deploy("org.nuxeo.ecm.automation.features:test-user-directories-substring-match-subfinal-mongodb-contrib.xml")
    public void testSubfinalMongoDB() throws Exception {
        assertSubfinalResults();
    }

    protected void assertSubanyResults() throws Exception {
        try (OperationContext ctx = new OperationContext(session)) {
            Map<String, String> params = new HashMap<>();
            params.put("searchType", "USER_TYPE");

            // infix match in lastName "from New Jersey": returned with subany (was not returned with subinitial)
            params.put("searchTerm", "New");
            Blob result = (Blob) automationService.run(ctx, SuggestUserEntries.ID, params);
            assertNotNull(result);
            JsonAssert.on(result.getString()).length(1).childrenContains("id", "jack");

            // suffix match in lastName "from New Jersey": returned with subany
            params.put("searchTerm", "Jersey");
            result = (Blob) automationService.run(ctx, SuggestUserEntries.ID, params);
            assertNotNull(result);
            JsonAssert.on(result.getString()).length(1).childrenContains("id", "jack");

            // infix match in firstName "Jack" via "ack"
            params.put("searchTerm", "ack");
            result = (Blob) automationService.run(ctx, SuggestUserEntries.ID, params);
            assertNotNull(result);
            // matches both Administrator (firstName "Jacky") and jack (firstName "Jack")
            JsonAssert.on(result.getString()).length(2).childrenContains("id", "Administrator", "jack");
        }
    }

    protected void assertSubfinalResults() throws Exception {
        try (OperationContext ctx = new OperationContext(session)) {
            Map<String, String> params = new HashMap<>();
            params.put("searchType", "USER_TYPE");

            // suffix match in lastName "from New Jersey": returned with subfinal
            params.put("searchTerm", "Jersey");
            Blob result = (Blob) automationService.run(ctx, SuggestUserEntries.ID, params);
            assertNotNull(result);
            JsonAssert.on(result.getString()).length(1).childrenContains("id", "jack");

            // infix-only match: not returned with subfinal
            params.put("searchTerm", "New");
            result = (Blob) automationService.run(ctx, SuggestUserEntries.ID, params);
            assertNotNull(result);
            JsonAssert.on(result.getString()).length(0);
        }
    }

}
