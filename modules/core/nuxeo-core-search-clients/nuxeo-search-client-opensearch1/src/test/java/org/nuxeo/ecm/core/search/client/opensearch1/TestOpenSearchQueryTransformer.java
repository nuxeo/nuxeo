/*
 * (C) Copyright 2014-2025 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nuxeo
 */
package org.nuxeo.ecm.core.search.client.opensearch1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.search.SearchServiceImpl.RELATION_INDEXING_ENABLED_PROP;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import org.apache.commons.lang3.SystemUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.schema.CoreSchemaFeature;
import org.nuxeo.ecm.core.search.SearchIndex;
import org.nuxeo.ecm.core.search.SearchQuery;
import org.nuxeo.ecm.core.search.client.opensearch1.hint.CommonOpenSearchHintQueryBuilder;
import org.nuxeo.ecm.core.search.client.opensearch1.hint.FuzzyOpenSearchHintQueryBuilder;
import org.nuxeo.ecm.core.search.client.opensearch1.hint.GeoBoundingBoxOpenSearchHintQueryBuilder;
import org.nuxeo.ecm.core.search.client.opensearch1.hint.GeoDistanceOpenSearchHintQueryBuilder;
import org.nuxeo.ecm.core.search.client.opensearch1.hint.GeoShapeOpenSearchHintQueryBuilder;
import org.nuxeo.ecm.core.search.client.opensearch1.hint.MatchOpenSearchHintQueryBuilder;
import org.nuxeo.ecm.core.search.client.opensearch1.hint.MatchPhraseOpenSearchHintQueryBuilder;
import org.nuxeo.ecm.core.search.client.opensearch1.hint.MatchPhrasePrefixOpenSearchHintQueryBuilder;
import org.nuxeo.ecm.core.search.client.opensearch1.hint.MoreLikeThisOpenSearchHintQueryBuilder;
import org.nuxeo.ecm.core.search.client.opensearch1.hint.MultiMatchOpenSearchHintQueryBuilder;
import org.nuxeo.ecm.core.search.client.opensearch1.hint.NestedFilesOpenSearchHintQueryBuilder;
import org.nuxeo.ecm.core.search.client.opensearch1.hint.QueryStringOpenSearchHintQueryBuilder;
import org.nuxeo.ecm.core.search.client.opensearch1.hint.RegexOpenSearchHintQueryBuilder;
import org.nuxeo.ecm.core.search.client.opensearch1.hint.SimpleQueryStringOpenSearchHintQueryBuilder;
import org.nuxeo.ecm.core.search.client.opensearch1.hint.TestBoolQueryOpenSearchHintQueryBuilder;
import org.nuxeo.ecm.core.search.client.opensearch1.hint.TestTermOpenSearchHintQueryBuilder;
import org.nuxeo.ecm.core.search.client.opensearch1.hint.WildcardOpenSearchHintQueryBuilder;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.WithFrameworkProperty;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.search.builder.SearchSourceBuilder;

/**
 * Test that NXQL can be used to generate OpenSearch queries.
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
@RunWith(FeaturesRunner.class)
@Features(CoreSchemaFeature.class)
@Deploy("org.nuxeo.ecm.core:OSGI-INF/SecurityService.xml")
@Deploy("org.nuxeo.ecm.core.search.client.opensearch1.test:OSGI-INF/opensearch-search-client-schema-test-contrib.xml")
public class TestOpenSearchQueryTransformer {

    protected static final SearchIndex SEARCH_INDEX = SearchIndex.of("test", "opensearch", "enhanced");

    protected static final Function<String, SearchQuery> SEARCH_QUERY_BUILDER = nxql -> SearchQuery.builder(nxql)
                                                                                                   .searchIndex(
                                                                                                           SEARCH_INDEX)
                                                                                                   .build();

    protected static final OpenSearchQueryTransformer TRANSFORMER = new OpenSearchQueryTransformer(
            Map.of("enhanced", "nxutest"), Map.of());

    protected static final Function<String, String> BUILDER = SEARCH_QUERY_BUILDER.andThen(TRANSFORMER)
                                                                                  .andThen(SearchRequest::source)
                                                                                  .andThen(SearchSourceBuilder::query)
                                                                                  .andThen(Objects::toString);

    @Test
    public void testConverterSelect() {
        String es = BUILDER.apply("select * from Document");
        assertEqualsEvenUnderWindows("""
                {
                  "match_all" : {
                    "boost" : 1.0
                  }
                }""", es);
        es = BUILDER.apply("select * from CommonDocument, Document");
        assertEqualsEvenUnderWindows("""
                {
                  "match_all" : {
                    "boost" : 1.0
                  }
                }""", es);
        es = BUILDER.apply("select * from CommonDocument");
        assertEqualsEvenUnderWindows("""
                {
                  "bool" : {
                    "must" : [
                      {
                        "match_all" : {
                          "boost" : 1.0
                        }
                      }
                    ],
                    "filter" : [
                      {
                        "terms" : {
                          "ecm:primaryType" : [
                            "CommonDocument"
                          ],
                          "boost" : 1.0
                        }
                      }
                    ],
                    "adjust_pure_negative" : true,
                    "boost" : 1.0
                  }
                }""", es);
        es = BUILDER.apply("select * from CommonDocument, OpenSearchDocument");
        assertEqualsEvenUnderWindows("""
                {
                  "bool" : {
                    "must" : [
                      {
                        "match_all" : {
                          "boost" : 1.0
                        }
                      }
                    ],
                    "filter" : [
                      {
                        "terms" : {
                          "ecm:primaryType" : [
                            "CommonDocument",
                            "OpenSearchDocument"
                          ],
                          "boost" : 1.0
                        }
                      }
                    ],
                    "adjust_pure_negative" : true,
                    "boost" : 1.0
                  }
                }""", es);
    }

    @Test
    public void testConverterEQUALS() {
        String es = BUILDER.apply("select * from Document where f1=1");
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "term" : {
                        "f1" : {
                          "value" : "1",
                          "boost" : 1.0
                        }
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);

        es = BUILDER.apply("select * from Document where f1 != 1");
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "bool" : {
                        "must_not" : [
                          {
                            "term" : {
                              "f1" : {
                                "value" : "1",
                                "boost" : 1.0
                              }
                            }
                          }
                        ],
                        "adjust_pure_negative" : true,
                        "boost" : 1.0
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);

        es = BUILDER.apply("select * from Document where f1 <> 1");
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "bool" : {
                        "must_not" : [
                          {
                            "term" : {
                              "f1" : {
                                "value" : "1",
                                "boost" : 1.0
                              }
                            }
                          }
                        ],
                        "adjust_pure_negative" : true,
                        "boost" : 1.0
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);

    }

    @Test
    public void testConverterIN() {
        String es = BUILDER.apply("select * from Document where f1 IN (1)");
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "terms" : {
                        "f1" : [
                          "1"
                        ],
                        "boost" : 1.0
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);
        es = BUILDER.apply("select * from Document where f1 NOT IN (1, '2', 3)");
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "bool" : {
                        "must_not" : [
                          {
                            "terms" : {
                              "f1" : [
                                "1",
                                "2",
                                "3"
                              ],
                              "boost" : 1.0
                            }
                          }
                        ],
                        "adjust_pure_negative" : true,
                        "boost" : 1.0
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);
    }

    @Test
    public void testConverterLIKE() {
        String es = BUILDER.apply("select * from Document where f1 LIKE 'foo%'");
        assertEqualsEvenUnderWindows("""
                {
                  "match_phrase_prefix" : {
                    "f1" : {
                      "query" : "foo",
                      "slop" : 0,
                      "max_expansions" : 50,
                      "zero_terms_query" : "NONE",
                      "boost" : 1.0
                    }
                  }
                }""", es);
        es = BUILDER.apply("select * from Document where f1 LIKE '%Foo%'");
        assertEqualsEvenUnderWindows("""
                {
                  "wildcard" : {
                    "f1" : {
                      "wildcard" : "*Foo*",
                      "boost" : 1.0
                    }
                  }
                }""", es);
        es = BUILDER.apply("select * from Document where f1 NOT LIKE 'Foo%'");
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "bool" : {
                        "must_not" : [
                          {
                            "match_phrase_prefix" : {
                              "f1" : {
                                "query" : "Foo",
                                "slop" : 0,
                                "max_expansions" : 50,
                                "zero_terms_query" : "NONE",
                                "boost" : 1.0
                              }
                            }
                          }
                        ],
                        "adjust_pure_negative" : true,
                        "boost" : 1.0
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);
        // invalid input
        BUILDER.apply("select * from Document where f1 LIKE '(foo.*$#@^'");
    }

    @Test
    public void testConverterLIKEWildcard() {
        String es;
        es = BUILDER.apply("SELECT * FROM Document WHERE f1 LIKE '%foo'");
        assertEqualsEvenUnderWindows("""
                {
                  "wildcard" : {
                    "f1" : {
                      "wildcard" : "*foo",
                      "boost" : 1.0
                    }
                  }
                }""", es);
        es = BUILDER.apply("SELECT * FROM Document WHERE f1 LIKE '_foo'");
        assertEqualsEvenUnderWindows("""
                {
                  "wildcard" : {
                    "f1" : {
                      "wildcard" : "?foo",
                      "boost" : 1.0
                    }
                  }
                }""", es);
        es = BUILDER.apply("SELECT * FROM Document WHERE f1 LIKE '?foo'");
        assertEqualsEvenUnderWindows("""
                {
                  "wildcard" : {
                    "f1" : {
                      "wildcard" : "\\\\?foo",
                      "boost" : 1.0
                    }
                  }
                }""", es);
        // * is also accepted as a wildcard (compat)
        es = BUILDER.apply("SELECT * FROM Document WHERE f1 LIKE '*foo'");
        assertEqualsEvenUnderWindows("""
                {
                  "wildcard" : {
                    "f1" : {
                      "wildcard" : "*foo",
                      "boost" : 1.0
                    }
                  }
                }""", es);
        // NXQL escaping
        es = BUILDER.apply("SELECT * FROM Document WHERE f1 LIKE 'foo\\_bar\\%'");
        assertEqualsEvenUnderWindows("""
                {
                  "wildcard" : {
                    "f1" : {
                      "wildcard" : "foo_bar%",
                      "boost" : 1.0
                    }
                  }
                }""", es);
    }

    @Test
    public void testConverterILIKE() {
        String es = BUILDER.apply("select * from Document where f1 ILIKE 'Foo%'");
        assertEqualsEvenUnderWindows("""
                {
                  "match_phrase_prefix" : {
                    "f1.lowercase" : {
                      "query" : "foo",
                      "slop" : 0,
                      "max_expansions" : 50,
                      "zero_terms_query" : "NONE",
                      "boost" : 1.0
                    }
                  }
                }""", es);
        es = BUILDER.apply("select * from Document where f1 ILIKE '%Foo%'");
        assertEqualsEvenUnderWindows("""
                {
                  "wildcard" : {
                    "f1.lowercase" : {
                      "wildcard" : "*foo*",
                      "boost" : 1.0
                    }
                  }
                }""", es);
        es = BUILDER.apply("select * from Document where f1 NOT ILIKE 'Foo%'");
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "bool" : {
                        "must_not" : [
                          {
                            "match_phrase_prefix" : {
                              "f1.lowercase" : {
                                "query" : "foo",
                                "slop" : 0,
                                "max_expansions" : 50,
                                "zero_terms_query" : "NONE",
                                "boost" : 1.0
                              }
                            }
                          }
                        ],
                        "adjust_pure_negative" : true,
                        "boost" : 1.0
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);
    }

    @Test
    public void testConverterIsNULL() {
        String es = BUILDER.apply("select * from Document where f1 IS NULL");
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "bool" : {
                        "must_not" : [
                          {
                            "exists" : {
                              "field" : "f1",
                              "boost" : 1.0
                            }
                          }
                        ],
                        "adjust_pure_negative" : true,
                        "boost" : 1.0
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);
        es = BUILDER.apply("select * from Document where f1 IS NOT NULL");
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "exists" : {
                        "field" : "f1",
                        "boost" : 1.0
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);
    }

    @Test
    public void testConverterBETWEEN() {
        String es = BUILDER.apply("select * from Document where f1 BETWEEN 1 AND 2");
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "range" : {
                        "f1" : {
                          "from" : "1",
                          "to" : "2",
                          "include_lower" : true,
                          "include_upper" : true,
                          "boost" : 1.0
                        }
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);
        es = BUILDER.apply("select * from Document where f1 NOT BETWEEN 1 AND 2");
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "bool" : {
                        "must_not" : [
                          {
                            "range" : {
                              "f1" : {
                                "from" : "1",
                                "to" : "2",
                                "include_lower" : true,
                                "include_upper" : true,
                                "boost" : 1.0
                              }
                            }
                          }
                        ],
                        "adjust_pure_negative" : true,
                        "boost" : 1.0
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);
    }

    @Test
    public void testConverterSTARTSWITH() {
        String es = BUILDER.apply("select * from Document where ecm:path STARTSWITH '/the/path'");
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "bool" : {
                        "must" : [
                          {
                            "term" : {
                              "ecm:path.children" : {
                                "value" : "/the/path",
                                "boost" : 1.0
                              }
                            }
                          }
                        ],
                        "must_not" : [
                          {
                            "term" : {
                              "ecm:path" : {
                                "value" : "/the/path",
                                "boost" : 1.0
                              }
                            }
                          }
                        ],
                        "adjust_pure_negative" : true,
                        "boost" : 1.0
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);
        es = BUILDER.apply("select * from Document where ecm:path STARTSWITH '/'");
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "exists" : {
                        "field" : "ecm:parentId",
                        "boost" : 1.0
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);
        es = BUILDER.apply("select * from Document where ecm:path STARTSWITH '/the/path/'");
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "bool" : {
                        "must" : [
                          {
                            "term" : {
                              "ecm:path.children" : {
                                "value" : "/the/path",
                                "boost" : 1.0
                              }
                            }
                          }
                        ],
                        "must_not" : [
                          {
                            "term" : {
                              "ecm:path" : {
                                "value" : "/the/path",
                                "boost" : 1.0
                              }
                            }
                          }
                        ],
                        "adjust_pure_negative" : true,
                        "boost" : 1.0
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);
        // for other field than ecm:path we want to match the root
        es = BUILDER.apply("select * from Document where dc:coverage STARTSWITH 'Europe/France'");
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "term" : {
                        "dc:coverage.children" : {
                          "value" : "Europe/France",
                          "boost" : 1.0
                        }
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);
    }

    @Test
    // force NXQL.nowPlusPeriodAndDuration to return a known date/time, for tests
    @WithFrameworkProperty(name = NXQL.TEST_NXQL_NOW, value = "2001-02-03T04:05:06.007Z")
    public void testConverterNOW() {
        String es = BUILDER.apply("SELECT * FROM Document WHERE f1 = NOW()");
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "term" : {
                        "f1" : {
                          "value" : "2001-02-03T04:05:06.007Z",
                          "boost" : 1.0
                        }
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);
        es = BUILDER.apply("SELECT * FROM Document WHERE f1 = NOW('-P1D')");
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "term" : {
                        "f1" : {
                          "value" : "2001-02-02T04:05:06.007Z",
                          "boost" : 1.0
                        }
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);
    }

    @Test
    public void testConverterAncestorId() {
        String es = BUILDER.apply(
                "select * from Document where ecm:ancestorId = 'c5904f77-299a-411e-8477-81d3102a81f9'");
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "term" : {
                        "ecm:ancestorId" : {
                          "value" : "c5904f77-299a-411e-8477-81d3102a81f9",
                          "boost" : 1.0
                        }
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);
        es = BUILDER.apply("select * from Document where ecm:ancestorId != 'c5904f77-299a-411e-8477-81d3102a81f9'");
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "bool" : {
                        "must_not" : [
                          {
                            "term" : {
                              "ecm:ancestorId" : {
                                "value" : "c5904f77-299a-411e-8477-81d3102a81f9",
                                "boost" : 1.0
                              }
                            }
                          }
                        ],
                        "adjust_pure_negative" : true,
                        "boost" : 1.0
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);
    }

    @Test
    public void testConverterIsVersion() {
        String es = BUILDER.apply("select * from Document where ecm:isVersion = 1");
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "term" : {
                        "ecm:isVersion" : {
                          "value" : "true",
                          "boost" : 1.0
                        }
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);
        String es2 = BUILDER.apply("select * from Document where ecm:isCheckedInVersion = 1");
        assertEqualsEvenUnderWindows(es, es2);
    }

    @Test
    public void testConverterFulltext() {
        // Given a search on a fulltext field
        String es = BUILDER.apply("select * from Document where ecm:fulltext='+foo -bar'");
        // then we have a simple query text and not a filter
        assertEqualsEvenUnderWindows("""
                {
                  "simple_query_string" : {
                    "query" : "+foo -bar",
                    "fields" : [
                      "all_field^1.0"
                    ],
                    "analyzer" : "fulltext",
                    "flags" : -1,
                    "default_operator" : "and",
                    "analyze_wildcard" : false,
                    "auto_generate_synonyms_phrase_query" : true,
                    "fuzzy_prefix_length" : 0,
                    "fuzzy_max_expansions" : 50,
                    "fuzzy_transpositions" : true,
                    "boost" : 1.0
                  }
                }""", es);
        es = BUILDER.apply("select * from Document where ecm:fulltext_someindex LIKE '+foo -bar'");
        // don't handle nxql fulltext index definition, match to _all field
        assertEqualsEvenUnderWindows("""
                {
                  "simple_query_string" : {
                    "query" : "+foo -bar",
                    "fields" : [
                      "all_field^1.0"
                    ],
                    "analyzer" : "fulltext",
                    "flags" : -1,
                    "default_operator" : "and",
                    "analyze_wildcard" : false,
                    "auto_generate_synonyms_phrase_query" : true,
                    "fuzzy_prefix_length" : 0,
                    "fuzzy_max_expansions" : 50,
                    "fuzzy_transpositions" : true,
                    "boost" : 1.0
                  }
                }""", es);
        es = BUILDER.apply("select * from Document where ecm:fulltext.dc:title!='+foo -bar'");
        // request on field match field.fulltext
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "bool" : {
                        "must_not" : [
                          {
                            "simple_query_string" : {
                              "query" : "+foo -bar",
                              "fields" : [
                                "dc:title.fulltext^1.0"
                              ],
                              "analyzer" : "fulltext",
                              "flags" : -1,
                              "default_operator" : "and",
                              "analyze_wildcard" : false,
                              "auto_generate_synonyms_phrase_query" : true,
                              "fuzzy_prefix_length" : 0,
                              "fuzzy_max_expansions" : 50,
                              "fuzzy_transpositions" : true,
                              "boost" : 1.0
                            }
                          }
                        ],
                        "adjust_pure_negative" : true,
                        "boost" : 1.0
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);
    }

    @Test
    public void testConverterFulltextElasticsearchPrefix() {
        // Given a search on a fulltext field with the
        // elasticsearch-specific prefix
        String es = BUILDER.apply("SELECT * FROM Document WHERE ecm:fulltext = 'es: foo bar'");
        // then we have a simple query text and not a filter
        // and we have the OR operator
        assertEqualsEvenUnderWindows("""
                {
                  "simple_query_string" : {
                    "query" : "foo bar",
                    "fields" : [
                      "all_field^1.0"
                    ],
                    "analyzer" : "fulltext",
                    "flags" : -1,
                    "default_operator" : "or",
                    "analyze_wildcard" : false,
                    "auto_generate_synonyms_phrase_query" : true,
                    "fuzzy_prefix_length" : 0,
                    "fuzzy_max_expansions" : 50,
                    "fuzzy_transpositions" : true,
                    "boost" : 1.0
                  }
                }""", es);
    }

    @Test
    public void testConverterIsTrashedWithProperty() {
        String sqlNotTrashed = "SELECT * FROM Document WHERE ecm:isTrashed = 0";
        String sqlTrashed = "SELECT * FROM Document WHERE ecm:isTrashed = 1";

        String es = BUILDER.apply(sqlTrashed);
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "term" : {
                        "ecm:isTrashed" : {
                          "value" : "true",
                          "boost" : 1.0
                        }
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);

        es = BUILDER.apply(sqlNotTrashed);
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "term" : {
                        "ecm:isTrashed" : {
                          "value" : "false",
                          "boost" : 1.0
                        }
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);
    }

    @Test
    public void testConverterWhereCombination() {
        String es = BUILDER.apply("select * from Document where f1=1 AND f2=2");
        assertEqualsEvenUnderWindows("""
                {
                  "bool" : {
                    "must" : [
                      {
                        "constant_score" : {
                          "filter" : {
                            "term" : {
                              "f1" : {
                                "value" : "1",
                                "boost" : 1.0
                              }
                            }
                          },
                          "boost" : 1.0
                        }
                      },
                      {
                        "constant_score" : {
                          "filter" : {
                            "term" : {
                              "f2" : {
                                "value" : "2",
                                "boost" : 1.0
                              }
                            }
                          },
                          "boost" : 1.0
                        }
                      }
                    ],
                    "adjust_pure_negative" : true,
                    "boost" : 1.0
                  }
                }""", es);
        es = BUILDER.apply("select * from Document where f1=1 OR f2=2");
        assertEqualsEvenUnderWindows("""
                {
                  "bool" : {
                    "should" : [
                      {
                        "constant_score" : {
                          "filter" : {
                            "term" : {
                              "f1" : {
                                "value" : "1",
                                "boost" : 1.0
                              }
                            }
                          },
                          "boost" : 1.0
                        }
                      },
                      {
                        "constant_score" : {
                          "filter" : {
                            "term" : {
                              "f2" : {
                                "value" : "2",
                                "boost" : 1.0
                              }
                            }
                          },
                          "boost" : 1.0
                        }
                      }
                    ],
                    "adjust_pure_negative" : true,
                    "boost" : 1.0
                  }
                }""", es);

        es = BUILDER.apply("select * from Document where f1=1 AND f2=2 AND f3=3");
        // assertEquals("foo", es);

        es = BUILDER.apply("select * from Document where f1=1 OR f2=2 OR f3=3");
        // assertEquals("foo", es);

        es = BUILDER.apply("select * from Document where f1=1 OR f2 LIKE 'foo' OR f3=3");
        // assertEquals("foo", es);

        es = BUILDER.apply("select * from Document where (f1=1 OR f2=2) AND f3=3");
        assertEqualsEvenUnderWindows("""
                {
                  "bool" : {
                    "must" : [
                      {
                        "bool" : {
                          "should" : [
                            {
                              "constant_score" : {
                                "filter" : {
                                  "term" : {
                                    "f1" : {
                                      "value" : "1",
                                      "boost" : 1.0
                                    }
                                  }
                                },
                                "boost" : 1.0
                              }
                            },
                            {
                              "constant_score" : {
                                "filter" : {
                                  "term" : {
                                    "f2" : {
                                      "value" : "2",
                                      "boost" : 1.0
                                    }
                                  }
                                },
                                "boost" : 1.0
                              }
                            }
                          ],
                          "adjust_pure_negative" : true,
                          "boost" : 1.0
                        }
                      },
                      {
                        "constant_score" : {
                          "filter" : {
                            "term" : {
                              "f3" : {
                                "value" : "3",
                                "boost" : 1.0
                              }
                            }
                          },
                          "boost" : 1.0
                        }
                      }
                    ],
                    "adjust_pure_negative" : true,
                    "boost" : 1.0
                  }
                }""", es);
    }

    @Test
    public void testConverterComplex() {
        String es = BUILDER.apply("select * from Document where (f1 LIKE '1%' OR f2 LIKE '2%') AND f3=3");
        assertEqualsEvenUnderWindows("""
                {
                  "bool" : {
                    "must" : [
                      {
                        "bool" : {
                          "should" : [
                            {
                              "match_phrase_prefix" : {
                                "f1" : {
                                  "query" : "1",
                                  "slop" : 0,
                                  "max_expansions" : 50,
                                  "zero_terms_query" : "NONE",
                                  "boost" : 1.0
                                }
                              }
                            },
                            {
                              "match_phrase_prefix" : {
                                "f2" : {
                                  "query" : "2",
                                  "slop" : 0,
                                  "max_expansions" : 50,
                                  "zero_terms_query" : "NONE",
                                  "boost" : 1.0
                                }
                              }
                            }
                          ],
                          "adjust_pure_negative" : true,
                          "boost" : 1.0
                        }
                      },
                      {
                        "constant_score" : {
                          "filter" : {
                            "term" : {
                              "f3" : {
                                "value" : "3",
                                "boost" : 1.0
                              }
                            }
                          },
                          "boost" : 1.0
                        }
                      }
                    ],
                    "adjust_pure_negative" : true,
                    "boost" : 1.0
                  }
                }""", es);
        // assertEquals("foo", es);
        es = BUILDER.apply(
                "select * from Document where ecm:fulltext='foo bar' AND ecm:path STARTSWITH '/foo/bar' OR ecm:path='/foo/'");
        // assertEquals("foo", es);

        es = BUILDER.apply("select * from CommonDocument where f1 IN ('foo', 'bar', 'foo') AND NOT f2>=3");
        assertEqualsEvenUnderWindows("""
                {
                  "bool" : {
                    "must" : [
                      {
                        "bool" : {
                          "must" : [
                            {
                              "constant_score" : {
                                "filter" : {
                                  "terms" : {
                                    "f1" : [
                                      "foo",
                                      "bar",
                                      "foo"
                                    ],
                                    "boost" : 1.0
                                  }
                                },
                                "boost" : 1.0
                              }
                            },
                            {
                              "bool" : {
                                "must_not" : [
                                  {
                                    "constant_score" : {
                                      "filter" : {
                                        "range" : {
                                          "f2" : {
                                            "from" : "3",
                                            "to" : null,
                                            "include_lower" : true,
                                            "include_upper" : true,
                                            "boost" : 1.0
                                          }
                                        }
                                      },
                                      "boost" : 1.0
                                    }
                                  }
                                ],
                                "adjust_pure_negative" : true,
                                "boost" : 1.0
                              }
                            }
                          ],
                          "adjust_pure_negative" : true,
                          "boost" : 1.0
                        }
                      }
                    ],
                    "filter" : [
                      {
                        "terms" : {
                          "ecm:primaryType" : [
                            "CommonDocument"
                          ],
                          "boost" : 1.0
                        }
                      }
                    ],
                    "adjust_pure_negative" : true,
                    "boost" : 1.0
                  }
                }""", es);
    }

    @Test
    public void testConverterWhereWithoutSelect() {
        String es = BUILDER.apply("f1=1");
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "term" : {
                        "f1" : {
                          "value" : "1",
                          "boost" : 1.0
                        }
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);
        es = BUILDER.apply(null);
        assertEqualsEvenUnderWindows("""
                {
                  "match_all" : {
                    "boost" : 1.0
                  }
                }""", es);
        es = BUILDER.apply("");
        assertEqualsEvenUnderWindows("""
                {
                  "match_all" : {
                    "boost" : 1.0
                  }
                }""", es);
    }

    @Test
    public void testConvertComplexProperties() {
        String es = BUILDER.apply("select * from Document where file:content/name = 'foo'");
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "term" : {
                        "file:content.name" : {
                          "value" : "foo",
                          "boost" : 1.0
                        }
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);
    }

    @Test
    public void testConvertComplexListProperties() {
        String es = BUILDER.apply("select * from Document where dc:subjects/* = 'foo'");
        // this is supported and match any element of the list
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "term" : {
                        "dc:subjects" : {
                          "value" : "foo",
                          "boost" : 1.0
                        }
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);

        es = BUILDER.apply("select * from Document where files:files/*/file/length=123");
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "term" : {
                        "files:files.file.length" : {
                          "value" : "123",
                          "boost" : 1.0
                        }
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);

    }

    @Test
    public void testConvertComplexListPropertiesUnsupported() {
        String es = BUILDER.apply("select * from Document where dc:subjects/3 = 'foo'");
        // This is not supported and generate query that is going to match nothing
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "term" : {
                        "dc:subjects.3" : {
                          "value" : "foo",
                          "boost" : 1.0
                        }
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);

        es = BUILDER.apply("select * from Document where dc:subjects/*1 = 'foo'");
        // This is not supported and generate query that is going to match nothing
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "term" : {
                        "dc:subjects1" : {
                          "value" : "foo",
                          "boost" : 1.0
                        }
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);
        es = BUILDER.apply("select * from Document where files:files/*1/file/length=123");
        // This is not supported and generate query that is going to match nothing
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "term" : {
                        "files:files1.file.length" : {
                          "value" : "123",
                          "boost" : 1.0
                        }
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);

    }

    @Test
    public void testOrderByFromNxql() {
        var osSearchRequest = SEARCH_QUERY_BUILDER.andThen(TRANSFORMER).apply("name='foo' ORDER BY name DESC");
        String es = osSearchRequest.source().query().toString();
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "term" : {
                        "name" : {
                          "value" : "foo",
                          "boost" : 1.0
                        }
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);
        var osSorts = osSearchRequest.source().sorts();
        assertEquals(1, osSorts.size());
        assertEquals("""
                {
                  "name" : {
                    "order" : "desc",
                    "unmapped_type" : "keyword"
                  }
                }""", osSorts.getFirst().toString());
    }

    @Test
    public void testOrderByWithComplexProperties() {
        var osSearchRequest = SEARCH_QUERY_BUILDER.andThen(
                TRANSFORMER).apply("SELECT * FROM CommonDocument ORDER BY file:content/name DESC");
        String es = osSearchRequest.source().query().toString();
        assertEqualsEvenUnderWindows("""
                {
                  "bool" : {
                    "must" : [
                      {
                        "match_all" : {
                          "boost" : 1.0
                        }
                      }
                    ],
                    "filter" : [
                      {
                        "terms" : {
                          "ecm:primaryType" : [
                            "CommonDocument"
                          ],
                          "boost" : 1.0
                        }
                      }
                    ],
                    "adjust_pure_negative" : true,
                    "boost" : 1.0
                  }
                }""", es);
        var osSorts = osSearchRequest.source().sorts();
        assertEquals(1, osSorts.size());
        assertEquals("""
                {
                  "file:content.name" : {
                    "order" : "desc",
                    "unmapped_type" : "keyword"
                  }
                }""", osSorts.getFirst().toString());
    }

    @Test
    public void testConvertHint() {
        String es = BUILDER.apply("select * from Document where /*+ES: INDEX(some:field) */ dc:title = 'foo'");
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "term" : {
                        "some:field" : {
                          "value" : "foo",
                          "boost" : 1.0
                        }
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);

        es = BUILDER.apply("select * from Document where /*+ES: INDEX(some:field) */ dc:title != 'foo'");
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "bool" : {
                        "must_not" : [
                          {
                            "term" : {
                              "some:field" : {
                                "value" : "foo",
                                "boost" : 1.0
                              }
                            }
                          }
                        ],
                        "adjust_pure_negative" : true,
                        "boost" : 1.0
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);
    }

    @Test
    public void testConvertHintOperator() {
        String es = newQueryBuilder(Map.of("match", new MatchOpenSearchHintQueryBuilder())).apply(
                "select * from Document where /*+ES: INDEX(some:field) ANALYZER(my_analyzer) OPERATOR(match) */ dc:subjects = 'foo'");
        assertEqualsEvenUnderWindows("""
                {
                  "match" : {
                    "some:field" : {
                      "query" : "foo",
                      "operator" : "OR",
                      "analyzer" : "my_analyzer",
                      "prefix_length" : 0,
                      "max_expansions" : 50,
                      "fuzzy_transpositions" : true,
                      "lenient" : false,
                      "zero_terms_query" : "NONE",
                      "auto_generate_synonyms_phrase_query" : true,
                      "boost" : 1.0
                    }
                  }
                }""", es);
        es = newQueryBuilder(Map.of("match_phrase", new MatchPhraseOpenSearchHintQueryBuilder())).apply(
                "select * from Document where /*+ES: OPERATOR(match_phrase) */ dc:title = 'foo'");
        assertEqualsEvenUnderWindows("""
                {
                  "match_phrase" : {
                    "dc:title" : {
                      "query" : "foo",
                      "slop" : 0,
                      "zero_terms_query" : "NONE",
                      "boost" : 1.0
                    }
                  }
                }""", es);
        es = newQueryBuilder(Map.of("match_phrase_prefix", new MatchPhrasePrefixOpenSearchHintQueryBuilder())).apply(
                "select * from Document where /*+ES: OPERATOR(match_phrase_prefix) */ dc:title = 'this is a test'");
        assertEqualsEvenUnderWindows("""
                {
                  "match_phrase_prefix" : {
                    "dc:title" : {
                      "query" : "this is a test",
                      "slop" : 0,
                      "max_expansions" : 50,
                      "zero_terms_query" : "NONE",
                      "boost" : 1.0
                    }
                  }
                }""", es);
        es = newQueryBuilder(Map.of("multi_match", new MultiMatchOpenSearchHintQueryBuilder())).apply(
                "select * from Document where /*+ES: INDEX(dc:title^3,dc:description) OPERATOR(multi_match) */ dc:title = 'this is a test'");
        // fields are not ordered
        assertIn(es, """
                {
                  "multi_match" : {
                    "query" : "this is a test",
                    "fields" : [
                      "dc:description^1.0",
                      "dc:title^3.0"
                    ],
                    "type" : "best_fields",
                    "operator" : "OR",
                    "slop" : 0,
                    "prefix_length" : 0,
                    "max_expansions" : 50,
                    "zero_terms_query" : "NONE",
                    "auto_generate_synonyms_phrase_query" : true,
                    "fuzzy_transpositions" : true,
                    "boost" : 1.0
                  }
                }""", """
                {
                  "multi_match" : {
                    "query" : "this is a test",
                    "fields" : [ "dc:description", "dc:title^3" ]
                  }
                }""");

        es = newQueryBuilder(Map.of("regex", new RegexOpenSearchHintQueryBuilder())).apply(
                "select * from Document where /*+ES: OPERATOR(regex) */ dc:title = 's.*y'");
        assertEqualsEvenUnderWindows("""
                {
                  "regexp" : {
                    "dc:title" : {
                      "value" : "s.*y",
                      "flags_value" : 255,
                      "max_determinized_states" : 10000,
                      "boost" : 1.0
                    }
                  }
                }""", es);

        es = newQueryBuilder(Map.of("fuzzy", new FuzzyOpenSearchHintQueryBuilder())).apply(
                "select * from Document where /*+ES: OPERATOR(fuzzy) */ dc:title = 'ki'");
        assertEqualsEvenUnderWindows("""
                {
                  "fuzzy" : {
                    "dc:title" : {
                      "value" : "ki",
                      "fuzziness" : "AUTO",
                      "prefix_length" : 0,
                      "max_expansions" : 50,
                      "transpositions" : true,
                      "boost" : 1.0
                    }
                  }
                }""", es);

        es = newQueryBuilder(Map.of("wildcard", new WildcardOpenSearchHintQueryBuilder())).apply(
                "select * from Document where /*+ES: OPERATOR(wildcard) */ dc:title = 'ki*y'");
        assertEqualsEvenUnderWindows("""
                {
                  "wildcard" : {
                    "dc:title" : {
                      "wildcard" : "ki*y",
                      "boost" : 1.0
                    }
                  }
                }""", es);

        es = newQueryBuilder(Map.of("simple_query_string", new SimpleQueryStringOpenSearchHintQueryBuilder())).apply(
                "select * from Document where /*+ES: OPERATOR(simple_query_string) */ dc:title = '\"fried eggs\" +(eggplant | potato) -frittata'");
        assertEqualsEvenUnderWindows("""
                {
                  "simple_query_string" : {
                    "query" : "\\"fried eggs\\" +(eggplant | potato) -frittata",
                    "fields" : [
                      "dc:title^1.0"
                    ],
                    "flags" : -1,
                    "default_operator" : "or",
                    "analyze_wildcard" : false,
                    "auto_generate_synonyms_phrase_query" : true,
                    "fuzzy_prefix_length" : 0,
                    "fuzzy_max_expansions" : 50,
                    "fuzzy_transpositions" : true,
                    "boost" : 1.0
                  }
                }""", es);

        es = newQueryBuilder(Map.of("query_string", new QueryStringOpenSearchHintQueryBuilder())).apply(
                "select * from Document where /*+ES: INDEX(dc:title,dc:description) ANALYZER(fulltext) OPERATOR(query_string) */ dc:title = 'this AND that OR thus'");
        // fields are not ordered
        assertEqualsEvenUnderWindows("""
                {
                  "query_string" : {
                    "query" : "this AND that OR thus",
                    "fields" : [
                      "dc:description^1.0",
                      "dc:title^1.0"
                    ],
                    "type" : "best_fields",
                    "default_operator" : "or",
                    "analyzer" : "fulltext",
                    "max_determinized_states" : 10000,
                    "enable_position_increments" : true,
                    "fuzziness" : "AUTO",
                    "fuzzy_prefix_length" : 0,
                    "fuzzy_max_expansions" : 50,
                    "phrase_slop" : 0,
                    "escape" : false,
                    "auto_generate_synonyms_phrase_query" : true,
                    "fuzzy_transpositions" : true,
                    "boost" : 1.0
                  }
                }""", es);

        es = newQueryBuilder(Map.of("common", new CommonOpenSearchHintQueryBuilder())).apply(
                "select * from Document where /*+ES: OPERATOR(common) */ dc:title = 'this is bonsai cool'");
        assertEqualsEvenUnderWindows("""
                {
                  "common" : {
                    "dc:title" : {
                      "query" : "this is bonsai cool",
                      "high_freq_operator" : "OR",
                      "low_freq_operator" : "OR",
                      "cutoff_frequency" : 0.01,
                      "boost" : 1.0
                    }
                  }
                }""", es);

        es = newQueryBuilder(Map.of("more_like_this", new MoreLikeThisOpenSearchHintQueryBuilder())).apply(
                "select * from Document where /*+ES: INDEX(dc:title.fulltext^2,dc:description.fulltext) OPERATOR(more_like_this) */ ecm:uuid = '1234'");
        assertEqualsEvenUnderWindows("""
                {
                  "more_like_this" : {
                    "fields" : [
                      "dc:title.fulltext",
                      "dc:description.fulltext"
                    ],
                    "like" : [
                      {
                        "_id" : "1234"
                      }
                    ],
                    "max_query_terms" : 12,
                    "min_term_freq" : 1,
                    "min_doc_freq" : 3,
                    "max_doc_freq" : 2147483647,
                    "min_word_length" : 0,
                    "max_word_length" : 0,
                    "minimum_should_match" : "30%",
                    "boost_terms" : 0.0,
                    "include" : false,
                    "fail_on_unsupported_field" : true,
                    "boost" : 1.0
                  }
                }""", es);

        es = newQueryBuilder(Map.of("more_like_this", new MoreLikeThisOpenSearchHintQueryBuilder())).apply(
                "select * from Document where /*+ES: INDEX(all_field) OPERATOR(more_like_this) */ ecm:uuid IN ('1234', '4567')");
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "more_like_this" : {
                        "fields" : [
                          "all_field"
                        ],
                        "like" : [
                          {
                            "_id" : "1234"
                          },
                          {
                            "_id" : "4567"
                          }
                        ],
                        "max_query_terms" : 12,
                        "min_term_freq" : 1,
                        "min_doc_freq" : 3,
                        "max_doc_freq" : 2147483647,
                        "min_word_length" : 0,
                        "max_word_length" : 0,
                        "minimum_should_match" : "30%",
                        "boost_terms" : 0.0,
                        "include" : false,
                        "fail_on_unsupported_field" : true,
                        "boost" : 1.0
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);

        es = newQueryBuilder(Map.of("more_like_this", new MoreLikeThisOpenSearchHintQueryBuilder())).apply(
                "select * from Document where /*+ES: OPERATOR(more_like_this) */ ecm:uuid IN ('1234', '4567')");
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "more_like_this" : {
                        "fields" : [
                          "ecm:uuid"
                        ],
                        "like" : [
                          {
                            "_id" : "1234"
                          },
                          {
                            "_id" : "4567"
                          }
                        ],
                        "max_query_terms" : 12,
                        "min_term_freq" : 1,
                        "min_doc_freq" : 3,
                        "max_doc_freq" : 2147483647,
                        "min_word_length" : 0,
                        "max_word_length" : 0,
                        "minimum_should_match" : "30%",
                        "boost_terms" : 0.0,
                        "include" : false,
                        "fail_on_unsupported_field" : true,
                        "boost" : 1.0
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);

        es = newQueryBuilder(Map.of("testTermQuery", new TestTermOpenSearchHintQueryBuilder())).apply(
                "select * from Document where /*+ES: OPERATOR(testTermQuery) */ ecm:uuid = '1234'");
        assertEqualsEvenUnderWindows("""
                {
                  "term" : {
                    "ecm:uuid" : {
                      "value" : "1234",
                      "boost" : 1.0
                    }
                  }
                }""", es);

        es = newQueryBuilder(Map.of("testBoolQuery", new TestBoolQueryOpenSearchHintQueryBuilder())).apply(
                "select * from Document where /*+ES: OPERATOR(testBoolQuery) */ ecm:uuid = '1234'");
        assertEqualsEvenUnderWindows("""
                {
                  "bool" : {
                    "must" : [
                      {
                        "fuzzy" : {
                          "ecm:uuid" : {
                            "value" : "1234",
                            "fuzziness" : "AUTO",
                            "prefix_length" : 0,
                            "max_expansions" : 50,
                            "transpositions" : true,
                            "boost" : 1.0
                          }
                        }
                      },
                      {
                        "wildcard" : {
                          "ecm:uuid" : {
                            "wildcard" : "1234",
                            "boost" : 1.0
                          }
                        }
                      }
                    ],
                    "adjust_pure_negative" : true,
                    "boost" : 1.0
                  }
                }""", es);

        es = newQueryBuilder(Map.of("nestedFilesQuery", new NestedFilesOpenSearchHintQueryBuilder())).apply(
                "SELECT * FROM Document WHERE /*+ES: INDEX(files:files.file.encoding, files:files.file.digest) OPERATOR(nestedFilesQuery) */ nested:value IN ('myEncoding', 'anyDigest')");
        assertEquals("""
                {
                  "constant_score" : {
                    "filter" : {
                      "nested" : {
                        "query" : {
                          "bool" : {
                            "must" : [
                              {
                                "term" : {
                                  "files:files.file.encoding" : {
                                    "value" : "myEncoding",
                                    "boost" : 1.0
                                  }
                                }
                              },
                              {
                                "term" : {
                                  "files:files.file.digest" : {
                                    "value" : "anyDigest",
                                    "boost" : 1.0
                                  }
                                }
                              }
                            ],
                            "adjust_pure_negative" : true,
                            "boost" : 1.0
                          }
                        },
                        "path" : "files:files.file",
                        "ignore_unmapped" : false,
                        "score_mode" : "none",
                        "boost" : 1.0
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);
    }

    @Test
    public void testConvertHintLike() {
        String es = BUILDER.apply(
                "select * from Document where /*+ES: INDEX(some:field) ANALYZER(my_analyzer) */ dc:subjects LIKE 'foo*'");
        assertEqualsEvenUnderWindows("""
                {
                  "match_phrase_prefix" : {
                    "some:field" : {
                      "query" : "foo",
                      "analyzer" : "my_analyzer",
                      "slop" : 0,
                      "max_expansions" : 50,
                      "zero_terms_query" : "NONE",
                      "boost" : 1.0
                    }
                  }
                }""", es);

        es = BUILDER.apply("select * from Document where /*+ES: INDEX(some:field) */ dc:subjects LIKE '%foo%'");
        assertEqualsEvenUnderWindows("""
                {
                  "wildcard" : {
                    "some:field" : {
                      "wildcard" : "*foo*",
                      "boost" : 1.0
                    }
                  }
                }""", es);

    }

    @Test
    public void testConvertHintFulltext() {
        // search on title and description, boost title
        String es = BUILDER.apply(
                "select * from Document where /*+ES: INDEX(dc:title.fulltext^4,dc:description.fulltext) */ ecm:fulltext = 'foo'");
        // fields are not ordered
        assertIn(es, """
                {
                  "simple_query_string" : {
                    "query" : "foo",
                    "fields" : [
                      "dc:description.fulltext^1.0",
                      "dc:title.fulltext^4.0"
                    ],
                    "analyzer" : "fulltext",
                    "flags" : -1,
                    "default_operator" : "and",
                    "analyze_wildcard" : false,
                    "auto_generate_synonyms_phrase_query" : true,
                    "fuzzy_prefix_length" : 0,
                    "fuzzy_max_expansions" : 50,
                    "fuzzy_transpositions" : true,
                    "boost" : 1.0
                  }
                }""", """
                {
                  "simple_query_string" : {
                    "query" : "foo",
                    "fields" : [ "dc:description.fulltext", "dc:title.fulltext^3" ],
                    "analyzer" : "fulltext",
                    "default_operator" : "and"
                  }
                }""");
    }

    protected void assertEqualsEvenUnderWindows(String expected, String actual) {
        expected = normalizeString(expected);
        actual = normalizeString(actual);
        assertEquals(expected, actual);
    }

    private String normalizeString(String str) {
        if (SystemUtils.IS_OS_WINDOWS) {
            str = str.trim();
            str = str.replace("\n", "");
            str = str.replace("\r", "");
        }
        return str;
    }

    protected void assertIn(String actual, String... expected) {
        actual = normalizeString(actual);
        for (String exp : expected) {
            exp = normalizeString(exp);
            if (exp.equals(actual)) {
                return;
            }
        }
        // fail
        assertEquals(expected[0], actual);
    }

    @Test
    public void testConvertHintGeo() {
        var geoBuilder = newQueryBuilder(Map.of("geo_bounding_box", new GeoBoundingBoxOpenSearchHintQueryBuilder(), //
                "geo_distance", new GeoDistanceOpenSearchHintQueryBuilder(), //
                "geo_shape", new GeoShapeOpenSearchHintQueryBuilder() //
        ));
        String es = geoBuilder.apply(
                "select * from Document where /*+ES: OPERATOR(geo_bounding_box) */ osm:location IN ('40.73, -74.1', '40.81, -71.12')");
        String response = """
                {
                  "constant_score" : {
                    "filter" : {
                      "geo_bounding_box" : {
                        "osm:location" : {
                          "top_left" : [
                            -74.1,
                            40.81
                          ],
                          "bottom_right" : [
                            -71.12,
                            40.73
                          ]
                        },
                        "validation_method" : "STRICT",
                        "type" : "MEMORY",
                        "ignore_unmapped" : false,
                        "boost" : 1.0
                      }
                    },
                    "boost" : 1.0
                  }
                }""";
        assertEqualsEvenUnderWindows(response, es);
        es = geoBuilder.apply(
                "select * from Document where /*+ES: OPERATOR(geo_bounding_box) */ osm:location IN ('drj7tee', 'dr5r9y')");
        // we cannot do this because lat and lon are not rounded to match the input
        // assertTruEqualsEvenUnderWindows(response, es);
        assertTrue(es.contains("geo_bounding_box"));
        assertTrue(es, es.contains("bottom_right"));

        es = geoBuilder.apply("select * from Document where /*+ES: OPERATOR(geo_distance) */ "
                + "osm:location IN ('40.73, -74.1', '20km')");
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "geo_distance" : {
                        "osm:location" : [
                          -74.1,
                          40.73
                        ],
                        "distance" : 20000.0,
                        "distance_type" : "arc",
                        "validation_method" : "STRICT",
                        "ignore_unmapped" : false,
                        "boost" : 1.0
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);

        es = geoBuilder.apply("select * from Document where /*+ES: OPERATOR(geo_shape) */"
                + "osm:location IN ('FRA', 'type-unused', 'shapes', 'location')");
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "geo_shape" : {
                        "osm:location" : {
                          "indexed_shape" : {
                            "id" : "FRA",
                            "index" : "shapes",
                            "path" : "location"
                          },
                          "relation" : "within"
                        },
                        "ignore_unmapped" : false,
                        "boost" : 1.0
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);

    }

    @Test
    @Deploy("org.nuxeo.ecm.core.search.client.opensearch1.test:OSGI-INF/opensearch-search-client-max-expansions-test-contrib.xml")
    public void testMatchPhrasePrefixWithCustomMaxExpansions() {
        String es = BUILDER.apply("select * from Document where f1 LIKE 'foo%'");
        final String expected = """
                {
                  "match_phrase_prefix" : {
                    "f1" : {
                      "query" : "foo",
                      "slop" : 0,
                      "max_expansions" : 200,
                      "zero_terms_query" : "NONE",
                      "boost" : 1.0
                    }
                  }
                }""";
        assertEqualsEvenUnderWindows(expected, es);
        es = newQueryBuilder(Map.of("match_phrase_prefix", new MatchPhrasePrefixOpenSearchHintQueryBuilder())).apply(
                "SELECT * FROM Document WHERE /*+ES: INDEX(f1) OPERATOR(match_phrase_prefix) */ ecm:fulltext.dc:title LIKE 'foo'");
        assertEqualsEvenUnderWindows(expected, es);
    }

    @Test
    public void shouldFailWhenESHintOperatorIsUnknown() {
        var uso = assertThrows(QueryParseException.class, () -> BUILDER.apply(
                "select * from Document where /*+ES: OPERATOR(unExitingHint) */ ecm:uuid = '1234'"));
        assertEquals("Operator: unExitingHint is unknown", uso.getMessage());
    }

    @Test
    @WithFrameworkProperty(name = RELATION_INDEXING_ENABLED_PROP, value = "true")
    public void testConverterSelectWithRelationEnabled() {
        // When relation indexing is enabled, "FROM Document, Relation" means all doc types (match_all)
        String es = BUILDER.apply("select * from Document, Relation");
        assertEqualsEvenUnderWindows("""
                {
                  "match_all" : {
                    "boost" : 1.0
                  }
                }""", es);
        // "FROM Document" excludes Relation documents by adding a type filter
        es = BUILDER.apply("select * from Document");
        assertEqualsEvenUnderWindows("""
                {
                  "bool" : {
                    "must" : [
                      {
                        "match_all" : {
                          "boost" : 1.0
                        }
                      }
                    ],
                    "filter" : [
                      {
                        "terms" : {
                          "ecm:primaryType" : [
                            "CommonDocument",
                            "OpenSearchDocument",
                            "Document"
                          ],
                          "boost" : 1.0
                        }
                      }
                    ],
                    "adjust_pure_negative" : true,
                    "boost" : 1.0
                  }
                }""", es);
        // An empty query or null query should produce "FROM Document, Relation" via the flag and match_all
        es = BUILDER.apply(null);
        assertEqualsEvenUnderWindows("""
                {
                  "match_all" : {
                    "boost" : 1.0
                  }
                }""", es);
        es = BUILDER.apply("");
        assertEqualsEvenUnderWindows("""
                {
                  "match_all" : {
                    "boost" : 1.0
                  }
                }""", es);
        // A where clause without select should also produce match_all (from Document, Relation via the flag)
        es = BUILDER.apply("f1=1");
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "term" : {
                        "f1" : {
                          "value" : "1",
                          "boost" : 1.0
                        }
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);
    }

    protected static Function<String, String> newQueryBuilder(Map<String, OpenSearchHintQueryBuilder> hintBuilders) {
        return SEARCH_QUERY_BUILDER.andThen(new OpenSearchQueryTransformer(Map.of("enhanced", "nxutest"), hintBuilders))
                                   .andThen(SearchRequest::source)
                                   .andThen(SearchSourceBuilder::query)
                                   .andThen(Objects::toString);
    }
}
