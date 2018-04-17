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
 *     Miguel Nixo
 */
package org.nuxeo.ecm.automation.core.test.directory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.directory.test.DirectoryFeature;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.operations.services.directory.CreateVocabularyEntry;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 8.3
 */
@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, DirectoryFeature.class })
@RepositoryConfig(init = DefaultRepositoryInit.class)
@Deploy("org.nuxeo.ecm.actions")
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.automation.features")
@Deploy("org.nuxeo.ecm.automation.features:test-vocabularies-contrib.xml")
public class CreateVocabularyEntryTest {

    @Inject
    protected CoreSession session;

    @Inject
    protected AutomationService service;

    @Inject
    protected DirectoryService directoryService;

    private String vocabularyName = "continent";

    @Test
    public void shouldCreateEntry() throws OperationException {
        String entryId = "test_entry";

        assertEquals("vocabulary", directoryService.getDirectorySchema(vocabularyName));
        Session vocabularySession = directoryService.open(vocabularyName);
        assertFalse(vocabularySession.hasEntry(entryId));

        OperationContext context = new OperationContext(session);
        OperationChain chain = new OperationChain("shouldCreateEntry");
        chain.add(CreateVocabularyEntry.ID).set("vocabularyName", vocabularyName).set("id", entryId);
        service.run(context, chain);

        assertTrue(vocabularySession.hasEntry(entryId));
    }

    @Test
    public void shouldNotCreateEntry() throws OperationException {
        String entryId = "europe";

        assertEquals("vocabulary", directoryService.getDirectorySchema(vocabularyName));
        Session vocabularySession = directoryService.open(vocabularyName);
        assertTrue(vocabularySession.hasEntry(entryId));
        int numberOfEntriesBefore = vocabularySession.query(new HashMap<>()).size();

        OperationContext context = new OperationContext(session);
        OperationChain chain = new OperationChain("shouldNotCreateEntry");
        chain.add(CreateVocabularyEntry.ID).set("vocabularyName", vocabularyName).set("id", entryId);
        service.run(context, chain);

        int numberOfEntriesAfter = vocabularySession.query(new HashMap<>()).size();
        assertTrue(vocabularySession.hasEntry(entryId));
        assertEquals(numberOfEntriesBefore, numberOfEntriesAfter);
    }

}
