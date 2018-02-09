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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.query.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelFactory;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.query.api.AbstractPageProvider;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

/**
 * @since 7.4
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.platform.query.api", "org.nuxeo.ecm.core.io" })
@Deploy("org.nuxeo.ecm.platform.query.api.test:test-pageprovider-track-contrib.xml")
public class TestPageProviderTracking {

    @Inject
    protected RuntimeHarness harness;

    @Inject
    protected CoreSession coreSession;

    @Inject
    protected PageProviderService pps;

    @Test
    public void testTrackingFlag() throws Exception {
        PageProviderDefinition def = pps.getPageProviderDefinition("CURRENT_DOCUMENT_CHILDREN");
        assertFalse(def.isUsageTrackingEnabled());

        def = pps.getPageProviderDefinition("CURRENT_DOCUMENT_CHILDREN_TRACK");
        assertTrue(def.isUsageTrackingEnabled());

        def = pps.getPageProviderDefinition("CURRENT_DOCUMENT_CHILDREN_FETCH");
        assertTrue(def.isUsageTrackingEnabled());
    }

    @Test
    public void testTrackingProperty() throws Exception {

        Framework.getProperties().setProperty(AbstractPageProvider.PAGEPROVIDER_TRACK_PROPERTY_NAME, "CURRENT_DOCUMENT_CHILDREN2");

        PageProviderDefinition def = pps.getPageProviderDefinition("CURRENT_DOCUMENT_CHILDREN2");
        assertFalse(def.isUsageTrackingEnabled());

        Map<String, Serializable> props = new HashMap<String, Serializable>();
        props.put(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY, (Serializable) coreSession);

        PageProvider<?> pp = pps.getPageProvider("CURRENT_DOCUMENT_CHILDREN2", null, 10L, 0L, props,
                coreSession.getRootDocument().getId());
        assertNotNull(pp);

        SearchEventsAccumulator.reset();
        pp.getCurrentPage();
        List<Map<String, Serializable>> events = SearchEventsAccumulator.getStackedEvents();
        assertEquals(1, events.size());
    }

    @Test
    public void testTrackingListener() throws Exception {

        Map<String, Serializable> props = new HashMap<String, Serializable>();
        props.put(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY, (Serializable) coreSession);

        PageProvider<?> pp = pps.getPageProvider("CURRENT_DOCUMENT_CHILDREN_TRACK", null, 10L, 0L, props,
                coreSession.getRootDocument().getId());
        assertNotNull(pp);

        SearchEventsAccumulator.reset();
        pp.getCurrentPage();
        List<Map<String, Serializable>> events = SearchEventsAccumulator.getStackedEvents();
        assertEquals(1, events.size());

        pp = pps.getPageProvider("CURRENT_DOCUMENT_CHILDREN", null, 10L, 0L, props,
                coreSession.getRootDocument().getId());
        assertNotNull(pp);

        SearchEventsAccumulator.reset();
        pp.getCurrentPage();
        events = SearchEventsAccumulator.getStackedEvents();
        assertEquals(0, events.size());
    }

    protected DocumentModel getSearchDoc() {
        DocumentModel doc = DocumentModelFactory.createDocumentModel("AdvancedSearch");

        doc.setPropertyValue("search:title", "Title");

        doc.setPropertyValue("search:subjects", new String[] { "S1", "S2" });

        doc.setPropertyValue("search:fulltext_all", "whatever");

        return doc;
    }

    @Test
    public void testTrackingData() throws Exception {

        Map<String, Serializable> props = new HashMap<String, Serializable>();
        props.put(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY, (Serializable) coreSession);

        PageProvider<?> pp = pps.getPageProvider("CURRENT_DOCUMENT_CHILDREN_TRACK", null, 10L, 0L, props,
                coreSession.getRootDocument().getId());
        assertNotNull(pp);

        SearchEventsAccumulator.reset();
        pp.getCurrentPage();
        List<Map<String, Serializable>> events = SearchEventsAccumulator.getStackedEvents();
        assertEquals(1, events.size());

        assertNotNull(events.get(0).get("effectiveQuery"));
        assertEquals("Administrator", events.get(0).get("principal"));
        assertNotNull(events.get(0).get("searchPattern"));

        pp = pps.getPageProvider("ADVANCED_SEARCH", null, 10L, 0L, props, coreSession.getRootDocument().getId());
        assertNotNull(pp);
        pp.setSearchDocumentModel(getSearchDoc());
        SearchEventsAccumulator.reset();
        pp.getCurrentPage();
        events = SearchEventsAccumulator.getStackedEvents();
        assertEquals(1, events.size());

        assertNotNull(events.get(0).get("effectiveQuery"));
        assertNotNull(events.get(0).get("searchDocumentModelAsJson"));
        assertNotNull(events.get(0).get("whereClause_fixedPart"));
        assertNotNull(events.get(0).get("queryParams"));

    }

}
