/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Gabriel Barata
 */
package org.nuxeo.ecm.platform.query.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.api.PageSelections;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 9.3
 *
 * see NXP-23092
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.query.api")
@Deploy("org.nuxeo.ecm.platform.query.api.test:test-schemas-contrib.xml")
@Deploy("org.nuxeo.ecm.platform.query.api.test:test-pageprovider-contrib.xml")
public class TestPageProviderCurrentPage {

    protected static final String DUMMY_FETCH_DOCUMENTS = "DUMMY_FETCH_DOCUMENTS";

    protected static final int SECOND_PAGE_NUM_DOCS = 25;

    @Inject
    protected PageProviderService pps;

    @Inject
    protected CoreSession session;

    @Before
    public void createTestDocuments() {
        PageProviderDefinition ppd = pps.getPageProviderDefinition(DUMMY_FETCH_DOCUMENTS);
        long num_docs = ppd.getMaxPageSize() + SECOND_PAGE_NUM_DOCS;
        for (int i=0; i < num_docs;i++) {
            DocumentModel doc = session.createDocumentModel("File");
            doc.setPathInfo("/", "File " + i);
            doc.setPropertyValue("dc:source", "dummy");
            session.createDocument(doc);
        }
        session.save();
    }

    @Test
    public void testPageProviderCurrentPage() {
        PageProviderDefinition ppd = pps.getPageProviderDefinition(DUMMY_FETCH_DOCUMENTS);
        HashMap<String, Serializable> props = new HashMap<String, Serializable>();
        props.put(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY, (Serializable) session);
        PageProvider<?> pp = pps.getPageProvider(DUMMY_FETCH_DOCUMENTS, ppd, null, null, ppd.getMaxPageSize(), 0L,
            props);
        // check that both pages are different
        assertEquals(0, pp.getCurrentPageIndex());
        assertEquals(pp.getMaxPageSize(), pp.getCurrentPageSize());
        assertEquals(2, pp.getNumberOfPages());
        List<?> page = pp.getCurrentPage();
        String prevId = ((DocumentModel)page.get(0)).getId();
        pp.setCurrentPage(1);
        assertEquals(1, pp.getCurrentPageIndex());
        assertEquals(SECOND_PAGE_NUM_DOCS, pp.getCurrentPageSize());
        assertEquals(2, pp.getNumberOfPages());
        page = pp.getCurrentPage();
        assertNotEquals(prevId, ((DocumentModel)page.get(0)).getId());
        prevId = ((DocumentModel)page.get(0)).getId();
        // since we only have two pages, requesting a third one should return an empty page
        pp.setCurrentPage(2);
        assertEquals(2, pp.getCurrentPageIndex());
        assertEquals(0, pp.getCurrentPageSize());
        assertEquals(2, pp.getNumberOfPages());
        // however, fetching the current selected page should return the last one instead
        PageSelections<?> selections = pp.getCurrentSelectPage();
        assertEquals(SECOND_PAGE_NUM_DOCS, selections.getSize());
        assertEquals(prevId, ((DocumentModel)selections.getEntries().get(0).getData()).getId());
    }

    @Test
    public void testPageProviderCurrentPageWithoutPageSize() {
        PageProviderDefinition ppd = pps.getPageProviderDefinition(DUMMY_FETCH_DOCUMENTS);
        HashMap<String, Serializable> props = new HashMap<String, Serializable>();
        props.put(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY, (Serializable) session);
        PageProvider<?> pp = pps.getPageProvider(DUMMY_FETCH_DOCUMENTS, ppd, null, null, 0L, 0L, props);
        // check that both pages are different
        assertEquals(0, pp.getCurrentPageIndex());
        assertEquals(pp.getMaxPageSize(), pp.getCurrentPageSize());
        assertEquals(1, pp.getNumberOfPages());
        List<?> page = pp.getCurrentPage();
        String prevId = ((DocumentModel)page.get(0)).getId();
        pp.setCurrentPage(1);
        assertEquals(0, pp.getCurrentPageIndex());
        assertEquals(pp.getMaxPageSize(), pp.getCurrentPageSize());
        assertEquals(1, pp.getNumberOfPages());
        page = pp.getCurrentPage();
        assertEquals(prevId, ((DocumentModel)page.get(0)).getId());
    }

}
