/*
 * (C) Copyright 2015-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.platform.query.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.AbstractSession;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelFactory;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 7.3
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.query.api")
@Deploy("org.nuxeo.ecm.platform.query.api:test-pageprovider-namedparams-contrib.xml")
public class TestPageProviderNamedParameters {

    @Inject
    PageProviderService service;

    @Inject
    CoreSession session;

    @Before
    public void createTestDocuments() {
        final DocumentModel root = session.getRootDocument();
        // create docs in descending order so that docs are not ordered by
        // title by default
        for (int i = 4; i >= 0; i--) {
            DocumentModel doc = session.createDocumentModel("Folder");
            doc.setPropertyValue("dc:title", "Document number" + i); // no
                                                                     // space
            doc.setPathInfo(root.getPathAsString(), "doc_" + i);

            if (i == 2) {
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.YEAR, 2007);
                cal.set(Calendar.MONTH, 1); // 0-based
                cal.set(Calendar.DAY_OF_MONTH, 17);
                doc.setPropertyValue("dc:issued", cal);
            }

            session.createDocument(doc);
        }
        session.save();
    }

    protected Map<String, Serializable> getPageProviderProps() {
        return Collections.singletonMap(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY, (AbstractSession) session);
    }

    protected DocumentModel getSearchDocWithNamedParam(String propName, Serializable propValue) {
        DocumentModel doc = DocumentModelFactory.createDocumentModel("NamedParamDoc");
        if (propName != null) {
            if (propName.contains(":")) {
                doc.setPropertyValue(propName, propValue);
            } else {
                HashMap<String, Object> params = new HashMap<>();
                params.put(propName, propValue);
                doc.putContextData(PageProviderService.NAMED_PARAMETERS, params);
            }
        }
        return doc;
    }

    /**
     * @since 7.3
     */
    @Test
    public void testPageProviderWithNamedParameters() throws Exception {
        DocumentModel searchDoc = getSearchDocWithNamedParam("parameter1", "Document number2");
        PageProviderService pps = Framework.getService(PageProviderService.class);
        PageProvider<?> pp = pps.getPageProvider("namedParamProvider", searchDoc, null, null, null,
                getPageProviderProps());
        assertNotNull(pp);
        List<?> p = pp.getCurrentPage();
        assertNotNull(p);
        assertEquals(1, p.size());
    }

    /**
     * @since 7.3
     */
    @Test
    public void testPageProviderWithNamedParametersInvalid() throws Exception {
        DocumentModel searchDoc = getSearchDocWithNamedParam(null, null);
        PageProviderService pps = Framework.getService(PageProviderService.class);
        PageProvider<?> pp = pps.getPageProvider("namedParamProviderInvalid", searchDoc, null, null, null,
                getPageProviderProps());
        assertNotNull(pp);
        List<?> p = pp.getCurrentPage();
        assertNotNull(p);
        assertEquals(0, p.size());
        assertEquals(
                "Failed to execute query: SELECT * FROM Document where dc:title=:foo ORDER BY dc:title, Lexical Error: Illegal character <:> at offset 38",
                pp.getErrorMessage());
    }

    /**
     * @since 7.3
     */
    @Test
    public void testPageProviderWithNamedParametersAndDoc() throws Exception {
        DocumentModel searchDoc = getSearchDocWithNamedParam("np:title", "Document number2");
        PageProviderService pps = Framework.getService(PageProviderService.class);
        PageProvider<?> pp = pps.getPageProvider("namedParamProviderWithDoc", searchDoc, null, null, null,
                getPageProviderProps());
        assertNotNull(pp);
        List<?> p = pp.getCurrentPage();
        assertNotNull(p);
        assertEquals(1, p.size());
    }

    /**
     * @since 7.3
     */
    @Test
    public void testPageProviderWithNamedParametersAndDocInvalid() throws Exception {
        DocumentModel searchDoc = getSearchDocWithNamedParam("np:title", "Document number2");
        PageProviderService pps = Framework.getService(PageProviderService.class);
        PageProvider<?> pp = pps.getPageProvider("namedParamProviderWithDocInvalid", searchDoc, null, null, null,
                getPageProviderProps());
        assertNotNull(pp);
        List<?> p = pp.getCurrentPage();
        assertNotNull(p);
        assertEquals(0, p.size());
        assertEquals(
                "Failed to execute query: SELECT * FROM Document where dc:title=:foo ORDER BY dc:title, Lexical Error: Illegal character <:> at offset 38",
                pp.getErrorMessage());
    }

    /*
     * NXP-22829
     */
    @Test
    public void testPageProviderWithNamedParametersAndList() throws Exception {
        PageProviderService pps = Framework.getService(PageProviderService.class);
        Map<String, Serializable> ppp = getPageProviderProps();
        // test with an array
        DocumentModel searchDoc = getSearchDocWithNamedParam("types", new String[] { "File", "Folder" });
        PageProvider<?> pp = pps.getPageProvider("namedParamProviderWithList", searchDoc, null, null, null, ppp);
        assertNotNull(pp);
        List<?> p = pp.getCurrentPage();
        assertNotNull(p);
        assertEquals(5, p.size()); // root + 4 created folder
        // test with a list
        searchDoc = getSearchDocWithNamedParam("types", (Serializable) Arrays.asList("File", "Folder"));
        pp = pps.getPageProvider("namedParamProviderWithList", searchDoc, null, null, null, ppp);
        assertNotNull(pp);
        p = pp.getCurrentPage();
        assertNotNull(p);
        assertEquals(5, p.size()); // root + 4 created folder
    }

    /**
     * @since 7.3
     */
    @Test
    public void testPageProviderWithNamedParametersInWhereClause() throws Exception {
        DocumentModel searchDoc = getSearchDocWithNamedParam("parameter1", "Document number2");
        PageProviderService pps = Framework.getService(PageProviderService.class);
        PageProvider<?> pp = pps.getPageProvider("namedParamProviderWithWhereClause", searchDoc, null, null, null,
                getPageProviderProps());
        assertNotNull(pp);
        List<?> p = pp.getCurrentPage();
        assertNotNull(p);
        assertEquals(1, p.size());

        // retry without params
        searchDoc = getSearchDocWithNamedParam(null, null);
        pp = pps.getPageProvider("namedParamProviderWithWhereClause", searchDoc, null, null, null,
                getPageProviderProps());
        assertNotNull(pp);
        p = pp.getCurrentPage();
        assertNotNull(p);
        assertEquals(2, p.size());
    }

    /**
     * @since 7.3
     */
    @Test
    public void testPageProviderWithNamedParametersInWhereClauseWithDoc() throws Exception {
        DocumentModel searchDoc = getSearchDocWithNamedParam("np:title", "Document number2");
        PageProviderService pps = Framework.getService(PageProviderService.class);
        PageProvider<?> pp = pps.getPageProvider("namedParamProviderWithWhereClauseWithDoc", searchDoc, null, null,
                null, getPageProviderProps());
        assertNotNull(pp);
        List<?> p = pp.getCurrentPage();
        assertNotNull(p);
        assertEquals(1, p.size());

        // retry without params
        searchDoc = getSearchDocWithNamedParam(null, null);
        pp = pps.getPageProvider("namedParamProviderWithWhereClauseWithDoc", searchDoc, null, null, null,
                getPageProviderProps());
        assertNotNull(pp);
        p = pp.getCurrentPage();
        assertNotNull(p);
        assertEquals(2, p.size());
    }

    /**
     * @since 7.3
     */
    @Test
    public void testPageProviderWithNamedParametersComplex() throws Exception {
        DocumentModel searchDoc = DocumentModelFactory.createDocumentModel("NamedParamDoc");
        HashMap<String, Object> params = new HashMap<>();
        params.put("parameter1", "Document number2");
        searchDoc.putContextData(PageProviderService.NAMED_PARAMETERS, params);
        searchDoc.setPropertyValue("np:isCheckedIn", Boolean.FALSE.toString());
        searchDoc.setPropertyValue("np:dateMin", "2007-01-30 01:02:03+04:00");
        searchDoc.setPropertyValue("np:dateMax", "2007-03-23 01:02:03+04:00");

        PageProviderService pps = Framework.getService(PageProviderService.class);
        PageProvider<?> pp = pps.getPageProvider("namedParamProviderComplex", searchDoc, null, null, null,
                getPageProviderProps());
        assertNotNull(pp);
        List<?> p = pp.getCurrentPage();
        assertNotNull(p);
        assertEquals(1, p.size());

        // remove filter on dates
        searchDoc.setPropertyValue("np:dateMin", null);
        searchDoc.setPropertyValue("np:dateMax", null);
        pp = pps.getPageProvider("namedParamProviderComplex", searchDoc, null, null, null, getPageProviderProps());
        assertNotNull(pp);
        p = pp.getCurrentPage();
        assertNotNull(p);
        assertEquals(1, p.size());

        // remove filter on title
        searchDoc.putContextData(PageProviderService.NAMED_PARAMETERS, null);
        pp = pps.getPageProvider("namedParamProviderComplex", searchDoc, null, null, null, getPageProviderProps());
        assertNotNull(pp);
        p = pp.getCurrentPage();
        assertNotNull(p);
        assertEquals(2, p.size());
    }

}
