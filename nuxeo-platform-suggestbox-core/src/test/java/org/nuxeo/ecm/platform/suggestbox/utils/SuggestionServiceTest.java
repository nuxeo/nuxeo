/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.suggestbox.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.suggestbox.service.Suggestion;
import org.nuxeo.ecm.platform.suggestbox.service.SuggestionContext;
import org.nuxeo.ecm.platform.suggestbox.service.SuggestionException;
import org.nuxeo.ecm.platform.suggestbox.service.SuggestionService;
import org.nuxeo.runtime.api.Framework;

public class SuggestionServiceTest extends SQLRepositoryTestCase {

    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(SuggestionServiceTest.class);

    protected SuggestionService suggestionService;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // PageProvider API
        deployBundle("org.nuxeo.ecm.platform.query.api");

        // type icons
        deployBundle("org.nuxeo.ecm.platform.types.api");
        deployBundle("org.nuxeo.ecm.platform.types.core");
        deployBundle("org.nuxeo.ecm.webapp.base");

        // myself
        deployBundle("org.nuxeo.ecm.platform.suggestbox.core");

        // create some documents to be looked up
        makeSomeDocuments();

        suggestionService = Framework.getService(SuggestionService.class);
        assertNotNull(suggestionService);
    }

    protected void makeSomeDocuments() throws ClientException {
        openSession();
        DocumentModel domain = session.createDocumentModel("/",
                "default-domain", "Folder");
        session.createDocument(domain);

        DocumentModel file1 = session.createDocumentModel("/default-domain",
                "file1", "File");
        file1.setPropertyValue("dc:title", "First document");
        session.createDocument(file1);

        DocumentModel file2 = session.createDocumentModel("/default-domain",
                "file2", "File");
        file2.setPropertyValue("dc:title", "Second document");
        session.createDocument(file2);

        DocumentModel file3 = session.createDocumentModel("/default-domain",
                "file3", "File");
        file3.setPropertyValue("dc:title", "The 2012 document");
        session.createDocument(file3);

        session.save();
    }

    @Override
    public void tearDown() throws Exception {
        closeSession();
        super.tearDown();
    }

    @Test
    public void testDefaultSuggestionConfigurationWithKeyword()
            throws SuggestionException {
        // build a suggestion context
        NuxeoPrincipal admin = (NuxeoPrincipal) session.getPrincipal();
        Map<String, String> messages = getTestMessages();
        SuggestionContext context = new SuggestionContext("searchbox", admin).withLocale(
                Locale.US).withSession(session).withMessages(messages);

        // perform some test lookups to check the deployment of extension points
        List<Suggestion> suggestions = suggestionService.suggest("first",
                context);
        assertNotNull(suggestions);
        assertEquals(2, suggestions.size());

        Suggestion sugg1 = suggestions.get(0);
        assertEquals("document", sugg1.getType());
        assertEquals("First document", sugg1.getLabel());
        assertEquals("/icons/file.gif", sugg1.getIconURL());
        assertNotNull(sugg1.getValue());

        Suggestion sugg2 = suggestions.get(1);
        assertEquals("searchDocuments", sugg2.getType());
        assertEquals("Search documents with keywords: 'first'", sugg2.getLabel());
        assertEquals("/img/facetedSearch.png", sugg2.getIconURL());
        assertEquals("fsd:ecm_fulltext:first", sugg2.getValue());
    }

    @Test
    public void testDefaultSuggestionConfigurationWithDate()
            throws SuggestionException {
        // build a suggestion context
        NuxeoPrincipal admin = (NuxeoPrincipal) session.getPrincipal();
        Map<String, String> messages = getTestMessages();
        SuggestionContext context = new SuggestionContext("searchbox", admin).withLocale(
                Locale.US).withSession(session).withMessages(messages);

        // 2009 matches a date and no title
        List<Suggestion> suggestions = suggestionService.suggest("2009",
                context);
        assertNotNull(suggestions);
        assertEquals(5, suggestions.size());

        Suggestion sugg1 = suggestions.get(0);
        assertEquals("searchDocuments", sugg1.getType());
        assertEquals("Search documents created after Jan 1, 2009",
                sugg1.getLabel());
        assertEquals("/img/facetedSearch.png", sugg1.getIconURL());
        assertEquals("fsd:dc_created_min:2009-01-01", sugg1.getValue());

        Suggestion sugg2 = suggestions.get(1);
        assertEquals("searchDocuments", sugg2.getType());
        assertEquals("Search documents created before Jan 1, 2009",
                sugg2.getLabel());
        assertEquals("/img/facetedSearch.png", sugg2.getIconURL());
        assertEquals("fsd:dc_created_max:2009-01-01", sugg2.getValue());

        Suggestion sugg3 = suggestions.get(2);
        assertEquals("searchDocuments", sugg3.getType());
        assertEquals("Search documents modified after Jan 1, 2009",
                sugg3.getLabel());
        assertEquals("/img/facetedSearch.png", sugg3.getIconURL());
        assertEquals("fsd:dc_modified_min:2009-01-01", sugg3.getValue());

        Suggestion sugg4 = suggestions.get(3);
        assertEquals("searchDocuments", sugg4.getType());
        assertEquals("Search documents modified before Jan 1, 2009",
                sugg4.getLabel());
        assertEquals("/img/facetedSearch.png", sugg4.getIconURL());
        assertEquals("fsd:dc_modified_max:2009-01-01", sugg4.getValue());

        Suggestion sugg5 = suggestions.get(4);
        assertEquals("searchDocuments", sugg5.getType());
        assertEquals("Search documents with keywords: '2009'", sugg5.getLabel());
        assertEquals("/img/facetedSearch.png", sugg5.getIconURL());
        assertEquals("fsd:ecm_fulltext:2009", sugg5.getValue());

        // 2012 both matches a title and a date
        suggestions = suggestionService.suggest("2012", context);
        assertNotNull(suggestions);
        assertEquals(6, suggestions.size());

        sugg1 = suggestions.get(0);
        assertEquals("document", sugg1.getType());
        assertEquals("The 2012 document", sugg1.getLabel());
        assertEquals("/icons/file.gif", sugg1.getIconURL());
        assertNotNull(sugg1.getValue());
    }

    protected Map<String, String> getTestMessages() {
        HashMap<String, String> messages = new HashMap<String, String>();
        messages.put("label.searchDocumentsByKeyword",
                "Search documents with keywords: '{0}'");
        messages.put("label.search.beforeDate_fsd_dc_created",
                "Search documents created before {0}");
        messages.put("label.search.afterDate_fsd_dc_created",
                "Search documents created after {0}");
        messages.put("label.search.beforeDate_fsd_dc_modified",
                "Search documents modified before {0}");
        messages.put("label.search.afterDate_fsd_dc_modified",
                "Search documents modified after {0}");
        return messages;
    }

}
