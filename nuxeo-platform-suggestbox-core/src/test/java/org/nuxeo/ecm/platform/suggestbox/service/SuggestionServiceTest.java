/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.suggestbox.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.directory.memory.MemoryDirectoryDescriptor;
import org.nuxeo.ecm.platform.login.test.ClientLoginFeature;
import org.nuxeo.ecm.platform.login.test.DummyNuxeoLoginModule;
import org.nuxeo.ecm.platform.suggestbox.service.suggesters.I18nHelper;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, ClientLoginFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.query.api")
@Deploy("org.nuxeo.ecm.platform.types.api")
@Deploy("org.nuxeo.ecm.platform.types.core")
@Deploy("org.nuxeo.ecm.platform.webapp.types")
@Deploy("org.nuxeo.ecm.platform.usermanager.api")
@Deploy("org.nuxeo.ecm.platform.usermanager")
@Deploy("org.nuxeo.ecm.directory")
@Deploy("org.nuxeo.ecm.directory.types.contrib")
@Deploy("org.nuxeo.ecm.platform.suggestbox.core")
public class SuggestionServiceTest {

    @Inject
    DirectoryService directoryService;

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected ClientLoginFeature loginFeature;

    @Inject
    protected CoreSession session;

    protected SuggestionService suggestionService;

    protected Directory userdir;

    protected Directory groupDir;

    protected MemoryDirectoryDescriptor userDesc;

    protected MemoryDirectoryDescriptor groupDesc;

    @Before
    public void setUp() throws Exception {
        Set<String> userSet = new HashSet<String>(Arrays.asList("username", "password", "firstName", "lastName"));
        userDesc = new MemoryDirectoryDescriptor();
        userDesc.name = "userDirectory";
        userDesc.schemaName = "user";
        userDesc.schemaSet = userSet;
        userDesc.idField = "username";
        userDesc.passwordField = "password";
        directoryService.registerDirectoryDescriptor(userDesc);
        userdir = directoryService.getDirectory("userDirectory");

        Set<String> groupSet = new HashSet<String>(Arrays.asList("groupname", "grouplabel", "members"));
        groupDesc = new MemoryDirectoryDescriptor();
        groupDesc.name = "groupDirectory";
        groupDesc.schemaName = "group";
        groupDesc.schemaSet = groupSet;
        groupDesc.idField = "groupname";
        directoryService.registerDirectoryDescriptor(groupDesc);
        groupDir = directoryService.getDirectory("groupDirectory");

        // create some documents to be looked up
        makeSomeDocuments();

        loginFeature.login(DummyNuxeoLoginModule.ADMINISTRATOR_USERNAME);
        // create some users and groups
        try {
            makeSomeUsersAndGroups();
        } finally {
            loginFeature.logout();
        }

        suggestionService = Framework.getService(SuggestionService.class);
        assertNotNull(suggestionService);
    }

    @After
    public void tearDown() throws Exception {
        directoryService.unregisterDirectoryDescriptor(userDesc);
        directoryService.unregisterDirectoryDescriptor(groupDesc);
    }

    protected void makeSomeUsersAndGroups() {
        try (Session userSession = userdir.getSession()) {
            Map<String, Object> john = new HashMap<String, Object>();
            john.put("username", "john");
            john.put("firstName", "John");
            john.put("lastName", "Lennon");
            userSession.createEntry(john);

            Map<String, Object> bob = new HashMap<String, Object>();
            bob.put("username", "bob");
            bob.put("firstName", "Bob");
            bob.put("lastName", "Marley");
            userSession.createEntry(bob);

            Map<String, Object> noname = new HashMap<String, Object>();
            noname.put("username", "noname");
            userSession.createEntry(noname);
        }

        try (Session groupSession = groupDir.getSession()) {
            Map<String, Object> musicians = new HashMap<String, Object>();
            musicians.put("groupname", "musicians");
            musicians.put("grouplabel", "Musicians");
            musicians.put("members", Arrays.asList("john", "bob"));
            groupSession.createEntry(musicians);
        }
    }

    protected void makeSomeDocuments() {
        DocumentModel domain = session.createDocumentModel("/", "default-domain", "Folder");
        session.createDocument(domain);

        DocumentModel file1 = session.createDocumentModel("/default-domain", "file1", "File");
        file1.setPropertyValue("dc:title", "First document with a superuniqueword in the title");
        session.createDocument(file1);

        DocumentModel file2 = session.createDocumentModel("/default-domain", "file2", "File");
        file2.setPropertyValue("dc:title", "Second document");
        session.createDocument(file2);

        DocumentModel file3 = session.createDocumentModel("/default-domain", "file3", "File");
        file3.setPropertyValue("dc:title", "Third document");
        session.createDocument(file3);

        DocumentModel fileBob = session.createDocumentModel("/default-domain", "file-bob", "File");
        fileBob.setPropertyValue("dc:title", "The 2012 document about Bob Marley");
        session.createDocument(fileBob);

        session.save();
        waitForFulltextIndexing();
    }

    protected void waitForFulltextIndexing() {
        if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
        coreFeature.getStorageConfiguration().waitForFulltextIndexing();
    }

    @Test
    // TODO change the test when the redirection to the new search tab will be handled
    public void testDefaultSuggestionConfigurationWithKeyword() throws SuggestionException {
        assumeTrue("No multiple fulltext indexes",
                coreFeature.getStorageConfiguration().supportsMultipleFulltextIndexes());

        // build a suggestion context
        NuxeoPrincipal admin = session.getPrincipal();
        Map<String, String> messages = getTestMessages();
        SuggestionContext context = new SuggestionContext("searchbox", admin).withLocale(Locale.US).withSession(session).withMessages(
                messages);

        // perform some test lookups to check the deployment of extension points
        List<Suggestion> suggestions = suggestionService.suggest("superuni", context);
        assertNotNull(suggestions);
        assertEquals(1, suggestions.size());

        /*
         * Suggestion sugg0 = suggestions.get(0); assertEquals("searchDocuments", sugg0.getType());
         * assertEquals("Search documents with keywords: 'superuni'", sugg0.getLabel());
         * assertEquals("/img/facetedSearch.png", sugg0.getIconURL());
         */
        Suggestion sugg0 = suggestions.get(0);
        assertEquals("document", sugg0.getType());
        assertEquals("First document with a superuniqueword in the title", sugg0.getLabel());
        assertEquals("/icons/file.gif", sugg0.getIconURL());
    }

    @Test
    // TODO change the test when the redirection to the new search tab will be handled
    public void testDefaultSuggestionConfigurationWithDate() throws SuggestionException {
        assumeTrue("No multiple fulltext indexes",
                coreFeature.getStorageConfiguration().supportsMultipleFulltextIndexes());

        // build a suggestion context
        NuxeoPrincipal admin = session.getPrincipal();
        Map<String, String> messages = getTestMessages();
        SuggestionContext context = new SuggestionContext("searchbox", admin).withLocale(Locale.US).withSession(session).withMessages(
                messages);

        // 2009 matches a date and no title
        List<Suggestion> suggestions = suggestionService.suggest("2009", context);
        assertNotNull(suggestions);
        assertEquals(0, suggestions.size());

        /*
         * Suggestion sugg0 = suggestions.get(0); assertEquals("searchDocuments", sugg0.getType());
         * assertEquals("Search documents with keywords: '2009'", sugg0.getLabel());
         * assertEquals("/img/facetedSearch.png", sugg0.getIconURL()); Suggestion sugg1 = suggestions.get(1);
         * assertEquals("searchDocuments", sugg1.getType()); assertEquals("Search documents created after Jan 1, 2009",
         * sugg1.getLabel()); assertEquals("/img/facetedSearch.png", sugg1.getIconURL()); Suggestion sugg2 =
         * suggestions.get(2); assertEquals("searchDocuments", sugg2.getType());
         * assertEquals("Search documents created before Jan 1, 2009", sugg2.getLabel());
         * assertEquals("/img/facetedSearch.png", sugg2.getIconURL()); Suggestion sugg3 = suggestions.get(3);
         * assertEquals("searchDocuments", sugg3.getType()); assertEquals("Search documents modified after Jan 1, 2009",
         * sugg3.getLabel()); assertEquals("/img/facetedSearch.png", sugg3.getIconURL()); Suggestion sugg4 =
         * suggestions.get(4); assertEquals("searchDocuments", sugg4.getType());
         * assertEquals("Search documents modified before Jan 1, 2009", sugg4.getLabel());
         * assertEquals("/img/facetedSearch.png", sugg4.getIconURL()); // 2012 both matches a title and a date
         * suggestions = suggestionService.suggest("2012", context); assertNotNull(suggestions); assertEquals(6,
         * suggestions.size()); sugg1 = suggestions.get(1); assertEquals("document", sugg1.getType());
         * assertEquals("The 2012 document about Bob Marley", sugg1.getLabel()); assertEquals("/icons/file.gif",
         * sugg1.getIconURL());
         */
    }

    @Test
    public void testSearchUserLimit() throws Exception {
        assumeTrue("No multiple fulltext indexes",
                coreFeature.getStorageConfiguration().supportsMultipleFulltextIndexes());

        Framework.doPrivileged(() -> {
            try (Session userSession = userdir.getSession()) {
                for (int i = 0; i < 10; i++) {
                    Map<String, Object> user = new HashMap<String, Object>();
                    user.put("username", String.format("user%d", i));
                    user.put("firstName", "Nemo");
                    user.put("lastName", "Homonym");
                    userSession.createEntry(user);
                }
            }
        });

        // build a suggestion context
        NuxeoPrincipal admin = session.getPrincipal();
        Map<String, String> messages = getTestMessages();
        SuggestionContext context = new SuggestionContext("searchbox", admin).withLocale(Locale.US).withSession(session).withMessages(
                messages);

        // count user suggestions only
        int count = 0;
        List<Suggestion> suggestions = suggestionService.suggest("homonym", context);
        for (Suggestion suggestion : suggestions) {
            if ("user".equals(suggestion.getType())) {
                count++;
            }
        }
        assertEquals(5, count);
    }

    @Test
    // TODO change the test when the redirection to the new search tab will be handled
    public void testDefaultSuggestionConfigurationWithUsersAndGroups() throws SuggestionException {
        assumeTrue("No multiple fulltext indexes",
                coreFeature.getStorageConfiguration().supportsMultipleFulltextIndexes());

        // build a suggestion context
        NuxeoPrincipal admin = session.getPrincipal();
        Map<String, String> messages = getTestMessages();
        SuggestionContext context = new SuggestionContext("searchbox", admin).withLocale(Locale.US).withSession(session).withMessages(
                messages);

        // perform some test lookups to check the deployment of extension points
        List<Suggestion> suggestions = suggestionService.suggest("marl", context);
        assertNotNull(suggestions);
        assertEquals(2, suggestions.size());

        /*
         * Suggestion sugg0 = suggestions.get(0); assertEquals("searchDocuments", sugg0.getType());
         * assertEquals("Search documents with keywords: 'marl'", sugg0.getLabel());
         * assertEquals("/img/facetedSearch.png", sugg0.getIconURL());
         */

        Suggestion sugg1 = suggestions.get(0);
        assertEquals("document", sugg1.getType());
        assertEquals("The 2012 document about Bob Marley", sugg1.getLabel());
        assertEquals("/icons/file.gif", sugg1.getIconURL());

        Suggestion sugg2 = suggestions.get(1);
        assertEquals("user", sugg2.getType());
        assertEquals("Bob Marley", sugg2.getLabel());
        assertEquals("/icons/user.png", sugg2.getIconURL());

        /*
         * Suggestion sugg3 = suggestions.get(3); assertEquals("searchDocuments", sugg3.getType());
         * assertEquals("Search documents by Bob Marley", sugg3.getLabel()); assertEquals("/img/facetedSearch.png",
         * sugg3.getIconURL());
         */

        // Check that user suggestion for entries without firstname and lastname
        // return the user id
        // perform some test lookups to check the deployment of extension points
        suggestions = suggestionService.suggest("nonam", context);
        assertNotNull(suggestions);
        assertEquals(1, suggestions.size());

        /*
         * sugg0 = suggestions.get(0); assertEquals("searchDocuments", sugg0.getType());
         * assertEquals("Search documents with keywords: 'nonam'", sugg0.getLabel());
         * assertEquals("/img/facetedSearch.png", sugg0.getIconURL());
         */

        sugg1 = suggestions.get(0);
        assertEquals("user", sugg1.getType());
        assertEquals("noname", sugg1.getLabel());
        assertEquals("/icons/user.png", sugg1.getIconURL());

        /*
         * sugg2 = suggestions.get(2); assertEquals("searchDocuments", sugg2.getType());
         * assertEquals("Search documents by noname", sugg2.getLabel()); assertEquals("/img/facetedSearch.png",
         * sugg2.getIconURL());
         */
    }

    protected Map<String, String> getTestMessages() {
        Map<String, String> messages = new HashMap<String, String>();
        messages.put("label.searchDocumentsByKeyword", "Search documents with keywords: '{0}'");
        messages.put("label.search.beforeDate_fsd_dc_created", "Search documents created before {0}");
        messages.put("label.search.afterDate_fsd_dc_created", "Search documents created after {0}");
        messages.put("label.search.beforeDate_fsd_dc_modified", "Search documents modified before {0}");
        messages.put("label.search.afterDate_fsd_dc_modified", "Search documents modified after {0}");
        messages.put("label.searchDocumentsByUser_fsd_dc_creator", "Search documents by {0}");
        return messages;
    }

    @Test
    public void testSpecialCharacterHandlingInSuggesters() throws SuggestionException {
        assumeTrue("No multiple fulltext indexes",
                coreFeature.getStorageConfiguration().supportsMultipleFulltextIndexes());

        // check that special characters are not interpreted by the search
        // backend

        // build a suggestion context
        NuxeoPrincipal admin = session.getPrincipal();
        Map<String, String> messages = getTestMessages();
        SuggestionContext context = new SuggestionContext("searchbox", admin).withLocale(Locale.US).withSession(session).withMessages(
                messages);

        // smoke test to perform suggestion against suggesters registered by
        // default
        List<Suggestion> suggestions = suggestionService.suggest("!@#$%^&*()--", context);
        assertNotNull(suggestions);

        suggestions = suggestionService.suggest("\\\\", context);
        assertNotNull(suggestions);
    }

    @Test
    public void testSpecialCharacterHandlingInInterpolation() throws SuggestionException {
        // check the escaping of regexp replacement special chars
        String interpolated = I18nHelper.interpolate("A {1} interpolated message {0}", "\\", "$");
        assertEquals("A $ interpolated message \\", interpolated);

        interpolated = I18nHelper.interpolate("A {1} interpolated message {0}", "\\\\", "$");
        assertEquals("A $ interpolated message \\\\", interpolated);
    }
}
