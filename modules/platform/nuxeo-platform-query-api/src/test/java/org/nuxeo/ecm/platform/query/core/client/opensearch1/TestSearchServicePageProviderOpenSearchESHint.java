/*
 * (C) Copyright 2024-2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.ecm.platform.query.core.client.opensearch1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.apache.commons.lang3.SystemUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.search.SearchQuery;
import org.nuxeo.ecm.core.search.client.opensearch1.IgnoreIfNotOpenSearchSearchClient;
import org.nuxeo.ecm.core.search.client.opensearch1.OpenSearchQueryTransformer;
import org.nuxeo.ecm.core.search.client.opensearch1.hint.FuzzyOpenSearchHintQueryBuilder;
import org.nuxeo.ecm.core.test.CoreSearchFeature;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.api.PageProviderSpec;
import org.nuxeo.ecm.platform.query.nxql.SearchServicePageProvider;
import org.nuxeo.runtime.test.runner.ConditionalIgnore;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * @since 2025.0
 */
@RunWith(FeaturesRunner.class)
@Features(CoreSearchFeature.class)
@Deploy("org.nuxeo.ecm.core.query.test:OSGI-INF/test-aggregate-schemas-contrib.xml")
@Deploy("org.nuxeo.ecm.platform.query.api")
@Deploy("org.nuxeo.ecm.platform.query.api.test:test-searchservice-opensearch1-pageprovider-hint-contrib.xml")
@ConditionalIgnore(condition = IgnoreIfNotOpenSearchSearchClient.class)
public class TestSearchServicePageProviderOpenSearchESHint {

    @Inject
    protected CoreSession session;

    @Inject
    protected PageProviderService pageProviderService;

    @Inject
    protected TransactionalFeature txFeature;

    @Test
    public void testNxqlPredicateWithHint() {
        PageProviderDefinition ppdef = pageProviderService.getPageProviderDefinition("NXQL_WITH_HINT");
        assertNotNull(ppdef);
        long pageSize = 5;
        DocumentModel model = session.createDocumentModel("/", "doc", "AdvancedSearch");
        model.setProperty("advanced_search", "fulltext_all", "you know");
        model.setProperty("advanced_search", "description", "for search");
        SearchServicePageProvider pp = (SearchServicePageProvider) pageProviderService.getPageProvider(
                PageProviderSpec.builder(ppdef)
                                .searchDocument(model)
                                .pageSize(pageSize)
                                .currentPage(0L)
                                .property(CORE_SESSION_PROPERTY, (Serializable) session)
                                .build());
        assertNotNull(pp);
        pp.getCurrentPage(); // This is needed to build the nxql query
        var nxql = pp.getCurrentQuery();
        var queryTransformer = new OpenSearchQueryTransformer(Map.of("enhanced", "nxutest"),
                Map.of("fuzzy", new FuzzyOpenSearchHintQueryBuilder()));
        String esquery = queryTransformer.apply(SearchQuery.builder(nxql, session).build()).source().query().toString();
        assertEqualsEvenUnderWindows("""
                {
                  "bool" : {
                    "must" : [
                      {
                        "constant_score" : {
                          "filter" : {
                            "term" : {
                              "dc:title.fulltext" : {
                                "value" : "you know",
                                "boost" : 1.0
                              }
                            }
                          },
                          "boost" : 1.0
                        }
                      },
                      {
                        "fuzzy" : {
                          "my_field" : {
                            "value" : "for search",
                            "fuzziness" : "AUTO",
                            "prefix_length" : 0,
                            "max_expansions" : 50,
                            "transpositions" : true,
                            "boost" : 1.0
                          }
                        }
                      },
                      {
                        "constant_score" : {
                          "filter" : {
                            "terms" : {
                              "my_subject" : [
                                "foo",
                                "bar"
                              ],
                              "boost" : 1.0
                            }
                          },
                          "boost" : 1.0
                        }
                      }
                    ],
                    "adjust_pure_negative" : true,
                    "boost" : 1.0
                  }
                }""", esquery);
    }

    @Test
    public void testNxqlPredicateWithHintInParameter() {
        PageProviderDefinition ppdef = pageProviderService.getPageProviderDefinition("NXQL_WITH_HINT_IN_PARAMETER");
        assertNotNull(ppdef);
        long pageSize = 5;
        DocumentModel model = session.createDocumentModel("/", "doc", "AdvancedSearch");
        model.setProperty("advanced_search", "fulltext_all", "you know");
        model.setProperty("advanced_search", "description", "for search");
        SearchServicePageProvider pp = (SearchServicePageProvider) pageProviderService.getPageProvider(
                PageProviderSpec.builder(ppdef)
                                .searchDocument(model)
                                .pageSize(pageSize)
                                .currentPage(0L)
                                .property(CORE_SESSION_PROPERTY, (Serializable) session)
                                .build());
        assertNotNull(pp);
        pp.getCurrentPage(); // This is needed to build the nxql query
        var nxql = pp.getCurrentQuery();
        var queryTransformer = new OpenSearchQueryTransformer(Map.of("enhanced", "nxutest"),
                Map.of("fuzzy", new FuzzyOpenSearchHintQueryBuilder()));
        String esquery = queryTransformer.apply(SearchQuery.builder(nxql, session).build()).source().query().toString();
        assertEqualsEvenUnderWindows("""
                {
                  "bool" : {
                    "must" : [
                      {
                        "constant_score" : {
                          "filter" : {
                            "term" : {
                              "dc:title.fulltext" : {
                                "value" : "you know",
                                "boost" : 1.0
                              }
                            }
                          },
                          "boost" : 1.0
                        }
                      },
                      {
                        "fuzzy" : {
                          "my_field" : {
                            "value" : "for search",
                            "fuzziness" : "AUTO",
                            "prefix_length" : 0,
                            "max_expansions" : 50,
                            "transpositions" : true,
                            "boost" : 1.0
                          }
                        }
                      },
                      {
                        "constant_score" : {
                          "filter" : {
                            "terms" : {
                              "my_subject" : [
                                "foo",
                                "bar"
                              ],
                              "boost" : 1.0
                            }
                          },
                          "boost" : 1.0
                        }
                      }
                    ],
                    "adjust_pure_negative" : true,
                    "boost" : 1.0
                  }
                }""", esquery);
    }

    protected void assertEqualsEvenUnderWindows(String expected, String actual) {
        if (SystemUtils.IS_OS_WINDOWS) {
            // make tests pass under Windows
            expected = expected.trim();
            expected = expected.replace("\n", "");
            expected = expected.replace("\r", "");
            actual = actual.trim();
            actual = actual.replace("\n", "");
            actual = actual.replace("\r", "");
        }
        assertEquals(expected, actual);
    }

    @Test
    public void iCanUseFulltextOperatorPageProvider() {
        DocumentModel coll1 = session.createDocumentModel("/", "testMatchPhrasePrefix" + 1, "File");
        coll1.setPropertyValue("dc:title", coll1.getName());
        DocumentModel coll2 = session.createDocumentModel("/", "testMatchPhrasePrefix" + 2, "File");
        coll2.setPropertyValue("dc:title", coll2.getName());
        DocumentModel fufu = session.createDocumentModel("/", "furtiveFile", "File");
        fufu.setPropertyValue("dc:title", fufu.getName());

        session.createDocument(coll1);
        session.createDocument(coll2);
        session.createDocument(fufu);

        txFeature.nextTransaction();

        PageProviderDefinition ppdef = pageProviderService.getPageProviderDefinition("default_match_phrase_prefix");

        @SuppressWarnings("unchecked")
        PageProvider<DocumentModel> pp = (PageProvider<DocumentModel>) pageProviderService.getPageProvider(
                PageProviderSpec.builder(ppdef)
                                .currentPage(0L)
                                .property(CORE_SESSION_PROPERTY, (Serializable) session)
                                .parameters("testMat")
                                .build());

        List<DocumentModel> page = pp.getCurrentPage();
        assertEquals(2, page.size());
        assertEquals(coll1.getName(), page.get(0).getName());
        assertEquals(coll2.getName(), page.get(1).getName());
    }
}
