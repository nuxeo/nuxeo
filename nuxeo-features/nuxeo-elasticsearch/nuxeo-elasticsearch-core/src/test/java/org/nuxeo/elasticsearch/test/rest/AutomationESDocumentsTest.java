/*
 * (C) Copyright 20202 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.elasticsearch.test.rest;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.client.model.PaginableDocuments;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.automation.core.operations.services.DocumentPageProviderOperation;
import org.nuxeo.ecm.automation.test.EmbeddedAutomationServerFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.restapi.test.RestServerInit;
import org.nuxeo.elasticsearch.test.RepositoryElasticSearchFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features({ EmbeddedAutomationServerFeature.class, RepositoryElasticSearchFeature.class })
@Deploy("org.nuxeo.elasticsearch.core.test:elasticsearch-test-contrib.xml")
@Deploy("org.nuxeo.elasticsearch.core.test:pageprovider-test-contrib.xml")
@Deploy("org.nuxeo.elasticsearch.core.test:pageprovider2-test-contrib.xml")
@Deploy("org.nuxeo.elasticsearch.core.test:pageprovider2-coretype-test-contrib.xml")
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
public class AutomationESDocumentsTest {

    @Inject
    protected Session session;

    @Test
    public void iCanPerformESQLPageProviderOperationOnRepository() throws Exception {
        Properties namedParameters = new Properties();
        namedParameters.put("defaults:dc_nature_agg", "[\"article\"]");
        PaginableDocuments docs = (PaginableDocuments) session.newRequest(DocumentPageProviderOperation.ID)
                                                              .set("namedParameters", namedParameters)
                                                              .set("providerName", "default_search")
                                                              .execute();
        assertEquals(docs.getPageSize(), 20);
        assertEquals(docs.getResultsCount(), 11);
    }

    @Test
    public void iCanSkipAggregatesOnESQLPageProviderOperationOnRepository() throws Exception {
        PaginableDocuments docs = (PaginableDocuments) session.newRequest(DocumentPageProviderOperation.ID)
                                                              .set("providerName", "aggregates_1")
                                                              .execute();
        assertTrue(docs.hasAggregates());

        docs = (PaginableDocuments) session.newRequest(DocumentPageProviderOperation.ID)
                                           .setHeader(PageProvider.SKIP_AGGREGATES_PROP, "true")
                                           .set("providerName", "aggregates_1")
                                           .execute();
        assertFalse(docs.hasAggregates());
    }
}
